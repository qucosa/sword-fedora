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
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.DublinCore;
import org.purl.sword.server.fedora.fedoraObjects.Relationship;
import org.purl.sword.server.fedora.utils.METSObject;

import java.io.IOException;
import java.util.List;

/**
 * This class ingests METS documents.
 *
 * @author Glen Robson
 * @version 1.0
 * @since 26th February 2009
 */
public class METSFileHandler extends DefaultFileHandler implements FileHandler {
    private static final Logger LOG = Logger.getLogger(METSFileHandler.class);

    protected METSObject _mets = null;

    public METSFileHandler() {
        super("text/xml", "http://www.loc.gov/METS/");
    }

    /**
     * The METS document needs to be processed at the start of the ingest process so we do it hear
     * then call the super.ingestDeposit to handle the actual deposit
     *
     * @param pDeposit         The deposit and its associated collection
     * @param pServiceDocument The service document which this request applies to
     * @throws SWORDException if any problem occurred during ingest
     */
    public SWORDEntry ingestDeposit(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException {
        try {
            SAXBuilder tBuilder = new SAXBuilder();
            _mets = new METSObject(tBuilder.build(pDeposit.getFile()));
        } catch (IOException tIOExcpt) {
            String tMessage = "Couldn't retrieve METS from deposit: " + tIOExcpt.toString();
            LOG.error(tMessage);
            tIOExcpt.printStackTrace();
            throw new SWORDException(tMessage, tIOExcpt);
        } catch (JDOMException tJDOMExcpt) {
            String tMessage = "Couldn't build METS from deposit: " + tJDOMExcpt.toString();
            LOG.error(tMessage);
            tJDOMExcpt.printStackTrace();
            throw new SWORDException(tMessage, tJDOMExcpt);
        }

        return super.ingestDeposit(pDeposit, pServiceDocument);
    }

    /**
     * Retrieve the dublin core from the METS document if possible, if not use the super class. DefaultFileHandler
     *
     * @param pDeposit The deposit
     * @return The Dublin Core datastream
     */
    protected DublinCore getDublinCore(final DepositCollection pDeposit) {
        DublinCore tDC = _mets.getDublinCore();

        if (tDC == null) {
            return super.getDublinCore(pDeposit);
        } else {
            return tDC;
        }
    }

    /**
     * Retrieve the Relationships from the METS document if possible, if not use the super class. DefaultFileHandler
     *
     * @param pDeposit The deposit
     * @return The RELS-EXT datastream
     */
    protected Relationship getRelationships(final DepositCollection pDeposit) {
        Relationship tRelations = _mets.getRelationships();

        if (tRelations == null) {
            return super.getRelationships(pDeposit);
        } else {
            return tRelations;
        }
    }

    /**
     * Get the datastreams out of the METS
     *
     * @param pDeposit The deposit
     * @return A list of the datastreams
     * @throws SWORDException if there was a problem processing the METS
     */
    protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws SWORDException {
        try {
            return _mets.getDatastreams();
        } catch (JDOMException tJDOMExcpt) {
            String tMessage = "Couldn't retrieve datastreams from METS: " + tJDOMExcpt.toString();
            LOG.error(tMessage);
            tJDOMExcpt.printStackTrace();
            throw new SWORDException(tMessage, tJDOMExcpt);
        }
    }

    /**
     * Change the name of the uploaded file link to point to the METS datastream
     *
     * @param pDeposit The deposit
     * @return String the name of the METS datastream
     */
    protected String getLinkName(final DepositCollection pDeposit) {
        return "METS";
    }
}
