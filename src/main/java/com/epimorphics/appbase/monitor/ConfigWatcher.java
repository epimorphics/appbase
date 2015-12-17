/******************************************************************
 * File:        ConfigWatcher.java
 * Created by:  Dave Reynolds
 * Created on:  22 Dec 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.monitor.FileRecord.FileState;
import com.epimorphics.util.EpiException;

/**
 * Replacement for scanner-based monitoring of configuration
 * files using the more efficient directory watcher. Supports
 * multiple ConfigMonitors (or other processors).
 */
public class ConfigWatcher {
    static Logger log = LoggerFactory.getLogger( ConfigWatcher.class );
    
    protected static ConfigWatcher theWatcher = new ConfigWatcher();
    protected static WatchWorker   theWorker;
            
    protected static class WatchRecord {
        protected WatchKey key;
        protected Path path;
        protected FileRecord.Process processor;
        
        public WatchRecord(WatchKey key, Path path, FileRecord.Process processor) {
            this.key = key;
            this.path = path;
            this.processor = processor;
        }
        
        public File getFile() {
            return path.toFile();
        }
        
        public void process(@SuppressWarnings("rawtypes") WatchEvent.Kind kind, File file) {
            FileRecord record = null;
            if (kind == ENTRY_CREATE) {
                record = new FileRecord(file, FileState.NEW);
            } else if (kind == ENTRY_DELETE) {
                record = new FileRecord(file, FileState.DELETED);
            } else if (kind == ENTRY_MODIFY) {
                record = new FileRecord(file, FileState.MODIFIED);
            }
            if (record != null) {
                processor.process(record);
            } else {
                log.error("Overflow in file event handling");
            }
        }
    }
    
    
    protected final WatchService watcher;
    protected final Map<WatchKey,WatchRecord> keys;
    
    public ConfigWatcher() {
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new EpiException("Failed to create watch service", e);
        }
        this.keys = new HashMap<WatchKey,WatchRecord>();
    }
    
    protected void addWatch(File directory, final FileRecord.Process processor) throws IOException {
        addWatch(directory.toPath(), processor);
    }
    
    protected void addWatch(Path directory, final FileRecord.Process processor) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                register(dir, processor);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                FileRecord record = new FileRecord(file.toFile(), FileState.NEW);
                processor.process(record);
                return FileVisitResult.CONTINUE;
            }
        });
    }

   private synchronized void register(Path dir, FileRecord.Process processor) throws IOException {
       WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
       keys.put(key, new WatchRecord(key, dir, processor));
   }

   protected synchronized void startWatching() {
       if (theWorker == null) {
           theWorker = new WatchWorker();
           theWorker.start();
       }
   }
   
   protected synchronized void stopWatching() {
       if (theWorker != null) {
           theWorker.interrupt();
           theWorker = null;
       }
   }
   
   /**
    * Register a directory tree to be watched
    */
   public static void watch(File directory, final FileRecord.Process processor) throws IOException {
       theWatcher.addWatch(directory, processor);
   }

   /**
    * Start the shared watcher service running if it is not already running
    */
   public static void start() {
       theWatcher.startWatching();
   }
   
   /**
    * Interrupt (and hopefully stop) the shared watcher service. Does not wait for termination
    */
   public static void stop() {
       theWatcher.stopWatching();
   }
   
   public class WatchWorker extends Thread {
       public WatchWorker() {
           setDaemon(true);
       }
       
       @SuppressWarnings({ "rawtypes", "unchecked" })
       public void run() {
           log.info("Directory watcher started");
           for (;;) {
               
               // wait for key to be signalled
               WatchKey key;
               try {
                   key = watcher.take();
               } catch (InterruptedException x) {
                   return;
               }
               
               WatchRecord record = keys.get(key);
               if (record == null) {
                   log.error("Directory event on directory that isn't recognized");
               } else {
                   for (WatchEvent<?> event: key.pollEvents()) {
                       WatchEvent.Kind kind = event.kind();
                       
                       WatchEvent<Path> ev = (WatchEvent<Path>)(event);
                       Path name = ev.context();
                       Path child = record.path.resolve(name);

                       if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                           if (kind == ENTRY_CREATE) {
                               try {
                                   addWatch(child, record.processor);
                               } catch (IOException e) {
                                   log.error("IO error which registering a new directory", e);
                               }
                           }
                       } else {
                           record.process(kind, child.toFile());
                       }
                   }
               }
    
               // reset key and remove from set if directory no longer accessible
               boolean valid = key.reset();
               if (!valid) {
                   keys.remove(key);
    
                   // all directories are inaccessible
                   if (keys.isEmpty()) {
                       // All directories are inaccessible so could exit but would need to clean up to allow future restart 
                   }
               }
           }           
       }
   }
   
}
