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
import org.purl.sword.base.*;
import org.purl.sword.server.AtomDocumentServlet;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class CRUDAtomDocumentServlet extends AtomDocumentServlet {
    private static Logger log = Logger.getLogger(CRUDAtomDocumentServlet.class);

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("UPDATE " + request.getRequestURL().toString());
        CRUDSWORDServer server = obtainCRUDServerInstanceOrNull();
        if (server == null) {
            log.warn("UPDATE not supported: Configured SWORD server instance doesn't implement CRUD interface.");
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        } else {
            try {
                Deposit updateRequest = buildUpdateRequest(request);
                DepositResponse depositResponse = server.doUpdate(updateRequest);
                echoBackUserAgent(request, depositResponse);
                echoBackPackagingFormat(updateRequest, depositResponse);
                response.setStatus(depositResponse.getHttpResponse());
                setDepositLocationIfGiven(response, depositResponse);
                writeResponseContent(response, depositResponse);
            } catch (SWORDException e) {
                log.error(e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (SWORDAuthenticationException e) {
                log.error(e.getMessage());
                // Ask for credentials again
                String s = "Basic realm=\"SWORD\"";
                response.setHeader("WWW-Authenticate", s);
                response.setStatus(401);
            } catch (CRUDObjectNotFoundException e) {
                log.debug(e.getMessage());
                response.setStatus(404);
            } catch (SWORDErrorException e) {
                // Get the details and send the right SWORD error document
                log.error(e.toString());
                this.makeErrorDocument(e.getErrorURI(),
                        e.getStatus(),
                        e.getDescription(),
                        request,
                        response);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("DELETE " + request.getRequestURL().toString());
        CRUDSWORDServer server = obtainCRUDServerInstanceOrNull();
        if (server == null) {
            log.warn("DELETE not supported: Configured SWORD server instance doesn't implement CRUD interface.");
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        } else
            try {
                DeleteRequest deleteRequest = buildDeleteRequest(request);
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
            } catch (CRUDObjectNotFoundException e) {
                log.debug(e.getMessage());
                response.setStatus(404);
            } catch (SWORDErrorException e) {
                // Get the details and send the right SWORD error document
                log.error(e.toString());
                this.makeErrorDocument(e.getErrorURI(),
                        e.getStatus(),
                        e.getDescription(),
                        request,
                        response);
            }
    }

    private DeleteRequest buildDeleteRequest(HttpServletRequest request) throws SWORDAuthenticationException, SWORDErrorException {
        DeleteRequest deleteRequest = new DeleteRequest();
        setAuthenticationDetails(request, deleteRequest);
        deleteRequest.setIPAddress(request.getRemoteAddr());
        deleteRequest.setLocation(request.getRequestURI());
        setOnBehalfHeader(request, deleteRequest);
        setXNOOPHeader(request, deleteRequest);
        setDesiredState(request, deleteRequest);
        return deleteRequest;
    }

    private Deposit buildUpdateRequest(HttpServletRequest request) throws SWORDAuthenticationException, SWORDErrorException, SWORDException {
        Deposit deposit = new Deposit();
        setAuthenticationDetails(request, deposit);
        setOnBehalfHeader(request, deposit);
        setXNOOPHeader(request, deposit);
        setVerboseHeader(request, deposit);
        setContentLength(request, deposit);
        deposit.setIPAddress(request.getRemoteAddr());
        deposit.setLocation(getUrl(request));
        deposit.setPackaging(request.getHeader(HttpHeaders.X_PACKAGING));
        deposit.setSlug(request.getHeader(HttpHeaders.SLUG));
        deposit.setContentDisposition(request.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        deposit.setContentType(request.getContentType());
        try {
            deposit.setMd5(request.getHeader("Content-MD5"));
            // The base implementation in DepositServlet performs MD5 checking here. We
            // leave this to the FileHandler.
            deposit.setFile(request.getInputStream());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new SWORDException("Having trouble getting content from HTTP request", e);
        }
        return deposit;
    }

    private void echoBackPackagingFormat(Deposit updateRequest, DepositResponse depositResponse) {
        final String packagingFormat = updateRequest.getPackaging();
        if ((packagingFormat != null) && (!packagingFormat.isEmpty())) {
            depositResponse.getEntry().setPackaging(packagingFormat);
        }
    }

    private void echoBackUserAgent(HttpServletRequest request, DepositResponse depositResponse) {
        final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if ((userAgent != null) && (!userAgent.isEmpty())) {
            depositResponse.getEntry().setUserAgent(userAgent);
        }
    }

    private CRUDSWORDServer obtainCRUDServerInstanceOrNull() {
        return myRepository instanceof CRUDSWORDServer ? (CRUDSWORDServer) myRepository : null;
    }

    private void setAuthenticationDetails(HttpServletRequest request, Deposit deposit) throws SWORDAuthenticationException {
        String usernamePassword = getUsernamePassword(request);
        if ((usernamePassword != null) && (!usernamePassword.isEmpty())) {
            int p = usernamePassword.indexOf(":");
            if (p != -1) {
                deposit.setUsername(usernamePassword.substring(0, p));
                deposit.setPassword(usernamePassword.substring(p + 1));
            }
        } else if (authenticateWithBasic()) {
            throw new SWORDAuthenticationException("No credentials");
        }
    }

    private void setAuthenticationDetails(HttpServletRequest request, DeleteRequest deleteRequest) throws SWORDAuthenticationException {
        String usernamePassword = getUsernamePassword(request);
        if ((usernamePassword != null) && (!usernamePassword.isEmpty())) {
            int p = usernamePassword.indexOf(":");
            if (p != -1) {
                deleteRequest.setUsername(usernamePassword.substring(0, p));
                deleteRequest.setPassword(usernamePassword.substring(p + 1));
            }
        } else if (authenticateWithBasic()) {
            throw new SWORDAuthenticationException("No credentials");
        }
    }

    private void setContentLength(HttpServletRequest request, Deposit deposit) {
        String cl = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if ((cl != null) && (!cl.equals(""))) {
            deposit.setContentLength(Integer.parseInt(cl));
        }
    }

    private void setDepositLocationIfGiven(HttpServletResponse response, DepositResponse depositResponse) {
        final String depositResponseLocation = depositResponse.getLocation();
        if ((depositResponseLocation != null) && (!depositResponseLocation.isEmpty())) {
            response.setHeader("Location", depositResponseLocation);
        }
    }

    private void setOnBehalfHeader(HttpServletRequest request, DeleteRequest deleteRequest) {
        String onBehalfOf = request.getHeader(HttpHeaders.X_ON_BEHALF_OF);
        if ((onBehalfOf != null) && (!onBehalfOf.isEmpty())) {
            deleteRequest.setOnBehalfOf(onBehalfOf);
        }
    }

    private void setOnBehalfHeader(HttpServletRequest request, Deposit deposit) {
        String onBehalfOf = request.getHeader(HttpHeaders.X_ON_BEHALF_OF);
        if ((onBehalfOf != null) && (!onBehalfOf.isEmpty())) {
            deposit.setOnBehalfOf(onBehalfOf);
        }
    }

    private void setVerboseHeader(HttpServletRequest request, Deposit deposit) throws SWORDErrorException {
        String verbose = request.getHeader(HttpHeaders.X_VERBOSE);
        if ((verbose != null) && (verbose.equals("true"))) {
            deposit.setVerbose(true);
        } else if ((verbose != null) && (verbose.equals("false"))) {
            deposit.setVerbose(false);
        } else if (verbose == null) {
            deposit.setVerbose(false);
        } else {
            throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST, "Bad verbose header");
        }
    }

    private void setXNOOPHeader(HttpServletRequest request, Deposit deposit) throws SWORDErrorException {
        String noop = request.getHeader(HttpHeaders.X_NO_OP);
        log.warn("X_NO_OP value is " + noop);
        if ((noop != null) && (noop.equals("true"))) {
            deposit.setNoOp(true);
        } else if ((noop != null) && (noop.equals("false"))) {
            deposit.setNoOp(false);
        } else if (noop == null) {
            deposit.setNoOp(false);
        } else {
            throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST, "Bad no-op");
        }
    }

    private void setXNOOPHeader(HttpServletRequest request, DeleteRequest deleteRequest) throws SWORDErrorException {
        String noop = request.getHeader(HttpHeaders.X_NO_OP);
        log.warn("X_NO_OP value is " + noop);
        if ((noop != null) && (noop.equals("true"))) {
            deleteRequest.setNoOp(true);
        } else if ((noop != null) && (noop.equals("false"))) {
            deleteRequest.setNoOp(false);
        } else if (noop == null) {
            deleteRequest.setNoOp(false);
        } else {
            throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST, "Bad no-op");
        }
    }

    private void setDesiredState(HttpServletRequest request, DeleteRequest deleteRequest) {
        String state = "D";
        Map params = request.getParameterMap();
        if (params.containsKey("inactivate")) {
            state = "I";
        } else if (params.containsKey("revert")) {
            state = "A";
        }
        deleteRequest.setDesiredState(state);
    }

    private void writeResponseContent(HttpServletResponse response, DepositResponse depositResponse) throws IOException {
        response.setContentType("application/atom+xml;charset=UTF-8");
        Writer w = response.getWriter();
        w.write(depositResponse.marshall());
        w.flush();
    }
}
