package de.samply.reporter.template.script;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

public class Script {

  @JacksonXmlProperty(isAttribute = true, localName = "framework")
  @JsonProperty(value = "framework")
  private String framework;

  @JacksonXmlProperty(isAttribute = true, localName = "file-path")
  @JsonProperty(value = "file-path")
  private String filePath;

  @JacksonXmlText
  private String value;

  public String getFramework() {
    return framework;
  }

  public void setFramework(String framework) {
    this.framework = framework;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

}
