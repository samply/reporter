package de.samply.reporter.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.samply.reporter.template.script.ScriptReference;

public class ColumnTemplate {

  @JacksonXmlProperty(isAttribute = true, localName = "name")
  @JsonProperty(value = "name")
  private String name;

  @JacksonXmlProperty(isAttribute = true, localName = "header-format-script")
  @JsonProperty(value = "header-format-script")
  private ScriptReference headerFormatScript;

  @JacksonXmlProperty(isAttribute = true, localName = "value-format-script")
  @JsonProperty(value = "value-format-script")
  private ScriptReference valueFormatScript;


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ScriptReference getHeaderFormatScript() {
    return headerFormatScript;
  }

  public void setHeaderFormatScript(
      ScriptReference headerFormatScript) {
    this.headerFormatScript = headerFormatScript;
  }

  public ScriptReference getValueFormatScript() {
    return valueFormatScript;
  }

  public void setValueFormatScript(
      ScriptReference valueFormatScript) {
    this.valueFormatScript = valueFormatScript;
  }

}
