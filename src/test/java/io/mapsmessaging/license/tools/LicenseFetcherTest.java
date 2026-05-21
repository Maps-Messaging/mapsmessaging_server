package io.mapsmessaging.license.tools;

import io.mapsmessaging.license.LicenseFileStore;
import io.mapsmessaging.license.LicenseServerClient;
import io.mapsmessaging.license.LicenseServerResponse;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

class LicenseFetcherTest {

  @TempDir
  File tempDir;

  @Test
  void runMain_noArgs_returns1() throws Exception {
    int code = LicenseFetcher.runMain(new String[0]);
    Assertions.assertEquals(1, code);
  }

  @Test
  void runMain_createsDir_andReturns10WhenNoLicenses() throws Exception {
    File outputDir = new File(tempDir, "license");

    Logger logger = LoggerFactory.getLogger(LicenseFetcherTest.class);

    LicenseServerClient client = new FakeLicenseServerClient(logger, List.of());
    LicenseFileStore store = new LicenseFileStore(logger);

    int code = LicenseFetcher.run(outputDir, client, store, "build", UUID.fromString("11111111-2222-3333-4444-555555555555"));

    Assertions.assertEquals(10, code);
    Assertions.assertFalse(outputDir.exists() && outputDir.listFiles() != null && outputDir.listFiles().length > 0);
  }

  @Test
  void run_writesLicenseFile_returns0() throws Exception {
    File outputDir = new File(tempDir, "license");
    Assertions.assertTrue(outputDir.mkdirs());

    byte[] licenseBytes = "community-license".getBytes(StandardCharsets.UTF_8);

    LicenseServerResponse response = new LicenseServerResponse("community", licenseBytes);

    Logger logger = LoggerFactory.getLogger(LicenseFetcherTest.class);

    LicenseServerClient client = new FakeLicenseServerClient(logger, List.of(response));
    LicenseFileStore store = new LicenseFileStore(logger);

    int code = LicenseFetcher.run(outputDir, client, store, "build", UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));

    Assertions.assertEquals(0, code);

    File expected = new File(outputDir, "license_community.lic");
    Assertions.assertTrue(expected.exists());

    byte[] written = Files.readAllBytes(expected.toPath());
    Assertions.assertArrayEquals(licenseBytes, written);
  }

  private static final class FakeLicenseServerClient extends LicenseServerClient {

    private final List<LicenseServerResponse> responses;

    private FakeLicenseServerClient(Logger logger, List<LicenseServerResponse> responses) {
      super(logger);
      this.responses = responses;
    }

    @Override
    public List<LicenseServerResponse> fetchLicenses(String clientName, String clientSecret, String uniqueId, UUID serverUUID) {
      return responses;
    }
  }
}