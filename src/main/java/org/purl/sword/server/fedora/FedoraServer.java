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
 * Date: 18 October 2007
 *
 */
package org.purl.sword.server.fedora;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.RepositoryInfo;
import org.purl.sword.atom.Link;
import org.purl.sword.base.*;
import org.purl.sword.server.SWORDServer;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.baseExtensions.ServiceDocumentQueries;
import org.purl.sword.server.fedora.fileHandlers.FileHandler;
import org.purl.sword.server.fedora.fileHandlers.FileHandlerFactory;
import org.purl.sword.server.fedora.utils.XMLProperties;

import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FedoraServer implements SWORDServer {
    public static final String VERSION = "1.3";
    private static final Logger LOG = Logger.getLogger(FedoraServer.class);

    protected FedoraAPIM _APIM = null;
    protected FedoraAPIA _APIA = null;
    protected XMLProperties _props = null;
    protected FileHandlerFactory fileHandlerFactory;

    public FedoraServer() {
        _props = new XMLProperties();
        try {
            {
                fileHandlerFactory = FileHandlerFactory.getInstance(_props);
            }
            {
                JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
                factoryBean.setServiceClass(FedoraAPIA.class);
                factoryBean.setAddress(_props.getFedoraURL() + "/services/access");
                _APIA = (FedoraAPIA) factoryBean.create();
            }
            {
                JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
                factoryBean.setServiceClass(FedoraAPIM.class);
                factoryBean.setAddress(_props.getFedoraURL() + "/services/management");
                _APIM = (FedoraAPIM) factoryBean.create();
            }
        } catch (SWORDException e) {
            LOG.error("Invalid fedora section of configuration file");
            LOG.error(e.getMessage());
        }
    }

    /**
     * This is the method which retrieves the Service document. If you want to replace this method of retrieving the
     * service document override this method and change the server-class in web.xml to point to your extension
     *
     * @param pOnBehalfOf The user that is requesting the ServiceDocument
     * @return ServiceDocument the service document
     * @throws SWORDException if there was a problem reading the config file
     */
    protected ServiceDocument getServiceDocument(final String pOnBehalfOf) throws SWORDException {
        return _props.getServiceDocument(pOnBehalfOf);
    }

    protected ServiceDocument getServiceDocument(final String pOnBehalfOf, final String pLocation) throws SWORDException {
        return _props.getServiceDocument(pOnBehalfOf, pLocation);
    }

    /**
     * Answer a Service Document request sent on behalf of a user
     *
     * @param pServiceRequest The Service Document Request object
     * @return The ServiceDocument representing the service document
     * @throws SWORDAuthenticationException Thrown if the authentication fails
     * @throws SWORDException               Thrown in an un-handalable Exception occurs.
     *                                      This will be dealt with by sending a HTTP 500 Server Exception
     */
    public ServiceDocument doServiceDocument(ServiceDocumentRequest pServiceRequest) throws SWORDAuthenticationException, SWORDException {
        LOG.debug(org.purl.sword.base.Namespaces.NS_APP);
        if (pServiceRequest.getUsername() != null) {
            this.authenticates(pServiceRequest.getUsername(), pServiceRequest.getPassword());
        }

        String tOnBehalfOf = pServiceRequest.getOnBehalfOf();
        if (tOnBehalfOf == null) { // On Behalf off not supplied so send the username instead
            tOnBehalfOf = pServiceRequest.getUsername();
        }

        String[] tURIList = pServiceRequest.getLocation().split("/");
        String tLocation = tURIList[tURIList.length - 1];

        if (tLocation.equals("servicedocument")) {
            return this.getServiceDocument(tOnBehalfOf);
        } else { // sub service document
            return this.getServiceDocument(tOnBehalfOf, tLocation);
        }
    }

    /**
     * Answer a SWORD deposit
     *
     * @param pDeposit The Deposit object
     * @return The response to the deposit
     * @throws SWORDAuthenticationException Thrown if the authentication fails
     * @throws SWORDException               Thrown in an un-handalable Exception occurs.
     *                                      This will be dealt with by sending a HTTP 500 Server Exception
     */
    public DepositResponse doDeposit(Deposit pDeposit) throws SWORDAuthenticationException, SWORDException, SWORDErrorException {
        try {
            if (pDeposit.isVerbose()) {
                LOG.setLevel(Level.DEBUG);
            }

            if (pDeposit.getUsername() != null) {
                this.authenticates(pDeposit.getUsername(), pDeposit.getPassword());
            }

            String tLocation = pDeposit.getLocation();
            if (tLocation.endsWith("/")) {
                tLocation = tLocation.substring(0, tLocation.length() - 1);
            }
            String[] tWords = tLocation.split("/");
            final String tCollectionPID = tWords[tWords.length - 1];

            // If no on behalf of set then the deposit is owned by the username
            String tOnBehalfOf = pDeposit.getOnBehalfOf();
            if (pDeposit.getOnBehalfOf() == null) {
                tOnBehalfOf = pDeposit.getUsername();
            }

            ServiceDocumentQueries tServiceDoc = (ServiceDocumentQueries) this.getServiceDocument(tOnBehalfOf);

            authorizes(tServiceDoc, tOnBehalfOf, tCollectionPID);
            contentAcceptable(tServiceDoc, pDeposit, tCollectionPID);
            packageTypeAcceptable(tServiceDoc, pDeposit, tCollectionPID);

            // Call the file handlers and see which one responds that it can handle the deposit
            FileHandler tHandler = fileHandlerFactory.getFileHandler(pDeposit.getContentType(), pDeposit.getPackaging());
            SWORDEntry tEntry = tHandler.ingestDeposit(new DepositCollection(pDeposit, tCollectionPID), (ServiceDocument) tServiceDoc);

            // send response
            DepositResponse tResponse = new DepositResponse(Deposit.CREATED);
            tResponse.setEntry(tEntry);
            Link tLink = null;
            Iterator<Link> tLinksIter = tEntry.getLinks();
            while (tLinksIter.hasNext()) {
                tLink = tLinksIter.next();
                if (tLink.getRel().equals("edit")) {
                    break;
                }
            }
            tResponse.setLocation(tLink.getHref());
            // and save response for further gets, but don't crash since the everything went fine so far
            cacheResponse(tCollectionPID, tEntry);

            return tResponse;
        } catch (SWORDException tException) {
            tException.printStackTrace();
            LOG.error("Exception occured: " + tException);
            throw tException;
        } catch (IllegalArgumentException tArgException) {
            tArgException.printStackTrace();
            LOG.error("Exception occured: " + tArgException);
            throw tArgException;
        } catch (RuntimeException tRuntimeExcpt) {
            tRuntimeExcpt.printStackTrace();
            LOG.error("Exception occured: " + tRuntimeExcpt);
            throw tRuntimeExcpt;
        }
    }

    /**
     * Answer a request for an entry document
     *
     * @param pAtomDocumentRequest The Atom Document Request object
     * @return The response to the atom document request
     * @throws SWORDAuthenticationException Thrown if the authentication fails
     * @throws SWORDErrorException          Thrown if there was an error with the input not matching
     *                                      the capabilities of the server
     * @throws SWORDException               Thrown if an un-handalable Exception occurs.
     *                                      This will be dealt with by sending a HTTP 500 Server Exception
     */
    public AtomDocumentResponse doAtomDocument(AtomDocumentRequest pAtomDocumentRequest) throws SWORDAuthenticationException, SWORDErrorException, SWORDException {
        try {
            if (pAtomDocumentRequest.getUsername() != null) {
                this.authenticates(pAtomDocumentRequest.getUsername(), pAtomDocumentRequest.getPassword());
            }

            // send response
            AtomDocumentResponse tResponse = new AtomDocumentResponse(HttpServletResponse.SC_OK);
            String[] tLocationArray = pAtomDocumentRequest.getLocation().split("/");
            String tPid = tLocationArray[tLocationArray.length - 1].replaceAll(":", "_");
            Builder tBuilder = new Builder();
            File tFile = new File(_props.getEntryStoreLocation(), tPid);
            LOG.debug("Looking for " + tFile.getPath());
            if (tFile.exists() && tFile.isDirectory()) {
                // return RSS of directory entries
            } else {
                String tCollection = tLocationArray[tLocationArray.length - 2].replaceAll(":", "_");
                File tItem = new File(new File(_props.getEntryStoreLocation(), tCollection), tPid + ".xml");
                LOG.debug("Looking for item " + tItem.getPath());
                if (tItem.exists()) {
                    Document tDoc = tBuilder.build(new FileInputStream(tItem));

                    SWORDEntry tEntry = new SWORDEntry();

                    tEntry.unmarshall(tDoc.getRootElement());
                    tResponse.setEntry(tEntry);
                } else {
                    // Requested item doesn't exist
                    LOG.error("Couldn't find " + pAtomDocumentRequest.getLocation());
                    throw new SWORDException("Couldn't find " + pAtomDocumentRequest.getLocation());
                }
            }

            return tResponse;
        } catch (IOException tIOExcpt) {
            tIOExcpt.printStackTrace();
            LOG.error("Exception occured: " + tIOExcpt);
            throw new SWORDException(tIOExcpt.getMessage());
        } catch (UnmarshallException tUnmarshalExcpt) {
            tUnmarshalExcpt.printStackTrace();
            LOG.error("Exception occured: " + tUnmarshalExcpt);
            throw new SWORDException(tUnmarshalExcpt.getMessage());
        } catch (ParsingException tParseExcpt) {
            tParseExcpt.printStackTrace();
            LOG.error("Exception occured: " + tParseExcpt);
            throw new SWORDException(tParseExcpt.getMessage());
        }
    }

    protected void packageTypeAcceptable(ServiceDocumentQueries tServiceDoc, Deposit pDeposit, String tCollectionPID) throws SWORDException, SWORDErrorException {
        if (!tServiceDoc.isPackageTypeAllowed(pDeposit.getPackaging(), tCollectionPID)) {
            String tDesc = "Packaging Type " + pDeposit.getPackaging() + " is not accepted in collection " + tCollectionPID;
            LOG.debug(tDesc);
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, tDesc);
        }
    }

    protected void contentAcceptable(ServiceDocumentQueries tServiceDoc, Deposit pDeposit, String tCollectionPID) throws SWORDException, SWORDErrorException {
        if (!tServiceDoc.isContentTypeAllowed(pDeposit.getContentType(), tCollectionPID)) {
            String tDesc = "Type " + pDeposit.getContentType() + " is not accepted in collection " + tCollectionPID;
            LOG.debug(tDesc);
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, tDesc);
        }
    }

    protected void authorizes(ServiceDocumentQueries serviceDocument, String onBehalfOf, String collectionPID) throws SWORDException, SWORDErrorException {
        if (!serviceDocument.isAllowedToDeposit(onBehalfOf, collectionPID)) {
            String msg = "User: " + onBehalfOf + " has no write access to collection " + collectionPID;
            LOG.debug(msg);
            throw new SWORDErrorException(ErrorCodes.TARGET_OWNER_UKNOWN, msg);
        }
    }

    /**
     * Authenticate a user
     *
     * @param pUsername The username to authenticate
     * @param pPassword The password to authenticate with
     * @return Whether or not the user credentials authenticate
     */
    public void authenticates(final String pUsername, final String pPassword) throws SWORDAuthenticationException, SWORDException {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(BindingProvider.USERNAME_PROPERTY, pUsername);
        requestHeaders.put(BindingProvider.PASSWORD_PROPERTY, pPassword);

        ((BindingProvider) _APIA).getRequestContext()
                .putAll(requestHeaders);
        ((BindingProvider) _APIM).getRequestContext()
                .putAll(requestHeaders);

        RepositoryInfo tInfo = _APIA.describeRepository();
        LOG.debug("Name =" + tInfo.getRepositoryName());
        LOG.debug("Repository Version =" + tInfo.getRepositoryVersion());
    }

    protected void cacheResponse(String tCollectionPID, SWORDEntry tEntry) throws SWORDException {
        File tCollectionDir = new File(_props.getEntryStoreLocation(), tCollectionPID.replaceAll(":", "_"));
        if (!tCollectionDir.exists()) {
            if (tCollectionDir.mkdirs()) {
            } else {
                LOG.warn("Cannot create directory: " + tCollectionDir.toString());
            }
        }
        FileOutputStream tStream = null;
        try {
            tStream = new FileOutputStream(
                    new File(tCollectionDir, tEntry.getId().replaceAll(":", "_") + ".xml"));
            Serializer tSerializer = new Serializer(tStream, "UTF-8");
            tSerializer.setIndent(3);
            Document tDoc = new Document(tEntry.marshall());
            tSerializer.write(tDoc);
        } catch (IOException e) {
            LOG.error("Error while caching response: " + e.getMessage());
        }
    }
}
