/*
 * Copyright (c) 2014, SLUB Dresden
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
package org.purl.sword.server.fedora;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fcrepo.server.types.gen.*;
import org.purl.sword.atom.Link;
import org.purl.sword.base.*;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.baseExtensions.ServiceDocumentQueries;
import org.purl.sword.server.fedora.fileHandlers.FileHandler;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.math.BigInteger;
import java.util.Iterator;

public class CRUDFedoraServer extends FedoraServer implements CRUDSWORDServer {

    private static final Logger log = Logger.getLogger(CRUDFedoraServer.class);

    /**
     * Perform a DELETE request on behalf of a user. The user must have
     * the permission to deposit in the targeted collection.
     *
     * @param deleteRequest The delete request object
     */
    public void doDelete(DeleteRequest deleteRequest) throws SWORDAuthenticationException, SWORDException, SWORDErrorException, CRUDObjectNotFoundException {
        RequestInfo requestInfo = new RequestInfo(deleteRequest);

        authenticates(requestInfo.getUsername(), requestInfo.getPassword());
        final String collectionPID = requestInfo.getCollectionPID();
        final String objectPID = requestInfo.getObjectPID();
        final String onBehalfOf = requestInfo.getOnBehalfOf();

        ServiceDocumentQueries serviceDocument = (ServiceDocumentQueries) this.getServiceDocument(onBehalfOf);
        authorizes(serviceDocument, onBehalfOf, collectionPID);

        if (deleteRequest.isNoOp()) {
            log.debug("NOOP. No delete performed.");
            return;
        }

        FieldSearchResult fsr = getFieldSearchResult(objectPID);
        if (fsr.getResultList().getObjectFields().isEmpty()) {
            throw new CRUDObjectNotFoundException("Object " + objectPID + " not found");
        }

        setObjectState(objectPID, fsr, "D", "Deleted on behalf of " + onBehalfOf);
        log.debug("Set object state to deleted: " + objectPID);

        safeDeleteCachedResponse(collectionPID, objectPID);
    }

    /**
     * Perform UPDATE request on behalf of a user. The user must have the permission to deposit in the targeted
     * collection. The updated object must exist in the given location.
     *
     * @param deposit The update request object
     * @see {CRUDSWORDServer.doUpdate}
     */
    public DepositResponse doUpdate(Deposit deposit) throws SWORDAuthenticationException, SWORDException, SWORDErrorException, CRUDObjectNotFoundException {
        if (deposit.isVerbose()) {
            log.setLevel(Level.DEBUG);
        }

        RequestInfo requestInfo = new RequestInfo(deposit);

        authenticates(requestInfo.getUsername(), requestInfo.getPassword());
        final String collectionPID = requestInfo.getCollectionPID();
        final String objectPID = requestInfo.getObjectPID();
        final String onBehalfOf = requestInfo.getOnBehalfOf();

        ServiceDocumentQueries serviceDocument = (ServiceDocumentQueries) this.getServiceDocument(onBehalfOf);
        authorizes(serviceDocument, onBehalfOf, collectionPID);
        contentAcceptable(serviceDocument, deposit, collectionPID);
        packageTypeAcceptable(serviceDocument, deposit, collectionPID);

        FileHandler fileHandler = fileHandlerFactory.getFileHandler(deposit.getContentType(), deposit.getPackaging());

        final DepositResponse depositResponse = new DepositResponse(HttpServletResponse.SC_OK);

        if (deposit.isNoOp()) {
            log.debug("NOOP. No update performed.");
            depositResponse.setHttpResponse(HttpServletResponse.SC_ACCEPTED);
            return depositResponse;
        }

        FieldSearchResult fsr = getFieldSearchResult(objectPID);
        if (fsr.getResultList().getObjectFields().isEmpty()) {
            throw new CRUDObjectNotFoundException("Object " + objectPID + " not found");
        }

        // use deposit ID to transport target object PID
        deposit.setDepositID(objectPID);

        SWORDEntry swordEntry = fileHandler.updateDeposit(
                new DepositCollection(deposit, collectionPID), (ServiceDocument) serviceDocument);
        depositResponse.setEntry(swordEntry);

        Link link = getEditLink(swordEntry);
        if (link != null) {
            depositResponse.setLocation(link.getHref());
        }


        cacheResponse(collectionPID, swordEntry);

        return depositResponse;
    }

    protected Link getEditLink(SWORDEntry swordEntry) {
        Link link = null;
        Iterator<Link> linkIterator = swordEntry.getLinks();
        while (linkIterator.hasNext()) {
            link = linkIterator.next();
            if (link.getRel().equals("edit")) {
                break;
            }
        }
        return link;
    }

    private FieldSearchResult getFieldSearchResult(final String objectPID) {
        FieldSearchQuery fsq = new FieldSearchQuery();
        fsq.setConditions(new JAXBElement<FieldSearchQuery.Conditions>(
                new QName("conditions"),
                FieldSearchQuery.Conditions.class,
                new FieldSearchQuery.Conditions() {{
                    getCondition().add(new Condition() {{
                        setProperty("pid");
                        setOperator(ComparisonOperator.EQ);
                        setValue(objectPID);
                    }});
                }}
        ));
        return _APIA.findObjects(
                new ArrayOfString() {{
                    getItem().add("pid");
                    getItem().add("ownerId");
                    getItem().add("label");
                }},
                BigInteger.ONE,
                fsq);
    }

    private void safeDeleteCachedResponse(String collectionPID, String objectPID) {
        try {
            File collectionDir = new File(_props.getEntryStoreLocation(), collectionPID.replaceAll(":", "_"));
            File responseFile = new File(collectionDir, objectPID.replaceAll(":", "_") + ".xml");
            if (responseFile.exists()) {
                responseFile.delete();
            }
        } catch (Exception e) {
            log.warn("Safe delete of cached response failed: " + e.getMessage());
        }
    }

    private void setObjectState(String objectPID, FieldSearchResult fsr, String state, String message) {
        ObjectFields ofs = fsr.getResultList().getObjectFields().get(0);
        String label = ofs.getLabel().getValue();
        String ownerId = ofs.getOwnerId().getValue();
        _APIM.modifyObject(objectPID, state, label, ownerId, message);
    }

    private class RequestInfo {
        private String location;
        private String collectionPID;
        private String objectPID;
        private String onBehalfOf;
        private String username;
        private String password;

        public RequestInfo(DeleteRequest dr) {
            location = dr.getLocation();
            onBehalfOf = (dr.getOnBehalfOf() == null) ? dr.getUsername() : dr.getOnBehalfOf();
            username = dr.getUsername();
            password = dr.getPassword();
            initPIDs();
        }

        public RequestInfo(Deposit d) {
            location = d.getLocation();
            onBehalfOf = (d.getOnBehalfOf() == null) ? d.getUsername() : d.getOnBehalfOf();
            username = d.getUsername();
            password = d.getPassword();
            initPIDs();
        }

        public String getCollectionPID() {
            return collectionPID;
        }

        public String getObjectPID() {
            return objectPID;
        }

        public String getOnBehalfOf() {
            return onBehalfOf;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        private void initPIDs() {
            String s = location;
            if (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            String[] tWords = s.split("/");
            collectionPID = tWords[tWords.length - 2];
            objectPID = tWords[tWords.length - 1];
        }
    }
}
