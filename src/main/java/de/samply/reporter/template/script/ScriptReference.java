package de.samply.reporter.template.script;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ScriptReference {

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "script")
  private Script script;

  public Script getScript() {
    return script;
  }

  public void setScript(Script script) {
    this.script = script;
  }

}
