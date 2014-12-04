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
  *
  */
package org.purl.sword.server.fedora.fileHandlers;

import org.apache.log4j.Logger;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.utils.XMLProperties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class reads in the file handlers from the config file and decides which
 * one can handle the request. The first one it comes across that says it can
 * handle the requested is returned. If none are matched a DefaultFileHandler is
 * used.
 *
 * @author Glen Robson
 * @version 1.0
 * @since 18 October 2007
 */
public class FileHandlerFactory {

    private static final Logger LOG = Logger.getLogger(FileHandlerFactory.class);
    private static FileHandlerFactory instance;
    private final List<FileHandler> fileHandlers;

    private FileHandlerFactory(XMLProperties xmlProperties) {
        this.fileHandlers = loadFileHandlers(xmlProperties);
    }

    private List<FileHandler> loadFileHandlers(XMLProperties tProps) {
        List<FileHandler> tHandlers = new ArrayList<FileHandler>();
        try {
            for (String tClassName : tProps.getFileHandlerClasses()) {
                LOG.debug("Loading " + tClassName + " as a file handler");
                try {
                    tHandlers.add((FileHandler) Class.forName(tClassName).newInstance());
                } catch (ClassNotFoundException tClassExcpt) {
                    LOG.warn("Couldn't find class " + tClassName + " in CLASSPATH");
                } catch (InstantiationException tInstExcpt) {
                    LOG.error("Couldn't instantiate " + tClassName + " ensure it has a default constructor and implements FileHandler interface");
                } catch (IllegalAccessException tIllegalAccess) {
                    LOG.error("Couldn't instantiate " + tClassName + " ensure it has a default constructor and implements FileHandler interface");
                }
            }
        } catch (SWORDException e) {
            LOG.error(e.getMessage());
        }
        return tHandlers;
    }

    public static FileHandlerFactory getInstance(XMLProperties xmlProperties) {
        if (instance == null) instance = new FileHandlerFactory(xmlProperties);
        return instance;
    }

    /**
     * Find the file handler which can handle the mime type and packaging
     *
     * @param pContentType The mime type of the deposit
     * @param pPackaging   The packaging
     * @return The file handler which can handle the deposit
     * @throws SWORDException if there was a problem reading the config file or a file handler couldn't be created
     */
    public FileHandler getFileHandler(final String pContentType, final String pPackaging) throws SWORDException {
        LOG.debug("Looking for " + pContentType + " and packaging " + pPackaging);

        Iterator<FileHandler> tHandlerIter = fileHandlers.iterator();
        FileHandler tHandler;
        while (tHandlerIter.hasNext()) {
            tHandler = tHandlerIter.next();
            if (tHandler.isHandled(pContentType, pPackaging)) {
                LOG.debug("Found handler " + tHandler.getClass().getName());
                return tHandler;
            }
        }

        // Nothing found
        throw new SWORDException("No file handler for " + pContentType + " with packaging: " + pPackaging);
    }
}
