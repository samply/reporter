package de.samply.reporter.zip;

public class ExporterUnzipperException extends Exception {

  public ExporterUnzipperException(String message) {
    super(message);
  }

  public ExporterUnzipperException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExporterUnzipperException(Throwable cause) {
    super(cause);
  }

  public ExporterUnzipperException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
