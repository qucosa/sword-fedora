package org.purl.sword.server.fedora.fileHandlers;

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
  * This handler ingests a jpeg image and assigns a disseminator
  * to the object when it has been ingested.
  * 
  */
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.Disseminator;
import org.purl.sword.server.fedora.fedoraObjects.DSBinding;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import org.apache.log4j.Logger;

public class JpegHandler extends DefaultFileHandler implements FileHandler {
	private static final Logger LOG = Logger.getLogger(JpegHandler.class);
	public JpegHandler() {
		super("image/jpeg", "");
	}
	
	/**
	 * This file handler can handle mime type of image/jpeg and a format namespace 
	 * which is either null or empty
	 *
	 * @param String the mime type
	 * @param String format namespace
	 * @return boolean if this handler can handle the current deposit
	 */
	public boolean isHandled(final String pMimeType, final String pFormatNamespace) {
		return pMimeType.equals("image/jpeg") && (pFormatNamespace == null || pFormatNamespace.trim().length() == 0);
	}

	/** 
	 * The only thing different from a default deposit is the assiging of a disseminator
	 *
	 * @param DepositCollection the deposit
	 * @param List<Datastream> a list of the datastreams
	 * @return List<Disseminator> the list of disseminators
	 */ 
	protected List<Disseminator> getDisseminators(final DepositCollection pDeposit, final List<Datastream> pDatastreams) {
		Datastream tImageDs = pDatastreams.get(0); // Assumes one image deposited
		
		List<DSBinding> tBindings = new ArrayList<DSBinding>(4);
		tBindings.add(new DSBinding("THUMBRES_IMG", tImageDs.getId()));
		tBindings.add(new DSBinding("MEDRES_IMG", tImageDs.getId()));
		tBindings.add(new DSBinding("HIGHRES_IMG", tImageDs.getId()));
		tBindings.add(new DSBinding("VERYHIGHRES_IMG", tImageDs.getId()));
		Disseminator tDissem = new Disseminator("DISS1", "demo:1", "demo:2", tBindings);

		List<Disseminator> tDissList = new ArrayList<Disseminator>(1);
		tDissList.add(tDissem);
		return tDissList;
	}
}
