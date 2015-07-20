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
package org.fcrepo.kernel.modeshape.rdf.converters;

import com.google.common.base.Converter;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.infinispan.schematic.document.ParsingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.Calendar;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDID;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDNCName;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDNMTOKEN;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDName;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDQName;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDanyURI;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDbase64Binary;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDdate;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDgDay;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDgMonth;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDgMonthDay;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDgYear;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDhexBinary;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDlanguage;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDnegativeInteger;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDnonNegativeInteger;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDnonPositiveInteger;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDnormalizedString;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDpositiveInteger;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDtime;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDtoken;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDunsignedByte;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDunsignedInt;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDunsignedLong;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDunsignedShort;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createLangLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.modeshape.jcr.RepositoryConfiguration.read;

/**
 * @author cabeer
 * @author ajs6f
 */
@RunWith(Parameterized.class)
public class ValueConverterTest {

    static Repository repo;

    private Session session;

    private Converter<Resource, FedoraResource> subjects;
    private Converter<Value, RDFNode> testObj;

    @Parameter(value = 0)
    public RDFNode externalValue;

    @Test
    public void test() {
        assertThat(testObj.convert(testObj.reverse().convert(externalValue)), sameValueAs(externalValue));
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return asList(new Object[][]{
                {createTypedLiteral("x")},
                {createTypedLiteral(0)},
                {createTypedLiteral(1L)},
                {createTypedLiteral(new BigDecimal("2.123"))},
                {createTypedLiteral((double)3)},
                {createTypedLiteral(3.1415)},
                {createTypedLiteral(Calendar.getInstance())},
                {createTypedLiteral((byte)1)},
                {createTypedLiteral((short)42)},
                {createTypedLiteral("255", XSDunsignedByte)},
                {createTypedLiteral("255", XSDunsignedShort)},
                {createTypedLiteral("255", XSDunsignedInt)},
                {createTypedLiteral("255", XSDunsignedLong)},
                {createTypedLiteral("-255", XSDnonPositiveInteger)},
                {createTypedLiteral("255", XSDnonNegativeInteger)},
                {createTypedLiteral("255", XSDpositiveInteger)},
                {createTypedLiteral("-255", XSDnegativeInteger)},
                {createTypedLiteral(true)},
                {createResource("info:x")},
                {createTypedLiteral("2014-10-24T01:23:45Z", XSDdateTime)},
                {createTypedLiteral("some-invalid-data", XSDdateTime)},
                // Types outside the JCR type system boundaries:
                {createTypedLiteral("2014-10-24", XSDdate)},
                {createTypedLiteral("01:02:03", XSDtime)},
                {createTypedLiteral("---31", XSDgDay)},
                {createTypedLiteral("--10", XSDgMonth)},
                {createTypedLiteral("--02-29", XSDgMonthDay)},
                {createTypedLiteral("2001", XSDgYear)},
                {createTypedLiteral("ABCDEF", XSDhexBinary)},
                {createTypedLiteral("eHl6", XSDbase64Binary)},
                {createTypedLiteral("eHl6", XSDnormalizedString)},
                {createTypedLiteral("some:uri", XSDanyURI)},
                {createTypedLiteral("tokenize this", XSDtoken)},
                {createTypedLiteral("name", XSDName)},
                {createTypedLiteral("qname", XSDQName)},
                {createTypedLiteral("en-us", XSDlanguage)},
                {createTypedLiteral("name", XSDNMTOKEN)},
                {createTypedLiteral("some-id", XSDID)},
                {createTypedLiteral("ncname", XSDNCName)},
                {createTypedLiteral(2.0123f)},
                {createLangLiteral("xyz", "de")},
                // Problems
                // These types can't be represented in isolation
                // {ResourceFactory.createTypedLiteral("some-id", XSDDatatype.XSDENTITY)},
                // {ResourceFactory.createTypedLiteral("#some-id", XSDDatatype.XSDIDREF)},
        });
    }

    @Before
    public void setUp() throws RepositoryException {

        session = repo.login();
        subjects = new DefaultIdentifierTranslator(session);
        testObj = new ValueConverter(session, subjects);
    }


    private SameValueAsMatcher sameValueAs(final RDFNode expected) {
        return new SameValueAsMatcher(expected);
    }

    class SameValueAsMatcher extends BaseMatcher<RDFNode> {

        private final RDFNode expected;

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
            }
            return object.equals(expected);
        }
    }

    @BeforeClass
    public static void setUpJcrRepository() throws ParsingException, FileNotFoundException, RepositoryException {
        engine = new ModeShapeEngine();
        engine.start();
        final RepositoryConfiguration config = read("{ \"name\": \"test\" }");
        repo = engine.deploy(config);
    }

    @AfterClass
    public static void shutdownJcr() {
        engine.shutdown();
    }

    private static ModeShapeEngine engine;

}
