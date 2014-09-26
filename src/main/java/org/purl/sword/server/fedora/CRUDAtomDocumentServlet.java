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

import org.apache.log4j.Logger;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.AtomDocumentServlet;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CRUDAtomDocumentServlet extends AtomDocumentServlet {
    private static Logger log = Logger.getLogger(CRUDAtomDocumentServlet.class);

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("DELETE " + request.getRequestURL().toString());
        CRUDSWORDServer server = obtainCRUDServerInstanceOrNull();
        if (server == null) {
            log.warn("DELETE not supported: Configured SWORD server instance doesn't implement CRUD interface.");
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        try {
            DeleteRequest deleteRequest = new DeleteRequest();
            setAuthenticationDetails(request, deleteRequest);
            deleteRequest.setIPAddress(request.getRemoteAddr());
            deleteRequest.setLocation(getUrl(request));
            server.doDelete(deleteRequest);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (SWORDException e) {
            log.error(e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (SWORDAuthenticationException e) {
            log.error(e.getMessage());
            // Ask for credentials again
            String s = "Basic realm=\"SWORD\"";
            response.setHeader("WWW-Authenticate", s);
            response.setStatus(401);
        }
    }

    private void setAuthenticationDetails(HttpServletRequest request, DeleteRequest deleteRequest) throws SWORDAuthenticationException {
        String usernamePassword = getUsernamePassword(request);
        if ((usernamePassword != null) && (!usernamePassword.equals(""))) {
            int p = usernamePassword.indexOf(":");
            if (p != -1) {
                deleteRequest.setUsername(usernamePassword.substring(0, p));
                deleteRequest.setPassword(usernamePassword.substring(p + 1));
            }
        } else if (authenticateWithBasic()) {
            throw new SWORDAuthenticationException("No credentials");
        }
    }

    private CRUDSWORDServer obtainCRUDServerInstanceOrNull() {
        return myRepository instanceof CRUDSWORDServer ? (CRUDSWORDServer) myRepository : null;
    }
}
