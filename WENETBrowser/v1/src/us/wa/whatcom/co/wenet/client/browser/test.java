package us.wa.whatcom.co.wenet.client.browser;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class test
{
	public test(String q) {
	
		q = "//a[b &lt 3]";
		// Open the file and run an XPATH query against it to grab the proper implementation
	try {	File impFile = new File("browserImplementations.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document impDoc = db.parse(impFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
//		XPathExpression xpe = xPath.compile(q);
		NodeList nl = (NodeList) xpath.evaluate(q, impDoc, XPathConstants.NODESET);
		printNodes(nl);
		
		} catch(Exception exc) {
			exc.printStackTrace(System.out);
			}
		}
		
	public void printNodes(NodeList nl) {
		if(nl == null) { return; }
		if(nl.getLength() == 0) { return; }
		
		for(int i=0; i<nl.getLength(); i++) {
			Node n = nl.item(i); 

			if(n.getNodeValue() != null && n.getNodeValue().toString().trim().length() > 0) 
				System.out.println(n.getNodeValue());
			
			NodeList nl2 = n.getChildNodes();			
			printNodes(nl2);
			}
			
		return;
		}

	public static void main(String argv[]) {
		test t = new test(argv[0]);
		}
}