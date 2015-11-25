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

import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Generic super class for all datastreams
 *
 * @author Glen Robson
 * @version 1.0
 * @since 18 October 2007
 */
public abstract class Datastream {
    protected ControlGroup _controlGroup;
    protected String _createDate = null;
    protected String _id = "";
    protected String _label = null;
    protected String _mimeType = "";
    protected State _state = State.ACTIVE;
    protected boolean _versionable = true;
    private String digest = "none";
    private String digestType = "DISABLED";

    public Datastream(final String pID, final State pState, final ControlGroup pControlGroup, final String pMimeType) {
        this(pID, pState, pControlGroup, pMimeType, null, null, true);
    }

    /**
     * @param pID           ID of the datastream e.g. DS1 or METS or DC
     * @param pState        If this object is Active, Inactive or Deleted
     * @param pControlGroup How this object is located in the Repository; Managed. External, Inline or Referenced
     * @param pMimeType     Mime type
     * @param pDate         Date when the datastream was created
     * @param pLabel        Label
     * @param pVersionable  Whether updates to this object will over right the original or if a new version will be created
     */
    public Datastream(final String pID, final State pState, final ControlGroup pControlGroup, final String pMimeType,
                      final String pDate, final String pLabel, final boolean pVersionable) {
        _id = pID;
        _state = pState;
        _controlGroup = pControlGroup;
        _mimeType = pMimeType;
        _createDate = pDate;
        _label = pLabel;
        _versionable = pVersionable;
    }


    /**
     * Get id.
     *
     * @return id as String.
     */
    public String getId() {
        return _id;
    }

    /**
     * Set id.
     *
     * @param pId The value to set.
     */
    public void setId(final String pId) {
        _id = pId;
    }

    /**
     * Get state.
     *
     * @return State as String.
     */
    public State getState() {
        return _state;
    }

    /**
     * Set state.
     *
     * @param pState The value to set.
     */
    public void setState(final State pState) {
        _state = pState;
    }

    /**
     * Get controlGroup.
     *
     * @return controlGroup as String.
     */
    public ControlGroup getControlGroup() {
        return _controlGroup;
    }

    /**
     * Set controlGroup.
     *
     * @param pControlGroup The value to set.
     */
    public void setControlGroup(final ControlGroup pControlGroup) {
        _controlGroup = pControlGroup;
    }

    /**
     * Get mimeType.
     *
     * @return mimeType as String.
     */
    public String getMimeType() {
        return _mimeType;
    }

    /**
     * Set mimeType.
     *
     * @param pMimeType The value to set.
     */
    public void setMimeType(final String pMimeType) {
        _mimeType = pMimeType;
    }

    /**
     * Get createDate.
     *
     * @return createDate as String.
     */
    public String getCreateDate() {
        return _createDate;
    }

    /**
     * Set createDate.
     *
     * @param pCreateDate The value to set.
     */
    public void setCreateDate(final String pCreateDate) {
        _createDate = pCreateDate;
    }

    /**
     * Get label.
     *
     * @return label as String.
     */
    public String getLabel() {
        return _label;
    }

    /**
     * Set label.
     *
     * @param pLabel The value to set.
     */
    public void setLabel(final String pLabel) {
        _label = pLabel;
    }

    /**
     * Get versionable.
     *
     * @return versionable as boolean.
     */
    public boolean isVersionable() {
        return _versionable;
    }

    /**
     * Set versionable.
     *
     * @param pVersionable The value to set.
     */
    public void setVersionable(final boolean pVersionable) {
        _versionable = pVersionable;
    }

    /**
     * Creates the xml for a datastream so that it can be slotted into a FOXML ingest document
     *
     * @param namespace The XML namespace
     * @return Element the datastream XML
     */
    public Element toFOXML(final Namespace namespace) {
        Element tDatastream = new Element("datastream", namespace);
        tDatastream.setAttribute("ID", this.getId());
        tDatastream.setAttribute("STATE", this.getState().toString());
        tDatastream.setAttribute("CONTROL_GROUP", this.getControlGroup().toString());
        tDatastream.setAttribute("VERSIONABLE", String.valueOf(isVersionable()));

        Element tDatastreamVersion = new Element("datastreamVersion", namespace);
        tDatastream.addContent(tDatastreamVersion);
        tDatastreamVersion.setAttribute("ID", this.getId() + ".0");
        if (this.getLabel() != null) {
            tDatastreamVersion.setAttribute("LABEL", this.getLabel());
        }
        if (this.getCreateDate() != null) {
            tDatastreamVersion.setAttribute("CREATED", this.getCreateDate());
        }
        tDatastreamVersion.setAttribute("MIMETYPE", this.getMimeType());

        Element tContentDigest = new Element("contentDigest", namespace);
        tDatastreamVersion.addContent(tContentDigest);
        String digestType = this.getDigestType();
        String digest = this.getDigest();
        tContentDigest.setAttribute("TYPE", digestType);
        tContentDigest.setAttribute("DIGEST", digest);

        tDatastreamVersion.addContent(this.dsToFOXML(namespace));

        return tDatastream;
    }

    public abstract Element dsToFOXML(final Namespace FOXML);

    public String getDigestType() {
        return digestType;
    }

    public void setDigestType(String digestType) {
        this.digestType = digestType;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }
}
