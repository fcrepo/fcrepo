<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:owl="http://www.w3.org/2002/07/owl#"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
  <xsl:output method="html"/>
  <xsl:variable name="title" select="/rdf:RDF/owl:Ontology/rdfs:label"/>
  <xsl:variable name="about" select="/rdf:RDF/owl:Ontology/@rdf:about"/>
  <xsl:variable name="priorVersion" select="/rdf:RDF/owl:Ontology/owl:priorVersion/@rdf:resource"/>
  <xsl:template match="/rdf:RDF">
    <html>
      <head>
        <title><xsl:value-of select="$title"/></title>
        <style>
          h4 { margin-bottom: 0.25em; }
          body { font-family: sans-serif; }
          .about { font-family: monospace; margin-left: 1em; }
          .label { margin-left: 1em; font-style:italic; }
          .comment { margin-left: 1em; }
          .property { margin-left: 1em; }
          .version { margin-left: 1em; }
        </style>
      </head>
      <body>
        <h1><xsl:value-of select="$title"/></h1>
        <div class="about"><xsl:value-of select="$about"/></div>
        <xsl:for-each select="/rdf:RDF/owl:Ontology/rdfs:comment">
          <div class="comment"><xsl:value-of select="."/></div>
        </xsl:for-each>
        <xsl:if test="/rdf:RDF/owl:Ontology/owl:versionInfo and not(/rdf:RDF/owl:Ontology/owl:versionInfo = '')">
          <div class="version">Version: <xsl:value-of select="/rdf:RDF/owl:Ontology/owl:versionInfo"/></div>
        </xsl:if>
        <xsl:if test="/rdf:RDF/owl:Ontology/owl:priorVersion and not(/rdf:RDF/owl:Ontology/owl:priorVersion = '')">
          <div class="version">Prior version: <a>
            <xsl:attribute name="href"><xsl:value-of select="$priorVersion"/></xsl:attribute>
            <xsl:value-of select="$priorVersion"/></a>
          </div>
        </xsl:if>

      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
