
	var lastRoot = null;
		
	String.prototype.trim = function() { return this.replace(/^\s+|\s+$/, ''); };

	//*******************************************************************************

	function formatTextDate(ctl) {
		txt = ctl.value;
		if(txt == undefined) { return; }

		txt = txt.trim();
		if(txt.length == 0) { return; }

		if(txt.length == 8 && txt.indexOf("/") == -1) {
			out = txt.substr(0,2) + "/" + txt.substr(2,2) + "/" + txt.substring(4);
			ctl.value = out;
			return;
			}

		if(txt.length == 6 && txt.indexOf("/") == -1) {
			out = txt.substr(0,2) + "/" + txt.substr(2,2) + "/20" + txt.substring(4);
			ctl.value = out;
			return;
			}

		if(txt.indexOf("/") > -1) {
			month = "";
			day = "";
			year = "";

			var slashInd = txt.indexOf("/");
			if(slashInd > 0) { month = txt.substring(0, slashInd); }

			var slash2Ind = txt.indexOf("/", slashInd+1);
			if(slash2Ind > 0) { 
				day = txt.substring(slashInd+1, slash2Ind);
				year = txt.substring(slash2Ind+1);
				}

			if(month.length == 1) { out = "0" + month; } else { out = month; }
			if(day.length == 1) { out += "/0" + day; } else { out += "/" + day; }
			if(year.length == 2) { out += "/20" + year; } else { out += "/" + year; }
			ctl.value = out;
			return;
			}

		return;	
		}

	//*******************************************************************************
	// [[0, 0, 0] - [255, 255, 255]] --> [000000-ffffff] 
	
	function rgb2Hex(c){ 
		var c_ = new Array(3); 
		for (i = 0; i < 3; i++){ 
    			c_[i] = int2Hex(c[i]); 
    			} 
    			
    		return c_ 
    		} 

	// [0-255] --> [00-ff] 
	function int2Hex(nb) { 
	    	nb = (nb > 255)? 255: (nb < 0)? 0:nb; 
	    	n_ = Math.floor(nb/16); 
	    	n__ = nb % 16; 
	    	return  n_.toString(16) + n__.toString(16) 
		} 

	//*******************************************************************************
	var currentColor;
	var targetColor;
	var redDelta;
	var greenDelta;
	var blueDelta;
	var effectIteration=0;
	var totalSteps = 10;

	var innerBox = 0;
	var innerBoxIteration=0;
	var innerBoxCount=1;
	
	function splashEffect(outDiv) {
	
		var txt = "<p class=QUERYSPLASH_MESSAGEFONT>Please wait while the browser completes the query </p>";
		txt += "<table border=5 width=400 cellspacing=10 cellpadding=6><tr align=center>";
		txt += "<td id=Cell1><fieldset width=10 height=10 id=Box1>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "<td id=Cell2><fieldset id=Box2>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "<td id=Cell3><fieldset id=Box3>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "<td id=Cell4><fieldset id=Box4>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "<td id=Cell5><fieldset id=Box5>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "<td id=Cell6><fieldset id=Box6>&nbsp&nbsp&nbsp&nbsp</fieldset></td>";
		txt += "</tr></table>";
		outDiv.innerHTML = txt;
		
		r = Math.floor(Math.random()*255);
		g = Math.floor(Math.random()*255);
		b = Math.floor(Math.random()*255);		
		currentColor = new Array(new Array(r,g,b), new Array(r,g,b), new Array(r,g,b), new Array(r,g,b), new Array(r,g,b), new Array(r,g,b));
	
		r = Math.floor(Math.random()*255);
		g = Math.floor(Math.random()*255);
		b = Math.floor(Math.random()*255);		
		targetColor = new Array(r,g,b);	

		redDelta = (targetColor[0] - currentColor[0][0]) / totalSteps;
		greenDelta = (targetColor[1] - currentColor[0][1]) / totalSteps;
		blueDelta = (targetColor[2] - currentColor[0][2]) / totalSteps;
		hexOutput = rgb2Hex(currentColor[0]);	
			
		document.all.Cell1.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell2.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell3.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell4.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell5.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell6.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];

		document.all.Box1.style.background="#ffffff";
		document.all.Box2.style.background="#ffffff";
		document.all.Box3.style.background="#ffffff";
		document.all.Box4.style.background="#ffffff";
		document.all.Box5.style.background="#ffffff";
		document.all.Box6.style.background="#ffffff";
		}
	
	function updateSplashEffect() {
		innerBoxIteration++;
		effectIteration++;
		colAry = currentColor[0];

		updated = 0;
		if(colAry[0] >= targetColor[0]+(redDelta+1) || colAry[0] <= targetColor[0]-(redDelta+1)) { newRed = Math.floor(colAry[0] + redDelta); } else { newRed = Math.floor(colAry[0]); }
		if(colAry[1] >= targetColor[1]+(greenDelta+1) || colAry[1] <= targetColor[1]-(greenDelta+1)) { newGreen = Math.floor(colAry[1] + greenDelta); } else { newGreen = Math.floor(colAry[1]); }
		if(colAry[2] >= targetColor[2]+(blueDelta+1) || colAry[2] <= targetColor[2]-(blueDelta+1)) { newBlue = Math.floor(colAry[2] + blueDelta); } else { newBlue = Math.floor(colAry[2]); }

		col = new Array(newRed, newGreen, newBlue);
		currentColor[0] = col;
				
		// Take the last color off the list
//		currentColor.pop();
	
		hexOutput = rgb2Hex(currentColor[0]);				
		document.all.Cell1.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell2.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell3.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell4.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell5.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Cell6.style.background="#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
		document.all.Header.style.color = "#" + hexOutput[0] + hexOutput[1] + hexOutput[2];

		// See if we have reached our target color, if so, pick a new one
		if(effectIteration == totalSteps) {
			effectIteration = 0;
			r = Math.floor(Math.random()*255);
			g = Math.floor(Math.random()*255);
			b = Math.floor(Math.random()*255);		
			targetColor = new Array(r,g,b);	
			
			redDelta = (targetColor[0] - currentColor[0][0]) / totalSteps;
			greenDelta = (targetColor[1] - currentColor[0][1]) / totalSteps;
			blueDelta = (targetColor[2] - currentColor[0][2]) / totalSteps;
			}

		if(innerBoxIteration == innerBoxCount) {
			innerBoxIteration = 0;
			innerBox++;
			if(innerBox == 6) { innerBox = 0; }

			// Update the inner box
			document.all.Box1.style.background="#ffffff";
			document.all.Box2.style.background="#ffffff";
			document.all.Box3.style.background="#ffffff";
			document.all.Box4.style.background="#ffffff";
			document.all.Box5.style.background="#ffffff";
			document.all.Box6.style.background="#ffffff";

//			var colorOut1 = "#bbbbbb";
			var colorOut2 = "#ffffff";
			var colorOut3 = "#ffffff";
			var colorOut1 = "#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
//			var colorOut2 = "#" + hexOutput[0] + hexOutput[1] + hexOutput[2];
//			var colorOut3 = "#" + hexOutput[0] + hexOutput[1] + hexOutput[2];

			switch(innerBox) {
				case 0: { document.all.Box1.style.background=colorOut1; document.all.Box2.style.background=colorOut2; document.all.Box6.style.background=colorOut3; break; }
				case 1: { document.all.Box2.style.background=colorOut1; document.all.Box3.style.background=colorOut2; document.all.Box1.style.background=colorOut3; break; }
				case 2: { document.all.Box3.style.background=colorOut1; document.all.Box4.style.background=colorOut2; document.all.Box2.style.background=colorOut3; break; }
				case 3: { document.all.Box4.style.background=colorOut1; document.all.Box5.style.background=colorOut2; document.all.Box3.style.background=colorOut3; break; }
				case 4: { document.all.Box5.style.background=colorOut1; document.all.Box6.style.background=colorOut2; document.all.Box4.style.background=colorOut3; break; }
				case 5: { document.all.Box6.style.background=colorOut1; document.all.Box1.style.background=colorOut2; document.all.Box5.style.background=colorOut3; break; }
				}
			}

		
		setTimeout('updateSplashEffect();', 250);
		}

	//*******************************************************************************
	// This method takes a value and an xpath and combines them with the appropriate
	// xpath string method. op=0 is EXACT op=1 is CONTAINS op=2 is STARTS-WITH op=3 is END-WITH
	
	function makeXPathString(xpath, value, op) {
		if(value.trim().length == 0) { return ""; }
		var str = xpath + "='" + value + "' and "; 
		if(op == 0) { str = xpath + "='" + value + "' and "; }
		if(op == 1) { str = "contains(" + xpath + ", '" +  value + "') and "; }
		if(op == 2) { str = "starts-with(" + xpath + ", '" +  value + "') and "; }
		if(op == 3) { str = "ends-with(" + xpath + ", '" +  value + "') and "; } 
		return str;
		}

	//*******************************************************************************
	// Converts a date of mm/dd/yyyy

	function convertDate(stringDate) {

		var slashInd1 = stringDate.indexOf("/");
		if(slashInd1 == -1) { return null; }
		var slashInd2 = stringDate.indexOf("/", slashInd1+1);
		if(slashInd2 == -1) { return null; }

		var month = stringDate.substr(0, slashInd1);
		var day = stringDate.substring(slashInd1+1, slashInd2);
		var year = stringDate.substring(slashInd2+1);

		if(isNaN(parseInt(month,10)) || isNaN(parseInt(day,10)) || isNaN(parseInt(year,10)) ) { return null; }

		// Because the javascript date.setMonth() method MUST have its month parameter in 0-11 months instead
		// of 1-12 months we have to convert the month into an integer and subtract one... blah blah blah
		
		var monthNum = parseInt(month,10) - 1;

		var date = new Date();
		date.setMonth(monthNum, day);
		date.setFullYear(year);
		return date;
		}

	//*******************************************************************************

	function makeTimestamp(dateobj) {
	
		// Remember, javascript is bloody stupid and returns months in 0-11 instead of 1-12

		var startStamp = "";
		startStamp += dateobj.getFullYear() + "-";

		if((dateobj.getMonth()+1) < 10 )
			startStamp += "0" + (dateobj.getMonth()+1) + "-";
		else
			startStamp += (dateobj.getMonth()+1) + "-";

		if((dateobj.getDate()) < 10 )
			startStamp += "0" + dateobj.getDate();
		else
			startStamp += dateobj.getDate();

		return startStamp;
		}

	//*******************************************************************************

	function ViewSessionDetails() {
		top.WorkspaceFrame.location = 'SessionDetails.html';
		}


	function SetDisplayTheme(themeName) {
	
		var theURL = '../../servlet/WENETBrowserServlet?act=SetDisplayTheme&theme=' + themeName;
	
		// Load up the instance data from the requested URL
		var xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.3.0");
		xmlhttp.open("GET", theURL, false);
		xmlhttp.send();
		if(xmlhttp.readyState != 4 || xmlhttp.status != 200) {
			alert("Error setting theme. Response code: " + xmlhttp.status);
			return;
			}			

		top.NavigationFrame.showNavigation();
		}

	//*******************************************************************************

	function ViewSummary(RootType, QueryName, Directory) {

		if(Directory == null || Directory == undefined) { Directory = ""; } 		
						
		// Grab the appropriate summary display xslt file for the queried root type
		var xml = top.SessionDataFrame.sessionDocument;
		var displayPath = "session/implementation/theRoots/root[rootName='" + RootType + "']/presentation[type='Summary']/file";
		var displayFile = xml.selectSingleNode(displayPath).text;
		
		var displayFullFile = Directory + displayFile; 

		// If its an html file then just load it up and send the user
		if(displayFile.indexOf(".html") >= 0 || displayFile.indexOf(".htm") >= 0) {
			top.SessionDataFrame.queryName = QueryName;
			top.SessionDataFrame.queryRoot = RootType;
			top.WorkspaceFrame.location = displayFullFile;
			lastRoot = RootType;
			lastQueryName = QueryName;
			lastDirectory = Directory;
			return;
			}

		// Load up the sumamary page template
		top.WorkspaceFrame.location = 'Summary.html';

		var xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.3.0");
		xmlhttp.open("GET", displayFullFile, false);
		xmlhttp.send();
		
		// Find the ###QUERYNAME tokens inside the xsl and replace it with the real query name
		// The javascript replace() method wouldnt work for some reason... I had to slice and dice!
		var summaryXSLFile = xmlhttp.responseText;
		var tokenInd = summaryXSLFile.indexOf("###QUERYNAME");
		var mutatedXSLT = summaryXSLFile;

		while(tokenInd >= 0) {
			var beforeToken = mutatedXSLT.substr(0, tokenInd);
			var afterToken = mutatedXSLT.substring(tokenInd+12,  summaryXSLFile.length);
			mutatedXSLT = beforeToken + QueryName + afterToken;
			tokenInd = mutatedXSLT.indexOf("###QUERYNAME");
			}

		// Create a DOM out of the mutated XSLT file
		var xsl = new ActiveXObject("Microsoft.XMLDOM");
		xsl.async = false;
		xsl.loadXML(mutatedXSLT);

		// Transform the returned data with the summary XSL and display it to the user
		top.WorkspaceFrame.document.all('WorkspaceContent').innerHTML = xml.transformNode(xsl);
		}

	//*******************************************************************************

	function RetrieveInstance(RootType, instanceURL) {

		// Load up the query splash screen
		top.WorkspaceFrame.location = '../QuerySplash.html';
		top.SessionDataFrame.queryName = "";

		// Build the URL to call the servlet
		var theURL = "../../servlet/WENETBrowserServlet?act=GetInstance&URL=" + instanceURL;

		// Give the URL for the session data frame to actually retrieve
		top.InstanceDataFrame.instanceRoot = RootType;
		top.InstanceDataFrame.instanceWaiting = true;
		top.InstanceDataFrame.instanceLoaded = false;
		top.InstanceDataFrame.instanceURL = theURL;
		top.InstanceDataFrame.reloadInstance();				      
		}

	//*******************************************************************************
	
	function ViewLastInstance() {
		if(lastRoot != null) { ViewInstance(lastRoot); }
		return;
		}

	//*******************************************************************************

	function ViewRootInstance(RootType) {

		// Load up the appropriate XSLT for that root types instance display
		var xml = top.SessionDataFrame.sessionDocument;
		var displayPath = "session/implementation/theRoots/root[rootName='" + RootType + "']/presentation[type='Detail']/file";
		var displayFile = xml.selectSingleNode(displayPath);
		var displayFullFile = displayFile.text;

		// If its an html file then just load it up and send the user
		if(displayFullFile.indexOf(".html") >= 0 || displayFullFile.indexOf(".htm") >= 0) {
		
			top.InstanceDataFrame.loadInstanceDoc(RootType);			
			top.WorkspaceFrame.location = 'Presentation/' + displayFullFile;
			lastRoot = RootType;
			
		 	top.NavigationFrame.driveTree();
			return;
			}
		}
		
	//*******************************************************************************

	function ViewInstance(RootType) {

		// Load up the appropriate XSLT for that root types instance display
		var xml = top.SessionDataFrame.sessionDocument;
		var displayPath = "session/implementation/theRoots/root[rootName='" + RootType + "']/presentation[type='Detail']/file";
		var displayFile = xml.selectSingleNode(displayPath);
		var displayFullFile = displayFile.text;

		// If its an html file then just load it up and send the user
		if(displayFullFile.indexOf(".html") >= 0 || displayFullFile.indexOf(".htm") >= 0) {
			top.WorkspaceFrame.location = 'Presentation/' + displayFullFile;
			lastRoot = RootType;

		 	top.NavigationFrame.driveTree();
			return;
			}

/* xslt code...
		var xsl = new ActiveXObject("Microsoft.XMLDOM");
		xsl.async = false;
		xsl.load(xsltFullFile);

		// Load up the instance data from the requested URL
		var xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.3.0");
		xmlhttp.open("GET", instanceURL, false);
		xmlhttp.send();
		if(xmlhttp.readyState != 4 || xmlhttp.status != 200) {
			alert("Error retrieving instance data from requested URL. Response code: " + xmlhttp.status);
			return;
			}			
		
		// Get a list of the browser supported namespaces 
		var browserNSAry = new Array();
		var browserPrefixAry = new Array();
		var browserNamespaces = xml.selectNodes("session/browserNamespaces/supportedNamespace");
		for(i=0; i<browserNamespaces.length; i++) {
			browserNSAry.push( browserNamespaces[i].selectSingleNode("location").text );
			browserPrefixAry.push( browserNamespaces[i].selectSingleNode("prefix").text );		
			}

		// Scan through the document and extract the namespaces they are using
		var resultsDoc = xmlhttp.responseText;
		var namespaceAry = new Array();
		var prefixAry = new Array();
		var nsInd = resultsDoc.indexOf("xmlns:");
		while(nsInd != -1) {
			var eqlInd = resultsDoc.indexOf("=\"", nsInd);
			var quoteInd = resultsDoc.indexOf("\"", eqlInd+2);
			namespaceAry.push(resultsDoc.substring(eqlInd+2, quoteInd));
			prefixAry.push(resultsDoc.substring(nsInd, eqlInd));
			}

		// Translate the prefixes to our own
		for(i=0; i<browserNSAry.length; i++) {
			var browserNamespace = browserNSAry[i];
			var browserPrefix = browserPrefixAry[i];
			
			for(i=0; i<namespaceAry.length; i++) {
				if(namespaceAry[i] == browserNamespace) {
					resultsDoc.replace(prefixAry[i] + ":", browserPrefix);
					resultsDoc.replace(prefixAry[i] + "=", browserPrefix);
					}
				}
			}
		
		// Make an XML document out of our now namespace mangled xml text
		var resultsDOM = new ActiveXObject("Microsoft.XMLDOM");
		resultsDOM.async = false;
		resultsDOM.load(resultsDoc);
		
		// Transform the returned data and display it to the user
		top.WorkspaceFrame.document.all('WorkspaceContent').innerHTML = resultsDOM.transformNode(xsl);
*/
		}


	//*******************************************************************************

	function RefreshSession(async) {

		var theURL = '../../servlet/WENETBrowserServlet?act=GetSession';

		top.SessionDataFrame.queryRoot = null;
		top.SessionDataFrame.queryName = null;
		top.SessionDataFrame.queryWaiting = false;
		top.SessionDataFrame.sessionWaiting = true;
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;

		if(async == true) 
			top.SessionDataFrame.reloadSession();
		else
			top.SessionDataFrame.reloadSyncSession();
		}

	//*******************************************************************************

	function ClearSession() {
		var theURL = '../../servlet/WENETBrowserServlet?act=Logout';

		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
		xmlhttp.open("GET", theURL, false);
		xmlhttp.send();

		top.location.href = '/services/';
		}

	//*******************************************************************************

	function AddQuery(RootType, QueryName, XPath, Timeout, Targets) {

		// Load up the query splash screen
		top.WorkspaceFrame.location = '../QuerySplash.html';
		
		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=AddQuery&';
		theURL += "qname=" + QueryName + "&xpath=" + XPath + "&timeout=" + Timeout + "&nspaces=xmlns:j='http://www.it.ojp.gov/jxdm/3.0.2'";

		for(var i=0; i<Targets.length; i++) {
			theURL += '&target=' + Targets[i];
			}

		// Give the URL for the session data frame to actually retrieve
		top.SessionDataFrame.queryRoot = RootType;
		top.SessionDataFrame.queryName = QueryName;
		top.SessionDataFrame.queryWaiting = true;
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSession();		
		}
		
	//*******************************************************************************

	function AddBackgroundQuery(RootType, QueryName, XPath, Timeout, Targets) {

		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=AddQuery&';
		theURL += "qname=" + QueryName + "&xpath=" + XPath + "&timeout=" + Timeout + "&nspaces=xmlns:j='http://www.it.ojp.gov/jxdm/3.0.2'";

		for(var i=0; i<Targets.length; i++) {
			theURL += '&target=' + Targets[i];
			}

		// Give the URL for the session data frame to actually retrieve
		
		// alert("Adding query: " + QueryName + " with root: " + RootType);
		
		top.SessionDataFrame.queryRoot = RootType;
		top.SessionDataFrame.queryName = QueryName;
		top.SessionDataFrame.queryWaiting = true;
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSession();		
		}
				
		
	//*******************************************************************************
		
	function AddWorkspaceItem(Target, URL, ItemName) {

		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=AddWorkspaceResult&';
		theURL += 'target=' + Target + '&URL=' + URL + '&name=' + ItemName;		


		// Give the URL for the session data frame to actually retrieve
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSyncSession();

		if(top.SessionDataFrame.sessionLoaded == false) {
			top.WorkspaceFrame.location.href = 'ErrorPage.html?err=4';			
			return;
			}
		}
	//*******************************************************************************

	function RemoveQuery(qryName) {
		if(!confirm("Are you sure you want to delete query " + qryName)) { return; }
		
		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=RemoveQuery&qname=' + qryName;

		// Load up the instance data from the requested URL
		var xmlhttp = new ActiveXObject("Msxml2.XMLHTTP.3.0");
		xmlhttp.open("GET", theURL, false);
		xmlhttp.send();
		if(xmlhttp.readyState != 4 || xmlhttp.status != 200) {
			alert("Error deleting query from server. Response code: " + xmlhttp.status);
			return;
			}			


		// Now remove the query node from our session DOM, no need to reload completely
		var xml = top.SessionDataFrame.sessionDocument;
		var parent = xml.selectSingleNode("session/results");
		var qry = xml.selectSingleNode("session/results/query[queryName='" + qryName + "']");
		parent.removeChild(qry);

	 	top.NavigationFrame.driveTree();
		}
		
	//*******************************************************************************

	function RemoveWorkspaceItem(ItemName) {

		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=RemoveWorkspaceResult&name' + ItemName;


		// Give the URL for the session data frame to actually retrieve
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSyncSession();

		if(top.SessionDataFrame.sessionLoaded == false) {
			top.WorkspaceFrame.location.href = 'ErrorPage.html?err=4';			
			return;
			}
		}
		
	//*******************************************************************************

	function AddSavedQuery(QueryName, XPath, Timeout, Targets) {

		// Build the query URL
		var theURL = '../../servlet/WENETBrowserServlet?act=AddSavedQuery&';
		theURL += 'qname=' + QueryName + '&xpath=' + XPath;
		for(var i=0; i<Targets.length; i++) {
			theURL += '&target=' + Targets[i];
			}

		// Give the URL for the session data frame to actually retrieve
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSyncSession();

		if(top.SessionDataFrame.sessionLoaded == false) {
			top.WorkspaceFrame.location.href = 'ErrorPage.html?err=4';			
			return;
			}
		}
		
	//*******************************************************************************

	function RemoveSavedQuery(QueryName) {

		// Build the query URL
		var theURL = 'servlet/WENETBrowserServlet?act=RemoveSavedQuery&qname' + QueryName;

		// Give the URL for the session data frame to actually retrieve
		top.SessionDataFrame.sessionLoaded = false;
		top.SessionDataFrame.sessionURL = theURL;
		top.SessionDataFrame.reloadSyncSession();

		if(top.SessionDataFrame.sessionLoaded == false) {
			top.WorkspaceFrame.location.href = 'ErrorPage.html?err=4';			
			return;
			}
		}
		
	//*****************************************************************************
	
	function QueryForm(DisplayFile) {
		top.WorkspaceFrame.location = DisplayFile; 
		}
	 
	 function findPersonPhoto(objImg, strGivenName, strSurName, strDOB) {
	 	if (strGivenName.indexOf("!") < 0) {
	 		if (objImg.src.indexOf("webservice") < 0) {
			 	//alert(strSurName + ", " + strGivenName + " - " + strDOB);
			 	aryDOB = strDOB.split("-");
			 	strDOB = aryDOB[1] + "/" + aryDOB[2] + "/" + aryDOB[0];
			 	//strDOB = prompt("Fix DOB:", strDOB);
			 	var theURL = "/mugshotwebservice/getimage?size=full&perspective=front&firstname=" + strGivenName + "&lastname=" + strSurName + "&dob=" + strDOB;
			 	//theURL = prompt("The MUGHSOT URL:", theURL);
			 	objImg.src = theURL;
			 }
		 }
	 }