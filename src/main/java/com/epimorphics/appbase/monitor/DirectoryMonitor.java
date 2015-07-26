/******************************************************************
 * File:        FileMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  25 Jul 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.monitor.FileRecord.FileState;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.util.EpiException;

/**
 * Monitor a set of directories for new files arriving and fires an action
 * on each as it becomes stable. Each directory can have an associated
 * set of configuration parameters (expressed as a json object) which is 
 * passed to the triggered action. 
 */
public abstract class DirectoryMonitor implements FileRecord.Process {
    public static final String DIRECTORY_PARAM = "dir";
    public static final String WAIT_TIME_PARAM = "waitTime";
    
    public static final int DEFAULT_WAIT_TIME = 3;
    
    protected Map<String, JsonObject> watchedDirectories = new HashMap<>();
    protected Map<File, FileTracker> fileTrackers = new HashMap<>();
    
    /**
     * Operation called on each new file as it becomes stable. 
     */
    protected abstract void action(File file, JsonObject parameters);

    protected void action(File file) {
        action(file, getParameters(file));
    }
    
    protected synchronized JsonObject getParameters(File file) {
        String dir = file.getParentFile().getPath();
        return watchedDirectories.get(dir);
    }
    
    /**
     * Add a directory to watch.
     */
    public synchronized void addWatch(String dir, JsonObject params) {
        try {
            File fdir = new File(dir);
            watchedDirectories.put( fdir.getCanonicalPath(), params);
            ConfigWatcher.watch(fdir, this);
        } catch (IOException e) {
            throw new EpiException("Monitor cannot access watched directory: " + dir, e);
        }
    }
    
    public synchronized void process(FileRecord record) {
        File file = record.getFile();
        if (record.getState() == FileState.NEW) {
            startTracker(file);
        } else if (record.getState() == FileState.DELETED) {
            FileTracker tracker = fileTrackers.get(file);
            if (tracker != null) {
                tracker.interrupt();
                clearTracker(file);
            }
        } else if (record.getState() == FileState.MODIFIED) {
            FileTracker tracker = fileTrackers.get(file);
            if (tracker != null) {
                tracker.fileChanged();
            } else {
                startTracker(file);
            }
        }
    }
    
    protected synchronized void startTracker(File file) {
        int waitTime = JsonUtil.getIntValue( getParameters(file), WAIT_TIME_PARAM, DEFAULT_WAIT_TIME);
        FileTracker tracker = new FileTracker(file, waitTime);
        fileTrackers.put(file, tracker);
        tracker.start();
    }
    
    protected synchronized void clearTracker(File file) {
        fileTrackers.put(file, null);
    }
    
    public class FileTracker extends Thread {
        protected File file;
        protected long waitTimeMS;
        protected long lastChecksum;
        protected long lastChecktime;
        
        public FileTracker(File file, int waitTime) {
            this.file = file;
            this.waitTimeMS = waitTime * 1000;
            this.setDaemon(true);
        }
        
        public synchronized void fileChanged() {
            lastChecksum = checksum(file);
            lastChecktime = System.currentTimeMillis();
        }
        
        @Override
        public void run() {
            fileChanged();
            while (!isStable()) {
                try {
                    long wait = lastChecktime + waitTimeMS - System.currentTimeMillis() + 1;
                    sleep( wait );
                } catch (InterruptedException e) {
                    return;
                }
            }
            action(file);
            clearTracker(file);
        }
        
        private synchronized boolean isStable() {
            return (checksum(file) == lastChecksum) && ( System.currentTimeMillis() - lastChecktime >= waitTimeMS );
        }
    }
    
    /**
     * Compute a checksum for the file or directory that consists of the name, length and the last modified date
     * for a file.
     */
    public static long checksum(File file)
    {
        CRC32 crc = new CRC32();
        checksum(file, crc);
        return crc.getValue();
    }

    private static void checksum(File file, CRC32 crc)
    {
        crc.update(file.getName().getBytes());
        checksum(file.lastModified(), crc);
        checksum(file.length(), crc);
    }

    private static void checksum(long l, CRC32 crc)
    {
        for (int i = 0; i < 8; i++)
        {
            crc.update((int) (l & 0x000000ff));
            l >>= 8;
        }
    }
}
