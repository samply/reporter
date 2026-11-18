package de.samply.reporter.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import de.samply.reporter.template.script.ScriptReference;
import de.samply.reporter.utils.CloneUtils;
import de.samply.reporter.utils.CloneUtilsException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

@JsonRootName("report")
public class ReportTemplate {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    @JsonProperty(value = "id")
    private String id;

    @JacksonXmlProperty(isAttribute = true, localName = "ignore")
    @JsonProperty(value = "ignore")
    private Boolean ignore;

    @JacksonXmlProperty(isAttribute = true, localName = "filename")
    @JsonProperty(value = "filename")
    private String filename;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("sheet")
    private List<SheetTemplate> sheetTemplates = new ArrayList<>();

    @JacksonXmlProperty(isAttribute = true, localName = "exporter")
    @JsonProperty("exporter")
    private Exporter exporter;

    @JacksonXmlProperty(isAttribute = true, localName = "init-script")
    @JsonProperty("init-script")
    private ScriptReference initScript;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean getIgnore() {
        return (ignore != null) ? ignore : false;
    }

    public void setIgnore(Boolean ignore) {
        this.ignore = ignore;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<SheetTemplate> getSheetTemplates() {
        return sheetTemplates;
    }

    public void setSheetTemplates(
            List<SheetTemplate> sheetTemplates) {
        this.sheetTemplates = sheetTemplates;
    }

    public Exporter getExporter() {
        return exporter;
    }

    public void setExporter(Exporter exporter) {
        this.exporter = exporter;
    }

    public ScriptReference getInitScript() {
        return initScript;
    }

    public void setInitScript(ScriptReference initScript) {
        this.initScript = initScript;
    }

    @Override
    public ReportTemplate clone() throws CloneNotSupportedException {
        try {
            return CloneUtils.clone(this);
        } catch (CloneUtilsException e) {
            throw new CloneNotSupportedException(ExceptionUtils.getStackTrace(e));
        }
    }

}
