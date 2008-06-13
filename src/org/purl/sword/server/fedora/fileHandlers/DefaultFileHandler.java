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
  * This is the catch all for files which are not recognised by the other 
  * file handlers. You should extends this class if you are writing new 
  * file handlers. It allows you to only extend the methods which you need to.
  */

import org.apache.log4j.Logger;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.Collection;
import org.purl.sword.base.SWORDException;

import org.w3.atom.Source;
import org.w3.atom.Generator;
import org.w3.atom.Content;
import org.w3.atom.Link;
import org.w3.atom.Author;
import org.w3.atom.Contributor;
import org.w3.atom.Rights;
import org.w3.atom.Summary;
import org.w3.atom.Title;
import org.w3.atom.InvalidMediaTypeException;

import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.baseExtensions.XMLServiceDocument;

import org.purl.sword.server.fedora.utils.XMLProperties;

import org.purl.sword.server.fedora.fedoraObjects.FedoraObject;
import org.purl.sword.server.fedora.fedoraObjects.DublinCore;
import org.purl.sword.server.fedora.fedoraObjects.Relationship;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.Disseminator;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;
import org.purl.sword.server.fedora.fedoraObjects.Property;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.TimeZone;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DefaultFileHandler implements FileHandler {
	private static final Logger LOG = Logger.getLogger(DefaultFileHandler.class);
	/** The mime type of the deposit */
	protected String _contentType = "";
	/** The format namespace of the deposit */
	protected String _formatNamespace = "";
	/** Access to the properties file */
	protected XMLProperties _props = null;

	/**
	 * Call this from child classes as it initates the Properties file, the content type and format namespace
	 * @param String the mime type of the deposit
	 * @param String the format namespace
	 */ 
	public DefaultFileHandler(final String pContentType, final String pFormatNamespace) {
		_props = new XMLProperties();
		this.setContentType(pContentType);
		this.setFormatNamespace(pFormatNamespace);
	}

	/**
	 * This decides whether the File handler can handle the current deposit. Child classes 
	 * must override this method
	 * @param String the mime type
	 * @param String format namespace
	 * @return boolean if this handler can handle the current deposit
	 */
	public boolean isHandled(final String pContentType, final String pFormatNamespace) {
		return true; // catch all for deposits so can handle anything
	}

	/**
	 * This is the main method that is called to ingest a deposit. Override this if
	 * you want complete control over the ingest. This method calls all the other methods.
	 * @param DepositCollection the deposit and its associated collection
	 * @param ServiceDocument the service document which this request applies to
	 * @throws SWORDException if any problems occured during ingest
	 */
	public SWORDEntry ingestDepost(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException {

		FedoraObject tNewObject = new FedoraObject(pDeposit.getUsername(), pDeposit.getPassword());

		tNewObject.setIdentifiers(this.getIdentifiers(pDeposit));
		tNewObject.setDC(this.getDublinCore(pDeposit));
		tNewObject.setRelationships(this.getRelationships(pDeposit));
		try {
			List<Datastream> tDatastreamList = this.getDatastreams(pDeposit);
			this.ensureValidDSIds(tDatastreamList);
			tNewObject.setDatastreams(tDatastreamList);
		} catch (IOException tIOExcpt) {
			tIOExcpt.printStackTrace();
			LOG.debug("Excpetion");
			LOG.error("Couldn't access uploaded file" + tIOExcpt.toString());
			throw new SWORDException("Couldn't access uploaded file", tIOExcpt);
		}
		tNewObject.setDisseminators(this.getDisseminators(pDeposit, tNewObject.getDatastreams()));

		if (!pDeposit.isNoOp()){ // Don't ingest if no op is set
			tNewObject.ingest();
		}
		
		return this.getSWORDEntry(pDeposit, pServiceDocument, tNewObject);
	}

	/** 
	 * This returns the Properties associated with the fedora object
	 * some of these properties are manditory so it is a good idea to call this method
	 * from any method that overrides it.
	 * @param DepositCollection the deposit and its associated collection
	 * @return List<Property> a list of properties for this object
	 */
	protected List<Property> getIdentifiers(final DepositCollection pDeposit) {
		List<Property> tIdentifiers = new ArrayList<Property>();

		tIdentifiers.add(new Property("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "FedoraObject"));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#state", "Active"));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#label", "Object created through the SWORD deposit system"));
		if (pDeposit.getOnBehalfOf() != null) {
			tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#ownerId", pDeposit.getOnBehalfOf()));
		} else {
			tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#ownerId", pDeposit.getUsername()));
		}
			
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#createdDate", this.getCurrDateAsFedora()));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/view#lastModifiedDate", this.getCurrDateAsFedora()));
		if (pDeposit.getSlug() != null) {
			tIdentifiers.add(new Property("org.purl.sword.slug", pDeposit.getSlug(), Property.TYPE.EXTERNAL));
		}

		return tIdentifiers;
	}

	/**
	 * This returns the dublin core from the deposit. This method guesses at 
	 * dublin core values so you don't need to call it from child classes if you
	 * override it. All fedora objects have a dublin core record.
	 * @param DepositCollection the deposit and its associated collection
	 * @return DublinCore the dublin core from the deposit
	 */
	protected DublinCore getDublinCore(final DepositCollection pDeposit) {
		DublinCore tDC = new DublinCore();
		
		tDC.getTitle().add("Uploaded by the JISC funded SWORD project");

		if (pDeposit.getSlug() != null) {
			tDC.getIdentifier().add(pDeposit.getSlug());
		}

		if (pDeposit.getOnBehalfOf() != null) {
			tDC.getCreator().add(pDeposit.getOnBehalfOf());
		} else {
			tDC.getCreator().add(pDeposit.getUsername());
		}

		tDC.getFormat().add(pDeposit.getContentType());

		return tDC;
	}

	/**
	 * This returns the relationships for a deposit. This implementation
	 * assigns it to the collection specified in the service document.
	 *
	 * @param DepositCollection the deposit and its associated collection
	 * @return Relationship the relationships
	 */
	protected Relationship getRelationships(final DepositCollection pDeposit) {
		Relationship tRelation = new Relationship();
		tRelation.add("isMemberOf", pDeposit.getCollectionPid());

		return tRelation;
	}

	/**
	 * This method ensures the datastream names are unique. If a duplicate is found then it will append
	 * a number to it to make it unique. 
	 * 
	 * @param List<Datastream> the list of datastreams to be added
	 */
	protected void ensureValidDSIds(final List<Datastream> pDatastreamList) {
		Map<String,Integer> tDatastreamNames = new HashMap<String,Integer>(pDatastreamList.size());

		for (Datastream tDatastream : pDatastreamList) {
			LOG.debug("Checking " + tDatastream.getId());
			tDatastream.setId(this.getValidFileName(tDatastream.getId()));
	
			if (tDatastreamNames.get(tDatastream.getId()) == null) {
				tDatastreamNames.put(tDatastream.getId(), 1);
			} else {
				tDatastreamNames.put(tDatastream.getId(), tDatastreamNames.get(tDatastream.getId()) + 1);
				tDatastream.setId(tDatastream.getId() + "-" + tDatastreamNames.get(tDatastream.getId()));
			}
		}
	}

	/**
	 * This is the method that is most commonly overridden to provide new file handlers. Ensure you remove temp
	 * files unless you use LocalDatastream which cleans up after its self.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @return List<Datastream> a list of datastreams to add
	 * @throws IOException if can't access a datastream
	 * @throws SWORDException if there are any other problems
	 */
	protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws IOException, SWORDException {
		LOG.debug("copying file");

		String tTempFileName = this.getTempDir() + "uploaded-file.tmp";
		IOUtils.copy(pDeposit.getFile(), new FileOutputStream(tTempFileName));
		Datastream tDatastream = new LocalDatastream(this.getGenericFileName(pDeposit), this.getContentType(), tTempFileName);
	
		List<Datastream> tDatastreams = new ArrayList<Datastream>();
		tDatastreams.add(tDatastream);

		return tDatastreams;
	}

	/**
	 * Override this method if you want to add disseminators to an object. This implementation does not add any disseminators.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param List<Datastream> a list of the datastreams for the object
	 */
	protected List<Disseminator> getDisseminators(final DepositCollection pDeposit, final List<Datastream> pDatastream) {
		LOG.debug("No disseminators are added");
		return new ArrayList<Disseminator>();
	}

	/** 
	 * This method is the general method that converts the service document and deposit into a SWORD entry. This is the overall method
	 * so if you want complete control on how the SWORDEntry is created overried this method otherwise overside the other SWORD Entry methods.
    * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param ServiceDocument the service document associated with this request
	 * @param FedoraObject the object that has been ingested
	 */ 
	protected SWORDEntry getSWORDEntry(final DepositCollection pDeposit, final ServiceDocument pServiceDocument, final FedoraObject pFedoraObj) throws SWORDException {
		SWORDEntry tEntry = new SWORDEntry();

		this.addServiceDocEntries(tEntry, ((XMLServiceDocument)pServiceDocument).getCollection(pDeposit.getCollectionPid()));
		this.addDepositEntries(tEntry, pDeposit);
		this.addIngestEntries(pDeposit, tEntry, pFedoraObj);
		this.addDCEntries(tEntry, pFedoraObj.getDC());
				
		tEntry.setPublished(this.getCurrDateAsAtom());
		tEntry.setUpdated(this.getCurrDateAsAtom());

		Source tSource = new Source();
		Generator tGenerator = new Generator();
		tSource.setGenerator(tGenerator);
		tGenerator.setUri(_props.getReposiotryUri());
		tGenerator.setVersion("1.0");
		tEntry.setSource(tSource);

		tEntry.setVerboseDescription("Your deposit was added to the repository with identifier " + pFedoraObj.getPid() + ". Thank you for depositing.");

		return tEntry;
	}

	/**
	 * This just sets the treatment entry from the Collection found in the service document
	 * @param SWORDEntry the entry to add the treatment value to
	 * @param Collection the service document collection
	 */ 
	protected void addServiceDocEntries(final SWORDEntry pEntry, final Collection pCollection) {
		pEntry.setTreatment(pCollection.getTreatment());
	}
	
	/** 
	 * This just sets the no op option and FormatNamespace from the deposit.
	 * @param SWORDEntry the entry to add the no op value to
	 * @param DepositCollection the deposit object
	 */
	protected void addDepositEntries(final SWORDEntry pEntry, final DepositCollection pDeposit) {
		if (this.getFormatNamespace() != null && this.getFormatNamespace().trim().length() != 0) {
			pEntry.setFormatNamespace(this.getFormatNamespace());
		}	
		pEntry.setNoOp(pDeposit.isNoOp());
		
		Author tAuthor = new Author();
		tAuthor.setName(pDeposit.getUsername());
		pEntry.addAuthors(tAuthor);
		if (pDeposit.getOnBehalfOf() != null) {
			Contributor tContributor = new Contributor();
			tContributor.setName(pDeposit.getOnBehalfOf());
			pEntry.addContributor(tContributor);
		}

	}

	/**
	 * This methods adds entries about the ingest e.g. The URL to the deposit and links. The entries added here are Content and Link.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param SWORDEntry the entry to add the values to
	 * @param FedoraObject the ignested object
	 * @throws SWORDException if there is a problem accessing the properties file to find the URL to the deposit
	 */
	protected void addIngestEntries(final DepositCollection pDeposit, final SWORDEntry pEntry, final FedoraObject pFedoraObj) throws SWORDException {
		Content tContent = new Content();
		try {
			tContent.setType(this.getContentType());
		} catch (InvalidMediaTypeException tInvalidMedia) {
			LOG.error("Invalid media type '" + this.getContentType() + "':" + tInvalidMedia.toString());
		}
		tContent.setSource(pFedoraObj.getURLToDS(this.getLinkName(pDeposit)));
		pEntry.setContent(tContent);

		pEntry.setId(pFedoraObj.getPid());

		Link tEditMedia = new Link();
		tEditMedia.setHref(pFedoraObj.getURLToDS(this.getLinkName(pDeposit)));
		tEditMedia.setHreflang("en");
		tEditMedia.setRel("edit-media");
		pEntry.addLink(tEditMedia);

		Link tEdit = new Link();
		tEdit.setHref(pFedoraObj.getURLToObject());
		tEdit.setHreflang("en");
		tEdit.setRel("edit");

		pEntry.addLink(tEdit);

	}

	/** 
	 * This specifies the datastream name for the object that was deposited. For example if a zip
	 * file was deposited the this would return the datastream that contains the zip file in Fedora not 
	 * a link to the files that were in the zip file. This is used for creating the links.
	 *
	 * @param DepositCollection the deposit
	 * @return String the datastream name for the actual file deposited
	 */
	protected String getLinkName(final DepositCollection pDeposit) {
		return this.getGenericFileName(pDeposit);
	}

	/** 
	 * This method tries to copy some of the dublin core elmenets into the SWORD entry object. The following
	 * entries are added; Author, Contributor, Rights, Summary and Title.
    *
	 * @param SWORDEntry the entry to add to
	 * @param DublinCore the dublin core datastream to retrieve the data from
	 */
	protected void addDCEntries(final SWORDEntry pEntry, final DublinCore pDC) {
		/*Iterator<String> tAuthorsIter = pDC.getCreator().iterator();
		Author tAuthor = null;
		while (tAuthorsIter.hasNext()) {
			tAuthor = new Author();
			tAuthor.setName(tAuthorsIter.next());
			pEntry.addAuthors(tAuthor);
		}*/

		Iterator<String> tCategoryIter = pDC.getSubject().iterator();
		while (tCategoryIter.hasNext()) {
			pEntry.addCategory(tCategoryIter.next());
		}

		// This includes the on behalf of
		/*Iterator<String> tContributorsIter = pDC.getContributor().iterator();
		Contributor tContributor = null;
		while (tContributorsIter.hasNext()) {
			tContributor = new Contributor();
			tContributor.setName(tContributorsIter.next());
			pEntry.addContributor(tContributor);
		}*/

		Iterator<String> tRightsIter = pDC.getRights().iterator();
		StringBuffer tRightsStr = new StringBuffer();
		while (tRightsIter.hasNext()) {
			tRightsStr.append(tRightsIter.next());
		}
		Rights tRights = new Rights();
		tRights.setContent(tRightsStr.toString());
		pEntry.setRights(tRights);

		Iterator<String> tDescriptionIter = pDC.getDescription().iterator();
		StringBuffer tDescriptionStr = new StringBuffer();
		while (tDescriptionIter.hasNext()) {
			tDescriptionStr.append(tDescriptionIter.next());
		}
		Summary tSummary = new Summary();
		tSummary.setContent(tDescriptionStr.toString());
		pEntry.setSummary(tSummary);


		Iterator<String> tTitleIter = pDC.getTitle().iterator();
		StringBuffer tTitleStr = new StringBuffer();
		while (tTitleIter.hasNext()) {
			tTitleStr.append(tTitleIter.next());
		}
		Title tTitle = new Title();
		tTitle.setContent(tTitleStr.toString());
		pEntry.setTitle(tTitle);


	}

	/** 
	 * Utility method to convert the current date to a date Fedora understands
	 * @return String the date in Fedora's requried format
	 */
	protected String getCurrDateAsFedora() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(new Date());
	}

	/** 
	 * Utility method to convert the current date to a date from the Atom spec
	 * @return String the date in Atom's requried format
	 */
	protected String getCurrDateAsAtom() {
       DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(new Date());
	}
	
	/**
	 * Get _contentType.
	 *
	 * @return _contentType as String.
	 */
	public String getContentType() {
		LOG.debug("Returning " + _contentType);
	    return _contentType;
	}
	
	/**
	 * Set _contentType.
	 *
	 * @param _contentType the value to set.
	 */
	public void setContentType(final String pContentType) {
	     _contentType = pContentType;
	}
	
	/**
	 * Get _formatNamespace.
	 *
	 * @return _formatNamespace as String.
	 */
	public String getFormatNamespace() {
	    return _formatNamespace;
	}
	
	/**
	 * Set _formatNamespace.
	 *
	 * @param _formatNamespace the value to set.
	 */
	public void setFormatNamespace(final String pFormatNamespace) {
	     _formatNamespace = pFormatNamespace;
	}

	/** 
	 * Returns the directory where deposited files can be stored before upload to fedora
	 * 
	 * @return String the tempory directory
	 * @throws SWORDException if there is a problem retrieving the temp dir value from the properties file
	 */
	public String getTempDir() throws SWORDException {
		String tTempDir = _props.getTempDir(); 
		if (!tTempDir.endsWith(System.getProperty("file.separator"))) {
			tTempDir += System.getProperty("file.separator");
		}
		return tTempDir;
	}

	/**
	 * If the file name is unkown this method tries to get it from the deposit object getFilename method
	 * but if this is null it sets it to uploaded
	 *
	 * @param DepositCollection the deposit and its associated collection
	 * @param String the datastream name to use for the deposit.
	 */
	protected String getGenericFileName(final DepositCollection pDeposit) {
		LOG.debug("Filename '" + pDeposit.getFilename() + "'");
		String tFilename = "";
		if (pDeposit.getFilename() == null) {
			tFilename = "upload";
		} else {
			tFilename = this.getValidFileName(pDeposit.getFilename());
		}
		return tFilename;
	}

	protected String getValidFileName(final String pName) {
		String tFilename = pName;
		if (!(pName.matches("[a-z].*") || pName.matches("[A-Z].*") || pName.matches("\\_.*"))) {
			LOG.debug("datastream name must begin with a letter or _");
			tFilename = "Uploaded-" + pName;
		}

		if (tFilename.indexOf(".") != -1) {
			// Remove extension
			tFilename = tFilename.substring(0, tFilename.indexOf("."));
		}	

		LOG.debug("Replacing filename: " + pName + " with " + tFilename);
		return tFilename;
	}
}
