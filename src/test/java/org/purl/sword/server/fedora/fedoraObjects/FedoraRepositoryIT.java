package org.purl.sword.server.fedora.fedoraObjects;

import org.jdom.Namespace;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.purl.sword.server.fedora.utils.XMLProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Needs running Fedora instance")
public class FedoraRepositoryIT {

    private static FedoraRepository fedoraRepository;

    @BeforeClass
    public static void connectToRepository() throws Exception {
        XMLProperties xmlConfiguration = mock(XMLProperties.class);
        when(xmlConfiguration.getFedoraURL()).thenReturn("http://localhost:8080/fedora");

        fedoraRepository = new FedoraRepository(xmlConfiguration, "fedoraAdmin", "fedoraAdmin");
        fedoraRepository.connect();
    }

    @Test
    public void should_return_datastream() throws Exception {
        Datastream datastream = fedoraRepository.getDatastream("qucosa:18188", "SLUB-INFO");
        // TODO No useful assertion here
        System.out.println(datastream.toFOXML(Namespace.XML_NAMESPACE).toString());
    }

}
