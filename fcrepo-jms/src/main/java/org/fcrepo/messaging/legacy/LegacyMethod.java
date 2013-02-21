package org.fcrepo.messaging.legacy;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.fcrepo.utils.FedoraTypesUtils;

public class LegacyMethod {

    private static final Abdera abdera = new Abdera();

    private static final Parser abderaParser = abdera.getParser();
    
	private static final String BASE_URL = "http://localhost:8080/rest"; //TODO Figure out where to get the base url

	private static final Properties FEDORA_TYPES = new Properties();
	
	public static final String FORMAT = "info:fedora/fedora-system:ATOM-APIM-1.0";

	private static final String FORMAT_PREDICATE = "http://www.fedora.info/definitions/1/0/types/formatURI";

    private static final Date ONE_BCE = new Date(-62167392000000L);

    private static final Date ONE_CE = new Date(-62135769600000L);
    
	//TODO get this out of the build properties
	public final static String SERVER_VERSION = "4.0.0-SNAPSHOT";

	private final static String TYPES_NS = "http://www.fedora.info/definitions/1/0/types/";
	
	private final static String VERSION_PREDICATE = "info:fedora/fedora-system:def/view#version";
	
	private final static String XSD_NS = "http://www.w3.org/2001/XMLSchema"; 

	private static String[] METHODS = new String[]{"ingest", "modifyObject", "purgeObject",
		                                           "addDatastream", "modifyDatastream", "purgeDatastream"};
	private final static List<String> METHOD_NAMES = Arrays.asList(METHODS);
	
	static {
		try {
			FEDORA_TYPES.load(LegacyMethod.class.getResourceAsStream("/org/fcrepo/messaging/legacy/map.properties"));
		} catch (IOException e) { // it's in the jar.
			e.printStackTrace();
		}
	}

	private final Entry delegate;

	public LegacyMethod(Event jcrEvent, Node resource) throws RepositoryException {
		this(newEntry());
		
		boolean isDatastreamNode = FedoraTypesUtils.isFedoraDatastream.apply(resource);
		boolean isObjectNode = FedoraTypesUtils.isFedoraObject.apply(resource) && !isDatastreamNode;

		if (isDatastreamNode || isObjectNode) {
			setMethodName(mapMethodName(jcrEvent, isObjectNode));
			String returnValue = getReturnValue(jcrEvent, resource);
			setContent(getEntryContent(getMethodName(), returnValue));
			if (isDatastreamNode) {
				setPid(resource.getParent().getName());
				setDsId(resource.getName());
			} else {
				setPid(resource.getName());
			}
		}
		else {
			setMethodName(null);
		}
		String userID = (jcrEvent.getUserID() == null) ? "unknown" : jcrEvent.getUserID();
		setUserId(userID);
		setModified(new Date(jcrEvent.getDate()));
	}

	public LegacyMethod(Entry atomEntry) {
        delegate = atomEntry;
	}
	
	public LegacyMethod(String atomEntry) {
        delegate = (Entry) abderaParser.parse(new StringReader(atomEntry)).getRoot();
	}
	
	public Entry getEntry() {
		return delegate;
	}
	
	public void setContent(String content) {
		delegate.setContent(content);
	}
	
	public void setUserId(String val) {
		if (val == null) val = "unknown";
		delegate.addAuthor(val, null, BASE_URL);
	}
	
	public String getUserID(){
		return delegate.getAuthor().getName();
	}
	
	public void setModified(Date date) {
		delegate.setUpdated(date);
	}
	
	public Date getModified(){
		return delegate.getUpdated();
	}
	
	public void setMethodName(String val){
		delegate.setTitle(val).setBaseUri(BASE_URL);
	}
	
	public String getMethodName() {
		return delegate.getTitle();
	}
	
	public void setPid(String val) {
		List<Category> vals = delegate.getCategories("fedora-types:pid");
		if (vals == null || vals.isEmpty()) {
		    delegate.addCategory("xsd:string", val, "fedora-types:pid");
		} else {
			vals.get(0).setTerm(val);
		}
		delegate.setSummary(val);
	}
	
	public String getPid() {
		List<Category> categories = delegate.getCategories("xsd:string");
		for (Category c: categories) {
			if ("fedora-types:pid".equals(c.getLabel())) return c.getTerm();
		}
		return null;
	}

	public void setDsId(String val) {
		List<Category> vals = delegate.getCategories("fedora-types:dsID");
		if (vals == null || vals.isEmpty()) {
		    delegate.addCategory("xsd:string", val, "fedora-types:dsID");
		} else {
			vals.get(0).setTerm(val);
		}
	}
	
	public String getDsId() {
		List<Category> categories = delegate.getCategories("xsd:string");
		for (Category c: categories) {
			if ("fedora-types:dsID".equals(c.getLabel())) return c.getTerm();
		}
		return null;
	}
	
	public void writeTo(Writer writer) throws IOException {
		delegate.writeTo(writer);
	}

	private static Entry newEntry(){
		Entry entry = abdera.newEntry();
		entry.declareNS(XSD_NS, "xsd");
		entry.declareNS(TYPES_NS, "fedora-types");
		entry.setId("urn:uuid:" + UUID.randomUUID().toString());
		entry.addCategory(FORMAT_PREDICATE, FORMAT, "format");
		entry.addCategory(VERSION_PREDICATE, SERVER_VERSION, "version");
		return entry;
	}

	private static String getEntryContent(String methodName, String returnVal) {
		//String parm = (String)FEDORA_TYPES.get(methodName + ".parm");
		String datatype = (String)FEDORA_TYPES.get(methodName + ".datatype");
		return objectToString(returnVal, datatype);
	}

	private static String objectToString(String obj, String xsdType) {
		if (obj == null) {
			return "null";
		}
		String javaType = obj.getClass().getCanonicalName();
		String term;
		//TODO Most of these types are not yet relevant to FCR4, but we can borrow their serializations as necessary
		if (javaType != null && javaType.equals("java.util.Date")) { // several circumstances yield null canonical names
			//term = convertDateToXSDString((Date) obj);
			term = "[UNSUPPORTED" + xsdType + "]";
		} else if (xsdType.equals("fedora-types:ArrayOfString")) {
			//term = array2string(obj);
			term = "[UNSUPPORTED" + xsdType + "]";
		} else if (xsdType.equals("xsd:boolean")) {
			term = obj.toString();
		} else if (xsdType.equals("xsd:nonNegativeInteger")) {
			term = obj.toString();
		} else if (xsdType.equals("fedora-types:RelationshipTuple")) {
			//RelationshipTuple[] tuples = (RelationshipTuple[]) obj;
			//TupleArrayTripleIterator iter =
			//		new TupleArrayTripleIterator(new ArrayList<RelationshipTuple>(Arrays
			//				.asList(tuples)));
			//ByteArrayOutputStream os = new ByteArrayOutputStream();
			//try {
			//	iter.toStream(os, RDFFormat.NOTATION_3, false);
			//} catch (TrippiException e) {
			//	e.printStackTrace();
			//}
			//term = new String(os.toByteArray());
			term = "[UNSUPPORTED" + xsdType + "]";
		} else if (javaType != null && javaType.equals("java.lang.String")) {
			term = (String) obj;
			term = term.replaceAll("\"", "'");
		} else {
			term = "[OMITTED]";
		}
		return term;
	}

	private static String getReturnValue(Event jcrEvent, Node jcrNode) throws RepositoryException {
		switch (jcrEvent.getType()) {
		case NODE_ADDED:
			return jcrNode.getName();
		case NODE_REMOVED:
			return convertDateToXSDString(jcrEvent.getDate());
		case PROPERTY_ADDED:
			return convertDateToXSDString(jcrEvent.getDate());
		case PROPERTY_CHANGED:
			return convertDateToXSDString(jcrEvent.getDate());
		case PROPERTY_REMOVED:
			return convertDateToXSDString(jcrEvent.getDate());
		}
		return null;
	}

	private static String mapMethodName(Event jcrEvent, boolean isObjectNode) {
		switch (jcrEvent.getType()) {
		case NODE_ADDED:
			return isObjectNode? "ingest" : "addDatastream";
		case NODE_REMOVED:
			return isObjectNode? "purgeObject" : "purgeDatastream";
		case PROPERTY_ADDED:
			return isObjectNode? "modifyObject" : "modifyDatastream";
		case PROPERTY_CHANGED:
			return isObjectNode? "modifyObject" : "modifyDatastream";
		case PROPERTY_REMOVED:
			return isObjectNode? "modifyObject" : "modifyDatastream";
		}
		return null;
	}

	/**
	 * Converts an instance of java.util.Date into an ISO 8601 String
	 * representation. Uses the date format yyyy-MM-ddTHH:mm:ss.SSSZ or
	 * yyyy-MM-ddTHH:mm:ssZ, depending on whether millisecond precision is
	 * desired.
	 *
	 * @param date
	 *        Instance of java.util.Date.
	 * @param millis
	 *        Whether or not the return value should include milliseconds.
	 * @return ISO 8601 String representation of the Date argument or null if
	 *         the Date argument is null.
	 */
	public static String convertDateToString(Date date, boolean millis) {
		if (date == null) {
			return null;
		} else {
			DateFormat df;
			if (millis) {
				df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			} else {
				df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			}
			df.setTimeZone(TimeZone.getTimeZone("UTC"));

			if (date.before(ONE_CE)) {
				StringBuilder sb = new StringBuilder(df.format(date));
				sb.insert(0, "-");
				return sb.toString();
			} else {
				return df.format(date);
			}
		}
	}
	/**
	 * Converts an instance of <code>Date</code> into the canonical lexical
	 * representation of an XSD dateTime with the following exceptions: - Dates
	 * before 1 CE (i.e. 1 AD) are handled according to ISO 8601:2000 Second
	 * Edition: "0000" is the lexical representation of 1 BCE "-0001" is the
	 * lexical representation of 2 BCE
	 *
	 * @param date
	 *        Instance of java.util.Date.
	 * @return the lexical form of the XSD dateTime value, e.g.
	 *         "2006-11-13T09:40:55.001Z".
	 * @see <a
	 *      href="http://www.w3.org/TR/xmlschema-2/#date-canonical-representation">3.2.7.2
	 *      Canonical representation</a>
	 */
	public static String convertDateToXSDString(long date) {
		return convertDateToXSDString(new Date(date));
	}
	
	public static String convertDateToXSDString(Date date) {
		StringBuilder lexicalForm;
		String dateTime = convertDateToString(date, true);
		int len = dateTime.length() - 1;
		if (dateTime.indexOf('.', len - 4) != -1) {
			while (dateTime.charAt(len - 1) == '0') {
				len--; // fractional seconds may not with '0'.
			}
			if (dateTime.charAt(len - 1) == '.') {
				len--;
			}
			lexicalForm = new StringBuilder(dateTime.substring(0, len));
			lexicalForm.append('Z');
		} else {
			lexicalForm = new StringBuilder(dateTime);
		}

		if (date.before(ONE_CE)) {
			DateFormat df = new SimpleDateFormat("yyyy");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			StringBuilder year =
					new StringBuilder(String.valueOf(Integer.parseInt(df
							.format(date)) - 1));
			while (year.length() < 4) {
				year.insert(0, '0');
			}
			lexicalForm
			.replace(0, lexicalForm.indexOf("-", 4), year.toString());
			if (date.before(ONE_BCE)) {
				lexicalForm.insert(0, "-");
			}
		}
		return lexicalForm.toString();
	}
	
	public static boolean canParse(Message jmsMessage) {
		try {
			return FORMAT.equals(jmsMessage.getJMSType()) && METHOD_NAMES.contains(jmsMessage.getStringProperty("methodName"));
		} catch (JMSException e) {
			e.printStackTrace();
			return false;
		}
	}
}
