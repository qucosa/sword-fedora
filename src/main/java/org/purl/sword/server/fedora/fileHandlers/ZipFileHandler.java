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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;
import org.purl.sword.server.fedora.utils.ZipFileAccess;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles Zip file deposits.
 *
 * @author Glen Robson
 * @version 1.0
 * @since 26th February 2009
 */
public class ZipFileHandler extends DefaultFileHandler implements FileHandler {
    private static final Logger LOG = Logger.getLogger(ZipFileHandler.class);

    protected ZipFileAccess _zipFile = null;

    public ZipFileHandler() {
        super("application/zip", "");
    }

    /**
     * To ensure the temp directories are deleted after ingest this method is overridden to remove
     * the temp dirs but it calls the super.ingestDeposit first
     *
     * @param pDeposit         The deposit and its associated collection
     * @param pServiceDocument The service document which this request applies to
     * @throws SWORDException if any problem occurred during ingest
     */
    public SWORDEntry ingestDeposit(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException {
        _zipFile = new ZipFileAccess(super.getTempDir());
        SWORDEntry tEntry = super.ingestDeposit(pDeposit, pServiceDocument);
        LOG.debug("Cleaning up local zip files in " + super.getTempDir() + "zip-extract");
        // ensure the directories are deleted
        _zipFile.removeLocalFiles();

        return tEntry;
    }

    /**
     * This returns a list of datastreams that were contained in the Zip file
     *
     * @param pDeposit The deposit
     * @return A list of datastreams
     * @throws IOException    if there was a problem extracting the archive or accessing the files
     * @throws SWORDException if there were any other problems
     */
    protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws IOException, SWORDException {
        List<Datastream> tDatastreams = new ArrayList<Datastream>();
        LOG.debug("copying file");

        String tZipTempFileName = super.getTempDir() + "uploaded-file.tmp";
        IOUtils.copy(pDeposit.getFile(), new FileOutputStream(tZipTempFileName));
        // Add the original zip file
        Datastream tDatastream = new LocalDatastream(super.getGenericFileName(pDeposit), this.getContentType(), tZipTempFileName);
        tDatastreams.add(tDatastream);

        tDatastreams.addAll(_zipFile.getFiles(tZipTempFileName));

        return tDatastreams;
    }
}
