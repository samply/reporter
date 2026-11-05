package de.samply.reporter.context;

import de.samply.reporter.logger.BufferedLoggerFactory;
import de.samply.reporter.logger.Logger;
import de.samply.reporter.script.CsvRecordIterator;
import de.samply.reporter.template.ColumnTemplate;
import de.samply.reporter.template.ReportTemplate;
import de.samply.reporter.template.SheetTemplate;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Context {

    private final Logger logger = BufferedLoggerFactory.getLogger(Context.class);
    private final Path resultsDirectory;
    private final ReportTemplate reportTemplate;
    private final Path[] sourcePaths;
    private final CsvConfig csvConfig;
    private final Map<String, Function<String[], String>> functionMap = new HashMap<>();
    private final MultiMap multiMap = new MultiMap();

    private final Map<String, Integer> sheetNameColumnNameIndex = new HashMap<>();

    public Context(Path resultsDirectory, ReportTemplate reportTemplate,
                   Path[] sourcePaths, CsvConfig csvConfig) {
        this.resultsDirectory = resultsDirectory;
        this.reportTemplate = reportTemplate;
        this.sourcePaths = sourcePaths;
        sortSourcePaths(sourcePaths);
        this.csvConfig = csvConfig;
    }

    private void sortSourcePaths(Path[] sourcePaths) {
        Arrays.sort(sourcePaths, Comparator.comparing(path -> path.getFileName().toString().length()));
    }

    public Logger getLogger() {
        return logger;
    }

    public CsvConfig getCsvConfig() {
        return csvConfig;
    }

    public void defineFunction(String functionName, Function<String[], String> function) {
        functionMap.put(functionName, function);
    }

    public String executeFunction(String functionName, String... parameters) {
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

    public Set<String> getKeySet(String... keys) {
        return multiMap.getKeySet(keys);
    }

    public void putElement(Object element, String... keys) {
        multiMap.put(element, keys);
    }

    public Path getResultsDirectory() {
        return resultsDirectory;
    }

    public ReportTemplate getReportTemplate() {
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
        try (FileReader fileReader = new FileReader(sourcePath.toFile(), csvConfig.charset());
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
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
                .setQuote(null)
                .get()
                .parse(bufferedReader)) {
            csvParser.getRecords().forEach(recordConsumer);
        }
    }

    public boolean hasOnlyHeaders(Path path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile(), csvConfig.charset()))) {
            int lineCount = 0;
            while (reader.readLine() != null) {
                lineCount++;
                if (lineCount > 1) {
                    return false;
                }
            }
            return true;
        }
    }

    public List<String> fetchHeaders(Path path) throws IOException {
        String[] result = {};
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile(), csvConfig.charset()))) {
            String line = reader.readLine();
            if (line != null && line.trim().length() > 0) {
                result = line.split(csvConfig.delimiter());
            }
        }
        return List.of(result);
    }

    public Integer getColumnIndex(String sheetName, String columnName) {
        String key = sheetName + columnName;
        Integer index = sheetNameColumnNameIndex.get(key);
        if (index == null) {
            SheetTemplate sheetTemplate = null;
            for (SheetTemplate tempSheetTemplate : reportTemplate.getSheetTemplates()) {
                if (tempSheetTemplate.getName().equalsIgnoreCase(sheetName)) {
                    sheetTemplate = tempSheetTemplate;
                    break;
                }
            }
            if (sheetTemplate != null) {
                int tempIndex = 0;
                for (ColumnTemplate columnTemplate : sheetTemplate.getColumnTemplates()) {
                    if (columnTemplate.getName().equalsIgnoreCase(columnName)) {
                        index = tempIndex;
                        break;
                    } else {
                        tempIndex++;
                    }
                }
            }
            sheetNameColumnNameIndex.put(key, index);
        }
        return index;
    }

}
