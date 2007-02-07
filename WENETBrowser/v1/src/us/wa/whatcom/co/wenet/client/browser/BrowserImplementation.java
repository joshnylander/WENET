package us.wa.whatcom.co.wenet.client.browser;
import java.util.*;

public class BrowserImplementation
{
public ArrayList supportedRoots;
public ArrayList seedServicePoints;
public HashMap aliasMap;
public String implName;
public String implDesc;
public String largeApplicationIcon;
public String smallApplicationIcon;
public String appDirectory;
public HashMap displaySchemeMap;
public HashMap columnAliasMappings;

	public BrowserImplementation() {
		columnAliasMappings = new HashMap();
		supportedRoots = new ArrayList();
		seedServicePoints = new ArrayList();
		aliasMap = new HashMap();
		displaySchemeMap = new HashMap();
		implName="";
		}
	
	public String toString() {
		return implName;
		}
}