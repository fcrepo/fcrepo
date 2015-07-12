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
public class GeonameIT extends AbstractIntegrationRdfIT {
    @Test
    public void testRoundtripGeonamesRdf() {
        final String s = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix cc: <http://creativecommons.org/ns#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
                "@prefix gn: <http://www.geonames.org/ontology#> .\n" +
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#> .\n" +
                "\n" +
                "<>\n" +
                "    gn:alternateName \"Aebura\"@la, \"Ambrun\"@oc, \"Eburodunum\"@la ;\n" +
                "    gn:countryCode \"FR\" ;\n" +
                "    gn:featureClass gn:P ;\n" +
                "    gn:featureCode <http://www.geonames.org/ontology#P.PPL> ;\n" +
                "    gn:locationMap <http://www.geonames.org/3020251/embrun.html> ;\n" +
                "    gn:name \"Embrun\" ;\n" +
                "    gn:nearbyFeatures <http://sws.geonames.org/3020251/nearby.rdf> ;\n" +
                "    gn:parentADM1 <http://sws.geonames.org/2985244/> ;\n" +
                "    gn:parentADM2 <http://sws.geonames.org/3013738/> ;\n" +
                "    gn:parentADM3 <http://sws.geonames.org/3016701/> ;\n" +
                "    gn:parentADM4 <http://sws.geonames.org/6446638/> ;\n" +
                "    gn:parentCountry <http://sws.geonames.org/3017382/> ;\n" +
                "    gn:parentFeature <http://sws.geonames.org/6446638/> ;\n" +
                "    gn:population \"7069\" ;\n" +
                "    gn:postalCode \"05200\", \"05201 CEDEX\", \"05202 CEDEX\", \"05208 CEDEX\", \"05209 CEDEX\" ;\n" +
                "    gn:wikipediaArticle <http://fr.wikipedia.org/wiki/Embrun_%28Hautes-Alpes%29> ;\n" +
                "    a gn:Feature ;\n" +
                "    rdfs:isDefinedBy <#about.rdf> ;\n" +
                "    wgs84_pos:lat \"44.56387\" ;\n" +
                "    wgs84_pos:long \"6.49526\" .\n" +
                "\n" +
                "<#about.rdf>\n" +
                "    cc:attributionName \"GeoNames\"^^<http://www.w3.org/2001/XMLSchema#string> ;\n" +
                "    cc:attributionURL <http://sws.geonames.org/3020251/> ;\n" +
                "    cc:license <http://creativecommons.org/licenses/by/3.0/> ;\n" +
                "    dcterms:created \"2006-01-15\"^^<http://www.w3.org/2001/XMLSchema#date> ;\n" +
                "    dcterms:modified \"2012-03-30\"^^<http://www.w3.org/2001/XMLSchema#date> ;\n" +
                "    a foaf:Document ;\n" +
                "    foaf:primaryTopic <http://sws.geonames.org/3020251/> .";
        createLDPRSAndCheckResponse(getRandomUniqueId(), s);

    }
}
