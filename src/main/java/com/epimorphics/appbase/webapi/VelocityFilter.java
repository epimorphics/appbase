/******************************************************************
 * File:        VelocityRender.java
 * Created by:  Dave Reynolds
 * Created on:  2 Dec 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.templates.VelocityRender;

/**
 * The filter implementation for driving the VelocityRenderer
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VelocityFilter implements Filter {
    static Logger log = LoggerFactory.getLogger(VelocityFilter.class);
            
    List<VelocityRender> renderers;

    public VelocityFilter() {
    }

    protected List<VelocityRender> listRenderers() {
        if (renderers == null) {
            renderers = new ArrayList<>();
            for (App app : AppConfig.listApps()) {
                VelocityRender renderer = app.getA(VelocityRender.class);
                if (renderer != null) {
                    renderers.add(renderer);
                }
            }
            log.warn("Velocity filter defined by no renderers found");
        }
        return renderers;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest hrequest = (HttpServletRequest) request;
            HttpServletResponse hresponse = (HttpServletResponse) response;
            for (VelocityRender render : listRenderers()) {
                if (render.render(hrequest, hresponse, null)) {
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

}

