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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Fedora object and it's properties.
 */
public class FedoraObject {
    private static final Namespace NS_FOXML = Namespace.getNamespace("foxml", "info:fedora/fedora-system:def/foxml#");

    private final String pid;
    private List<Property> identifiers;
    private DublinCore dc;
    private Relationship relsext;
    private List<Datastream> datastreams;
    private List<Disseminator> disseminators;

    public FedoraObject(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public List<Property> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Property> identifiers) {
        this.identifiers = identifiers;
    }

    public DublinCore getDc() {
        return dc;
    }

    public void setDc(DublinCore dc) {
        this.dc = dc;
    }

    public Relationship getRelsext() {
        return relsext;
    }

    public void setRelsext(Relationship relsext) {
        this.relsext = relsext;

        // Ensure RELS-EXT has the PID
        relsext.setPid(pid);
    }

    public List<Datastream> getDatastreams() {
        return datastreams;
    }

    public void setDatastreams(List<Datastream> datastreams) {
        this.datastreams = datastreams;
    }

    public List<Disseminator> getDisseminators() {
        return disseminators;
    }

    public void setDisseminators(List<Disseminator> disseminators) {
        this.disseminators = disseminators;
    }

    /**
     * Converts this object into FOXML ready for ingestion
     *
     * @return Document the FOXML
     */
    public Document toFOXML(boolean fedora3compatibility) {
        Document tFOXML = new Document();

        Element tDigitalObject = new Element("digitalObject", NS_FOXML);
        tFOXML.setRootElement(tDigitalObject);
        tDigitalObject.setAttribute("PID", pid);

        if (fedora3compatibility) {
            tDigitalObject.setAttribute("VERSION", "1.1");
        }

        tDigitalObject.addContent(this.getObjectPropsXML(fedora3compatibility));
        tDigitalObject.addContent(this.addDSXML(fedora3compatibility));

        if (!fedora3compatibility) {
            // Don't add disseminators to Fedora 3 as they are handled by content models
            tDigitalObject.addContent(this.addDisseminatorsXML());
        }

        return tFOXML;
    }

    private List<Element> addDSXML(boolean fedora3compatibility) {
        List<Element> tDatastreamsList = new ArrayList<Element>();
        tDatastreamsList.add(dc.toFOXML(NS_FOXML));

        if (!fedora3compatibility) {
            relsext.removeFedoraModelsOnSerialization(true);
        }
        tDatastreamsList.add(relsext.toFOXML(NS_FOXML));

        for (Datastream tDSXML : this.getDatastreams()) {
            tDatastreamsList.add(tDSXML.toFOXML(NS_FOXML));
        }
        return tDatastreamsList;
    }

    private List<Element> addDisseminatorsXML() {
        List<Element> tDisseminatorsList = new ArrayList<Element>();
        for (Disseminator tDissXML : this.getDisseminators()) {
            tDisseminatorsList.add(tDissXML.toFOXML(NS_FOXML));
        }
        return tDisseminatorsList;
    }

    private Element getObjectPropsXML(boolean fedora3compatibility) {
        Element tObjectPropsEl = new Element("objectProperties", NS_FOXML);
        for (Property tProp : this.getIdentifiers()) {
            if (fedora3compatibility && tProp.getName().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                continue;
            }
            tObjectPropsEl.addContent(tProp.toFOXML(NS_FOXML));
        }
        return tObjectPropsEl;
    }

}
