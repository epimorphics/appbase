/******************************************************************
 * File:        ScriptAction.java
 * Created by:  Dave Reynolds
 * Created on:  8 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import static com.epimorphics.json.JsonUtil.EMPTY_OBJECT;
import static com.epimorphics.json.JsonUtil.fromJson;
import static com.epimorphics.json.JsonUtil.getStringValue;
import static com.epimorphics.json.JsonUtil.merge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

public class ScriptAction extends BaseAction implements Action  {
    public static final String ACTION_TYPE = "script";
    public static final String SCRIPT_PARAM = "@script";
    public static final String DEFAULT_SHELL = "/bin/bash";

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
            throw new EpiException("Script file not executable: " + scriptF);
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
                        args[2+i] = fromJson( conf.get( argMap.get(i) ) ).toString();
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

            scriptPB.redirectErrorStream(true);
            scriptPB.directory(scriptDir);
            Process scriptProcess = scriptPB.start();

            BufferedReader in = new BufferedReader( new InputStreamReader(scriptProcess.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {
                monitor.report(line);
            }
            in.close();
            int status = scriptProcess.waitFor();
            if (status == 0) {
                monitor.report("Script completed");
            } else {
                monitor.report("Script failed with status: " + status);
                monitor.setFailed();
                return JsonUtil.makeJson("errorStatus", status);
            }
            
        } catch (IOException e) {
            monitor.report("Problem configuring script " + script  + ", " + e);
            monitor.setFailed();
            
        } catch (InterruptedException e) {
            monitor.report("Script action " + script + " interrupted");
            monitor.setFailed();  // what does interruption really mean here?
        } finally {
            if (argFile != null) {
                argFile.delete();
            }
        }
        
        return EMPTY_OBJECT;
    }
    
}
