/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.rdf;

import org.junit.Test;

/**
 * @author cabeer
 */
public class ModsRdfIT extends AbstractIntegrationRdfIT {
    @Test
    public void testRoundtripModsRDF() {
        // converted RDF-XML to TTL from
        // https://raw.githubusercontent.com/blunalucero/MODS-RDF/master/Sample_record_Academic_Commons_MODS_RDF.rdf
        // and fixed document URI and relative URI reference to use null-relative and hash uris
        final String s = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix identifier: <http://id.loc.gov/vocabulary/identifier/> .\n" +
                "@prefix relator: <http://id.loc.gov/vocabulary/relator/> .\n" +
                "@prefix note: <http://id.loc.gov/vocabulary/note/> .\n" +
                "@prefix xs: <http://www.w3.org/2001/XMLSchema> .\n" +
                "@prefix abstract: <http://id.loc.gov/vocabulary/abstract/> .\n" +
                "@prefix access: <http://id.loc.gov/vocabulary/access/> .\n" +
                "@prefix fo: <http://www.w3.org/1999/XSL/Format> .\n" +
                "@prefix class: <http://id.loc.gov/vocabulary/class/> .\n" +
                "@prefix fn: <http://www.w3.org/2005/xpath-functions> .\n" +
                "@prefix mods: <http://www.loc.gov/mods/v3> .\n" +
                "@prefix ri: <http://id.loc.gov/ontologies/RecordInfo#> .\n" +
                "@prefix modsrdf: <http://www.loc.gov/mods/rdf/v1#> .\n" +
                "@prefix madsrdf: <http://www.loc.gov/mads/rdf/v1#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "\n" +
                "<>\n" +
                "    identifier:hdl \"http://hdl.handle.net/10022/AC:P:11905\" ;\n" +
                "    modsrdf:LanguageOfResource \"English\" ;\n" +
                "    modsrdf:abstract \"A mechanism had been recently proposed to show how an....\" ;\n" +
                "    modsrdf:administrativeMedatata [\n" +
                "        ri:languageOfCataloging \"eng\" ;\n" +
                "        ri:recordContentSource \"NNC\" ;\n" +
                "        ri:recordIdentifier \"5890\" ;\n" +
                "        ri:recordInfoRecordChangeDate \"2012-08-01 14:40:32 -0400\"^^<xsd:date> ;\n" +
                "        ri:recordInfoRecordCreationDate \"2011-12-06 10:27:50 -0500\"^^<xsd:date> ;\n" +
                "        a ri:AdministrativeMedatata\n" +
                "    ] ;\n" +
                "    modsrdf:genre [\n" +
                "        madsrdf:elementList ([\n" +
                "                madsrdf:elementValue \"Articles\" ;\n" +
                "                a madsrdf:GenreFormElement\n" +
                "            ]\n" +
                "        ) ;\n" +
                "        a madsrdf:GenreForm ;\n" +
                "        rdfs:label \"Articles\"\n" +
                "    ] ;\n" +
                "    modsrdf:locationOfResource [\n" +
                "        modsrdf:locationPhysicalLocation \"NNC\" ;\n" +
                "        a modsrdf:Location\n" +
                "    ] ;\n" +
                "    modsrdf:name <#d1e24>, <#d1e42>, <#d1e60>, <#d1e9> ;\n" +
                "    modsrdf:resourceDateIssued \"1992\"^^<xsd:date> ;\n" +
                "    modsrdf:roleRelationship [\n" +
                "        modsrdf:roleRelationshipName <#d1e24> ;\n" +
                "        modsrdf:roleRelationshipRole \"author\" ;\n" +
                "        a modsrdf:RoleRelationship\n" +
                "    ], [\n" +
                "        modsrdf:roleRelationshipName <#d1e42> ;\n" +
                "        modsrdf:roleRelationshipRole \"author\" ;\n" +
                "        a modsrdf:RoleRelationship\n" +
                "    ], [\n" +
                "        modsrdf:roleRelationshipName <#d1e60> ;\n" +
                "        modsrdf:roleRelationshipRole \"originator\" ;\n" +
                "        a modsrdf:RoleRelationship\n" +
                "    ], [\n" +
                "        modsrdf:roleRelationshipName <#d1e9> ;\n" +
                "        modsrdf:roleRelationshipRole \"author\" ;\n" +
                "        a modsrdf:RoleRelationship\n" +
                "    ] ;\n" +
                "    modsrdf:subjectComplex [\n" +
                "        madsrdf:componentList ([\n" +
                "                madsrdf:elementList ([\n" +
                "                        madsrdf:elementValue \"Geophysics\" ;\n" +
                "                        a madsrdf:TopicElement\n" +
                "                    ]\n" +
                "                ) ;\n" +
                "                a madsrdf:Topic ;\n" +
                "                rdfs:label \"Geophysics\"\n" +
                "            ]\n" +
                "        ) ;\n" +
                "        a madsrdf:ComplexSubject ;\n" +
                "        rdfs:label \"Geophysics. \"\n" +
                "    ] ;\n" +
                "    modsrdf:titlePrincipal [\n" +
                "        madsrdf:elementList ([\n" +
                "                madsrdf:elementValue \"A detailed chronology of the Australasian impact event, " +
                "the Brunhes-Matuyama geomagnetic polarity reversal, and global climate change\" ;\n" +
                "                a madsrdf:mainTitleElement\n" +
                "            ]\n" +
                "        ) ;\n" +
                "        a madsrdf:Title ;\n" +
                "        rdfs:label \"A detailed chronology of the Australasian impact event, the Brunhes-Matuyama " +
                "geomagnetic polarity reversal, and global climate change\"\n" +
                "    ] ;\n" +
                "    a <http://id.loc.gov/vocabulary/resourceType#Text>, modsrdf:ModsResource .\n" +
                "\n" +
                "<#d1e24>\n" +
                "    madsrdf:elementList ([\n" +
                "            madsrdf:elementValue \"Kent\" ;\n" +
                "            a madsrdf:FamilyNameElement\n" +
                "        ]\n" +
                "        [\n" +
                "            madsrdf:elementValue \"Dennis V.\" ;\n" +
                "            a madsrdf:GivenNameElement\n" +
                "        ]\n" +
                "    ) ;\n" +
                "    a madsrdf:PersonalName ;\n" +
                "    rdfs:label \"  Dennis V.  Kent    \" .\n" +
                "\n" +
                "<#d1e42>\n" +
                "    madsrdf:elementList ([\n" +
                "            madsrdf:elementValue \"Mello\" ;\n" +
                "            a madsrdf:FamilyNameElement\n" +
                "        ]\n" +
                "        [\n" +
                "            madsrdf:elementValue \"Gilberto A.\" ;\n" +
                "            a madsrdf:GivenNameElement\n" +
                "        ]\n" +
                "    ) ;\n" +
                "    a madsrdf:PersonalName ;\n" +
                "    rdfs:label \"  Gilberto A.  Mello    \" .\n" +
                "\n" +
                "<#d1e60>\n" +
                "    madsrdf:elementList ([\n" +
                "            madsrdf:elementValue \"Columbia University. Lamont-Doherty Earth Observatory\" ;\n" +
                "            a madsrdf:FullNameElement\n" +
                "        ]\n" +
                "    ) ;\n" +
                "    a madsrdf:CorporateName ;\n" +
                "    rdfs:label \"Columbia University. Lamont-Doherty Earth Observatory\" .\n" +
                "\n" +
                "<#d1e9>\n" +
                "    madsrdf:elementList ([\n" +
                "            madsrdf:elementValue \"Schneider\" ;\n" +
                "            a madsrdf:FamilyNameElement\n" +
                "        ]\n" +
                "        [\n" +
                "            madsrdf:elementValue \"David A.\" ;\n" +
                "            a madsrdf:GivenNameElement\n" +
                "        ]\n" +
                "    ) ;\n" +
                "    a madsrdf:PersonalName ;\n" +
                "    rdfs:label \"  David A.  Schneider    \" .";

        createLDPRSAndCheckResponse(getRandomUniqueId(), s);
    }
}
