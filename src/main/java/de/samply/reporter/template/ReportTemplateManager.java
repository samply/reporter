package de.samply.reporter.template;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.xml.XmlMapper;
import de.samply.reporter.app.ReporterConst;
import de.samply.reporter.template.script.Script;
import de.samply.reporter.template.script.ScriptParser;
import de.samply.reporter.template.script.ScriptReference;
import de.samply.reporter.utils.VariablesReplacer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.Files.*;

@Component
public class ReportTemplateManager {

    private final VariablesReplacer variablesReplacer;
    private final String customTemplateId;
    private final Map<String, ReportTemplate> idQualityReportTemplateMap = new HashMap<>();
    private final Map<String, Path> idQualityReportTemplatePathMap = new HashMap<>();
    private final Path reportTemplateDirectory;

    public ReportTemplateManager(
            VariablesReplacer variablesReplacer,
            @Value(ReporterConst.CUSTOM_TEMPLATE_ID_SV) String customTemplateId,
            @Value(ReporterConst.REPORT_TEMPLATE_DIRECTORY_SV) String reportTemplateDirectory
    ) {
        this.variablesReplacer = variablesReplacer;
        this.customTemplateId = customTemplateId;
        this.reportTemplateDirectory = Path.of(reportTemplateDirectory);
        loadTemplates(this.reportTemplateDirectory);
    }


    private void loadTemplates(Path templateDirectory) {
        try {
            loadTemplatesWithoutExceptionHandling(templateDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTemplatesWithoutExceptionHandling(Path templateDirectory)
            throws IOException {
        if (exists(templateDirectory)) {
            try (Stream<Path> pathStream = list(templateDirectory)) {
                pathStream.filter(path -> !isDirectory(path)).forEach(this::loadTemplate);
            }
        }
    }

    private void loadTemplate(Path templatePath) {
        try {
            loadTemplateWithoutExceptionHandling(templatePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTemplateWithoutExceptionHandling(Path templatePath) throws IOException {
        ReportTemplate reportTemplate = fetchTemplate(templatePath);
        if (!reportTemplate.getIgnore()) {
            idQualityReportTemplateMap.put(reportTemplate.getId(), reportTemplate);
            idQualityReportTemplatePathMap.put(reportTemplate.getId(), templatePath);
        }
    }

    public ReportTemplate fetchTemplate(Path templatePath) throws IOException {
        return fetchTemplate(readString(templatePath));
    }

    public ReportTemplate fetchTemplate(String template) throws IOException {
        ReportTemplate reportTemplate;
        try {
            reportTemplate = new XmlMapper()
                    .readValue(ScriptParser.readTemplateAndParseScripts(template), ReportTemplate.class);
        } catch (JacksonException e) {
            // Jackson 3 exceptions are unchecked; keep the IOException contract of this method
            throw new IOException(e);
        }
        return fetchScriptFilesAndAddToTemplate(reportTemplate);
    }

    private ReportTemplate fetchScriptFilesAndAddToTemplate(ReportTemplate template) throws IOException {
        try {
            fetchScriptReferences(template).forEach(scriptReference -> {
                try {
                    fetchScriptFileAndAddToScriptReference(scriptReference);
                    filterIgnoredLinesInScript(scriptReference);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return template;
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private List<ScriptReference> fetchScriptReferences(ReportTemplate template) {
        ScriptReferenceList result = new ScriptReferenceList();
        result.add(template.getInitScript());
        template.getSheetTemplates().forEach(sheetTemplate -> {
            sheetTemplate.getFormatScripts().forEach(result::add);
            sheetTemplate.getColumnTemplates().stream().map(ColumnTemplate::getValueFormatScript).forEach(result::add);
            sheetTemplate.getColumnTemplates().stream().map(ColumnTemplate::getHeaderFormatScript).forEach(result::add);
            result.add(sheetTemplate.getValuesScript());
        });
        return result.toList();
    }

    private static class ScriptReferenceList {
        private final List<ScriptReference> scriptReferenceList = new ArrayList<>();

        public void add(ScriptReference scriptReference) {
            if (scriptReference != null) {
                scriptReferenceList.add(scriptReference);
            }
        }

        public List<ScriptReference> toList() {
            return scriptReferenceList;
        }
    }

    private void fetchScriptFileAndAddToScriptReference(ScriptReference scriptReference) throws IOException {
        Script script = scriptReference.getScript();
        Optional<Path> path = fetchPath(script);
        if (path.isPresent()) {
            script.setValue(fetchScript(path.get()));
        }
    }

    private Optional<Path> fetchPath(Script script) {
        if (script != null && script.getFilePath() != null) {
            Path path = Path.of(script.getFilePath());
            return Optional.of((path.isAbsolute()) ? path : this.reportTemplateDirectory.resolve(path));
        }
        return Optional.empty();
    }

    private String fetchScript(Path scriptPath) throws IOException {
        return new String(readAllBytes(scriptPath), StandardCharsets.UTF_8);
    }

    private void filterIgnoredLinesInScript(ScriptReference scriptReference) {
        if (scriptReference != null && scriptReference.getScript() != null && scriptReference.getScript().getValue() != null) {
            Script script = scriptReference.getScript();
            script.setValue(filterIgnoredLinesInScript(script.getValue()));
        }
    }

    private String filterIgnoredLinesInScript(String script) {
        StringBuilder result = new StringBuilder();
        Arrays.stream(script.split("\n"))
                .filter(this::isNotIgnoredLine).map(line -> line.concat("\n")).forEach(result::append);
        return result.toString();
    }

    private boolean isNotIgnoredLine(String line) {
        return !Arrays.stream(ReporterConst.IGNORE_LINE_IN_SCRIPT_TOKENS)
                .anyMatch(stringArray -> Arrays.stream(stringArray).allMatch(line::contains));
    }

    public ReportTemplate fetchTemplateAndGenerateCustomTemplateId(String template) throws IOException {
        ReportTemplate result = fetchTemplate(template);
        result.setId(generateCustomTemplateId());
        return result;
    }

    private String generateCustomTemplateId() {
        return variablesReplacer.replaceTimestamp(customTemplateId);
    }

    public ReportTemplate getQualityReportTemplate(String qualityReportTemplateId) {
        return idQualityReportTemplateMap.get(qualityReportTemplateId);
    }

    public String[] getReportTemplateIds() {
        return idQualityReportTemplateMap.keySet().toArray(new String[0]);
    }

    public Optional<Path> getReportTemplatePath(String reportTemplateId) {
        return Optional.ofNullable(idQualityReportTemplatePathMap.get(reportTemplateId));
    }

}
