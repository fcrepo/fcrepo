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

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.StreamOps;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.writer.TurtleWriterBase;
import org.apache.jena.sparql.util.Context;

/**
 * @author awoods
 * @since 2017/01/03
 */
public class FedoraTurtleWriterBlocks extends TurtleWriterBase {

    @Override
    protected void output(final IndentedWriter out,
                          final Graph graph,
                          final PrefixMap prefixMap,
                          final String baseURI,
                          final Context context) {
        final StreamRDF dest = new FedoraWriterStreamRDFBlocks(out) ;
        dest.start() ;
        dest.base(baseURI) ;
        StreamOps.sendGraphToStream(graph, dest, prefixMap) ;
        dest.finish() ;
    }

}
