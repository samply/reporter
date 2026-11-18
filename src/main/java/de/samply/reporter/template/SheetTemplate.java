package de.samply.reporter.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.samply.reporter.template.script.ScriptReference;
import java.util.ArrayList;
import java.util.List;

public class SheetTemplate {

  @JacksonXmlProperty(isAttribute = true, localName = "values-script")
  @JsonProperty("values-script")
  private ScriptReference valuesScript;

  @JacksonXmlProperty(isAttribute = true, localName = "name")
  @JsonProperty(value = "name")
  private String name;

  @JacksonXmlProperty(isAttribute = true, localName = "file-path")
  @JsonProperty(value = "file-path")
  private String filePath;

  @JacksonXmlProperty(isAttribute = true, localName = "file-url")
  @JsonProperty(value = "file-url")
  private String fileUrl;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JsonProperty("column")
  private List<ColumnTemplate> columnTemplates = new ArrayList<>();

  @JacksonXmlElementWrapper(useWrapping = false)
  @JsonProperty("format-script")
  private List<ScriptReference> formatScripts = new ArrayList<>();


  public ScriptReference getValuesScript() {
    return valuesScript;
  }

  public void setValuesScript(ScriptReference valuesScript) {
    this.valuesScript = valuesScript;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ColumnTemplate> getColumnTemplates() {
    return columnTemplates;
  }

  public void setColumnTemplates(List<ColumnTemplate> columnTemplates) {
    this.columnTemplates = columnTemplates;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public List<ScriptReference> getFormatScripts() {
    return formatScripts;
  }

  public void setFormatScripts(List<ScriptReference> formatScripts) {
    this.formatScripts = formatScripts;
  }

}
