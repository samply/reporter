package de.samply.reporter.report;

import de.samply.reporter.app.ReporterConst;
import de.samply.reporter.context.CellContext;
import de.samply.reporter.context.CellStyleContext;
import de.samply.reporter.context.Context;
import de.samply.reporter.context.ContextGenerator;
import de.samply.reporter.exporter.ExporterClient;
import de.samply.reporter.exporter.ExporterClientException;
import de.samply.reporter.logger.BufferedLoggerFactory;
import de.samply.reporter.logger.Logger;
import de.samply.reporter.report.metainfo.ReportMetaInfo;
import de.samply.reporter.report.metainfo.ReportMetaInfoManager;
import de.samply.reporter.report.metainfo.ReportMetaInfoManagerException;
import de.samply.reporter.report.workbook.WorkbookManager;
import de.samply.reporter.report.workbook.WorkbookManagerFactory;
import de.samply.reporter.script.ScriptEngineException;
import de.samply.reporter.script.ScriptEngineManager;
import de.samply.reporter.script.ScriptResult;
import de.samply.reporter.template.ColumnTemplate;
import de.samply.reporter.template.ReportTemplate;
import de.samply.reporter.template.SheetTemplate;
import de.samply.reporter.template.script.Script;
import de.samply.reporter.utils.ExternalSheetUtils;
import de.samply.reporter.utils.ExternalSheetUtilsException;
import de.samply.reporter.utils.FileUtils;
import de.samply.reporter.utils.PercentageLogger;
import de.samply.reporter.zip.ExporterUnzipper;
import de.samply.reporter.zip.ExporterUnzipperException;
import de.samply.reporter.zip.Zipper;
import de.samply.reporter.zip.ZipperException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReportGenerator {

    private final static Logger logger = BufferedLoggerFactory.getLogger(ReportGenerator.class);
    private final ExporterClient exporterClient;
    private final ExporterUnzipper exporterUnzipper;
    private final ScriptEngineManager scriptEngineManager;
    private final ContextGenerator contextGenerator;
    private final WorkbookManagerFactory workbookManagerFactory;
    private final ReportMetaInfoManager reportMetaInfoManager;
    private final Integer workbookWindow;
    private final RunningReportsManager runningReportsManager;


    public ReportGenerator(
            ExporterClient exporterClient,
            ExporterUnzipper exporterUnzipper,
            ScriptEngineManager scriptEngineManager,
            ContextGenerator contextGenerator,
            WorkbookManagerFactory workbookManagerFactory,
            ReportMetaInfoManager reportMetaInfoManager,
            RunningReportsManager runningReportsManager,
            @Value(ReporterConst.EXCEL_WORKBOOK_WINDOW_SV) int workbookWindow
    ) {
        this.exporterClient = exporterClient;
        this.exporterUnzipper = exporterUnzipper;
        this.scriptEngineManager = scriptEngineManager;
        this.contextGenerator = contextGenerator;
        this.workbookWindow = workbookWindow;
        this.reportMetaInfoManager = reportMetaInfoManager;
        this.workbookManagerFactory = workbookManagerFactory;
        this.runningReportsManager = runningReportsManager;
    }

    public void generate(ReportTemplate template, ReportMetaInfo reportMetaInfo)
            throws ReportGeneratorException {
        try {
            runningReportsManager.addRunningReportId(reportMetaInfo.id());
            exporterClient.fetchExportFiles(filePath -> generate(template, filePath, reportMetaInfo), template,
                    () -> finalizeReportGeneration(reportMetaInfo));
        } catch (ExporterClientException | RuntimeException e) {
            finalizeReportGeneration(reportMetaInfo);
            throw new ReportGeneratorException(e);
        }
    }

    private void finalizeReportGeneration(ReportMetaInfo reportMetaInfo) {
        runningReportsManager.removeRunningReportId(reportMetaInfo.id());
        BufferedLoggerFactory.clearBuffer();
    }

    private void generate(ReportTemplate template, String filePath, ReportMetaInfo reportMetaInfo) {
        logger.info("Extracting paths...");
        Path[] paths = extractPaths(filePath);
        logger.info("Generating context...");
        Context context = contextGenerator.generate(template, paths);
        logger.info("Generating raw report...");
        Map<Script, ScriptResult> scriptResultMap = scriptEngineManager.generateRawReport(
                template, context);
        logger.info("Generating excel file");
        WorkbookManager workbookManager = workbookManagerFactory.create();
        logger.info("Filling excel file with data...");
        fillWorkbookWithData(workbookManager, template, scriptResultMap);
        logger.info("Adding format to excel file...");
        workbookManager.apply(workbook -> addFormatToWorkbook(workbook, template, context));
        logger.info("Writing excel file...");
        workbookManager.writeWorkbook(reportMetaInfo.path());
        logger.info("Removing temporal files...");
        removeTemporalFiles(paths[0].getParent(), scriptResultMap.values());
        logger.info("Zipping files if necessary...");
        createZipIfMoreThanOneFile(workbookManager, reportMetaInfo);
        runningReportsManager.removeRunningReportId(reportMetaInfo.id());
        logger.info("Excel file generated satisfactory.");
        BufferedLoggerFactory.clearBuffer();
    }

    private void removeTemporalFiles(Path sourceFilesDirectory, Collection<ScriptResult> scriptResults) {
        try {
            removeTemporalFilesWithoutExceptionHandling(sourceFilesDirectory, scriptResults);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeTemporalFilesWithoutExceptionHandling(Path sourceFilesDirectory,
                                                             Collection<ScriptResult> scriptResults)
            throws IOException {
        FileSystemUtils.deleteRecursively(sourceFilesDirectory);
        scriptResults.forEach(scriptResult -> {
            try {
                Files.delete(scriptResult.rawResult());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Path[] extractPaths(String filePath) {
        try {
            return exporterUnzipper.extractFiles(filePath);
        } catch (ExporterUnzipperException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillWorkbookWithData(WorkbookManager workbookManager, ReportTemplate template,
                                      Map<Script, ScriptResult> scriptResultMap) {
        template.getSheetTemplates()
                .forEach(sheetTemplate -> {
                    logger.info("Adding sheet '" + sheetTemplate.getName() + "'...");
                    if (sheetTemplate.getFileUrl() != null || sheetTemplate.getFilePath() != null) {
                        addSheetFromSourceExcelFile(workbookManager, sheetTemplate);
                    } else {
                        fillSheetWithData(workbookManager, sheetTemplate, scriptResultMap);
                    }
                });
    }

    private void addSheetFromSourceExcelFile(WorkbookManager workbookManager, SheetTemplate template) {
        try {
            ExternalSheetUtils.addSheetFromSourceExcelFile(workbookManager, template);
        } catch (ExternalSheetUtilsException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillSheetWithData(WorkbookManager workbookManager, SheetTemplate template,
                                   Map<Script, ScriptResult> scriptResultMap) {
        if (template.getValuesScript() != null) {
            ScriptResult result = scriptResultMap.get(template.getValuesScript().getScript());
            if (result != null) {
                fillSheetWithData(workbookManager, template, result);
                logger.info("Adding autosize to sheet '" + template.getName() + "'...");
                workbookManager.apply(template, this::autoSizeSheet);
                logger.info("Adding auto filter to sheet '" + template.getName() + "'...");
                workbookManager.apply(template, this::addAutoFilter);
            }
        }
    }

    private void fillSheetWithData(WorkbookManager workbookManager, SheetTemplate template, ScriptResult result) {
        try {
            fillSheetWithDataWithoutExceptionHandling(workbookManager, template, result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillSheetWithDataWithoutExceptionHandling(WorkbookManager workbookManager, SheetTemplate template, ScriptResult result)
            throws IOException {
        PercentageLogger percentageLogger = new PercentageLogger(logger,
                (int) FileUtils.fetchNumberOfLines(result.rawResult()),
                "Filling sheet" + template.getName() + " with data...");
        Files.readAllLines(result.rawResult())
                .forEach(line -> {
                    fillRowWithData(workbookManager.createRow(template), line, result);
                    percentageLogger.incrementCounter();
                });
        workbookManager.fetchLastSheetAndCreateIfNotExist(template); // Be sure that sheet is created even if it is empty
    }

    private void fillRowWithData(Row row, String line, ScriptResult result) {
        AtomicInteger columnIndex = new AtomicInteger(0);
        Arrays.stream(line.split(result.csvConfig().delimiter())).forEach(
                value -> setCellValue(row.createCell(columnIndex.getAndIncrement()), value));
    }

    private void setCellValue(Cell cell, String value) {
        if (value == null) {
            cell.setCellValue(ReporterConst.EMPTY_EXCEL_CELL);
        } else if (NumberUtils.isParsable(value)) {
            if (!NumberUtils.isDigits(value)) {
                cell.setCellValue(Double.valueOf(value));
            } else if (value.length() < 10 && (value.length() == 1 || value.charAt(0) != '0')) {
                cell.setCellValue(Integer.valueOf(value));
            } else {
                // Beyond the int range or with leading zeros it is rather an ID or a code: keep it as text
                cell.setCellValue(value);
            }
        } else {
            cell.setCellValue(value);
        }
    }

    private void autoSizeSheet(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum >= 0) {
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int j = 0; j <= headerRow.getLastCellNum(); j++) {
                    sheet.autoSizeColumn(j);
                }
            }
        }
    }

    private void addAutoFilter(Sheet sheet) {
        int rowStartIndex = 0;
        int rowEndIndex = sheet.getLastRowNum();
        if (rowEndIndex >= 0 && sheet.getRow(0) != null) {
            int columnStartIndex = 0;
            int columnEndIndex = sheet.getRow(0).getLastCellNum() - 1;

            CellRangeAddress cra = new CellRangeAddress(rowStartIndex, rowEndIndex, columnStartIndex,
                    columnEndIndex);
            sheet.setAutoFilter(cra);
        }
    }

    private void addFormatToWorkbook(Workbook workbook, ReportTemplate template, Context context) {
        template.getSheetTemplates().forEach(sheetTemplate -> {
            CellStyleContext cellStyleContext = new CellStyleContext(workbook);
            AtomicInteger columnNumber = new AtomicInteger(0);
            logger.info("Adding general format to all cells...");
            sheetTemplate.getFormatScripts().forEach(formatScript -> {
                if (formatScript.getScript() != null) {
                    addFormatToAllCellsOfASheet(workbook, sheetTemplate, cellStyleContext, context,
                            formatScript.getScript());
                }
            });
            logger.info("Adding column format to column cells...");
            sheetTemplate.getColumnTemplates().forEach(
                    columnTemplate -> addFormatToWorkbook(workbook, sheetTemplate, columnTemplate,
                            columnNumber.getAndIncrement(), cellStyleContext, context));
        });
    }

    private void addFormatToWorkbook(Workbook workbook, SheetTemplate sheetTemplate,
                                     ColumnTemplate columnTemplate, int columnNumber, CellStyleContext cellStyleContext,
                                     Context context) {
        Sheet sheet = workbook.getSheet(sheetTemplate.getName());
        if (sheet != null) {
            logger.info("Adding header format to column '" + columnTemplate.getName() + "' in sheet '"
                    + sheetTemplate.getName() + "'...");
            addHeaderFormatToWorkbook(columnTemplate, sheet, columnNumber, cellStyleContext, context);
            logger.info("Adding value format to column '" + columnTemplate.getName() + "' in sheet '"
                    + sheetTemplate.getName() + "'...");
            addValueFormatToWorkbook(columnTemplate, sheet, columnNumber, cellStyleContext, context);
        }
    }

    private void addHeaderFormatToWorkbook(ColumnTemplate template, Sheet sheet, int columnNumber,
                                           CellStyleContext cellStyleContext, Context context) {
        if (template.getHeaderFormatScript() != null) {
            Script script = template.getHeaderFormatScript().getScript();
            if (script != null) {
                fetchCellContext(script, cellStyleContext, context).applyCellStyleToCell(
                        sheet.getRow(0).getCell(columnNumber));
            }
        }
    }

    private void addValueFormatToWorkbook(ColumnTemplate template, Sheet sheet, int columnNumber,
                                          CellStyleContext cellStyleContext, Context context) {
        if (template.getValueFormatScript() != null) {
            Script script = template.getValueFormatScript().getScript();
            if (script != null) {
                CellContext cellContext = fetchCellContext(script, cellStyleContext, context);
                int lastRowNum = sheet.getLastRowNum();
                if (lastRowNum > 0) {
                    for (int i = 1; i <= lastRowNum; i++) {
                        cellContext.applyCellStyleToCell(sheet.getRow(i).getCell(columnNumber));
                    }
                }
            }
        }
    }

    private void addFormatToAllCellsOfASheet(Workbook workbook, SheetTemplate sheetTemplate,
                                             CellStyleContext cellStyleContext, Context context, Script script) {
        CellContext cellContext = fetchCellContext(script, cellStyleContext, context);
        Sheet sheet = workbook.getSheet(sheetTemplate.getName());
        if (sheet != null) {
            logger.info("Adding format to sheet '" + sheetTemplate.getName() + "'...");
            cellContext.applySheetStyleToSheet(sheet);
            PercentageLogger percentageLogger = new PercentageLogger(logger, sheet.getLastRowNum(),
                    "Adding format to all cells of sheet '" + sheetTemplate.getName() + "'...");
            sheet.forEach(row -> {
                row.forEach(cellContext::applyCellStyleToCell);
                percentageLogger.incrementCounter();
            });
        }
    }

    private CellContext fetchCellContext(Script script, CellStyleContext cellStyleContext,
                                         Context context) {
        try {
            return scriptEngineManager.generateCellContext(script, cellStyleContext, context);
        } catch (ScriptEngineException e) {
            throw new RuntimeException(e);
        }
    }

    private void createZipIfMoreThanOneFile(WorkbookManager workbookManager, ReportMetaInfo reportMetaInfo) {
        try {
            createZipIfMoreThanOneFileWithoutHandlingException(workbookManager, reportMetaInfo);
        } catch (ReportMetaInfoManagerException | IOException | ZipperException e) {
            logger.info(ExceptionUtils.getStackTrace(e));
        }
    }

    private void createZipIfMoreThanOneFileWithoutHandlingException(WorkbookManager workbookManager, ReportMetaInfo reportMetaInfo)
            throws ReportMetaInfoManagerException, IOException, ZipperException {
        if (workbookManager.isMultiWorkbook()) {
            Path zippedPath = Zipper.zip(workbookManager.fetchRealPaths(reportMetaInfo.path()));
            reportMetaInfoManager.reset();
            reportMetaInfoManager.addReportMetaInfoToFile(
                    new ReportMetaInfo(reportMetaInfo.id(), zippedPath, reportMetaInfo.timestamp(), reportMetaInfo.templateId()));
        }
    }

    public boolean isReportRunning(ReportMetaInfo reportMetaInfo) {
        return runningReportsManager.isReportIdRunning(reportMetaInfo.id());
    }
}
