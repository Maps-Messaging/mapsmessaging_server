package io.mapsmessaging.license;

import java.util.Base64;

import com.google.gson.Gson;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseManagementException;
import io.mapsmessaging.keymgr.LicenseManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.license.features.Features;

import java.util.List;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class LicenseController {

  private static final String LICENSE_SERVER_URL = "http://localhost:8080/api/v1/licenses/retrieveLicense";

  private final List<Features> licenses;

  public LicenseController(String licensePath) {
    File licenseDir = new File(licensePath);
    if (!licenseDir.exists() || !licenseDir.isDirectory()) {
      throw new IllegalArgumentException("Invalid license path: " + licensePath);
    }

    installLicenses(licenseDir);
    licenses = loadInstalledLicenses(licenseDir);
    if(licenses.isEmpty()) {
      fetchLicenseFromServer(licenseDir, "", "");
    }
  }

  public FeatureManager getFeatureManager() {
    return new FeatureManager(licenses);
  }

  /**
   * Installs any licenses that have not yet been installed.
   *
   * @param licenseDir Directory containing license files.
   */
  private void installLicenses(File licenseDir) {
    File[] files = licenseDir.listFiles((dir, name) -> name.startsWith("license_") && name.endsWith(".lic"));
    if (files == null) return;

    for (File licenseFile : files) {
      String edition = extractEdition(licenseFile.getName());
      File installedFile = new File(licenseDir, "license_" + edition + ".lic_installed");

      if (!installedFile.exists()) {
        try {
          LicenseManager manager = getLicenseManager(edition);
          System.err.println("Installing License: " + edition);
          manager.install(manager.parameters().encryption().source(BIOS.file(licenseFile)));
          licenseFile.renameTo(installedFile);
        } catch (IllegalArgumentException e) {
          System.err.println("Unknown license edition: " + edition);
        } catch (LicenseManagementException e) {
            e.printStackTrace();
        }
      }
    }
  }

  private LicenseManager getLicenseManager(String edition) {
    for(LicenseManager manager : LicenseManager.values()) {
      if(edition.equalsIgnoreCase(manager.name())) {
        return manager;
      }
    }
    return null;
  }

  /**
   * Scans installed licenses and loads them.
   *
   * @param licenseDir Directory containing installed license files.
   */
  private  List<Features> loadInstalledLicenses(File licenseDir) {
    File[] files = licenseDir.listFiles((dir, name) -> name.startsWith("license_") && name.endsWith(".lic_installed"));
    if (files == null) return new ArrayList<>();

    List<Features> licenseList = new ArrayList<>();

    for (File installedFile : files) {
      String edition = extractEdition(installedFile.getName());

      try {
        LicenseManager manager = getLicenseManager(edition.toUpperCase());
        System.err.println("Loading License: " + edition);
        License license = manager.load();
        Gson gson = new Gson();
        Map<String, Object> extraData = (Map<String, Object>)license.getExtra();
        String json = gson.toJson(extraData);
        Features features = gson.fromJson(json, Features.class);
        licenseList.add(features);
      } catch (IllegalArgumentException e) {
        System.err.println("Unknown license edition: " + edition);
      } catch (LicenseManagementException e) {
        e.printStackTrace();
      }
    }
    return licenseList;
  }

  private void fetchLicenseFromServer(File licenseDir, String customerName, String customerKey) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(LICENSE_SERVER_URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // Build form-encoded request body
      String formData = "customerName=" + encode(customerName) + "&customerKey=" + encode(customerKey);

      try (OutputStream os = connection.getOutputStream()) {
        os.write(formData.getBytes(StandardCharsets.UTF_8));
      }

      // Read response
      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

          StringBuilder response = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
            response.append(line);
          }

          // Parse response
          List<Map<String, String>> licenses = parseLicenseResponse(response.toString());
          for (Map<String, String> licenseData : licenses) {
            String type = licenseData.get("type");
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] license = decoder.decode (licenseData.get("license"));
            if (type != null && license != null) {
              saveLicenseFile(licenseDir, type, license);
            }
          }
        }
      } else {
        System.err.println("Failed to retrieve license. Server responded with: " + responseCode);
      }
    } catch (IOException e) {
      System.err.println("Error contacting license server: " + e.getMessage());
    }
  }

  /**
   * Parses the JSON response from the license server.
   *
   * @param response JSON response string.
   * @return A list of license details (type and license string).
   */
  private List<Map<String, String>> parseLicenseResponse(String response) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(response, new TypeReference<List<Map<String, String>>>() {});
    } catch (Exception e) {
      System.err.println("Failed to parse license response: " + e.getMessage());
      return List.of();
    }
  }


  /**
   * Encodes form parameters to avoid issues with special characters.
   */
  private String encode(String value) {
    return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * Saves the retrieved license file to disk.
   */
  private void saveLicenseFile(File licenseDir, String edition, byte[] licenseContent) {
    System.err.println("Sink Content Length: " + licenseContent.length);
    System.err.println("Sink Content (Hex Preview): " + bytesToHex(licenseContent, licenseContent.length));

    File licenseFile = new File(licenseDir, "license_" + edition + ".lic");
    try (FileOutputStream fos = new FileOutputStream(licenseFile)) {
      fos.write(licenseContent);
      System.err.println("License file saved: " + licenseFile.getAbsolutePath());
    } catch (IOException e) {
      System.err.println("Failed to save license file: " + e.getMessage());
    }
  }


  private String bytesToHex(byte[] bytes, int limit) {
    StringBuilder sb = new StringBuilder();
    int count = Math.min(bytes.length, limit);
    int br = 0;
    for (int i = 0; i < count; i++) {
      sb.append(String.format("%02X ", bytes[i]));
      if(br++ == 60){
        br = 0;
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Extracts the edition name from a license file name.
   * Example: "license_enterprise.lic" -> "enterprise"
   *
   * @param filename License file name.
   * @return Extracted edition.
   */
  private String extractEdition(String filename) {
    return filename.replace("license_", "").replace(".lic", "").replace("_installed", "");
  }

  public static void main(String[] args) throws Exception {
    LicenseController licenseController = new LicenseController(".");
  }
}
