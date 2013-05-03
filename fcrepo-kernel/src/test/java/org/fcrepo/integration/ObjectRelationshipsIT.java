package org.fcrepo.integration;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ObjectRelationshipsIT {


	protected Logger logger;

	@Before
	public void setLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testSparqlUpdate() throws IOException {
		final Model model = ModelFactory.createDefaultModel();

		RDFReader arp = model.getReader();

		String str =
							 "<rdf:RDF\n" +
							 "\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
							 "\n" +
							 "<rdf:Description rdf:about=\"info:fedora/druid:xz144dc9157\">\n" +
							 "\t<lastModifiedDate xmlns=\"info:fedora/fedora-system:def/view#\" rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2013-04-24T05:28:36.664Z</lastModifiedDate>\n" +
							 "\t<disseminates xmlns=\"info:fedora/fedora-system:def/view#\" rdf:resource=\"info:fedora/druid:xz144dc9157/technicalMetadata\"/>\n" +
							 "\t<disseminates xmlns=\"info:fedora/fedora-system:def/view#\" rdf:resource=\"info:fedora/druid:xz144dc9157/provenanceMetadata\"/>\n" +
							 "</rdf:Description>\n" +
							 "\n" +
							 "</rdf:RDF>";

		InputStream in = new ByteArrayInputStream(str.getBytes());

		arp.read(model, in, "info:triples");

		in.close();

		GraphStore graphStore = GraphStoreFactory.create(model);
		UpdateAction.parseExecute("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
										   "INSERT { <http://example/egbook> dc:title  \"This is an example title\" } WHERE {}", graphStore);

		final ResIterator iterator = model.listSubjects();

		logger.info("Subjects:");
		while(iterator.hasNext()) {
			logger.info(iterator.next().toString());

		}

	}
}
