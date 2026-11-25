package de.samply.reporter.app;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import de.samply.reporter.exporter.ExporterClient;
import de.samply.reporter.logger.BufferedLoggerFactory;
import de.samply.reporter.logger.Logger;
import de.samply.reporter.logger.Logs;
import de.samply.reporter.report.ReportGenerator;
import de.samply.reporter.report.ReportGeneratorException;
import de.samply.reporter.report.metainfo.ReportMetaInfo;
import de.samply.reporter.report.metainfo.ReportMetaInfoManager;
import de.samply.reporter.report.metainfo.ReportMetaInfoManagerException;
import de.samply.reporter.template.Exporter;
import de.samply.reporter.template.ReportTemplate;
import de.samply.reporter.template.ReportTemplateManager;
import de.samply.reporter.utils.ProjectVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@RestController
public class ReporterController {

    private final Logger logger = BufferedLoggerFactory.getLogger(ReporterController.class);
    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ReportGenerator reportGenerator;
    private final ReportMetaInfoManager reportMetaInfoManager;
    private final ReportTemplateManager reportTemplateManager;
    private final ExporterClient exporterClient;
    private final String httpRelativePath;
    private final String httpServletRequestScheme;
    private final ObjectMapper objectMapper = new JsonMapper();


    public ReporterController(
            @Value(ReporterConst.HTTP_RELATIVE_PATH_SV) String httpRelativePath,
            @Value(ReporterConst.HTTP_SERVLET_REQUEST_SCHEME_SV) String httpServletRequestScheme,
            ReportGenerator reportGenerator,
            ReportTemplateManager reportTemplateManager,
            ReportMetaInfoManager reportMetaInfoManager,
            ExporterClient exporterClient) {
        this.httpRelativePath = httpRelativePath;
        this.httpServletRequestScheme = httpServletRequestScheme;
        this.reportGenerator = reportGenerator;
        this.reportTemplateManager = reportTemplateManager;
        this.reportMetaInfoManager = reportMetaInfoManager;
        this.exporterClient = exporterClient;
    }

    //@CrossOrigin(origins = "${CROSS_ORIGINS}", allowedHeaders = {"Authorization"})
    @GetMapping(value = ReporterConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @PostMapping(value = ReporterConst.GENERATE)
    @Operation(summary = "Generate Report", description = "Generate a report based on the provided template or custom template string.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request. Check request parameters."),
            @ApiResponse(responseCode = "500", description = "Internal server error during report generation")
    })
    public ResponseEntity<String> generate(
            HttpServletRequest httpServletRequest,
            @RequestParam(name = ReporterConst.REPORT_TEMPLATE_ID, required = false) String templateId,
            @RequestParam(name = ReporterConst.EXPORT_URL, required = false) String exportUrl,
            @RequestParam(name = ReporterConst.DAYS_UNTIL_EXPORT_EXPIRATION, required = false) Integer daysUntilExportExpiration,
            @RequestParam(name = ReporterConst.EXPIRES_EXPORT, required = false) Boolean expiresExport,
            @RequestHeader(name = "Content-Type", required = false) String contentType,
            @RequestHeader(name = ReporterConst.IS_INTERNAL_REQUEST, required = false) Boolean isInternalRequest,
            @RequestBody(required = false) String template
    ) throws ReportGeneratorException, ReportMetaInfoManagerException, JacksonException {
        ReportTemplate reportTemplate;
        if (template != null) {
            if (contentType != null && !contentType.equalsIgnoreCase(
                    MediaType.APPLICATION_XML_VALUE)) {
                return new ResponseEntity<>("Content Type not supported. Please set content type as "
                        + MediaType.APPLICATION_XML_VALUE, HttpStatus.BAD_REQUEST);
            }
            try {
                reportTemplate = reportTemplateManager.fetchTemplateAndGenerateCustomTemplateId(template);
            } catch (IOException e) {
                return new ResponseEntity<>(ExceptionUtils.getStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            if (templateId == null) {
                return new ResponseEntity<>("No template nor template id provided", HttpStatus.BAD_REQUEST);
            }
            reportTemplate = reportTemplateManager.getQualityReportTemplate(templateId);
            if (exportUrl != null || daysUntilExportExpiration != null || expiresExport != null) {
                try {
                    reportTemplate = reportTemplate.clone();
                } catch (CloneNotSupportedException e) {
                    return new ResponseEntity<>(ExceptionUtils.getStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
                }
                Exporter exporter = reportTemplate.getExporter();
                if (exporter == null) {
                    exporter = new Exporter();
                    reportTemplate.setExporter(exporter);
                }
                if (daysUntilExportExpiration != null) {
                    exporter.setExportExpirationInDays(daysUntilExportExpiration);
                }
                if (expiresExport != null && expiresExport == false) {
                    exporter.setExportExpirationInDays(ReporterConst.EXPORT_NOT_EXPIRES);
                }
                if (exportUrl != null) {
                    exporter.setExportUrl(exportUrl);
                }
            }
        }
        ReportMetaInfo reportMetaInfo = reportMetaInfoManager.createNewReportMetaInfo(reportTemplate);
        reportGenerator.generate(reportTemplate, reportMetaInfo);
        return new ResponseEntity<>(
                createRequestResponseEntity(httpServletRequest, reportMetaInfo.id(), isInternalRequest), HttpStatus.OK);
    }

    private String createRequestResponseEntity(HttpServletRequest request, String reportId, Boolean isInternalRequest)
            throws JacksonException {
        return objectMapper.writeValueAsString(
                new GenerateResponseEntity(fetchResponseUrl(request, reportId, isInternalRequest)));
    }

    private String fetchResponseUrl(HttpServletRequest httpServletRequest, String reportId, Boolean isInternalRequest) {
        ServletUriComponentsBuilder servletUriComponentsBuilder = ServletUriComponentsBuilder.fromRequestUri(
                httpServletRequest);
        if (isInternalRequest != null && isInternalRequest) {
            servletUriComponentsBuilder
                    .scheme("http")
                    .replacePath(ReporterConst.REPORT);
        } else {
            servletUriComponentsBuilder
                    .scheme(httpServletRequestScheme)
                    .replacePath(createHttpPath(ReporterConst.REPORT));
        }
        String result = servletUriComponentsBuilder
                .queryParam(ReporterConst.REPORT_ID, reportId).toUriString();
        logger.info("Response URL: " + result);
        return result;
    }

    private String createHttpPath(String httpPath) {
        return (httpRelativePath != null && httpRelativePath.length() > 0) ? httpRelativePath + '/'
                + httpPath : httpPath;
    }

    @GetMapping(value = ReporterConst.REPORT, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Fetch Report", description = "Retrieves a report as an octet stream based on the provided report ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Report not found"

            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Accepted. Report file not yet available"
            )
    })
    public ResponseEntity<InputStreamResource> fetchReport(
            @RequestParam(name = ReporterConst.REPORT_ID) String reportId
    ) throws ReportMetaInfoManagerException, FileNotFoundException {
        Optional<ReportMetaInfo> reportMetaInfo = reportMetaInfoManager.fetchReportMetaInfo(reportId);
        if (reportMetaInfo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!reportMetaInfo.get().path().toFile().exists()) {
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }
        return createResponseEntity(reportMetaInfo.get().path());
    }

    private ResponseEntity<InputStreamResource> createResponseEntity(Path path)
            throws FileNotFoundException {
        return createResponseEntity(new InputStreamResource(new FileInputStream(path.toFile())),
                path.getFileName().toString());
    }

    private ResponseEntity<InputStreamResource> createResponseEntity(
            InputStreamResource inputStreamResource, String filename) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + filename)
                .body(inputStreamResource);
    }

    @GetMapping(value = ReporterConst.REPORTS_LIST)
    @Operation(summary = "Fetch All Reports", description = "Retrieves a list of all available reports with optional pagination parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reports fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during report meta-information retrieval")
    })
    public ResponseEntity fetchAllReports(
            @Parameter(name = ReporterConst.REPORTS_LIST_PAGE, description = "The page number for pagination", required = false)
            @RequestParam(name = ReporterConst.REPORTS_LIST_PAGE, required = false) Integer page,
            @Parameter(name = ReporterConst.REPORTS_LIST_PAGE_SIZE, description = "The number of items per page for pagination", required = false)
            @RequestParam(name = ReporterConst.REPORTS_LIST_PAGE_SIZE, required = false) Integer pageSize
    ) throws ReportMetaInfoManagerException {
        return ResponseEntity.ok().body((page != null && pageSize != null) ?
                reportMetaInfoManager.fetchAllExistingReportMetaInfos(pageSize, page) :
                reportMetaInfoManager.fetchAllExistingReportMetaInfos());
    }

    @GetMapping(value = ReporterConst.REPORT_STATUS)
    @Operation(summary = "Fetch Report Status", description = "Retrieves the status of a specific report based on the provided report ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report status retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Report not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during report status retrieval")
    })
    public ResponseEntity<ReportStatus> fetchReportStatus(
            @Parameter(name = ReporterConst.REPORT_ID, description = "The ID of the report for which the status is to be retrieved", required = true)
            @RequestParam(name = ReporterConst.REPORT_ID) String reportId
    ) throws ReportMetaInfoManagerException {
        Optional<ReportMetaInfo> reportMetaInfo = reportMetaInfoManager.fetchReportMetaInfo(reportId);
        if (reportMetaInfo.isEmpty()) {
            return ResponseEntity.ok(ReportStatus.NOT_FOUND);
        }
        if (reportGenerator.isReportRunning(reportMetaInfo.get())) {
            return ResponseEntity.ok(ReportStatus.RUNNING);
        }
        if (!reportMetaInfo.get().path().toFile().exists()) {
            return ResponseEntity.ok(ReportStatus.ERROR);
        }
        return ResponseEntity.ok(ReportStatus.OK);
    }

    @GetMapping(value = ReporterConst.LOGS)
    @Operation(summary = "Fetch Logs", description = "Retrieves logs from the reporter and exporter components based on the specified parameters.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logs fetched successfully"
            )
    })
    public ResponseEntity<Logs[]> fetchLogs(
            @Parameter(name = ReporterConst.LOGS_SIZE, description = "The number of log lines to retrieve", required = true)
            @RequestParam(name = ReporterConst.LOGS_SIZE) int logsSize,
            @Parameter(name = ReporterConst.LOGS_LAST_LINE_REPORTER, description = "Optional parameter specifying the last retrieved log line for the reporter component", required = false)
            @RequestParam(name = ReporterConst.LOGS_LAST_LINE_REPORTER, required = false) String logsLastLine,
            @Parameter(name = ReporterConst.LOGS_LAST_LINE_EXPORTER, description = "Optional parameter specifying the last retrieved log line for the exporter component", required = false)
            @RequestParam(name = ReporterConst.LOGS_LAST_LINE_EXPORTER, required = false) String exporterLogsLastLine) {
        Logs reporterLogs = new Logs(ReporterConst.REPORTER, BufferedLoggerFactory.getLastLoggerLines(logsSize, logsLastLine));
        Logs exporterLogs = new Logs(ReporterConst.EXPORTER, exporterClient.fetchLogs(logsSize, exporterLogsLastLine));
        Logs[] logs = new Logs[]{reporterLogs, exporterLogs};
        return ResponseEntity.ok().body(logs);
    }

    @GetMapping(value = ReporterConst.REPORT_TEMPLATE_IDS)
    @Operation(summary = "Fetch Report Template IDs", description = "Retrieves an array of report template IDs.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report template IDs fetched successfully"
            )
    })
    public ResponseEntity<String[]> fetchTemplateIds() {
        return ResponseEntity.ok().body(reportTemplateManager.getReportTemplateIds());
    }

    @GetMapping(value = ReporterConst.REPORT_TEMPLATE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Fetch Report Template", description = "Retrieves a report template as an octet stream based on the provided report template ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report template fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Report template not found")
    })
    public ResponseEntity<InputStreamResource> fetchReporTemplate(
            @Parameter(name = ReporterConst.REPORT_TEMPLATE_ID, description = "The ID of the report template to be fetched", required = true)
            @RequestParam(name = ReporterConst.REPORT_TEMPLATE_ID) String reportTemplateId
    ) throws FileNotFoundException {
        Optional<Path> reportTemplatePath = reportTemplateManager.getReportTemplatePath(reportTemplateId);
        if (reportTemplatePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return createResponseEntity(reportTemplatePath.get());
    }

    @GetMapping(value = ReporterConst.RUNNING_REPORTS)
    @Operation(summary = "Fetch Running Reports", description = "Retrieves a list of running reports.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Running reports fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during running report meta-information retrieval")
    })
    public ResponseEntity fetchRunningReports() throws ReportMetaInfoManagerException {
        return ResponseEntity.ok().body(reportMetaInfoManager.fetchRunningReportMetaInfos());
    }


}
