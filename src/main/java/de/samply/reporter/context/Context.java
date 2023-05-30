package de.samply.reporter.context;

import de.samply.reporter.script.CsvRecordIterator;
import de.samply.reporter.template.ReportTemplate;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

  private final Logger logger = LoggerFactory.getLogger(Context.class);
  private Path resultsDirectory;
  private ReportTemplate reportTemplate;
  private Path[] sourcePaths;
  private CsvConfig csvConfig;
  private Map<String, Function<String[],String>> functionMap = new HashMap<>();
  private MultiMap multiMap = new MultiMap();

  public Context(Path resultsDirectory, ReportTemplate reportTemplate,
      Path[] sourcePaths, CsvConfig csvConfig) {
    this.resultsDirectory = resultsDirectory;
    this.reportTemplate = reportTemplate;
    this.sourcePaths = sourcePaths;
    this.csvConfig = csvConfig;
  }

  public Logger getLogger() {
    return logger;
  }

  public CsvConfig getCsvConfig() {
    return csvConfig;
  }

  public void defineFunction(String functionName, Function<String[], String> function){
    functionMap.put(functionName, function);
  }

  public String executeFunction(String functionName, String... parameters){
    Function<String[], String> function = functionMap.get(functionName);
    return (function != null) ? function.apply(parameters) : "";
  }

  public Iterator<CSVRecord> fetchCsvRecords(String filename) throws ContextException {
    Path sourcePath = findSourcePathByName(filename);
    return fetchCsvRecords(sourcePath);
  }

  public Iterator<CSVRecord> fetchCsvRecords(Path path) throws ContextException {
    return (path != null) ? new CsvRecordIterator(path, csvConfig)
        : Collections.emptyIterator();
  }


  private Path findSourcePathByName(String filename) {
    for (Path path : sourcePaths) {
      if (path.getFileName().toString().toLowerCase().contains(filename.toLowerCase())) {
        return path;
      }
    }
    return null;
  }

  public Object getElement(String... keys) {
    return multiMap.get(keys);
  }

  public List<Object> getAllElement(String... keys) {
    return multiMap.getAll(keys);
  }

  public Set<String> getKeySet(String... keys){
    return multiMap.getKeySet(keys);
  }

  public void putElement(Object element, String... keys) {
    multiMap.put(element, keys);
  }

  public Path getResultsDirectory() {
    return resultsDirectory;
  }

  public ReportTemplate getQualityReportTemplate() {
    return reportTemplate;
  }

  public List<Path> getSourcePaths() {
    return List.of(sourcePaths);
  }

  public void applyToRecords(String filename, Consumer<CSVRecord> recordConsumer) {
    Path path = findSourcePathByName(filename);
    if (path != null) {
      applyToRecords(path, recordConsumer);
    }
  }

  public void applyToRecords(Path sourcePath, Consumer<CSVRecord> recordConsumer) {
    try (FileReader fileReader = new FileReader(
        sourcePath.toFile()); BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      applyToRecords(bufferedReader, recordConsumer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyToRecords(BufferedReader bufferedReader, Consumer<CSVRecord> recordConsumer)
      throws IOException {
    try (CSVParser csvParser = Builder
        .create()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setDelimiter(csvConfig.delimiter())
        .setRecordSeparator(csvConfig.endOfLine())
        .build()
        .parse(bufferedReader)) {
      csvParser.getRecords().forEach(record -> recordConsumer.accept(record));
    }
  }

}