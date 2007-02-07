package us.wa.whatcom.co.wenet.client.toolkit;

import java.lang.*;
import java.util.*;

public class Cluster 
{
public String clusterName;
public String clusterURI;
public String parentClusterURI;
public Cluster parentCluster;
public ArrayList clusterMembers;
public WENETJavaToolkit theManager;

	public Cluster(WENETJavaToolkit tk) {
		clusterMembers = new ArrayList();
		theManager = tk;
		}


	public class ClusterMember {
		public Cluster clusterGroup;
		public String orgURI;
		public String serviceURI = "";
		public String serviceDescriptorURL;
		public ServiceDocument serviceDoc;

		public ClusterMember() {
			}
		}

}

