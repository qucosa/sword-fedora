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
 * @author Glen Robson
 * @version 1.0
 * Date: 26th February 2009
 *
 * This object encapsulates an object in Fedora and has methods to ingest its
 * self
 *
 */
package org.purl.sword.server.fedora.fedoraObjects;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.RepositoryInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
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
import java.util.*;

public class FedoraObject {
    private static final Logger LOG = Logger.getLogger(FedoraObject.class);
    protected static Namespace FOXML = Namespace.getNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
    protected String _pid = "";
    protected List<Property> _identifiers = null;
    protected DublinCore _dc = null;
    protected Relationship _relationship = null;
    protected List<Datastream> _datastreams = null;
    protected List<Disseminator> _disseminators = null;

    protected FedoraAPIM _APIM = null;
    protected XMLProperties _props = null;
    protected String _username = null;
    protected String _password = null;
    protected String _fedoraVersion = null;

    /**
     * Contacts the Fedora repository to retrieve the next avilable PID
     *
     * @param pUsername Username to access fedora
     * @param pPassword Password to access fedora
     * @throws SWORDException if conection refused to fedora repository
     */
    public FedoraObject(final String pUsername, final String pPassword) throws SWORDException {
        _username = pUsername;
        _password = pPassword;
        _props = new XMLProperties();

        FedoraAPIA tAPIA;
        {
            JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
            factoryBean.setServiceClass(FedoraAPIA.class);
            factoryBean.setAddress(_props.getFedoraURL() + "/services/access");
            tAPIA = (FedoraAPIA) factoryBean.create();
        }
        {
            JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
            factoryBean.setServiceClass(FedoraAPIM.class);
            factoryBean.setAddress(_props.getFedoraURL() + "/services/management");
            _APIM = (FedoraAPIM) factoryBean.create();
        }

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(BindingProvider.USERNAME_PROPERTY, pUsername);
        requestHeaders.put(BindingProvider.PASSWORD_PROPERTY, pPassword);

        ((BindingProvider) tAPIA).getRequestContext()
                .putAll(requestHeaders);
        ((BindingProvider) _APIM).getRequestContext()
                .putAll(requestHeaders);

        RepositoryInfo tInfo = tAPIA.describeRepository();
        _fedoraVersion = tInfo.getRepositoryVersion().trim();
        LOG.debug("Storing fedora version " + _fedoraVersion);

        // find next pid
        LOG.debug("Finding next pid user=" + _username + " password=" + _password);
        try {
            List<String> tPidArray = _APIM.getNextPID(BigInteger.valueOf(1), _props.getPIDNamespace());
            this.setPid(tPidArray.get(0));
        } catch (Exception tExcpt) {
            String tErrMessage = "Had problems retrieving the next pid from the repository; ";
            LOG.error(tErrMessage + tExcpt.toString());
            throw new SWORDException(tErrMessage, tExcpt);
        }
    }

    public String getPid() {
        return _pid;
    }

    public void setPid(final String pPID) {
        _pid = pPID;
    }

    public List<Property> getIdentifiers() {
        return _identifiers;
    }

    public void setIdentifiers(final List<Property> pIdentifiers) {
        _identifiers = pIdentifiers;
    }

    public DublinCore getDC() {
        return _dc;
    }

    public void setDC(final DublinCore pDC) {
        _dc = pDC;
    }

    public Relationship getRelationships() {
        // Ensure relationship has the PID
        _relationship.setPid(this.getPid());
        if (!_fedoraVersion.startsWith("3")) {
            // remove all fedora:model attributes for non Fedora 3 objects or
            // else they don't ingest
            _relationship.removeModels();
        }
        return _relationship;
    }

    public void setRelationships(final Relationship pRelations) {
        _relationship = pRelations;
        // Ensure relationship has the PID
        _relationship.setPid(this.getPid());
    }

    public List<Datastream> getDatastreams() {
        return _datastreams;
    }

    public void setDatastreams(final List<Datastream> pDatastreams) {
        _datastreams = pDatastreams;
    }

    public List<Disseminator> getDisseminators() {
        return _disseminators;
    }

    public void setDisseminators(final List<Disseminator> pDisseminators) {
        _disseminators = pDisseminators;
    }

    /**
     * Get fedoraVersion.
     *
     * @return fedoraVersion as String.
     */
    public String getFedoraVersion() {
        return _fedoraVersion;
    }

    /**
     * Set fedoraVersion.
     *
     * @param pFedoraVersion value to set.
     */
    public void setFedoraVersion(final String pFedoraVersion) {
        _fedoraVersion = pFedoraVersion;
    }

    protected void uploadLocalDatastreams() throws SWORDException {
        try {
            Iterator<Datastream> tDatastreamsIter = this.getDatastreams().iterator();
            Datastream tDatastream = null;
            while (tDatastreamsIter.hasNext()) {
                tDatastream = tDatastreamsIter.next();

                if (tDatastream instanceof LocalDatastream) {
                    ((LocalDatastream) tDatastream).upload(_username, _password);
                }
            }
        } catch (MalformedURLException tExcpt) {
            LOG.error("Can't access fedora for upload" + tExcpt.toString());
            throw new SWORDException("Can't access fedora for upload", tExcpt);
        } catch (IOException tIOExcpt) {
            LOG.error("Couldn't access uploaded file" + tIOExcpt.toString());
            throw new SWORDException("Couldn't access uploaded file: ", tIOExcpt);
        }
    }

    /**
     * Ingest this object into fedora. It first calls FedoraObject.toFOXML
     * to convert this object into FOXML and then ingests that using the Fedora
     * APIM methods
     *
     * @throws SWORDException if ingest failed
     */
    public void ingest() throws SWORDException {
        // Upload them
        this.uploadLocalDatastreams();

        LOG.debug("Ready to upload xml");
        ByteArrayOutputStream tByteArray = new ByteArrayOutputStream();
        XMLOutputter tOut = new XMLOutputter(Format.getPrettyFormat());

        // upload foxml
        Document tFOXML = this.toFOXML();
        try {
            tOut.output(tFOXML, tByteArray);
            tOut.output(tFOXML, new java.io.FileOutputStream("/tmp/test.xml"));

            String tXMLFormat = "";
            if (_fedoraVersion.startsWith("3")) {
                tXMLFormat = "info:fedora/fedora-system:FOXML-1.1";
            } else {
                tXMLFormat = "foxml1.0";
            }

            _APIM.ingest(tByteArray.toByteArray(), tXMLFormat, "ingested by the sword program");
        } catch (RemoteException tRemoteExcpt) {
            try {
                tOut.output(tFOXML, System.out);
            } catch (IOException tIOExcpt) {
            }
            String tErrMessage = "Had problems adding the object to the repository; ";
            LOG.error(tErrMessage + tRemoteExcpt.toString());
            throw new SWORDException(tErrMessage, tRemoteExcpt);
        } catch (Exception tExcpt) {
            try {
                tOut.output(tFOXML, System.out);
            } catch (IOException tIOExcpt) {
            }
            String tErrMessage = "Had problems adding the object to the repository; ";
            LOG.error(tErrMessage + tExcpt.toString());
            throw new SWORDException(tErrMessage, tExcpt);

        }
    }

    /**
     * Returns a externally accessible URL for a datastream
     *
     * @param pDSName the datastream that the URL should point to
     * @throws SWORDException if there was a problem reading the properties file where this value is stored
     */
    public String getURLToDS(final String pDSName) throws SWORDException {
        return _props.getExternalDSURL(this.getPid(), pDSName);
    }

    /**
     * Returns a externally accessible URL for an object
     *
     * @throws SWORDException if there was a problem reading the properties file where this value is stored
     */
    public String getURLToObject() throws SWORDException {
        return _props.getExternalURL(this.getPid());
    }

    /**
     * Converts this object into FOXML ready for ingestion
     *
     * @return Document the FOXML
     */
    public Document toFOXML() {
        Document tFOXML = new Document();

        Element tDigitalObject = new Element("digitalObject", FOXML);
        tFOXML.setRootElement(tDigitalObject);
        tDigitalObject.setAttribute("PID", this.getPid());
        if (_fedoraVersion.startsWith("3")) {
            tDigitalObject.setAttribute("VERSION", "1.1");
        }

        tDigitalObject.addContent(this.getObjectPropsXML());
        tDigitalObject.addContent(this.addDSXML());
        if (!_fedoraVersion.startsWith("3")) {
            // Don't add disseminators to Fedora 3 as they are handled by content models
            tDigitalObject.addContent(this.addDisseminatorsXML());
        }

        return tFOXML;
    }

    protected Element getObjectPropsXML() {
        Element tObjectPropsEl = new Element("objectProperties", FOXML);

        for (Property tProp : this.getIdentifiers()) {
            if (!(_fedoraVersion.startsWith("3") && tProp.getName().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))) {
                LOG.debug("Adding " + tProp.getName());
                tObjectPropsEl.addContent(tProp.toFOXML(FOXML));
            }
        }

        return tObjectPropsEl;
    }

    protected List<Element> addDSXML() {
        List<Element> tDatastreamsList = new ArrayList<Element>();

        tDatastreamsList.add(this.getDC().toFOXML(FOXML));
        tDatastreamsList.add(this.getRelationships().toFOXML(FOXML));

        for (Datastream tDSXML : this.getDatastreams()) {
            tDatastreamsList.add(tDSXML.toFOXML(FOXML));
        }

        return tDatastreamsList;
    }

    protected List<Element> addDisseminatorsXML() {
        List<Element> tDisseminatorsList = new ArrayList<Element>();

        for (Disseminator tDissXML : this.getDisseminators()) {
            tDisseminatorsList.add(tDissXML.toFOXML(FOXML));
        }

        return tDisseminatorsList;
    }


}
