<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fedora-access="http://www.fedora.info/definitions/1/0/access/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.w3.org/1999/XSL/Transform http://www.w3.org/2007/schema-for-xslt20.xsd
    http://www.w3.org/1999/xhtml http://www.w3.org/2002/08/xhtml/xhtml1-transitional.xsd"
    exclude-result-prefixes="xs" version="2.0">

    <!-- transforms an objectProfile into an HTML page -->

    <xsl:template match="/">
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <title>
                    <xsl:value-of select="/fedora-access:objectProfile/@pid"/>
                </title>
            </head>
            <body>
                <h3>Fedora object <xsl:value-of
                        select="/fedora-access:objectProfile/fedora-access:objLabel"/></h3>
                <div>
                    <span></span>
                </div>
            </body>

        </html>

    </xsl:template>

</xsl:stylesheet>
