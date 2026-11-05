package de.samply.reporter.script;

import de.samply.reporter.context.ContextException;
import de.samply.reporter.context.CsvConfig;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvRecordIterator implements Iterator<CSVRecord> {

  private final FileReader fileReader;
  private final BufferedReader bufferedReader;
  private final CSVParser csvParser;
  private final Iterator<CSVRecord> iterator;

  public CsvRecordIterator(Path sourcePath, CsvConfig csvConfig) throws ContextException {
    try {
      fileReader = new FileReader(sourcePath.toFile(), csvConfig.charset());
      bufferedReader = new BufferedReader(fileReader);
      csvParser = createCsvParser(bufferedReader, csvConfig);
      iterator = csvParser.iterator();
    } catch (IOException e) {
      throw new ContextException(e);
    }
  }

  private CSVParser createCsvParser(BufferedReader bufferedReader, CsvConfig csvConfig)
      throws IOException {
    return Builder
        .create()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setDelimiter(csvConfig.delimiter())
        .setRecordSeparator(csvConfig.endOfLine())
        .get()
        .parse(bufferedReader);
  }

  @Override
  public boolean hasNext() {
    boolean result = iterator.hasNext();
    if (!result) {
      try {
        close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return result;
  }

  public void close() throws IOException {
    if (csvParser != null && !csvParser.isClosed()) {
      csvParser.close();
    }
    if (bufferedReader != null) {
      bufferedReader.close();
    }
    if (fileReader != null) {
      fileReader.close();
    }
  }

  @Override
  public CSVRecord next() {
    return iterator.next();
  }

}
