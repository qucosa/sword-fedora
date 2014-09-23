/*
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
*/
package org.purl.sword.server.fedora.utils;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import java.io.File;

/**
 * This servlet sets up the log4j configuration and reads in the properties file
 * for the application.
 * <p/>
 * In the web.conf ensure the load-on-startup property is set to 1 so this servlet runs first.
 * The init-properties are shown below:
 * <p/>
 * <pre>
 * {@code
 * <servlet>
 *     <servlet-name>StartupServlet</servlet-name>
 *     <servlet-class>org.purl.sword.server.fedora.utils.StartupServlet</servlet-class>
 *     <init-param>
 *         <param-name>log-config</param-name>
 *         <param-value>WEB-INF/log4j.xml</param-value>
 *     </init-param>
 *     <init-param>
 *         <param-name>project.properties</param-name>
 *         <param-value>WEB-INF/properties.xml</param-value>
 *     </init-param>
 *     <load-on-startup>1</load-on-startup>
 * </servlet>
 *  }
 * </pre>
 * If you give relative file names, the path is resolved against the
 * context of the application. To resolve against file system, use
 * absolute paths, e.g.:
 * <p/>
 * <pre>
 *     {@code
 *     <init-param>
 *         <param-name>log-config</param-name>
 *         <param-value>/etc/swordapp/config/log4j.xml</param-value>
 *     </init-param>
 *     }
 * </pre>
 *
 * @author Glen Robson
 * @version 1.0
 * @since 2007-10-18
 */
public class StartupServlet extends HttpServlet {

    private static ServletConfig CONFIG;

    private static String propertiesLocation;
    private final Logger log = Logger.getLogger(StartupServlet.class);

    /**
     * This method loads in the properties file and log4j configuration
     */
    public void init() {
        CONFIG = this.getServletConfig();
        initializeLogging(getInitParameterConsideringOverride("log-config"));
        initializePropertiesLocation(getInitParameterConsideringOverride("project.properties"));
    }

    private void initializeLogging(String pathToLog4JConfig) {
        String logConfigPath = getAbsolutePathToResource(pathToLog4JConfig);
        DOMConfigurator.configure(logConfigPath);
        log.info("Taking log4j config from: " + logConfigPath);
    }

    private void initializePropertiesLocation(String pathToProjectProperties) {
        propertiesLocation = getAbsolutePathToResource(pathToProjectProperties);
        log.info("loading properties files from: " + propertiesLocation);
    }

    private String getInitParameterConsideringOverride(String name) {
        String contextParameterOverride = getServletContext().getInitParameter(name);
        return isPresentAndSet(contextParameterOverride) ? contextParameterOverride : super.getInitParameter(name);
    }

    private boolean isPresentAndSet(String initParameter) {
        return (initParameter != null) && (!initParameter.isEmpty());
    }

    private String getAbsolutePathToResource(String resourcePath) {
        if (resourcePath.startsWith("/")) {
            File file = new File(resourcePath);
            return file.getAbsolutePath();
        } else {
            // assume file is in servlet context
            return this.getServletContext().getRealPath(resourcePath);
        }
    }

    public static String getPropertiesLocation() {
        return propertiesLocation;
    }

    public static String realPathHelper(String resourcePath) {
        return CONFIG.getServletContext().getRealPath(resourcePath);
    }

}
