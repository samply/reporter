package de.samply.reporter.report.metainfo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import java.nio.file.Path;

public class PathSerializer extends ValueSerializer<Path> {

  @Override
  public void serialize(Path value, JsonGenerator gen, SerializationContext serializers)
      throws JacksonException {
    gen.writeString(value.getFileName().toString());
  }

}
