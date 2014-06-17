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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.util.OneToManyMap;

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
 * In development mode then by default the directory is scanned every 
 * 2 seconds to check for changes, this is configurable.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// Note: It might be useful to have a mode where the check is only done 
// when an access method is used more than 2 seconds after the last check. 
// Has the advantage that in a system where you aren't using the monitor work isn't
// being done. Has the disadvantage that you take the scan hit at exactly the time you
// want a result. No support for this at present.

public abstract class ConfigMonitor<T extends ConfigInstance> extends ComponentBase implements Runnable, Startup {
    static Logger log = LoggerFactory.getLogger(ConfigMonitor.class);
    
    protected boolean initialized = false;
    protected boolean productionMode = false;
    protected boolean waitForStable = true;
    protected int scanInterval = 2 * 1000;  // 2 seconds
    protected Scanner scanner;
    protected int fingerprintLength = 0;
    protected ScheduledFuture<?> scanTask;
    protected File scanDir;
    
    protected OneToManyMap<File, T> entries = new OneToManyMap<>();
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
        scanDir = asFile(dir);
        scanner = new Scanner(scanDir);
        scanner.setFingerprintLength(fingerprintLength);
    }
    
    /**
     * Set the number of milliseconds between monitor scans.
     * The default is 2 seconds. 
     * Not relevant in production mode.
     */
    public void setScanInterval(long intervalInMS) {
        scanInterval = (int) intervalInMS;
        if (initialized && !productionMode) {
            stopScanning();
            startScanning();
        }
    }

    /**
     * A scan may detect a change in a file while the change is still happening.
     * This can result in attempting to configure from an incomplete file. 
     * To avoid this then by default the scanner waits until two scans have given
     * a consistent checksum, which means that typically the latency is twice the scan interval.
     * Turn of this check by setting waitForStable to false.
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
     * Test whether a configured object of this name exists in the monitor
     */
    public synchronized boolean has(String name) {
        init();
        return entryIndex.containsKey(name);
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
            if (scanner == null) {
                log.warn("No directory scanner, failed to set directory to monitor?");
                // Don't raise exception because this is a valid set up for testing
            } else {
                refresh();
                if (!productionMode) startScanning();
            }
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
        doScan(!waitForStable);
    }
    
    protected abstract Collection<T> configure(File file);
    
    private synchronized void doScan(boolean returnImmediately) {
        Set<FileRecord> changes = scanner.scan(returnImmediately);
        synchronized (this) {
            for ( Scanner.FileRecord change : changes ) {
                try {
                    File file = change.file;
                    switch(change.state) {
                    case NEW:
                        addEntry(file, configure(file) );
                        break;
                        
                    case MODIFIED:
                        Collection<T> entrylist = configure(file);
                        removeEntry( file );
                        addEntry(file, entrylist);
                        break;
                        
                    case DELETED:
                        removeEntry( file );
                        break;
                    }
                } catch (Exception e) {
                    // Absorb problems here and log otherwise the scanner can be left part configured
                    log.error("Problem loading scanned changes", e);
                }
            }
        }
    }
    
    /**
     * Manually register an an instance, bypassing the loading from file.
     * Uses the name of the instance as the implicit filename but the file needed not
     * (and should not) exist. 
     */
    public synchronized void register(T item) {
        addEntry( new File(item.getName()), Collections.singleton(item));
    }

    /**
     * Remove a manually registered instance.
     */
    public synchronized void unregister(T item) {
        removeEntry( new File(item.getName()) );
    }
    
    // Assumes in synchronized block
    private void addEntry(File file, Collection<T> entrylist) {
        for (T entry : entrylist) {
            log.info("Adding monitored entry " + entry.getName() + " from: " + file);
            doAddEntry(entry);
            entries.put(file, entry);
        }
    }
    
    // Assumes in synchronized block
    private void removeEntry(File file) {
        log.info("Removing monitored entry/entries for: " + file);
        for (Iterator<T> i = entries.getAll(file); i.hasNext();) {
            T entry = i.next();
            if (entry != null) {
                doRemoveEntry(entry);
                entries.remove(file);
            }
        }
    }
    
    protected void doAddEntry(T entry) {
        String name = entry.getName();
        if (name != null) {
            entryIndex.put(name, entry);
        }
    }

    protected void doRemoveEntry(T entry) {
        String name = entry.getName();
        if (name != null) {
            entryIndex.remove(name);
        }
    }
}
