import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.xml.sax.SAXException;

public class NutchDumpContentTypeMain {
	private File csvDumpPath, xhtmlOutputPath, jsonOutputPath;

	public NutchDumpContentTypeMain(final Builder builder) {
		csvDumpPath = new File(builder.csvDumpPath);
		xhtmlOutputPath = new File(builder.xhtmlOutputPath);
		jsonOutputPath = new File(builder.jsonOutputPath);
	}

	public void process() {
		NutchContentTypeSummaryParser par = new NutchContentTypeSummaryParser();
		Metadata metadata = new Metadata();
		ParseContext context = new ParseContext();
		context.set(File.class, xhtmlOutputPath);
		InputStream in_stream;
		try {
			// ToHTMLContentHandler
			/*
			 * ToHTMLContentHandler handler = new ToHTMLContentHandler( new
			 * FileOutputStream(outputPath), "UTF-8");
			 */
			NutchDumpToJSonContentTypeHandler jsonhandler = new NutchDumpToJSonContentTypeHandler(
					new FileOutputStream(jsonOutputPath), "UTF-8");
			in_stream = TikaInputStream.get(csvDumpPath, metadata);
			// par.parse(in_stream, handler, metadata, context);
			par.parse(in_stream, jsonhandler, metadata, context);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static class Builder {
		private String csvDumpPath, xhtmlOutputPath, jsonOutputPath;

		public synchronized Builder csvDumpath(final String csvDumpPath) {
			this.csvDumpPath = csvDumpPath;
			return this;
		}

		public synchronized Builder xhtmlOutputPath(final String xhtmlOutputPath) {
			this.xhtmlOutputPath = xhtmlOutputPath;
			return this;
		}

		public synchronized Builder jsonOutputPath(final String jsonOutputPath) {
			this.jsonOutputPath = jsonOutputPath;
			return this;
		}
	}

	public static void main(String args[]) {

		// String nutchCSVDumpPath = "part-00000_acadis2.csv";
		// String summaryXhtmlOutputPath = "acadis2_types.htm";
		// String summaryJsonOutputPath = "acadis2_types.json";
		if (args.length != 3) {
			System.out.println("Please ensure there are 3 arguments for running the program");
			System.exit(-1);
		}
		String nutchCSVDumpPath = args[0];
		String summaryXhtmlOutputPath = args[1];
		String summaryJsonOutputPath = args[2];
		
		NutchDumpContentTypeMain main = new NutchDumpContentTypeMain(
				new Builder().csvDumpath(nutchCSVDumpPath)
						.xhtmlOutputPath(summaryXhtmlOutputPath)
						.jsonOutputPath(summaryJsonOutputPath));
		main.process();
	}
}
