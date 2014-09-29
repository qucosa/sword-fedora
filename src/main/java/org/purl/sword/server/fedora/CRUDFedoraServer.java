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

import org.apache.axis.client.Stub;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.log4j.Logger;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.api.*;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;
import org.purl.sword.server.fedora.baseExtensions.ServiceDocumentQueries;

import java.rmi.RemoteException;

public class CRUDFedoraServer extends FedoraServer implements CRUDSWORDServer {

    private static final Logger log = Logger.getLogger(CRUDFedoraServer.class);

    /**
     * Perform a DELETE request on behalf of a user. The user must have
     * the permission to deposit in the targeted collection.
     *
     * @param deleteRequest The delete request object
     */
    public void doDelete(DeleteRequest deleteRequest) throws SWORDAuthenticationException, SWORDException, SWORDErrorException {
        authenticates(
                deleteRequest.getUsername(),
                deleteRequest.getPassword());

        String onBehalfOf =
                (deleteRequest.getOnBehalfOf() == null) ? deleteRequest.getUsername() : deleteRequest.getOnBehalfOf();

        String tLocation = deleteRequest.getLocation();
        if (tLocation.endsWith("/")) {
            tLocation = tLocation.substring(0, tLocation.length() - 1);
        }
        String[] tWords = tLocation.split("/");
        final String collectionPID = tWords[tWords.length - 2];
        final String objectPID = tWords[tWords.length - 1];

        ServiceDocumentQueries serviceDocument = (ServiceDocumentQueries)this.getServiceDocument(onBehalfOf);
        // Check to see if user is allowed to deposit in collection
        // and is therefore allowed to delete
        if (!serviceDocument.isAllowedToDeposit(onBehalfOf, collectionPID)) {
            String msg = "User: " + onBehalfOf + " is not allowed to delete from collection " + collectionPID;
            log.debug(msg);
            throw new SWORDErrorException(ErrorCodes.TARGET_OWNER_UKNOWN, msg);
        }

        if (deleteRequest.isNoOp()) {
            log.debug("NOOP. No delete performed.");
            return;
        }

        // Modify the object's state to Deleted,
        // but leave other properties unchanged
        try {
            // TODO This call will fail due to Axis bug
            FieldSearchQuery fsq = new FieldSearchQuery();
            fsq.setConditions(new Condition[]{
                    new Condition("pid", ComparisonOperator.eq, objectPID)});
            FieldSearchResult fsr = _APIA.findObjects(
                    new String[]{"ownerId", "label"},
                    new NonNegativeInteger("1"),
                    fsq);

            if (fsr.getResultList().length != 1) {
                //TODO Generate NOT_FOUND error
                throw new SWORDException("Object " + objectPID + " not found");
            }

            ObjectFields ofs = fsr.getResultList()[1];
            String label = ofs.getLabel();
            String ownerId = ofs.getOwnerId();

            _APIM.modifyObject(objectPID, "D", label, ownerId, "Deleted on behalf of " + onBehalfOf);
            log.debug("Set object state to deleted: " + objectPID);
        } catch (RemoteException e) {
            throw new SWORDException(e.getMessage());
        }
    }

    @Override
    public void authenticates(String pUsername, String pPassword) throws SWORDAuthenticationException, SWORDException {
        super.authenticates(pUsername, pPassword);
        ((Stub)_APIM).setUsername(pUsername);
        ((Stub)_APIM).setPassword(pPassword);
    }
}
