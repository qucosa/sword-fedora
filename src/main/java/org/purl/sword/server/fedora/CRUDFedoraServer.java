package org.purl.sword.server.fedora;

import org.apache.log4j.Logger;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.server.fedora.baseExtensions.DeleteRequest;

public class CRUDFedoraServer extends FedoraServer implements CRUDSWORDServer {

    private static final Logger log = Logger.getLogger(CRUDFedoraServer.class);

    public void doDelete(DeleteRequest deleteRequest) throws SWORDAuthenticationException, SWORDException {
        // TODO Implement Delete
        log.debug("Perform DELETE: " + deleteRequest.getLocation());
    }
}
