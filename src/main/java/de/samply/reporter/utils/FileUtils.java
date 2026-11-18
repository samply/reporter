package de.samply.reporter.utils;

import de.samply.reporter.app.ReporterConst;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;

public class FileUtils {

  public static String fetchRandomFilename(String fileFormatExtension) {
    return RandomStringUtils.secure().nextAlphabetic(ReporterConst.RANDOM_FILENAME_SIZE) + "."
        + fileFormatExtension;
  }

  public static long fetchNumberOfLines(Path path) throws IOException {
    try (Stream<String> pathStream = Files.lines(path)) {
      return pathStream.count();
    }
  }


}
