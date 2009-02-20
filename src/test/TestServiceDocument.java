import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.IOException;
import org.xml.sax.SAXException;

import org.jdom.input.DOMBuilder;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.jdom.JDOMException;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestServiceDocument {
	private Namespace SWORD = Namespace.getNamespace("sword","http://purl.org/net/sword/");
	private Namespace ATOM = Namespace.getNamespace("atom","http://www.w3.org/2005/Atom");
	private Namespace APP = Namespace.getNamespace("app","http://www.w3.org/2007/app");
	//private Namespace APP = Namespace.getNamespace("app","http://purl.org/atom/app#");

	public TestServiceDocument() {
	}

	@Test
	public void testMediatedDeposit() throws IOException, JDOMException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthentication("SWORD", "sword", "sword");
		WebRequest tReq = new GetMethodWebRequest( "http://localhost/sword/servicedocument" );
		WebResponse tResp = tWC.getResponse(tReq);
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		XPath tPath = XPath.newInstance("/app:service/app:workspace/app:collection/sword:mediation");
		tPath.addNamespace(APP);
		tPath.addNamespace(SWORD);
		Element tMediation = (Element)tPath.selectSingleNode(tServiceDoc);
		assertNotNull("sword:mediation not present", tMediation);
		assertTrue("sword:mediation must contain either true or false", tMediation.getText().equals("true") || tMediation.getText().equals("false"));
	}

	public static void main(final String pArgs[]) throws IOException, SAXException {
		org.junit.runner.JUnitCore.main(TestServiceDocument.class.getName());
	}
}
