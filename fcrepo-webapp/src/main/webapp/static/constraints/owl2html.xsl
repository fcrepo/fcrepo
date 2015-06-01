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
        <xsl:if test="not(/rdf:RDF/owl:Ontology/owl:versionInfo = '')">
          <div class="version">Version: <xsl:value-of select="/rdf:RDF/owl:Ontology/owl:versionInfo"/></div>
        </xsl:if>
        <xsl:if test="not($priorVersion = '')">
          <div class="version">Prior version: <a>
            <xsl:attribute name="href"><xsl:value-of select="$priorVersion"/></xsl:attribute>
            <xsl:value-of select="$priorVersion"/></a>
          </div>
        </xsl:if>

        <div class="table-of-contents">
          <h2>Table of Contents</h2>
          <xsl:if test="/rdf:RDF/owl:Class">
            <h3>Classes</h3>
            <xsl:for-each select="/rdf:RDF/owl:Class">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="link"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:NamedIndividual">
            <h3>Named Individuals</h3>
            <xsl:for-each select="/rdf:RDF/owl:NamedIndividual">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="link"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:ObjectProperty">
            <h3>Object Properties</h3>
            <xsl:for-each select="/rdf:RDF/owl:ObjectProperty">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="link"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:DatatypeProperty">
            <h3>Datatype Properties</h3>
            <xsl:for-each select="/rdf:RDF/owl:DatatypeProperty">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="link"/>
            </xsl:for-each>
          </xsl:if>
        </div>

        <div class="contents">
          <h2>Entity Definitions</h2>
          <xsl:if test="/rdf:RDF/owl:Class">
            <h3>Classes</h3>
            <xsl:for-each select="/rdf:RDF/owl:Class">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="description"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:NamedIndividual">
            <h3>Named Individuals</h3>
            <xsl:for-each select="/rdf:RDF/owl:NamedIndividual">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="description"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:ObjectProperty">
            <h3>Object Properties</h3>
            <xsl:for-each select="/rdf:RDF/owl:ObjectProperty">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="description"/>
            </xsl:for-each>
          </xsl:if>

          <xsl:if test="/rdf:RDF/owl:DatatypeProperty">
            <h3>Datatype Properties</h3>
            <xsl:for-each select="/rdf:RDF/owl:DatatypeProperty">
              <xsl:sort select="@rdf:about"/>
              <xsl:call-template name="description"/>
            </xsl:for-each>
          </xsl:if>
        </div>
      </body>
    </html>
  </xsl:template>
  <xsl:template name="link">
    <xsl:variable name="id">
      <xsl:choose>
        <xsl:when test="@rdf:about and contains(@rdf:about,$about)">
          <xsl:value-of select="substring-after(@rdf:about,$about)"/>
        </xsl:when>
        <xsl:when test="@rdf:resource and contains(@rdf:resource,$about)">
          <xsl:value-of select="substring-after(@rdf:resource,$about)"/>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$id != ''">
        <a href="#{$id}"><xsl:value-of select="$id"/></a>
      </xsl:when>
      <xsl:when test="contains(@rdf:resource,'http://www.w3.org/2001/XMLSchema#')">
        <a href="{@rdf:resource}">xsd:<xsl:value-of select="substring-after(@rdf:resource,'#')"/></a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@rdf:resource"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text> </xsl:text>
  </xsl:template>
  <xsl:template name="description">
    <xsl:variable name="id" select="substring-after(@rdf:about,$about)"/>
    <div id="{$id}">
      <h4><xsl:value-of select="$id"/></h4>
      <xsl:if test="rdfs:label">
        <div class="label"><xsl:value-of select="rdfs:label"/></div>
      </xsl:if>
      <xsl:for-each select="rdfs:comment">
        <div class="comment"><xsl:value-of select="."/></div>
      </xsl:for-each>
      <div class="property">type:
        <xsl:value-of select="substring-after(name(),'owl:')"/>
      </div>
      <xsl:if test="rdfs:subClassOf">
        <div class="property">subclass of:
          <xsl:for-each select="rdfs:subClassOf">
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
      <xsl:if test="//*[contains(rdfs:domain/@rdf:resource,$id)]|//*[contains(rdfs:range/@rdf:resource,$id)]">
        <div class="property">used with:
          <xsl:for-each select="//*[contains(rdfs:domain/@rdf:resource,$id)]|//*[contains(rdfs:range/@rdf:resource,$id)]">
            <xsl:sort select="@rdf:about"/>
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
      <xsl:if test="//*[rdf:type/@rdf:resource=concat($about,$id)]">
        <div class="property">instances:
          <xsl:for-each select="//*[rdf:type/@rdf:resource=concat($about,$id)]">
            <xsl:sort select="@rdf:about"/>
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
      <xsl:if test="rdf:type">
        <div class="property">rdf:type:
          <xsl:for-each select="rdf:type">
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
      <xsl:if test="rdfs:domain">
        <div class="property">
          domain:
          <xsl:for-each select="rdfs:domain">
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
      <xsl:if test="rdfs:range">
        <div class="property">
          range:
          <xsl:for-each select="rdfs:range">
            <xsl:call-template name="link"/>
          </xsl:for-each>
        </div>
      </xsl:if>
    </div>
  </xsl:template>
</xsl:stylesheet>
