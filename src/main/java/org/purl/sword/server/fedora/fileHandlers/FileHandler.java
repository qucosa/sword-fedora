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

import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;

/**
 * All file handlers must implement this interface and must have a default
 * constructor which takes no parameters and throws no exceptions.
 *
 * @author Glen Robson
 * @version 1.0
 * @since 18 October 2007
 */
public interface FileHandler {
    /**
     * This decides whether the File handler can handle the current deposit.
     *
     * @param pMimeType  The mime type
     * @param pPackaging The packaging
     * @return True, if this handler can handle the current deposit
     */
    public boolean isHandled(final String pMimeType, final String pPackaging);

    /**
     * Take the deposit and ingest it into Fedora
     *
     * @param pDeposit         The deposit and its associated collection
     * @param pServiceDocument The service document which this request applies to
     * @throws SWORDException if any problem occurred during ingest
     */
    public SWORDEntry ingestDeposit(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException;

    /**
     * Update an previously ingested deposit by using the given deposit.
     *
     * @param depositCollection The deposit and its associated collection referencing the deposit to update
     * @param serviceDocument   The service document which this request applies to
     * @return SWORD entry document describing the result
     * @throws SWORDException If any problem occurred during ingest
     */
    public SWORDEntry updateDeposit(DepositCollection depositCollection, ServiceDocument serviceDocument) throws SWORDException;
}
