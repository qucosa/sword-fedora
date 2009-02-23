import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;

import java.io.IOException;
import java.io.PrintStream;
import java.io.FileInputStream;

import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;

public class TestDeposit {
	private Namespace SWORD = Namespace.getNamespace("sword","http://purl.org/net/sword/");
	private Namespace ATOM = Namespace.getNamespace("atom","http://www.w3.org/2005/Atom");

	protected String _depositUri = null;

	public TestDeposit() {
		_depositUri = System.getProperty("deposit_uri");
	}

	protected HttpClient getClient() {
		HttpClient tClient = new HttpClient();
		tClient.getParams().setAuthenticationPreemptive(true);

		Credentials tUserPass = new UsernamePasswordCredentials("sword", "sword");
		tClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), tUserPass);
	
		return tClient;
	}

	@Test
	public void testWrongPackaging() throws IOException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/mets/dspace");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Should have thrown a 415 error but looks like it has succeded", 415, tStatus);
	} 
	
	@Test
	public void testCorrectPackaging() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing content element", tEntityDoc.getRootElement().getChild("content", ATOM));
		Element tContent = tEntityDoc.getRootElement().getChild("content", ATOM);
		assertEquals("Invalid content type", tContent.getAttributeValue("type"), "application/zip");

		assertNotNull("Missing packaging element", tEntityDoc.getRootElement().getChild("packaging", SWORD));
		Element tPackaging = tEntityDoc.getRootElement().getChild("packaging", SWORD);
		assertEquals("Invalid content type", tPackaging.getText(), "http://purl.org/net/sword-types/METSDSpaceSIP");
	}

	@Test
	public void testNonMediatedAuthor() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing author element", tEntityDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");
	}

	@Test
	public void testMediatedAuthor() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-On-Behalf-Of", "Glen");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing author element", tEntityDoc.getRootElement().getChild("author", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM));
		Element tAuthorName = tEntityDoc.getRootElement().getChild("author", ATOM).getChild("name", ATOM);
		assertEquals("Author name incorrect", tAuthorName.getText(), "sword");

		assertNotNull("Missing contributor element", tEntityDoc.getRootElement().getChild("contributor", ATOM));
		assertNotNull("Missing name element", tEntityDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM));
		Element tContributerName = tEntityDoc.getRootElement().getChild("contributor", ATOM).getChild("name", ATOM);
		assertEquals("Contributer name incorrect", tContributerName.getText(), "Glen");
	}

	@Test
	public void testVerbose() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing verbose element", tEntityDoc.getRootElement().getChild("verboseDescription", SWORD));
	}
	
	@Test
	public void testNoVerbose() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		tPost.addRequestHeader("X-Verbose", "false");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNull("Verbose element present where should be removed", tEntityDoc.getRootElement().getChild("verboseDescription", SWORD));
	}

	@Test
	public void testUserAgent() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		assertNotNull("Missing user agent element", tEntityDoc.getRootElement().getChild("userAgent", SWORD));
		
		assertNotNull("Missing server user agent element", tEntityDoc.getRootElement().getChild("generator", ATOM));
	}

	@Test
	public void testLocation() throws IOException, JDOMException {
		PostMethod tPost = new PostMethod(_depositUri);
		tPost.addRequestHeader("Content-Type", "application/zip");
		tPost.addRequestHeader("X-Packaging", "http://purl.org/net/sword-types/METSDSpaceSIP");
		tPost.addRequestHeader("X-No-Op", "true");
		
		int tStatus = this.getClient().executeMethod(tPost);	

		assertEquals("Post returned a non 201 result", 201, tStatus);

		SAXBuilder tBuilder = new SAXBuilder();
		Document tEntityDoc = tBuilder.build(tPost.getResponseBodyAsStream());

		String tLocation = tPost.getResponseHeader("Location").getValue();
		assertNotNull("Missing Location in header", tLocation);
		assertNotNull("Missing link element", tEntityDoc.getRootElement().getChild("link", ATOM));

		Element tEditLink = tEntityDoc.getRootElement().getChild("link", ATOM);
		assertEquals("Location header doesn't match link href", tEditLink.getAttributeValue("href"), tLocation);
	}

	public static void main(final String pArgs[]) throws IOException, JDOMException {
		org.junit.runner.JUnitCore.main(TestDeposit.class.getName());
	}
}
