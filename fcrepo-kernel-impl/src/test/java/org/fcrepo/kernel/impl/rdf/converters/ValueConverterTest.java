/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.rdf.converters;

import com.google.common.base.Converter;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.infinispan.schematic.document.ParsingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertThat;

/**
 * @author cabeer
 */
@RunWith(Parameterized.class)
public class ValueConverterTest {

    static Repository repo;

    private Session session;

    private Converter<Resource, FedoraResource> subjects;
    private Converter<Value, RDFNode> testObj;

    @Parameterized.Parameter(value = 0)
    public RDFNode externalValue;

    @Test
    public void test() throws IOException {
        assertThat(testObj.convert(testObj.reverse().convert(externalValue)), sameValueAs(externalValue));
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ResourceFactory.createTypedLiteral("x")},
                {ResourceFactory.createTypedLiteral(0)},
                {ResourceFactory.createTypedLiteral(1L)},
                {ResourceFactory.createTypedLiteral(new BigDecimal("2.123"))},
                {ResourceFactory.createTypedLiteral((double)3)},
                {ResourceFactory.createTypedLiteral(3.1415)},
                {ResourceFactory.createTypedLiteral(Calendar.getInstance())},
                {ResourceFactory.createTypedLiteral((byte)1)},
                {ResourceFactory.createTypedLiteral((short)42)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDunsignedByte)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDunsignedShort)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDunsignedInt)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDunsignedLong)},
                {ResourceFactory.createTypedLiteral("-255", XSDDatatype.XSDnonPositiveInteger)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDnonNegativeInteger)},
                {ResourceFactory.createTypedLiteral("255", XSDDatatype.XSDpositiveInteger)},
                {ResourceFactory.createTypedLiteral("-255", XSDDatatype.XSDnegativeInteger)},
                {ResourceFactory.createTypedLiteral(true)},
                {ResourceFactory.createResource("info:x")},
                {ResourceFactory.createTypedLiteral("2014-10-24T01:23:45Z", XSDDatatype.XSDdateTime)},
                {ResourceFactory.createTypedLiteral("some-invalid-data", XSDDatatype.XSDdateTime)},
                // Types outside the JCR type system boundaries:
                {ResourceFactory.createTypedLiteral("2014-10-24", XSDDatatype.XSDdate)},
                {ResourceFactory.createTypedLiteral("01:02:03", XSDDatatype.XSDtime)},
                {ResourceFactory.createTypedLiteral("---31", XSDDatatype.XSDgDay)},
                {ResourceFactory.createTypedLiteral("--10", XSDDatatype.XSDgMonth)},
                {ResourceFactory.createTypedLiteral("--02-29", XSDDatatype.XSDgMonthDay)},
                {ResourceFactory.createTypedLiteral("2001", XSDDatatype.XSDgYear)},
                {ResourceFactory.createTypedLiteral("ABCDEF", XSDDatatype.XSDhexBinary)},
                {ResourceFactory.createTypedLiteral("eHl6", XSDDatatype.XSDbase64Binary)},
                {ResourceFactory.createTypedLiteral("eHl6", XSDDatatype.XSDnormalizedString)},
                {ResourceFactory.createTypedLiteral("some:uri", XSDDatatype.XSDanyURI)},
                {ResourceFactory.createTypedLiteral("tokenize this", XSDDatatype.XSDtoken)},
                {ResourceFactory.createTypedLiteral("name", XSDDatatype.XSDName)},
                {ResourceFactory.createTypedLiteral("qname", XSDDatatype.XSDQName)},
                {ResourceFactory.createTypedLiteral("en-us", XSDDatatype.XSDlanguage)},
                {ResourceFactory.createTypedLiteral("name", XSDDatatype.XSDNMTOKEN)},
                {ResourceFactory.createTypedLiteral("some-id", XSDDatatype.XSDID)},
                {ResourceFactory.createTypedLiteral("ncname", XSDDatatype.XSDNCName)},
                {ResourceFactory.createTypedLiteral(2.0123f)},
                {ResourceFactory.createLangLiteral("xyz", "de")},
                // Problems
                // These types can't be represented in isolation
                // {ResourceFactory.createTypedLiteral("some-id", XSDDatatype.XSDENTITY)},
                // {ResourceFactory.createTypedLiteral("#some-id", XSDDatatype.XSDIDREF)},
        });
    }

    @Before
    public void setUp() throws IOException, RepositoryException {

        session = repo.login();
        subjects = new DefaultIdentifierTranslator(session);
        testObj = new ValueConverter(session, subjects);
    }


    private SameValueAsMatcher sameValueAs(final RDFNode expected) {
        return new SameValueAsMatcher(expected);
    }

    class SameValueAsMatcher extends BaseMatcher<RDFNode> {

        private RDFNode expected;


        SameValueAsMatcher(final RDFNode expected) {

            this.expected = expected;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("is <" + expected + ">");
        }

        @Override
        public boolean matches(final Object o) {
            if (!(o instanceof RDFNode)) {
                return false;
            }
            final RDFNode object = (RDFNode)o;

            if (expected.isLiteral() && object.isLiteral()) {
                return expected.asLiteral().sameValueAs(object.asLiteral())
                        || expected.toString().equals(object.toString());
            } else {
                return object.equals(expected);
            }
        }
    }

    @BeforeClass
    public static void setUpJcrRepository() throws ParsingException, FileNotFoundException, RepositoryException {
        engine = new ModeShapeEngine();
        engine.start();
        final RepositoryConfiguration config = RepositoryConfiguration.read("{ \"name\": \"test\" }");

        repo = engine.deploy(config);
    }

    @AfterClass
    public static void shutdownJcr() {
        engine.shutdown();
    }


    private static ModeShapeEngine engine;

}