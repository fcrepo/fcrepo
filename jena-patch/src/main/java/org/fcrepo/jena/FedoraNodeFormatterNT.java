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
import org.apache.jena.atlas.lib.CharSpace;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.out.NodeFormatterNT;

/**
 * @author awoods
 * @since 2017/01/03
 */
public class FedoraNodeFormatterNT extends NodeFormatterNT {

    /**
     *
     * @param charSpace
     */
    public FedoraNodeFormatterNT(final CharSpace charSpace) {
        super(charSpace);
    }

    /**
     *
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
            // NOTE, Fedora: Remove condition
//        } else if ( JenaRuntime.isRDF11 && dt.equals(XSDDatatype.XSDstring) ) {
//            // RDF 1.1, xsd:string - output as short string.
//            formatLitString(w, lex) ;
        } else {
            // Datatype, no language tag, not short string.
            formatLitDT(w, lex, dt.getURI());
        }
    }
}
