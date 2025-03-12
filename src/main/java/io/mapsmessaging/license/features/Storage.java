package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Storage {
  private boolean s3Archive;
  private boolean compressionArchive;
  private boolean migrationArchive;

  private boolean fileSupport;
  private boolean cacheSupport;
}
