package io.mapsmessaging.license.tools;

import io.mapsmessaging.license.LicenseFileStore;
import io.mapsmessaging.license.LicenseServerClient;
import io.mapsmessaging.license.LicenseServerResponse;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class LicenseFetcher {

  public static void main(String[] args) throws Exception {
    int exitCode = runMain(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  static int runMain(String[] args) throws Exception {
    if (args == null || args.length != 1) {
      System.err.println("Usage: LicenseFetcher <license-dir>");
      return 1;
    }

    File licenseDir = new File(args[0]);
    if (!licenseDir.exists() && !licenseDir.mkdirs()) {
      System.err.println("Failed to create license directory: " + licenseDir.getAbsolutePath());
      return 2;
    }

    Logger logger = LoggerFactory.getLogger(LicenseFetcher.class);

    String uniqueId = "build";
    UUID serverUUID = UUID.randomUUID();

    LicenseServerClient client = new LicenseServerClient(logger);
    LicenseFileStore store = new LicenseFileStore(logger);

    return run(licenseDir, client, store, uniqueId, serverUUID);
  }

  static int run(
      File licenseDir,
      LicenseServerClient licenseServerClient,
      LicenseFileStore licenseFileStore,
      String uniqueId,
      UUID serverUUID
  ) {
    if (licenseDir == null || licenseServerClient == null || licenseFileStore == null) {
      return 3;
    }

    List<LicenseServerResponse> licenses = licenseServerClient.fetchLicenses("", "", uniqueId, serverUUID);
    if (licenses == null || licenses.isEmpty()) {
      return 10;
    }

    boolean wroteAtLeastOne = false;

    for (LicenseServerResponse license : licenses) {
      if (license == null || license.getType() == null || license.getLicenseContent() == null) {
        continue;
      }

      boolean saved = licenseFileStore.saveLicenseFile(
          licenseDir,
          license.getType(),
          license.getLicenseContent()
      );

      wroteAtLeastOne = wroteAtLeastOne || saved;
    }

    return wroteAtLeastOne ? 0 : 11;
  }
}