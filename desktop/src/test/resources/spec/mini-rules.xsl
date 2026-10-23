<?xml version="1.0" encoding="UTF-8"?>
<!-- Eigene Minimal-Regel nur für Tests: wie ein vorkompiliertes Schematron liefert sie SVRL.
     Regel TEST-0001: Jeder Betrag muss größer als 0 sein. -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                xmlns:t="urn:fileedit:test:1.0">
    <xsl:template match="/">
        <svrl:schematron-output>
            <xsl:for-each select="/t:rechnung/t:betrag[number(.) le 0]">
                <svrl:failed-assert id="TEST-0001" test="number(.) gt 0"
                                    location="/t:rechnung/t:betrag[{count(preceding-sibling::t:betrag) + 1}]">
                    <svrl:text>Der Betrag muss größer als 0 sein.</svrl:text>
                </svrl:failed-assert>
            </xsl:for-each>
        </svrl:schematron-output>
    </xsl:template>
</xsl:stylesheet>
