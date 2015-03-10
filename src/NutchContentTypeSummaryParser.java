import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.config.ServiceLoader;
import org.apache.tika.detect.AutoDetectReader;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class NutchContentTypeSummaryParser extends TXTParser {
	/**
	 * 
	 */
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss z");
	private static final long serialVersionUID = -6789266212469717735L;
	private static final Set<MediaType> SUPPORTED_TYPES = Collections
			.singleton(MediaType.TEXT_PLAIN);
	private static final ServiceLoader LOADER = new ServiceLoader(
			TXTParser.class.getClassLoader());

	private final Tika tika = new Tika();

	private Map<String, NutchEntry> fetchedSummaries;

	private Map<String, NutchEntry> unfetchedSummaries;

	private static final String SEP = "[,|;]";
	private static final Pattern PATT = Pattern.compile(new StringBuffer(
			"(\".*\")").append(SEP).append("(\\d*)").append(SEP)
			.append("(\".*\")").append(SEP).append("(.*)").append(SEP)
			.append("(.*)").append(SEP).append("(\\d*)").append(SEP)
			.append("(.*)").append(SEP).append("(.*)").append(SEP)
			.append("(.*)").append(SEP).append("(\".*\")").append(SEP)
			.append("(\".*\").*").toString());

	Pattern PATT_CONTENT_TYPE = Pattern.compile(new StringBuffer(
			"Content-Type:([^|]+)|.*").toString());

	private static final String TR = "tr";
	private static final String TD = "td";
	private static final String ID = "id";
	private static final String UL = "ul";
	private static final String LI = "li";

	public Set<MediaType> getSupportedTypes(ParseContext context) {
		return SUPPORTED_TYPES;
	}

	public void parse(InputStream stream, ContentHandler handler,
			Metadata metadata, ParseContext context) throws IOException,
			SAXException, TikaException {
		// Automatically detect the character encoding
		AutoDetectReader reader = new AutoDetectReader(
				new CloseShieldInputStream(stream), metadata, context.get(
						ServiceLoader.class, LOADER));

		try {
			Charset charset = reader.getCharset();
			MediaType type = new MediaType(MediaType.application("json"),
					charset);
			metadata.set(Metadata.CONTENT_TYPE, type.toString());
			// deprecated, see TIKA-431
			metadata.set(Metadata.CONTENT_ENCODING, charset.name());

			fetchedSummaries = new HashMap<String, NutchEntry>();
			unfetchedSummaries = new HashMap<String, NutchEntry>();

			String thisLine = reader.readLine();
			processHeader(thisLine);
			// int i = fetchedSummaries.size();
			int i = 0;
			int threshold = Integer.MAX_VALUE;
			while ((thisLine = reader.readLine()) != null) {
				processLine(thisLine, i);
				if (i >= threshold) {
					break;
				}
				i++;
			}

			XHTMLContentHandler xhtml = new XHTMLContentHandler(handler,
					metadata);
			xhtml.startDocument();
			populateXHTML(xhtml);
			xhtml.endDocument();
			/**
			 * output the xhtml format too.
			 */
			ToHTMLContentHandler toXhtmlhandler = new ToHTMLContentHandler(
					new FileOutputStream(context.get(File.class)), "UTF-8");
			xhtml = new XHTMLContentHandler(toXhtmlhandler, metadata);
			xhtml.startDocument();
			populateXHTML(xhtml);
			xhtml.endDocument();

		} finally {
			reader.close();
		}
	}

	private void populateXHTML(final XHTMLContentHandler xhtml) {
		Iterator<String> keyiter = fetchedSummaries.keySet().iterator();
		Iterator<String> unkeyiter = unfetchedSummaries.keySet().iterator();
		try {
			System.out.println(new StringBuffer(DATE_FORMAT.format(Calendar
					.getInstance().getTime())).append(
					" Fetched URLs mimetypes: ").toString());
			AttributesImpl attributes = new AttributesImpl();
			attributes.addAttribute("", "border", "border", "CDATA", "true");
			attributes.addAttribute("", "id", "id", "CDATA", "fetched_table");
			xhtml.startElement("table", attributes);
			while (keyiter.hasNext()) {
				String key = keyiter.next();
				NutchEntry entry = fetchedSummaries.get(key);
				System.out.println(key + "; " + entry);
				processRow(xhtml, key, entry);

			}
			xhtml.endElement("table");

			attributes = new AttributesImpl();
			attributes.addAttribute("", "border", "border", "CDATA", "true");
			attributes.addAttribute("", "id", "id", "CDATA", "unfetched_table");
			xhtml.startElement("table", attributes);

			System.out.println(new StringBuffer(DATE_FORMAT.format(Calendar
					.getInstance().getTime())).append(
					" Unfetched URLs mimetypes: ").toString());
			while (unkeyiter.hasNext()) {
				String key = unkeyiter.next();
				NutchEntry entry = unfetchedSummaries.get(key);
				System.out.println(key + "; " + entry);
				processRow(xhtml, key, entry);

			}

			xhtml.endElement("table");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void processRow(final XHTMLContentHandler xhtml, final String key,
			final NutchEntry entry) throws SAXException {

		xhtml.startElement(TR);
		xhtml.startElement(TD, "id", "category");
		xhtml.characters(key);
		xhtml.endElement(TD);
		if (entry.getExampleList() != null && !entry.getExampleList().isEmpty()) {
			xhtml.startElement(TD, "id", "examples");
			xhtml.startElement(UL);
			for (String li : entry.getExampleList()) {
				xhtml.startElement(LI);
				xhtml.characters(li);
				xhtml.endElement(LI);
			}
			xhtml.endElement(UL);
			xhtml.endElement(TD);
		}
		xhtml.startElement(TD, "id", "count");
		xhtml.characters(new StringBuffer().append(entry.count).toString());
		xhtml.endElement(TD);
		xhtml.endElement(TR);
	}

	private void processHeader(final String header) {
		// System.out.println(header.split(",").length);
	}

	private void processLine(final String line, final int lineNo) {

		// Create a Pattern object
		Matcher matcher = PATT.matcher(line);
		if (matcher.find()) {
			String url = matcher.group(1).replaceAll("\"", "");
			String statusCode = matcher.group(2);
			// String statuName = matcher.group(3);
			// String fetchTIme = matcher.group(4);
			// String modifyTime = matcher.group(5);
			// String retriesSinceFetch = matcher.group(6);
			// String retryIntervalSec = matcher.group(7);
			// String retryIntervalDay = matcher.group(8);
			// String score = matcher.group(9);
			// String signature = matcher.group(10);
			String meta = matcher.group(11).replaceAll("\"", "");

			String mimetype = null;
			short statuCode = Short.parseShort(statusCode);
			switch (statuCode) {
			case 1: // db_unfetched 1
				// status code = 1 means db_unfetched
				// Metadata metadata = new Metadata();
				URL urlcon;
				HttpURLConnection con = null;
				// InputStream stream = null;
				//url = "http://gcmd.gsfc.nasa.gov/KeywordSearch/RedirectAction.do?target=G4APKcnWYTs%3D";
				System.out.println("\t" + url);
				try {
					urlcon = new URL(url);
					con = (HttpURLConnection) urlcon.openConnection();
					con.setRequestMethod("GET");
					// con.setConnectTimeout(5000); // set timeout to 5 seconds
					con.setReadTimeout(5000);
					mimetype = con.getContentType();
					// Tika tika = new Tika();
					// mimetype = tika.detect(urlcon);
					// stream = TikaInputStream.get(urlcon, metadata);
					// mimetype = metadata.get("Content-Type");

				} catch (Exception e) {
					e.printStackTrace();
					mimetype = null;
				} finally {
					con.disconnect();
				}
				if (mimetype != null) {
					mimetype = mimetype.replaceAll(";.*", "");
					populateUnfetchedMap(url, mimetype, false);
				} else {
					mimetype = "failure";
					populateUnfetchedMap(url, mimetype, true);
				}

				System.out.println(new StringBuffer(DATE_FORMAT.format(Calendar
						.getInstance().getTime())).append(" at: ")
						.append(lineNo).append(", ").append(url).append(", ")
						.append(mimetype).toString());
				break;
			case 2: // db_fetchedTikaInputStream.get(stream, tmp);

				Matcher m = PATT_CONTENT_TYPE.matcher(meta);
				if (m.find()) {
					mimetype = m.group(1);
					populateFetchedMap(url, mimetype);
					System.out.println(new StringBuffer(DATE_FORMAT
							.format(Calendar.getInstance().getTime()))
							.append(" at: ").append(lineNo).append(", ")
							.append(url).append(", ").append(mimetype)
							.toString());
				} else {
					System.out
							.println("The Fetched URL does not have content-type: "
									+ url);
				}

				break;
			}

		}

	}

	private void populateFetchedMap(String url, String mimetype) {
		if (this.fetchedSummaries.containsKey(mimetype)) {
			this.fetchedSummaries.get(mimetype).increment();
		} else {
			this.fetchedSummaries.put(mimetype, new NutchEntry());
		}
	}

	private void populateUnfetchedMap(String url, String mimetype,
			boolean storeAll) {
		if (!storeAll) {

			if (this.unfetchedSummaries.containsKey(mimetype)) {
				NutchEntry entry = this.unfetchedSummaries.get(mimetype);
				entry.increment();
				entry.addExampleWithLimit(url);
			} else {
				NutchEntry entry = new NutchEntry();
				entry.addExample(url);
				this.unfetchedSummaries.put(mimetype, entry);
			}
		} else {
			System.out.println("nooo: " + url);
			if (this.unfetchedSummaries.containsKey(mimetype)) {
				NutchEntry entry = this.unfetchedSummaries.get(mimetype);
				entry.addExample(url);
				entry.increment();
			} else {
				NutchEntry entry = new NutchEntry();
				entry.addExample(url);
				this.unfetchedSummaries.put(mimetype, entry);

			}
		}
	}

	static class NutchEntry {
		// private String urlString;
		private int count;
		private List<String> exampleList;
		private static final int MAX_EXAMPLE_URLS = 10;

		NutchEntry() {
			this.count = 1;
			exampleList = new LinkedList<String>();
		}

		public void addExampleWithLimit(final String url) {
			if (this.count < MAX_EXAMPLE_URLS) {
				exampleList.add(url);
			}
		}

		public void addExample(final String url) {
			exampleList.add(url);
		}

		public List<String> getExampleList() {
			return this.exampleList;
		}

		public void increment() {
			this.count++;
		}

		public String toString() {
			StringBuffer sbuf = new StringBuffer();
			sbuf.append(count);
			return sbuf.toString();
		}

	}
}
