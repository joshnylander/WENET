<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">

SUMMARY PAGE <br/>

<xsl:for-each select="session/results/query[contains(queryName, '###QUERYNAME')]">
<table border="1" width="75%">

	<xsl:for-each select="resultList">

		<xsl:variable name="SP" select="serviceURI" />

	
		<xsl:for-each select="result">
		<tr>
			<td>
				<xsl:value-of select="$SP" />
			</td>
			<td>
				<xsl:value-of select="instanceURL" />
			</td>
		</tr>
		</xsl:for-each>
			
	</xsl:for-each>
	
</table>
</xsl:for-each>

<br/><br/>

------ its alive! --------- QUERYNAME: ###QUERYNAME<br/><br/>

<input type="button" value="Load Instance" onClick="ViewInstance('Person', 'http://www.yahoo.com');" />

</xsl:template>
</xsl:stylesheet>