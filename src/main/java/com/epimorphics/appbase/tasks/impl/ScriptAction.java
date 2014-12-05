/******************************************************************
 * File:        ScriptAction.java
 * Created by:  Dave Reynolds
 * Created on:  8 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import static com.epimorphics.json.JsonUtil.fromJson;
import static com.epimorphics.json.JsonUtil.getStringValue;
import static com.epimorphics.json.JsonUtil.merge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

public class ScriptAction extends BaseAction implements Action  {
    public static final String ACTION_TYPE = "script";
    public static final String SCRIPT_PARAM = "@script";
    public static final String ENV_PARAM = "@env";
    public static final String DEFAULT_SHELL = "/bin/bash";
    public static final String RESULT = "result";

    public enum ArgType { json, jsonRef, inline };
    
    protected File scriptDir;
    protected ArgType argType = ArgType.json;
    protected List<String> argMap;
    
    public void setArgType(ArgType type) {
        argType = type;
    }
    
    public void setArgMap(List<String> args) {
        argMap = args;
        argType = ArgType.inline;
    }
    
    @Override
    public void resolve(ActionManager am) {
        scriptDir = new File( am.getScriptDir() );
    }
    
    @Override
    public JsonObject doRun(JsonObject parameters, ProgressMonitorReporter monitor) {
        JsonObject conf = merge(configuration, parameters);
        
        String script = getStringValue(conf, SCRIPT_PARAM);
        if (script == null) {
            throw new EpiException("ScriptAction missing a script argument");
        }
        File scriptF = new File(scriptDir, script);
        if ( !scriptF.canExecute() ) {
            throw new EpiException("Script file not executable: " + scriptF.getPath());
        }
        
        monitor.report("Running script: " + script);

        File argFile = null;
        try {
            ProcessBuilder scriptPB = null;
            switch (argType) {
            case json:
                scriptPB = new ProcessBuilder(DEFAULT_SHELL, script, conf.toString());
                break;
            case inline:
                if (argMap == null || argMap.isEmpty()) {
                    scriptPB = new ProcessBuilder(DEFAULT_SHELL, script);
                } else {
                    String[] args = new String[2 + argMap.size()];
                    args[0] = DEFAULT_SHELL;
                    args[1] = script;
                    for (int i = 0; i < argMap.size(); i++) {
                        String argname = argMap.get(i);
                        JsonValue arg = conf.get( argname );
                        if (arg == null) {
                            monitor.reportError(String.format("Failed to find value for argument %s in script %s, aborting", argname, script));
                            return JsonUtil.emptyObject();
                        }
                        args[2+i] = fromJson( arg ).toString();
                    }
                    scriptPB = new ProcessBuilder(args);
                }
                break;
            case jsonRef:
                argFile = File.createTempFile("script-argument", ".json");
                FileOutputStream output = new FileOutputStream(argFile);
                JSON.write(output, conf);
                output.close();
                scriptPB = new ProcessBuilder(DEFAULT_SHELL, script, argFile.getPath());
                break;
            }

            scriptPB.redirectErrorStream(false);
            scriptPB.directory(scriptDir);
            
            JsonValue envSpec = configuration.get(ENV_PARAM);
            if (envSpec != null) {
                Map<String, String> environment = scriptPB.environment();
                JsonObject envSpecO = envSpec.getAsObject();   // validated in parser so safe
                for (String key : envSpecO.keySet()) {
                    JsonValue value = envSpecO.get(key);
                    if ( value.isString() ) {
                        environment.put(key, value.getAsString().value() );
                    } else {
                        environment.put(key, value.toString());
                    }
                }
            }
            Process scriptProcess = scriptPB.start();

            Thread stdout = new Thread( new CaptureOutput(scriptProcess.getInputStream(), monitor, "") );
            Thread stderr = new Thread( new CaptureOutput(scriptProcess.getErrorStream(), monitor, "error") );
//            BufferedReader in = new BufferedReader( new InputStreamReader(scriptProcess.getInputStream()) );
//            String line;
//            String lastLine = null;
//            while ((line = in.readLine()) != null) {
//                monitor.report(line);
//                lastLine = line;
//            }
//            in.close();
            stdout.start();
            stderr.start();
            int status = 1;
            try {
                status = scriptProcess.waitFor();
                stdout.join();
                stderr.join();
            } catch (InterruptedException e) {
                monitor.reportError("Script action " + script + " interrupted");
                scriptProcess.destroy();
                try {
                    // Catch any dying messages
                    stdout.join();
                    stderr.join();
                } catch (InterruptedException e2) {
                    // No further clean up attempts
                }
                status = 2;
            }
            String lastLine = "";
            List<ProgressMessage> messages = monitor.getMessages();
            if ( ! messages.isEmpty()) {
                lastLine = messages.get( messages.size() - 1 ).getMessage();
            }
            if (status == 0) {
                monitor.report("Script completed");
                return JsonUtil.makeJson(RESULT, lastLine);   // Return last line, if any, as the result
            } else {
                monitor.reportError("Script failed with status: " + status);
                return JsonUtil.makeJson("errorStatus", status);
            }
            
        } catch (IOException e) {
            monitor.reportError("Problem configuring script " + script  + ", " + e);
        } finally {
            if (argFile != null) {
                argFile.delete();
            }
        }
        
        return JsonUtil.emptyObject();
    }
    
    class CaptureOutput implements Runnable {
        protected BufferedReader in;
        protected ProgressMonitorReporter monitor;
        protected String type;

        public CaptureOutput(InputStream in, ProgressMonitorReporter monitor, String type) {
            this.in = new BufferedReader( new InputStreamReader(in) );
            this.monitor = monitor;
            this.type = type;
        }
        
        @Override
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    // Work around for JENA-767
                    String clean = line.replace("\b", "");
                    monitor.report(clean, type);
                }
                in.close();
            } catch (IOException e) {
                // Quietly exit if the stream dies
            }
        }
    
    }
    
}
