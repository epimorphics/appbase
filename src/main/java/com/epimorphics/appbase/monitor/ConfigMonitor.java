/******************************************************************
 * File:        ConfigMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  19 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.monitor.Scanner.FileRecord;

/**
 * Monitors a directory tree containing configuration information. 
 * For each file it instantiates a configurable object. If the file changes
 * the object will be re-instantiated. This is an abstract class and subclasses
 * should supply the method which does the instantiation. It should be
 * configured as a component in the application config.
 * <p>
 * If run in production mode then the configuration directory is only consulted
 * once the first time any access method is used or by an explicit refresh().
 * After that only an explicit refresh() will cause a directory scan.
 * </p><p>
 * In development mode then by default the directory is scanned everyone 
 * 2 seconds to check for changes, this is configurable.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// Note: It might be useful to have a mode where the check is only done if you
// when an access method is used more than 2 seconds after the last check. 
// Has the advantage that in a system where you aren't using the monitor work isn't
// being done. As the disadvantage that you take the scan hit at exactly the time you
// want a result. No support for this at present.

public abstract class ConfigMonitor<T extends ConfigInstance> extends ComponentBase implements Runnable, Startup {
    static Logger log = LoggerFactory.getLogger(ConfigMonitor.class);
    
    protected boolean initialized = false;
    protected boolean productionMode = false;
    protected boolean waitForStable = false;
    protected int scanInterval = 2 * 1000;  // 2 seconds
    protected Scanner scanner;
    protected int fingerprintLength = 0;
    protected ScheduledFuture<?> scanTask;
    
    protected Map<File, T> entries = new HashMap<>();
    protected Map<String, T> entryIndex = new HashMap<>();

    /**
     * Set production mode on/off. In production mode then the directory
     * will only be scanned once at first access and then only when explicitly refreshed
     * @param mode
     */
    public void setProductionMode(boolean mode) {
        productionMode = mode;
    }
    
    /**
     * Set the directory to be monitored
     */
    public void setDirectory(String dir) {
        File scanDir = asFile(dir);
        scanner = new Scanner(scanDir);
        scanner.setFingerprintLength(fingerprintLength);
    }
    
    /**
     * Set the number of milliseconds between monitor scans.
     * The default is 2 seconds. 
     * Not relevant in production mode.
     */
    public void setScanInterval(int intervalInMS) {
        scanInterval = intervalInMS;
        if (initialized && !productionMode) {
            stopScanning();
            startScanning();
        }
    }

    /**
     * A scan may detect a change in a file while the change is still happening.
     * This can result in attempting to configure from an incomplete file. 
     * For development use is this rarely a problem since the next scan will
     * will find the file has changed again and update the config.
     * If this is a problem, e.g. live scanning is being used in a production setting,
     * then set the waitForStable flag to true (default is false).
     */
    public void setWaitForStable(boolean wait) {
        waitForStable = wait;
    }
    
    /**
     * Set a length limit for how much a config file will be sampled as part of checking
     * for changes. Set to 0 to just use length and modification times for change detection.
     * Default is 0.
     */
    public void setFileSampleLength(long len) {
        fingerprintLength = (int)len;
        if (scanner != null) {
            scanner.setFingerprintLength(fingerprintLength);
        }
    }
    
    /**
     * Return a collection of all currently configured objects
     */
    public synchronized Collection<T> getEntries() {
        init();
        return entries.values();
    }
    
    /**
     * Return the current version of the given configured object
     */
    public synchronized T get(String name) {
        init();
        return entryIndex.get(name);
    }
    
    /**
     * Force a manual scan of the directory
     */
    public void refresh() {
        doScan(true);
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        init();
    }
    
    /**
     * Scan the directory if it has not already been scanned
     */
    protected void init() {
        if (!initialized) {
            refresh();
            if (!productionMode) startScanning();
            initialized = true;
        }
    }
    
    protected void startScanning() {
        scanTask = TimerManager.get().scheduleAtFixedRate(this, scanInterval, scanInterval, TimeUnit.MILLISECONDS);
    }
    
    protected void stopScanning() {
        if (scanTask != null) {
            scanTask.cancel(false);
            scanTask = null;
        }
    }
    
    public void run() {
        doScan(waitForStable);
    }
    
    protected abstract T configure(File file);
    
    private synchronized void doScan(boolean returnImmediately) {
        Set<FileRecord> changes = scanner.scan(returnImmediately);
        synchronized (this) {
            for ( Scanner.FileRecord change : changes ) {
                File file = change.file;
                switch(change.state) {
                case NEW:
                    addEntry(file, configure(file) );
                    break;
                    
                case MODIFIED:
                    T entry = configure(file);
                    removeEntry( file );
                    addEntry(file, entry);
                    break;
                    
                case DELETED:
                    removeEntry( file );
                    break;
                }
            }
        }
    }
    
    // Assumes in synchronized block
    private void addEntry(File file, T entry) {
        if (entry != null) {
            log.info("Adding monitored entry for: " + file);
            String name = entry.getName();
            if (name != null) {
                entryIndex.put(name, entry);
            }
            entries.put(file, entry);
        }
    }
    
    // Assumes in synchronized block
    private void removeEntry(File file) {
        log.info("Removing monitored entry for: " + file);
        T entry = entries.get(file);
        if (entry != null) {
            String name = entry.getName();
            if (name != null) {
                entryIndex.remove(name);
            }
            entries.remove(file);
        }
    }
    
}
