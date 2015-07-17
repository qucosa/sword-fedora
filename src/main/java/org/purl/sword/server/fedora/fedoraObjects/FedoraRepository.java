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
import org.fcrepo.server.types.gen.DatastreamDef;
import org.fcrepo.server.types.gen.RepositoryInfo;
import org.fcrepo.server.types.gen.Validation;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.utils.XMLProperties;

import javax.xml.ws.BindingProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
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

    public Validation validate(FedoraObject obj) throws SWORDException {
        try {
            return _APIM.validate(obj.getPid(), null);
        } catch (Exception e) {
            throw new SWORDException("Problems validating an object: ", e);
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

        boolean fedora3compatibility = (fedoraVersion.startsWith("3"));

        // upload foxml
        Document tFOXML = fedoraFedoraObject.toFOXML(fedora3compatibility);
        ByteArrayOutputStream tByteArray = new ByteArrayOutputStream();
        XMLOutputter tOut = new XMLOutputter(Format.getPrettyFormat());
        try {
            tOut.output(tFOXML, tByteArray);

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

    /**
     * Modify an existing XML datastream by updating content and properties.
     * If a local datastream is passed, it get's uploaded to Fedora prior to ingest.
     *
     * @param pid        PID of the targeted object
     * @param update     Updated Datastream information
     * @param logMessage Message for audit log
     * @throws SWORDException if something goes wrong
     */
    public void modifyDatastream(String pid, Datastream update, String logMessage) throws SWORDException {
        uploadDatastreamIfLocal(update);
        org.fcrepo.server.types.gen.Datastream original = _APIM.getDatastream(pid, update.getId(), null);
        if (update instanceof InlineDatastream) {
            byte[] content = serializeContent((InlineDatastream) update);
            _APIM.modifyDatastreamByValue(
                    pid,
                    update.getId(),
                    original.getAltIDs(),
                    update.getLabel(),
                    update.getMimeType(),
                    original.getFormatURI(),
                    content,
                    original.getChecksumType(),
                    original.getChecksum(),
                    logMessage,
                    false);
        } else if (update instanceof URLContentLocationDatastream) {
            _APIM.modifyDatastreamByReference(
                    pid,
                    update.getId(),
                    original.getAltIDs(),
                    update.getLabel(),
                    update.getMimeType(),
                    original.getFormatURI(),
                    ((URLContentLocationDatastream) update).getURL(),
                    original.getChecksumType(),
                    original.getChecksum(),
                    logMessage,
                    false);
        } else {
            throw new SWORDException("Unknown datastream type");
        }
    }

    /**
     * Add a new datastream to an existing object.
     * If a local datastream is passed, it get's uploaded to Fedora prior to ingest.
     * <p/>
     * The checksum type will be set to DISABLED.
     *
     * @param pid        PID of the targeted object
     * @param ds         Updated Datastream information
     * @param logMessage Message for audit log
     * @throws SWORDException if something goes wrong
     */
    public void addDatastream(String pid, Datastream ds, String logMessage) throws SWORDException {
        uploadDatastreamIfLocal(ds);
        if (ds instanceof URLContentLocationDatastream) {
            _APIM.addDatastream(
                    pid,
                    ds.getId(),
                    null,
                    ds.getLabel(),
                    ds.isVersionable(),
                    ds.getMimeType(),
                    null,
                    ((URLContentLocationDatastream) ds).getURL(),
                    ds.getControlGroup().toString(),
                    ds.getState().toString(),
                    "DISABLED",
                    null,
                    logMessage);
        } else if (ds instanceof InlineDatastream) {
            _APIM.addDatastream(
                    pid,
                    ds.getId(),
                    null,
                    ds.getLabel(),
                    ds.isVersionable(),
                    ds.getMimeType(),
                    null,
                    null,
                    ds.getControlGroup().toString(),
                    ds.getState().toString(),
                    "DISABLED",
                    null,
                    "[creation] " + logMessage);
            byte[] content = serializeContent((InlineDatastream) ds);
            _APIM.modifyDatastreamByValue(
                    pid,
                    ds.getId(),
                    null,
                    ds.getLabel(),
                    ds.getMimeType(),
                    null,
                    content,
                    null,
                    null,
                    "[content] " + logMessage,
                    false);
        } else {
            throw new SWORDException("Unknown datastream type");
        }
    }

    /**
     * Alter the state of a datastream
     *
     * @param pid        PID of the target object
     * @param dsid       ID of the datastream to be modified
     * @param logMessage Message for audit log
     */
    public void setDatastreamState(String pid, String dsid, State state, String logMessage) {
        _APIM.setDatastreamState(pid, dsid, state.toString(), logMessage);
    }

    /**
     * Check if a datastream exists for a given object.
     *
     * @param pid  PID of the object in question
     * @param dsid ID of the datastream
     * @return True, if a datastream with the given ID exists for the specified object. False otherwise.
     */
    public boolean hasDatastream(String pid, String dsid) {
        for (DatastreamDef dsdef : _APIA.listDatastreams(pid, null)) {
            if (dsdef.getID().equals(dsid)) {
                return true;
            }
        }
        return false;
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

    private byte[] serializeContent(InlineDatastream ds) throws SWORDException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(ds.toXML(), byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new SWORDException("Failed serializing XML data: " + e.getMessage());
        }
    }

    private void uploadDatastreamIfLocal(Datastream datastream) throws SWORDException {
        try {
            if (datastream instanceof LocalDatastream) {
                ((LocalDatastream) datastream).upload(username, password);
            }
        } catch (IOException tIOExcpt) {
            throw new SWORDException("Error accessing uploaded file: ", tIOExcpt);
        }
    }

    private void uploadLocalDatastreams(List<Datastream> datastreams) throws SWORDException {
        log.debug("Uploading local datastreams");
        for (Datastream datastream : datastreams) {
            uploadDatastreamIfLocal(datastream);
        }
    }

}
