/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.jena;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.out.NodeToLabel;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotChars;

/**
 * @author awoods
 * @since 2017/01/03
 */
public class FedoraNodeFormatterTTL extends NodeFormatterTTL {

    /**
     * @param baseIRI
     * @param prefixMap
     * @param nodeToLabel
     */
    public FedoraNodeFormatterTTL(final String baseIRI, final PrefixMap prefixMap, final NodeToLabel nodeToLabel) {
        super(baseIRI, prefixMap, nodeToLabel);
    }

    /**
     * @param w
     * @param n
     */
    @Override
    public void formatLiteral(final AWriter w, final Node n) {
        final RDFDatatype dt = n.getLiteralDatatype();
        final String lang = n.getLiteralLanguage();
        final String lex = n.getLiteralLexicalForm();

        if (lang != null && !lang.equals("")) {
            formatLitLang(w, lex, lang);
        } else if (dt == null) {
            // RDF 1.0, simple literal.
            formatLitString(w, lex);
            // NOTE, Fedora change: remove condition
//        } else if ( JenaRuntime.isRDF11 && dt.equals(XSDDatatype.XSDstring) ) {
//            // RDF 1.1, xsd:string - output as short string.
//            formatLitString(w, lex) ;
        } else {
            // Datatype, no language tag, not short string.
            formatLitDT(w, lex, dt.getURI());
        }
    }

    // NOTE, Fedora change: private -> protected
    protected static final String dtDecimal = XSDDatatype.XSDdecimal.getURI() ;
    protected static final String dtInteger = XSDDatatype.XSDinteger.getURI() ;
    protected static final String dtDouble  = XSDDatatype.XSDdouble.getURI() ;

    /**
     * Write in a short form, e.g. integer.
     *
     * @return True if a short form was output else false.
     */
    protected boolean writeLiteralAbbreviated(final AWriter w, final String lex, final String datatypeURI) {
        if (dtDecimal.equals(datatypeURI)) {
            if (validDecimal(lex)) {
                w.print(lex);
                return true;
            }
        } else if (dtInteger.equals(datatypeURI)) {
            if (validInteger(lex)) {
                w.print(lex);
                return true;
            }
        } else if (dtDouble.equals(datatypeURI)) {
            if (validDouble(lex)) {
                w.print(lex);
                return true;
            }
            // NOTE, Fedora change: Remove condition
//        } else if ( dtBoolean.equals(datatypeURI) ) {
//            // We leave "0" and "1" as-is assumign that if written like that,
//            // there was a reason.
//            if ( lex.equals("true") || lex.equals("false") ) {
//                w.print(lex) ;
//                return true ;
//            }
        }
        return false;
    }

    //********************************************************************
    // NOTE, Fedora: Below are added from NodeFormatterTTL without change.
    //********************************************************************

    private static boolean validInteger(String lex) {
        int N = lex.length() ;
        if ( N == 0 )
            return false ;
        int idx = 0 ;

        idx = skipSign(lex, idx) ;
        idx = skipDigits(lex, idx) ;
        return (idx == N) ;
    }

    private static boolean validDecimal(String lex) {
        // case : In N3, "." illegal, as is "+." and -." but legal in Turtle.
        int N = lex.length() ;
        if ( N <= 1 )
            return false ;
        int idx = 0 ;

        idx = skipSign(lex, idx) ;
        idx = skipDigits(lex, idx) ; // Maybe none.

        // DOT required.
        if ( idx >= N )
            return false ;

        char ch = lex.charAt(idx) ;
        if ( ch != '.' )
            return false ;
        idx++ ;
        // Digit required.
        if ( idx >= N )
            return false ;
        idx = skipDigits(lex, idx) ;
        return (idx == N) ;
    }

    private static boolean validDouble(String lex) {
        int N = lex.length() ;
        if ( N == 0 )
            return false ;
        int idx = 0 ;

        // Decimal part (except 12. is legal)

        idx = skipSign(lex, idx) ;

        int idx2 = skipDigits(lex, idx) ;
        boolean initialDigits = (idx != idx2) ;
        idx = idx2 ;
        // Exponent required.
        if ( idx >= N )
            return false ;
        char ch = lex.charAt(idx) ;
        if ( ch == '.' ) {
            idx++ ;
            if ( idx >= N )
                return false ;
            idx2 = skipDigits(lex, idx) ;
            boolean trailingDigits = (idx != idx2) ;
            idx = idx2 ;
            if ( idx >= N )
                return false ;
            if ( !initialDigits && !trailingDigits )
                return false ;
        }
        // "e" or "E"
        ch = lex.charAt(idx) ;
        if ( ch != 'e' && ch != 'E' )
            return false ;
        idx++ ;
        if ( idx >= N )
            return false ;
        idx = skipSign(lex, idx) ;
        if ( idx >= N )
            return false ; // At least one digit.
        idx = skipDigits(lex, idx) ;
        return (idx == N) ;
    }

    /**
     * Skip digits [0-9] and return the index just after the digits, which may
     * be beyond the length of the string. May skip zero.
     */
    private static int skipDigits(String str, int start) {
        int N = str.length() ;
        for (int i = start; i < N; i++) {
            char ch = str.charAt(i) ;
            if ( !RiotChars.isDigit(ch) )
                return i ;
        }
        return N ;
    }

    /** Skip any plus or minus */
    private static int skipSign(String str, int idx) {
        int N = str.length() ;
        char ch = str.charAt(idx) ;
        if ( ch == '+' || ch == '-' )
            return idx + 1 ;
        return idx ;
    }

}
