package org.purl.sword.server.fedora;

/**
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

import org.purl.sword.server.SWORDServer;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.SWORDContentTypeException;

import org.purl.sword.server.fedora.utils.XMLProperties;
import org.purl.sword.server.fedora.baseExtensions.ServiceDocumentQueries;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.fileHandlers.FileHandler;
import org.purl.sword.server.fedora.fileHandlers.FileHandlerFactory;

import javax.xml.rpc.ServiceException;

import java.rmi.RemoteException;

import org.apache.axis.client.Stub;
import org.apache.axis.AxisFault;
import org.w3c.dom.Element;

import info.fedora.www.definitions._1._0.api.FedoraAPIMServiceLocator;
import info.fedora.www.definitions._1._0.api.FedoraAPIMService;
import info.fedora.www.definitions._1._0.api.FedoraAPIM;

import info.fedora.www.definitions._1._0.types.UserInfo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class FedoraServer implements SWORDServer {
	private static final Logger LOG = Logger.getLogger(FedoraServer.class);

	protected FedoraAPIM _APIM = null;
	protected XMLProperties _props = null;
	public FedoraServer() {
		_props = new XMLProperties();
	
		try {
			FedoraAPIMService tService = new FedoraAPIMServiceLocator();
			((FedoraAPIMServiceLocator)tService).setmanagementEndpointAddress(_props.getFedoraURL() + "/services/management");
			_APIM = tService.getmanagement();
		} catch (ServiceException tServiceExcpt) {
			LOG.error("Can't connect to Fedora");
			LOG.error(tServiceExcpt.toString());
		} catch (SWORDException tSwordExcpt) {
			LOG.error("Invalid fedora section of configuraiton file");
			LOG.error(tSwordExcpt.toString());
		}
	}

	/**
	 * This is the method which retrieves the Service document. If you want to replace this method of retrieving the 
	 * service document override this method and change the server-class in web.xml to point to your extension
	 * 
    * @param String the user that is requesting the ServiceDocument
	 * @return ServiceDocument the service document
	 * @throws SWORDException if there was a problem reading the config file
	 */
	protected ServiceDocument getServiceDocument(final String pOnBehalfOf) throws SWORDException {
		return _props.getServiceDocument(pOnBehalfOf);
	}

	/**
	 * Answer a Service Document request sent on behalf of a user
	 * 
	 * @param sdr The Service Document Request object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDException Thrown in an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 *
	 * @return The ServiceDocument representing the service document
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest pServiceRequest) throws SWORDAuthenticationException, SWORDException {
		if (pServiceRequest.getUsername() != null) {
			this.authenticates(pServiceRequest.getUsername(), pServiceRequest.getPassword());
		}
			
		String tOnBehalfOf = pServiceRequest.getOnBehalfOf();
		if (tOnBehalfOf == null) { // On Behalf off not supplied so send the username instead
		 	tOnBehalfOf = pServiceRequest.getUsername();
		}
		
		return this.getServiceDocument(tOnBehalfOf);
	}
	
	/**
	 * Answer a SWORD deposit
	 * 
	 * @param deposit The Deposit object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDException Thrown in an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 * 
	 * @return The response to the deposit
	 */
	public DepositResponse doDeposit(Deposit pDeposit) throws SWORDAuthenticationException, SWORDException, SWORDContentTypeException {
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

			ServiceDocumentQueries tServiceDoc = (ServiceDocumentQueries)this.getServiceDocument(tOnBehalfOf);
			// Check to see if user is allowed to deposit in collection
			if (!tServiceDoc.isAllowedToDeposit(tOnBehalfOf, tCollectionPID)) {
				String tMessage = "User: " + tOnBehalfOf + " is not allowed to deposit in collection " + tCollectionPID;
				LOG.debug(tMessage);
				throw new SWORDAuthenticationException(tMessage, null);
			}

			// Check to see if content type is in the allowed list
			if (!tServiceDoc.isContentTypeAllowed(pDeposit.getContentType(), tCollectionPID)) {
				LOG.debug("Type " + pDeposit.getContentType() + " is not accepted in collection " + tCollectionPID);
				throw new SWORDContentTypeException(); 
			}

			// Call the file handlers and see which one responds that it can handle the deposit
			FileHandler tHandler = FileHandlerFactory.getFileHandler(pDeposit.getContentType(), pDeposit.getFormatNamespace());
			SWORDEntry tEntry = tHandler.ingestDepost(new DepositCollection(pDeposit, tCollectionPID), (ServiceDocument)tServiceDoc);
		
			// send response
			DepositResponse tResponse = new DepositResponse(Deposit.CREATED);
			tResponse.setEntry(tEntry);
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
	 * Authenticate a user
	 * 
	 * @param usenrame The username to authenticate
	 * @param password The password to autheenticate with
	 * 
	 * @return Whether or not the user credentials authenticate
	 */
	public void authenticates(final String pUsername, final String pPassword) throws SWORDAuthenticationException, SWORDException {
		// Now use the service to get a stub which implements the SDI.
		try {
			((Stub)_APIM).setUsername(pUsername);
			((Stub)_APIM).setPassword(pPassword);

		// Make the actual call
			UserInfo tUserInfo = _APIM.describeUser(pUsername);
			LOG.debug("UserID=" + tUserInfo.getId());
			LOG.debug("IsAdministrator=" + tUserInfo.isAdministrator());
			// if we get here then we have authenticated ok
			return;
		} catch (AxisFault tFault) {
			Element[] tFaults = tFault.getFaultDetails();
			for (int i = 0; i < tFaults.length; i++) {
				if (tFaults[i].getTagName().equals("HttpErrorCode") && tFaults[i].getFirstChild().getTextContent().equals("401")) {
					//tAuthorised  = false;	
					throw new SWORDAuthenticationException("Failed to authenticate", null);
				}
			}
			// Failed for anouther reason so throw sword exception
			throw new SWORDException("Couldn't connect to the Fedora server for authentication", tFault);
		} catch (RemoteException tRemoteExcpt) {
			throw new SWORDException("Couldn't connect to the Fedora server for authentication", tRemoteExcpt);
		}
	}
}
