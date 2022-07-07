package io.mapsmessaging.engine.utils;

import java.io.File;

public class FilePathHelper {

  public static String cleanPath(String path) {
    if (File.separatorChar == '/') {
      while (path.indexOf('\\') != -1) {
        path = path.replace("\\", File.separator);
      }
    } else {
      while (path.indexOf('/') != -1) {
        path = path.replace("/", File.separator);
      }
    }
    return path;
  }

  private FilePathHelper() {
  }
}
