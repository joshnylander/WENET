package us.wa.whatcom.co.wenet.client.browser;
import java.util.*;

public class BrowserRoot 
{
public String rootName;
public String largeRootIcon;
public String smallRootIcon;
public HashMap presentationXSLTs;
public HashMap summaryColumnsMap;
	
	public BrowserRoot() {
		presentationXSLTs = new HashMap();
		summaryColumnsMap = new HashMap();
		}
}