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
 */
package org.purl.sword.server.fedora.fedoraObjects;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.utils.XMLProperties;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This encapsulates datastreams which are on the local file system
 * and need to be uploaded to fedora. These files are set to a Managed state
 *
 * @author Glen Robson
 * @version 1.0
 * @since 18 October 2007
 */
public class LocalDatastream extends Datastream {
	private static final Logger LOG = Logger.getLogger(LocalDatastream.class);
	private String _path = "";
	private String _uploadedURL = null;
	private boolean cleanup = true;

	/**
	 * @param pID       Datastream ID
	 * @param pMimeType Mime type
	 * @param pPath     The absolute path for the datastream
	 */
	public LocalDatastream(final String pID, final String pMimeType, final String pPath) {
		super(pID, State.ACTIVE, ControlGroup.MANAGED, pMimeType);
		super.setLabel("SWORD Generic File Upload");
		this.setPath(pPath);
	}

	public String getPath() {
		return _path;
	}

	public void setPath(final String pPath) {
		_path = pPath;
	}

	/**
	 * This uploads the datastream to Fedora so that it is accessible on ingest
	 *
	 * @param pUsername Username for fedora repository
	 * @param pPassword Password for fedora repository
	 * @throws IOException    if there are problems accessing the file
	 * @throws SWORDException if there are problems contacting the repository
	 */
	public void upload(final String pUsername, final String pPassword) throws IOException, SWORDException {
		// Ensure no one uploads same object twice
		if (_uploadedURL != null) return;

		File file = getFileInstance();
		if (!file.exists()) {
			final String message = "File '" + file.getAbsolutePath() + "' doesn't exist";
			LOG.error(message);
			throw new IOException(message);
		}

		final String fedoraUploadUrl = new XMLProperties().getFedoraURL() + "/management/upload";
		String body = uploadFollowRedirects(fedoraUploadUrl, pUsername, pPassword, file);
		_uploadedURL = body.trim().replaceAll("\n", "");

		if (cleanup) {
			LOG.info("Deleting temporary upload file " + _path);
			file.delete();
		}
	}

	public boolean isCleanup() {
		return cleanup;
	}

	/**
	 * Configure if the source file should be deleted after it was successfully uploaded to Fedora.
	 * Default is `true`.
	 *
	 * @param cleanup True, if the source file should be deleted after successful upload.
	 */
	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}

	protected String uploadFollowRedirects(final String pURL, final String pUsername, final String pPassword, File file) throws IOException {
		HttpClient tClient = new HttpClient();
		tClient.getParams().setAuthenticationPreemptive(true);

		Credentials tUserPass = new UsernamePasswordCredentials(pUsername, pPassword);
		tClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), tUserPass);

		// execute and get the response
		LOG.info("Uploading " + this.getPath() + " to " + pURL);
		PostMethod tPost = new PostMethod(pURL);
		tPost.getParams().setParameter("Connection", "Keep-Alive");

		LOG.debug("Content length: " + tPost.getRequestHeader("Content-length") + " content type " + tPost.getRequestHeader("Content-Type"));
		tPost.setContentChunked(true);

		// add the file part
		Part[] parts = {new FilePart("file", file)};
		tPost.setRequestEntity(new MultipartRequestEntity(parts, tPost.getParams()));
		LOG.debug("Multipart length: " + tPost.getRequestEntity().getContentLength());

		int responseCode = tClient.executeMethod(tPost);
		if (responseCode >= 300 && responseCode <= 399) {
			Header tLocationHeader = tPost.getResponseHeader("location");
			// Redirected
			return this.uploadFollowRedirects(tLocationHeader.getValue(), pUsername, pPassword, file);
		}

		if (responseCode != 201) {
			LOG.error("Couldn't upload " + this.getPath() + " none 201 result: " + responseCode);
			throw new IOException("Couldn't upload file: " + this.getPath());
		}

		return tPost.getResponseBodyAsString();
	}

	/**
	 * Converts this datastream into FOXML so it can be ingested.
	 *
	 * @param namespace The FOXML namespace
	 * @return Element the FOXML datastream node
	 */
	public Element dsToFOXML(final Namespace namespace) {
		if (_uploadedURL == null) {
			throw new IllegalArgumentException("Please upload the datastream before ingesting");
		}
		Element tContentLocation = new Element("contentLocation", namespace);
		tContentLocation.setAttribute("TYPE", "URL");
		tContentLocation.setAttribute("REF", _uploadedURL);
		return tContentLocation;
	}

	private File getFileInstance() throws IOException {
		File file;
		if (_path.startsWith("file:")) {
			try {
				file = new File(new URI(_path));
			} catch (URISyntaxException e) {
				throw new IOException("File URI is not valid: " + _path);
			}
		} else {
			file = new File(_path);
		}
		return file;
	}
}
