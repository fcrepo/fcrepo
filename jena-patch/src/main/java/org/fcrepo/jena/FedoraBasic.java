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

import java.io.PrintWriter;

import org.apache.jena.rdf.model.* ;
import org.apache.jena.rdf.model.impl.Util;

/**
 * @author awoods
 * @since 2017/01/03
 */
public class FedoraBasic extends CopyOfBasic {

    protected void writeLiteral( Literal l, PrintWriter writer ) {
        String lang = l.getLanguage();
        String form = l.getLexicalForm();
        if (Util.isLangString(l)) {
            writer.print(" xml:lang=" + attributeQuoted( lang ));
        } else if (l.isWellFormedXML() && !blockLiterals) {
            // RDF XML Literals inline.
            writer.print(" " + rdfAt("parseType") + "=" + attributeQuoted( "Literal" )+">");
            writer.print( form );
            return ;
        } else {
            // Datatype (if not xsd:string and RDF 1.1)
            String dt = l.getDatatypeURI();
            // NOTE, Fedora change: Remove condition
//            if ( ! Util.isSimpleString(l) )
                writer.print( " " + rdfAt( "datatype" ) + "=" + substitutedAttribute( dt ) );
        }
        // Content.
        writer.print(">");
        writer.print( Util.substituteEntitiesInElementContent( form ) );
    }

}
