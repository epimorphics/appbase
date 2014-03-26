/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 * Modified by Dave Reynolds. Modifications (c) Copyright 2012, Epimorphics Limited
 */

package com.epimorphics.appbase.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans a directory to detect any files that have been added, changed or removed.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Scanner {
    static Logger log = LoggerFactory.getLogger( Scanner.class );
    
    protected int fingerprintLength = 1000;  
    protected final File directory;
    protected final FilenameFilter filter;

    public enum FileState { NEW, DELETED, MODIFIED };
    
    public class FileRecord {
        public File file;
        public FileState state;
        
        public FileRecord(File file, FileState state) {
            this.file = file;
            this.state = state;
        }
    }

    /**
     * Set the length of the file head which will be included in the
     * change detection fingerprint. Set to zero to only fingerpint based
     * on file metadata (datestamp, length)
     */
    public void setFingerprintLength(int length) {
        fingerprintLength = length;
    }

    // Store checksums of files or directories
    Map<File, Long> lastChecksums = new HashMap<File, Long>();
    Map<File, Long> storedChecksums = new HashMap<File, Long>();

    /**
     * Create a scanner for the specified directory
     *
     * @param directory the directory to scan
     */
    public Scanner(File directory)
    {
        this(directory, null);
    }

    /**
     * Create a scanner for the specified directory and file filter
     *
     * @param directory the directory to scan
     * @param filter a filter for file names
     */
    public Scanner(File directory, FilenameFilter filter)
    {
        this.directory = canon(directory);
        this.filter = filter;
    }

    /**
     * Initialize the list of known files.
     * This should be called before the first scan to initialize
     * the list of known files.  The purpose is to be able to detect
     * files that have been deleted while the scanner was inactive.
     *
     * @param checksums a map of checksums
     */
    public void initialize(Map<File, Long> checksums)
    {
        storedChecksums.putAll(checksums);
    }

    /**
     * Report a set of new, modified or deleted files.
     * Modifications are checked against a computed checksum on some file
     * attributes to detect any modification.
     * Upon restart, such checksums are not known so that all files will
     * be reported as modified. 
     *
     * @param reportImmediately report all files immediately without waiting for the checksum to be stable
     * @return a list of changes on the files included in the directory
     */
    public Set<FileRecord> scan(boolean reportImmediately)  {
        log.debug("Scanning " + directory);
        Set<File> removed = new HashSet<File>(storedChecksums.keySet());
        
        Set<FileRecord> results = scanDir(directory, removed, reportImmediately);
        for (Iterator<File> it = removed.iterator(); it.hasNext();)
        {
            File file = (File) it.next();
            // Make sure we'll handle a file that has been deleted
            results.add( new FileRecord(file, FileState.DELETED) );
            // Remove no longer used checksums
            lastChecksums.remove(file);
            storedChecksums.remove(file);
        }

        if (!results.isEmpty())
            log.debug(" ... found " + results.size() + " changes");
        return results;
    }
    
    private Set<FileRecord> scanDir(File dir, Set<File> removed, boolean reportImmediately) {
        Set<FileRecord> results = new HashSet<Scanner.FileRecord>();
        
        File[] list = dir.listFiles(filter);
        if (list == null) {
            return results;
        }

        
        for (int i = 0; i < list.length; i++) {
            File file  = list[i];
            if (file.isDirectory()) {
                results.addAll( scanDir(file, removed, reportImmediately) );
            } else {
                long lastChecksum = lastChecksums.get(file) != null ? ((Long) lastChecksums.get(file)).longValue() : 0;
                long storedChecksum = storedChecksums.get(file) != null ? ((Long) storedChecksums.get(file)).longValue() : 0;
                long newChecksum = checksum(file);
                lastChecksums.put(file, new Long(newChecksum));
                // Only handle file when it does not change anymore and it has changed since last reported
                if ((newChecksum == lastChecksum || reportImmediately) && newChecksum != storedChecksum)
                {
                    storedChecksums.put(file, new Long(newChecksum));
                    results.add( new FileRecord(file, (storedChecksum == 0) ? FileState.NEW : FileState.MODIFIED) );
                }
                removed.remove(file);
            }
        }
 
        return results;
    }

    private static File canon(File file)
    {
        try
        {
            return file.getCanonicalFile();
        }
        catch (IOException e)
        {
            return file;
        }
    }

    /**
     * Retrieve the previously computed checksum for a give file.
     *
     * @param file the file to retrieve the checksum
     * @return the checksum
     */
    public long getChecksum(File file)
    {
        Long c = storedChecksums.get(file);
        return c != null ? c.longValue() : 0;
    }

    /**
     * Compute a cheksum for the file or directory that consists of the name, length and the last modified date
     * for a file and its children in case of a directory
     *
     * @param file the file or directory
     * @return a checksum identifying any change
     */
    private long checksum(File file)
    {
        CRC32 crc = new CRC32();
        checksum(file, crc);
        return crc.getValue();
    }

    private void checksum(File file, CRC32 crc)
    {
        crc.update(file.getName().getBytes());
        if (file.isFile())
        {
            checksum(file.lastModified(), crc);
            checksum(file.length(), crc);
            // File time checksum not robust for sub-second changes, also finger print start of file
            if (fingerprintLength > 0 && file.canRead()){
                try {
                    InputStream is = new FileInputStream(file);
                    // TODO split into block reads so that very large fingerprintLengths don't blow memory
                    byte[] buffer = new byte[fingerprintLength];
                    int len = is.read(buffer, 0, fingerprintLength);
                    crc.update(buffer, 0, len);
                    is.close();
                } catch (IOException e) {
                    // Ignore 
                }
            }
        }
    }

    private void checksum(long l, CRC32 crc)
    {
        for (int i = 0; i < 8; i++)
        {
            crc.update((int) (l & 0x000000ff));
            l >>= 8;
        }
    }

}
