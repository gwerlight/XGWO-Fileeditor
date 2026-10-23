<?xml version="1.0" encoding="UTF-8"?>
<!-- Eigene Minimal-Regeln nur für Tests: Wie ein vorkompiliertes Schematron liefert das Stylesheet SVRL.
     TEST-0001 (Fehler):    Jeder Betrag muss größer als 0 sein.
     TEST-0002 (Warnung):   Ein Rabatt über 50 Prozent ist auffällig.
     TEST-0003 (Hinweis):   Mehr als drei Positionen. -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                xmlns:t="urn:fileedit:test:1">

    <xsl:template match="/">
        <svrl:schematron-output>
            <xsl:for-each select="/t:rechnung/t:position">
                <xsl:variable name="n" select="position()"/>
                <xsl:if test="number(t:betrag) le 0">
                    <svrl:failed-assert id="TEST-0001" role="error" test="number(t:betrag) gt 0"
                                        location="/t:rechnung/t:position[{$n}]/t:betrag">
                        <svrl:text>[TEST-0001] Der Betrag muss größer als 0 sein.</svrl:text>
                    </svrl:failed-assert>
                </xsl:if>
                <xsl:if test="number(t:rabatt) gt 50">
                    <svrl:successful-report id="TEST-0002" role="warning" test="number(t:rabatt) gt 50"
                                            location="/t:rechnung/t:position[{$n}]/t:rabatt">
                        <svrl:text>[TEST-0002] Ein Rabatt über 50 Prozent ist auffällig.</svrl:text>
                    </svrl:successful-report>
                </xsl:if>
            </xsl:for-each>
            <xsl:if test="count(/t:rechnung/t:position) gt 3">
                <svrl:successful-report id="TEST-0003" role="info" test="count(t:position) gt 3"
                                        location="/t:rechnung">
                    <svrl:text>[TEST-0003] Die Rechnung hat mehr als drei Positionen.</svrl:text>
                </svrl:successful-report>
            </xsl:if>
        </svrl:schematron-output>
    </xsl:template>
</xsl:stylesheet>
