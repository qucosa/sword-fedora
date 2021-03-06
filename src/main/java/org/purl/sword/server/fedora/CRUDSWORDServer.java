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

import org.purl.sword.base.*;
import org.purl.sword.server.SWORDServer;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;

/**
 * Extends SWORDServer interface with DELETE and UPDATE methods.
 */
public interface CRUDSWORDServer extends SWORDServer {

    /**
     * Perform a DELETE request
     *
     * @param deleteRequest The delete request object
     * @throws SWORDAuthenticationException Thrown if authentication fails
     * @throws SWORDException               Thrown if an unexpected Exception occurs
     *                                      This will be dealt with by sending a HTTP 500 Server Exception
     * @throws SWORDErrorException          Thrown if an SWORD specific error occurs
     * @throws CRUDObjectNotFoundException  Thrown if the document referenced by the given request object cannot be found
     */
    public void doDelete(DeleteRequest deleteRequest)
            throws
            SWORDAuthenticationException,
            SWORDException,
            SWORDErrorException,
            CRUDObjectNotFoundException;

    /**
     * Perform an UPDATE request
     *
     * @param deposit The update request object
     * @throws SWORDAuthenticationException Thrown if authentication fails
     * @throws SWORDException               Thrown if an unexpected Exception occurs
     *                                      This will be dealt with by sending a HTTP 500 Server Exception
     * @throws SWORDErrorException          Thrown if an SWORD specific error occurs
     * @throws CRUDObjectNotFoundException  Thrown if the document referenced by the given request object cannot be found
     */
    public DepositResponse doUpdate(Deposit deposit)
            throws
            SWORDAuthenticationException,
            SWORDException,
            SWORDErrorException,
            CRUDObjectNotFoundException;

}
