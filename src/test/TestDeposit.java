import org.junit.Test;
import static org.junit.Assert.*;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.HttpException;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileInputStream;

import org.jdom.input.DOMBuilder;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Element;
import org.jdom.Namespace;

import org.xml.sax.SAXException;

public class TestDeposit {
	private Namespace SWORD = Namespace.getNamespace("sword","http://purl.org/net/sword/");
	private Namespace ATOM = Namespace.getNamespace("atom","http://www.w3.org/2005/Atom");

	protected String _url = "http://localhost/sword/deposit/collection:open";

	public TestDeposit() {
	}

	@Test
	public void testWrongPackaging() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization( "sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/mets/dspace</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/mets/dspace");
		tReq.setHeaderField("X-No-Op", "true");

		try {
			WebResponse tResp = tWC.getResponse(tReq);

			// Shouldn't reach here as 412 error should have been thrown
			DOMBuilder tBuilder = new DOMBuilder();
			Document tServiceDoc = tBuilder.build(tResp.getDOM());

			XMLOutputter tOutXML = new XMLOutputter(Format.getPrettyFormat());
			tOutXML.output(tServiceDoc, System.out);

			fail("Should have thrown a 415 error but looks like it has succeded");
		} catch (HttpException tExcpt) {
			assertTrue("Should have thrown a 415 error", tExcpt.getResponseCode() == 415);
		}
	} 
	
	@Test
	public void testCorrectPackaging() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		//tReq.setHeaderField("X-No-Op", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		assertNotNull("Missing content element", tServiceDoc.getRootElement().getChild("content", ATOM));
		Element tContent = tServiceDoc.getRootElement().getChild("content", ATOM);
		assertEquals("Invalid content type", tContent.getAttributeValue("type"), "application/zip");

		assertNotNull("Missing packaging element", tServiceDoc.getRootElement().getChild("packaging", SWORD));
		Element tPackaging= tServiceDoc.getRootElement().getChild("packaging", SWORD);
		assertEquals("Invalid content type", tPackaging.getText(), "http://purl.org/net/sword-types/METSDSpaceSIP");
	}

	@Test
	public void testNonMediatedAuthor() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-No-Op", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		assertNotNull("Missing author element", tServiceDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tServiceDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tServiceDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");
	}

	@Test
	public void testMediatedAuthor() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-On-Behalf-Of", "Glen");
		tReq.setHeaderField("X-No-Op", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		assertNotNull("Missing author element", tServiceDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tServiceDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tServiceDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");

		assertNotNull("Missing contributor element", tServiceDoc.getRootElement().getChild("contributor", ATOM));
		assertNotNull("Missing name element", tServiceDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM));
		Element tContributerName = tServiceDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM);
		assertEquals("Contributer name incorrect", tContributerName.getText(), "Glen");
	}

	@Test
	public void testVerbose() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-On-Behalf-Of", "Glen");
		tReq.setHeaderField("X-No-Op", "true");
		tReq.setHeaderField("X-Verbose", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());


		assertNotNull("Missing verbose element", tServiceDoc.getRootElement().getChild("verboseDescription", SWORD));
	}
	
	@Test
	public void testNoVerbose() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-On-Behalf-Of", "Glen");
		tReq.setHeaderField("X-No-Op", "true");
		tReq.setHeaderField("X-Verbose", "false");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());


		assertNull("Verbose element present where should be removed", tServiceDoc.getRootElement().getChild("verboseDescription", SWORD));
	}

	@Test
	public void testUserAgent() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-On-Behalf-Of", "Glen");
		tReq.setHeaderField("X-No-Op", "true");
		tReq.setHeaderField("X-Verbose", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		// Shouldn't reach here as 412 error should have been thrown
		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		assertNotNull("Missing user agent element", tServiceDoc.getRootElement().getChild("userAgent", SWORD));
		
		assertNotNull("Missing server user agent element", tServiceDoc.getRootElement().getChild("generator", ATOM));
	}

	@Test
	public void testLocation() throws IOException, SAXException {
		WebConversation tWC = new WebConversation();
		tWC.setAuthorization("sword", "sword");
		PrintStream tOut = new PrintStream("/tmp/tout");
		tOut.println("		<?xml version='1.0'?>");
		tOut.println("   <entry xmlns='http://www.w3.org/2005/Atom'");
		tOut.println("	 xmlns:sword='http://purl.org/net/sword/'>");
		tOut.println("     <title>My Deposit</title>");
		tOut.println("     <id>info:something:1</id>");
		tOut.println("     <updated>2008-08-18T14:27:08Z</updated>");
		tOut.println("     <author><name>jbloggs</name></author>");
		tOut.println("     <summary type='text'>A summary</summary>");
		tOut.println("     <sword:userAgent>MyJavaClient/0.1 Restlet/2.0</sword:userAgent>");
		tOut.println("     <generator uri='http://www.myrepository.ac.uk/engine' version='1.0'/>");
		tOut.println("     <content type='application/zip'");
		tOut.println("        src='http://www.myrepository.ac.uk/geography-collection/deposit1.zip'/>	");
		tOut.println("     <sword:packaging>http://purl.org/net/sword-types/METSDSpaceSIP</sword:packaging>");
		tOut.println("     <link rel='edit'");
		tOut.println("        href='http://www.myrepository.ac.uk/geography-collection/atom/my_deposit.atom' />");
		tOut.println("  </entry>");

		WebRequest tReq = new PostMethodWebRequest(_url, new FileInputStream("/tmp/tout"), "text/xml");
		tReq.setHeaderField("Content-Type", "application/zip");
		tReq.setHeaderField("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tReq.setHeaderField("X-On-Behalf-Of", "Glen");
		tReq.setHeaderField("X-No-Op", "true");

		WebResponse tResp = tWC.getResponse(tReq);

		DOMBuilder tBuilder = new DOMBuilder();
		Document tServiceDoc = tBuilder.build(tResp.getDOM());

		String tLocation = tResp.getHeaderField("Location");
		assertNotNull("Missing Location in header", tLocation);
		assertNotNull("Missing link element", tServiceDoc.getRootElement().getChild("link", ATOM));

		Element tEditLink = tServiceDoc.getRootElement().getChild("link", ATOM);
		assertEquals("Location header doesn't match link href", tEditLink.getAttributeValue("href"), tLocation);


		//**/XMLOutputter tOutXML = new XMLOutputter(Format.getPrettyFormat());
		//**/tOutXML.output(tServiceDoc, System.out);
	}

	public static void main(final String pArgs[]) throws IOException, SAXException {
		org.junit.runner.JUnitCore.main(TestDeposit.class.getName());
	}
}
