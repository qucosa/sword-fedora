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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

import static org.apache.log4j.xml.DOMConfigurator.configure;

/**
 * This Listener sets up the log4j configuration and reads in the properties file
 * for the application.
 * <p/>
 * <p/>
 * The available context parameters are shown below (these are also the defaults):
 * <p/>
 * <pre>
 * {@code
 * <context-param>
 *   <param-name>log-config</param-name>
 *   <param-value>WEB-INF/log4j.xml</param-value>
 * </context-param>
 * <context-param>
 *   <param-name>project.properties</param-name>
 *   <param-value>WEB-INF/properties.xml</param-value>
 * </context-param>
 * }
 * </pre>
 * If you give relative file names, the path is resolved against the
 * context of the application. To resolve against file system, use
 * absolute paths, e.g.:
 * <p/>
 * <pre>
 *     {@code
 *     <context-param>
 *         <param-name>log-config</param-name>
 *         <param-value>/etc/swordapp/config/log4j.xml</param-value>
 *     </context-param>
 *     }
 * </pre>
 *
 * @author Glen Robson
 * @version 1.0
 * @since 2007-10-18
 */
public class StartupListener implements ServletContextListener {

    private static ServletContext context;
    private static String propertiesLocation;
    private final Logger log = Logger.getLogger(StartupListener.class);

    /**
     * Get the path to the configured properties file, set during application startup.
     *
     * @return Path to project properties XML file.
     */
    public static String getPropertiesLocation() {
        return propertiesLocation;
    }

    /**
     * Helper method to access the web application context getRealPath() method.
     *
     * @param resourcePath Path to a resource.
     * @return Absolute path on the server file system.
     */
    public static String realPathHelper(String resourcePath) {
        return context.getRealPath(resourcePath);
    }

    /**
     * Saves servlet context and properties file location for later access.
     * Initializes log4j system.
     *
     * @param sce Context event as raised by the servlet container.
     * @see javax.servlet.ServletContextListener
     */
    public void contextInitialized(ServletContextEvent sce) {
        context = sce.getServletContext();
        initLog4j();
        initPropertiesLocation();
    }

    /**
     * No op. Nothing needs to be destroyed on context destruction.
     *
     * @param sce Context event as raised by the servlet container.
     * @see javax.servlet.ServletContextListener
     */
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private String getAbsolutePathToResource(String resourcePath) throws Exception {
        File file;
        if (resourcePath.startsWith("/")) {
            file = new File(resourcePath);
        } else {
            final String realPath = context.getRealPath(resourcePath);
            if (realPath == null) {
                throw new Exception("Cannot obtain absolute path to file if wrapped in war-archive.");
            }
            file = new File(realPath);
        }
        return file.getAbsolutePath();
    }

    private String getFirstValid(String... values) {
        for (String s : values) if (s != null) return s;
        return null;
    }

    private void initLog4j() {
        try {
            String logConfigPath = getAbsolutePathToResource(
                    orDefaultIfNull(getFirstValid(
                                    System.getProperty("log4j.configuration"),
                                    context.getInitParameter("log-config")),
                            "WEB-INF/log4j.xml"));
            log.info("Taking log4j config from: " + logConfigPath);
            configure(logConfigPath);
        } catch (Exception e) {
            log.error("Error initializing logging: " + e.getMessage());
        }
    }

    private void initPropertiesLocation() {
        try {
            propertiesLocation = getAbsolutePathToResource(
                    orDefaultIfNull(getFirstValid(
                                    System.getProperty("project.properties"),
                                    context.getInitParameter("project.properties")),
                            "WEB-INF/properties.xml"));
            log.info("Loading properties files from: " + propertiesLocation);
        } catch (Exception e) {
            log.fatal("Fatal Error initializing properties: " + e.getMessage());
        }
    }

    private String orDefaultIfNull(String s, String defaultValue) {
        return (s == null) ? defaultValue : s;
    }
}
