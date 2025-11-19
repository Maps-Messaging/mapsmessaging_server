/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.license;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseManagementException;
import io.mapsmessaging.config.LicenseConfig;
import io.mapsmessaging.keymgr.LicenseManager;
import io.mapsmessaging.license.features.Features;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.GsonFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

public class LicenseController {

  private static final String LICENSE_SERVER_URL = "https://license.mapsmessaging.io/api/v1/license";

  private static final String LICENSE_KEY="license_";

  private final List<FeatureDetails> licenses;
  private final Logger logger = LoggerFactory.getLogger(LicenseController.class);

  public LicenseController(String licensePath, String uniqueId, UUID serverUUID) {
    File licenseDir = new File(licensePath);
    if (!licenseDir.exists() || !licenseDir.isDirectory()) {
      throw new IllegalArgumentException("Invalid license path: " + licensePath);
    }
    installLicenses(licenseDir);
    licenses = loadInstalledLicenses(licenseDir);
    if(licenses.isEmpty()) {
      fetchLicenseFromServer(licenseDir, uniqueId, serverUUID);
      installLicenses(licenseDir);
      licenses.addAll(loadInstalledLicenses(licenseDir));
    }
    Gson gson = GsonFactory.getInstance().getPrettyGson();
    for(FeatureDetails feature : licenses) {
      logger.log(ServerLogMessages.LICENSE_FEATURES_AVAILABLE, gson.toJson(feature.getFeature()));
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
    File[] files = licenseDir.listFiles((dir, name) -> name.startsWith(LICENSE_KEY) && name.endsWith(".lic"));
    if (files == null) {
      return;
    }

    for (File licenseFile : files) {
      String edition = extractEdition(licenseFile.getName());
      File installedFile = new File(licenseDir, LICENSE_KEY + edition + ".lic_installed");

      if (!installedFile.exists()) {
        processLicenseFile(licenseFile, edition, installedFile);
      }
    }
  }


  private void processLicenseFile(File licenseFile, String edition,  File installedFile) {
    try {
      LicenseManager manager = getLicenseManager(edition);
      if(manager != null) {
        logger.log(ServerLogMessages.LICENSE_INSTALLING, edition);
        manager.install(manager.parameters().encryption().source(BIOS.file(licenseFile)));
        if(!licenseFile.renameTo(installedFile)){
          logger.log(ServerLogMessages.LICENSE_FILE_RENAME_FAILED, licenseFile.getAbsolutePath(), installedFile.getAbsolutePath());
        }
      }
      else{
        logger.log(ServerLogMessages.LICENSE_MANAGER_NOT_FOUND, edition);
      }
    } catch (IllegalArgumentException | LicenseManagementException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_INSTALLING, edition, e);
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
  private  List<FeatureDetails> loadInstalledLicenses(File licenseDir) {
    File[] files = licenseDir.listFiles((dir, name) -> name.startsWith(LICENSE_KEY) && name.endsWith(".lic_installed"));
    if (files == null) return new ArrayList<>();

    List<FeatureDetails> licenseList = new ArrayList<>();

    for (File installedFile : files) {
      String edition = extractEdition(installedFile.getName());
      try {
        LicenseManager manager = getLicenseManager(edition.toUpperCase());
        if(manager != null) {
          logger.log(ServerLogMessages.LICENSE_LOADING, edition);
          if(!processLicense( manager.load(), licenseList)){
            logger.log(ServerLogMessages.LICENSE_UNINSTALLING, edition);
            if(!installedFile.delete()){
              logger.log(ServerLogMessages.LICENSE_FAILED_DELETE_FILE, installedFile.getAbsolutePath());
            }
            manager.uninstall();
          }
        }
        else{
          logger.log(ServerLogMessages.LICENSE_MANAGER_NOT_FOUND, edition);
        }
      } catch (IllegalArgumentException | LicenseManagementException e) {
        logger.log(ServerLogMessages.LICENSE_FAILED_LOADING, edition, e);
      }
    }
    return licenseList;
  }

  private boolean processLicense(License license,List<FeatureDetails> licenseList) {
    long now = System.currentTimeMillis();
    if(license != null) {
      if (license.getNotBefore().getTime() < now && license.getNotAfter().getTime() > now) {
        Gson gson = GsonFactory.getInstance().getSimpleGson();
        Map<String, Object> extraData = (Map<String, Object>) license.getExtra();
        String json = gson.toJson(extraData);
        Features features = gson.fromJson(json, Features.class);
        Date after = license.getNotAfter();
        Date before = license.getNotBefore();
        Date issued = license.getIssued();
        String info = license.getInfo();
        String who = license.getIssuer().getName();
        FeatureDetails featureDetails = new FeatureDetails();
        featureDetails.setFeature(features);
        featureDetails.setExpiry(Instant.ofEpochMilli(after.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        featureDetails.setInfo(info);
        licenseList.add(featureDetails);


        logger.log(ServerLogMessages.LICENSE_LOADED, info, who, issued, after, before,  gson.toJson(extraData));
        return true;
      } else {
        logger.log(ServerLogMessages.LICENSE_EXPIRED, license.getInfo(), license.getNotBefore(), license.getNotAfter());
        return (license.getNotAfter().getTime() > now); // Do NOT delete the license if it is still valid but can not yet be used
      }
    }
    return false;
  }

  private void fetchLicenseFromServer(File licenseDir, String uniqueId, UUID serverUUID) {
    try {
      LicenseConfig licenseConfig = new LicenseConfig();
      licenseConfig = (LicenseConfig) licenseConfig.load(null);

      String clientSecret = licenseConfig.getClientSecret();
      String clientName = licenseConfig.getClientName();
      HttpURLConnection connection = (HttpURLConnection) new URL(LICENSE_SERVER_URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");

// Build JSON request body
      String jsonBody = String.format(
          "{\"clientName\":\"%s\",\"clientSecret\":\"%s\",\"uniqueServerId\":\"%s\",\"serverUUID\":\"%s\"}",
          clientName, clientSecret, uniqueId, serverUUID.toString()
      );
      logger.log(ServerLogMessages.LICENSE_CONTACTING_SERVER, clientName);
      try (OutputStream os = connection.getOutputStream()) {
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
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
          List<Map<String, String>> mapList = parseLicenseResponse(response.toString());
          for (Map<String, String> licenseData : mapList) {
            String type = licenseData.get("type");
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] license = decoder.decode (licenseData.get("license"));
            if (type != null && license != null) {
              saveLicenseFile(licenseDir, type, license);
            }
          }
        }
      } else {
        logger.log(ServerLogMessages.LICENSE_ERROR_CONTACTING_SERVER, responseCode);
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_CONTACTING_SERVER, e);
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
      return List.of();
    }
  }

  /**
   * Saves the retrieved license file to disk.
   */
  private void saveLicenseFile(File licenseDir, String edition, byte[] licenseContent) {
    File licenseFile = new File(licenseDir, LICENSE_KEY + edition + ".lic");
    try (FileOutputStream fos = new FileOutputStream(licenseFile)) {
      fos.write(licenseContent);
      logger.log(ServerLogMessages.LICENSE_SAVED_TO_FILE, licenseFile.getAbsolutePath());
    } catch (IOException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_SAVED_TO_FILE, licenseFile.getAbsolutePath(), e);
    }
  }

  /**
   * Extracts the edition name from a license file name.
   * Example: "license_enterprise.lic" -> "enterprise"
   *
   * @param filename License file name.
   * @return Extracted edition.
   */
  private String extractEdition(String filename) {
    return filename.replace(LICENSE_KEY, "").replace(".lic", "").replace("_installed", "");
  }

}
