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
package org.purl.sword.server.fedora.fedoraObjects;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.RepositoryInfo;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.utils.XMLProperties;

import javax.xml.ws.BindingProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a connection to the Fedora repository. Provides methods to ingest objects.
 *
 * @author Glen Robson
 * @version 1.0
 *          Date: 26th February 2009
 */
public class FedoraRepository {
    private static final Logger log = Logger.getLogger(FedoraRepository.class);

    private FedoraAPIM _APIM = null;
    private FedoraAPIA _APIA = null;
    private XMLProperties configuration = null;

    private String username = null;
    private String password = null;
    private String fedoraVersion = null;

    /**
     * Initialize Fedora repository for connecting.
     *
     * @param configuration Configuration properties
     * @param username      Username to access fedora
     * @param password      Password to access fedora
     */
    public FedoraRepository(XMLProperties configuration, String username, String password) {
        this.username = username;
        this.password = password;
        this.configuration = configuration;
    }

    /**
     * Connect to Fedora repository.
     *
     * @return Reference to this repository object.
     * @throws SWORDException
     */
    public FedoraRepository connect() throws SWORDException {
        initializeAPIA(configuration.getFedoraURL());
        initializeAPIM(configuration.getFedoraURL());
        authenticate(username, password, (BindingProvider) _APIA);
        authenticate(username, password, (BindingProvider) _APIM);
        RepositoryInfo tInfo = _APIA.describeRepository();
        fedoraVersion = tInfo.getRepositoryVersion().trim();
        log.info("Connected to Fedora version " + fedoraVersion);
        return this;
    }

    public String mintPid() throws SWORDException {
        try {
            return _APIM.getNextPID(BigInteger.valueOf(1), configuration.getPIDNamespace()).get(0);
        } catch (Exception e) {
            throw new SWORDException("Problems retrieving the next pid from the repository: ", e);
        }
    }

    public String getFedoraVersion() {
        return fedoraVersion;
    }

    /**
     * Ingest an object into Fedora.
     *
     * @throws SWORDException if ingest failed
     */
    public void ingest(FedoraObject fedoraFedoraObject) throws SWORDException {
        uploadLocalDatastreams(fedoraFedoraObject.getDatastreams());

        log.debug("Ready to upload xml");
        ByteArrayOutputStream tByteArray = new ByteArrayOutputStream();
        XMLOutputter tOut = new XMLOutputter(Format.getPrettyFormat());

        boolean fedora3compatibility = (fedoraVersion.startsWith("3"));

        // upload foxml
        Document tFOXML = fedoraFedoraObject.toFOXML(fedora3compatibility);
        try {
            tOut.output(tFOXML, tByteArray);
            tOut.output(tFOXML, new java.io.FileOutputStream("/tmp/test.xml"));

            String tXMLFormat;
            if (fedora3compatibility) {
                tXMLFormat = "info:fedora/fedora-system:FOXML-1.1";
            } else {
                tXMLFormat = "foxml1.0";
            }
            _APIM.ingest(tByteArray.toByteArray(), tXMLFormat, "ingested by the sword program");
        } catch (RemoteException tRemoteExcpt) {
            try {
                tOut.output(tFOXML, System.out);
            } catch (IOException ignored) {
            }
            String tErrMessage = "Had problems adding the object to the repository; ";
            log.error(tErrMessage + tRemoteExcpt.toString());
            throw new SWORDException(tErrMessage, tRemoteExcpt);
        } catch (Exception tExcpt) {
            try {
                tOut.output(tFOXML, System.out);
            } catch (IOException ignored) {
            }
            String tErrMessage = "Had problems adding the object to the repository; ";
            log.error(tErrMessage + tExcpt.toString());
            throw new SWORDException(tErrMessage, tExcpt);
        }
    }

    private void authenticate(String username, String password, BindingProvider bindingProvider) {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(BindingProvider.USERNAME_PROPERTY, username);
        requestHeaders.put(BindingProvider.PASSWORD_PROPERTY, password);
        bindingProvider.getRequestContext().putAll(requestHeaders);
    }

    private void initializeAPIA(String fedoraURL) {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(FedoraAPIA.class);
        factoryBean.setAddress(fedoraURL + "/services/access");
        _APIA = (FedoraAPIA) factoryBean.create();
    }

    private void initializeAPIM(String fedoraURL) {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(FedoraAPIM.class);
        factoryBean.setAddress(fedoraURL + "/services/management");
        _APIM = (FedoraAPIM) factoryBean.create();
    }

    private void uploadLocalDatastreams(List<Datastream> datastreams) throws SWORDException {
        try {
            log.debug("Uploading local datastreams");
            for (Datastream datastream : datastreams) {
                if (datastream instanceof LocalDatastream) {
                    ((LocalDatastream) datastream).upload(username, password);
                }
            }
        } catch (MalformedURLException tExcpt) {
            throw new SWORDException("Can't access Fedora for upload", tExcpt);
        } catch (IOException tIOExcpt) {
            throw new SWORDException("Error accessing uploaded file: ", tIOExcpt);
        }
    }

}
