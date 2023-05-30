package de.samply.reporter.report.metainfo;

import de.samply.reporter.app.ReporterConst;
import de.samply.reporter.template.ReportTemplate;
import de.samply.reporter.utils.VariablesReplacer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportMetaInfoManager {

  private Path reportsMetaInfoFile;
  private Path reportsDirectory;
  private VariablesReplacer variablesReplacer;

  public ReportMetaInfoManager(
      VariablesReplacer variablesReplacer,
      @Value(ReporterConst.REPORTS_DIRECTORY_SV) String reportsDirectory,
      @Value(ReporterConst.REPORTS_META_INFO_FILENAME_SV) String reportsMetaInfoFilename
  ) throws ReportMetaInfoManagerException, IOException {
    this.reportsDirectory = Path.of(reportsDirectory);
    this.reportsMetaInfoFile = this.reportsDirectory.resolve(reportsMetaInfoFilename);
    this.variablesReplacer = variablesReplacer;

    ReportMetaInfo[] reportMetaInfos = fetchAllReportMetaInfos();
    recreateReportMetaInfoFile(List.of(reportMetaInfos));
  }

  public ReportMetaInfo createNewReportMetaInfo(ReportTemplate template)
      throws ReportMetaInfoManagerException {
    Path reportPath = reportsDirectory.resolve(
        variablesReplacer.fetchQualityReportFilename(template));
    String timestamp = fetchCurrentTimestamp();
    String reportId = generateReportId();
    ReportMetaInfo reportMetaInfo = new ReportMetaInfo(reportId, reportPath, timestamp);
    addReportMetaInfoToFile(reportMetaInfo);
    return reportMetaInfo;
  }

  private void addReportMetaInfoToFile(ReportMetaInfo reportMetaInfo)
      throws ReportMetaInfoManagerException {
    try {
      addReportMetaInfoToFileWithoutExceptionHandling(reportMetaInfo);
    } catch (IOException e) {
      throw new ReportMetaInfoManagerException(e);
    }
  }

  private void addReportMetaInfoToFileWithoutExceptionHandling(ReportMetaInfo reportMetaInfo)
      throws IOException {
    checkIfMetaInfoFileExistsAndCreateIfNot();
    Files.write(reportsMetaInfoFile, reportMetaInfo.toString().getBytes(),
        StandardOpenOption.APPEND);
  }

  private void checkIfMetaInfoFileExistsAndCreateIfNot() throws IOException {
    if (!reportsMetaInfoFile.toFile().exists()) {
      reportsMetaInfoFile.toFile().createNewFile();
    }
  }

  private String fetchCurrentTimestamp() {
    return new SimpleDateFormat(ReporterConst.DEFAULT_TIMESTAMP_FORMAT).format(
        Timestamp.from(Instant.now()));
  }

  private String generateReportId() {
    return RandomStringUtils.random(ReporterConst.RANDOM_REPORT_ID_SIZE, true, false);
  }

  public ReportMetaInfo[] fetchAllReportMetaInfos() throws ReportMetaInfoManagerException {
    try {
      return fetchAllReportMetaInfosWithoutExceptionHandling();
    } catch (IOException e) {
      throw new ReportMetaInfoManagerException(e);
    }
  }

  private ReportMetaInfo[] fetchAllReportMetaInfosWithoutExceptionHandling()
      throws IOException {
    List<ReportMetaInfo> result = new ArrayList<>();
    checkIfMetaInfoFileExistsAndCreateIfNot();
    Files.readAllLines(reportsMetaInfoFile)
        .forEach(line -> {
          ReportMetaInfo reportMetaInfo = ReportMetaInfo.create(reportsDirectory, line);
          if (reportMetaInfo.path().toFile().exists()) {
            result.add(reportMetaInfo);
          }
        });
    return result.toArray(new ReportMetaInfo[0]);
  }

  private void recreateReportMetaInfoFile(List<ReportMetaInfo> reportMetaInfos) throws IOException {
    if (reportsMetaInfoFile.toFile().exists()) {
      Files.delete(reportsMetaInfoFile);
    }
    reportMetaInfos.forEach(reportMetaInfo -> {
      try {
        addReportMetaInfoToFile(reportMetaInfo);
      } catch (ReportMetaInfoManagerException e) {
        throw new RuntimeException(e);
      }
    });

  }

}
