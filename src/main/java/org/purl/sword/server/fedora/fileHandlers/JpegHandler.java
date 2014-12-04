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
 *
 */
package org.purl.sword.server.fedora.fileHandlers;

import org.apache.commons.io.IOUtils;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.fedoraObjects.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This handler ingests a jpeg image and assigns a disseminator
 * to the object when it has been ingested.
 *
 * @author Glen Robson
 * @version 1.0
 * @since 26th February 2009
 */
public class JpegHandler extends DefaultFileHandler implements FileHandler {

    public JpegHandler() {
        super("image/jpeg", "");
    }

    // this should work on both fedora 2.x and 3.x if its on 2.x it should be ignored
    protected Relationship getRelationships(final DepositCollection pDeposit) {
        Relationship tRelationship = super.getRelationships(pDeposit);

        tRelationship.addModel("info:fedora/demo:UVA_STD_IMAGE_1");

        return tRelationship;
    }

    /**
     * This is the method that is most commonly overridden to provide new file handlers. Ensure you remove temp
     * files unless you use LocalDatastream which cleans up after its self.
     *
     * @param pDeposit The deposit and its associated collection
     * @return A list of datastreams to add
     * @throws IOException    if can't access a datastream
     * @throws SWORDException if there are any other problems
     */
    protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws IOException, SWORDException {
        List<Datastream> tDatastreams = super.getDatastreams(pDeposit);

        FileInputStream tInput = null;
        String tFileName = ((LocalDatastream) tDatastreams.get(0)).getPath();
        String tTempFileName = this.getTempDir() + "uploaded-file.tmp";

        IOUtils.copy(tInput = new FileInputStream(tFileName), new FileOutputStream(tTempFileName + ".thum"));
        tInput.close();
        Datastream tThum = new LocalDatastream("THUMBRES_IMG", this.getContentType(), tTempFileName + ".thum");
        tDatastreams.add(tThum);

        IOUtils.copy(tInput = new FileInputStream(tFileName), new FileOutputStream(tTempFileName + ".mid"));
        tInput.close();
        Datastream tMid = new LocalDatastream("MEDRES_IMG", this.getContentType(), tTempFileName + ".mid");
        tDatastreams.add(tMid);

        IOUtils.copy(tInput = new FileInputStream(tFileName), new FileOutputStream(tTempFileName + ".high"));
        tInput.close();
        Datastream tLarge = new LocalDatastream("HIGHRES_IMG", this.getContentType(), tTempFileName + ".high");
        tDatastreams.add(tLarge);

        IOUtils.copy(tInput = new FileInputStream(tFileName), new FileOutputStream(tTempFileName + ".vhigh"));
        tInput.close();
        Datastream tVLarge = new LocalDatastream("VERYHIGHRES_IMG", this.getContentType(), tTempFileName + ".vhigh");
        tDatastreams.add(tVLarge);

        return tDatastreams;
    }

    /**
     * The only thing different from a default deposit is the assigning of a disseminator
     *
     * @param pDeposit    The deposit
     * @param datastreams A list of the datastreams
     * @return The list of disseminators
     */
    protected List<Disseminator> getDisseminators(final DepositCollection pDeposit, final List<Datastream> datastreams) {
        Datastream tImageDs = datastreams.get(0); // Assumes one image deposited

        List<DSBinding> tBindings = new ArrayList<DSBinding>(4);
        tBindings.add(new DSBinding("THUMBRES_IMG", "THUMBRES_IMG"));
        tBindings.add(new DSBinding("MEDRES_IMG", "MEDRES_IMG"));
        tBindings.add(new DSBinding("HIGHRES_IMG", "HIGHRES_IMG"));
        tBindings.add(new DSBinding("VERYHIGHRES_IMG", "VERYHIGHRES_IMG"));
        Disseminator tDissem = new Disseminator("DISS1", "demo:1", "demo:2", tBindings);

        List<Disseminator> tDissList = new ArrayList<Disseminator>(1);
        tDissList.add(tDissem);
        return tDissList;
    }
}
