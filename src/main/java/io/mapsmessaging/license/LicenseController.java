/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import com.google.gson.Gson;
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

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class LicenseController {

  private static final String LICENSE_KEY = "license_";

  private final List<FeatureDetails> licenses;
  private final Logger logger = LoggerFactory.getLogger(LicenseController.class);

  public LicenseController(String licensePath, String uniqueId, UUID serverUUID) {
    File licenseDir = new File(licensePath);
    if (!licenseDir.exists() || !licenseDir.isDirectory()) {
      throw new IllegalArgumentException("Invalid license path: " + licensePath);
    }

    installLicenses(licenseDir);
    licenses = loadInstalledLicenses(licenseDir);

    if (licenses.isEmpty()) {
      boolean fetched = fetchLicenseFromServer(licenseDir, uniqueId, serverUUID);
      if (!fetched) {
        LicenseFileStore licenseFileStore = new LicenseFileStore(logger);
        licenseFileStore.ensureFallbackLicensePresent(licenseDir);
      }

      installLicenses(licenseDir);
      licenses.addAll(loadInstalledLicenses(licenseDir));
    }

    Gson gson = GsonFactory.getInstance().getPrettyGson();
    for (FeatureDetails feature : licenses) {
      logger.log(ServerLogMessages.LICENSE_FEATURES_AVAILABLE, gson.toJson(feature.getFeature()));
    }
  }

  public FeatureManager getFeatureManager() {
    return new FeatureManager(licenses);
  }

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

  private void processLicenseFile(File licenseFile, String edition, File installedFile) {
    try {
      LicenseManager manager = getLicenseManager(edition);
      if (manager != null) {
        logger.log(ServerLogMessages.LICENSE_INSTALLING, edition);
        manager.install(manager.parameters().encryption().source(BIOS.file(licenseFile)));
        if (!licenseFile.renameTo(installedFile)) {
          logger.log(ServerLogMessages.LICENSE_FILE_RENAME_FAILED, licenseFile.getAbsolutePath(), installedFile.getAbsolutePath());
        }
      } else {
        logger.log(ServerLogMessages.LICENSE_MANAGER_NOT_FOUND, edition);
      }
    } catch (IllegalArgumentException | LicenseManagementException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_INSTALLING, edition, e);
    }
  }

  private LicenseManager getLicenseManager(String edition) {
    for (LicenseManager manager : LicenseManager.values()) {
      if (edition.equalsIgnoreCase(manager.name())) {
        return manager;
      }
    }
    return null;
  }

  private List<FeatureDetails> loadInstalledLicenses(File licenseDir) {
    File[] files = licenseDir.listFiles((dir, name) -> name.startsWith(LICENSE_KEY) && name.endsWith(".lic_installed"));
    if (files == null) {
      return new ArrayList<>();
    }

    List<FeatureDetails> licenseList = new ArrayList<>();

    for (File installedFile : files) {
      String edition = extractEdition(installedFile.getName());
      try {
        LicenseManager manager = getLicenseManager(edition.toUpperCase());
        if (manager != null) {
          logger.log(ServerLogMessages.LICENSE_LOADING, edition);
          if (!processLicense(manager.load(), licenseList)) {
            logger.log(ServerLogMessages.LICENSE_UNINSTALLING, edition);
            if (!installedFile.delete()) {
              logger.log(ServerLogMessages.LICENSE_FAILED_DELETE_FILE, installedFile.getAbsolutePath());
            }
            manager.uninstall();
          }
        } else {
          logger.log(ServerLogMessages.LICENSE_MANAGER_NOT_FOUND, edition);
        }
      } catch (IllegalArgumentException | LicenseManagementException e) {
        logger.log(ServerLogMessages.LICENSE_FAILED_LOADING, edition, e);
      }
    }
    return licenseList;
  }

  @SuppressWarnings("unchecked")
  private boolean processLicense(License license, List<FeatureDetails> licenseList) {
    long now = System.currentTimeMillis();
    if (license != null) {
      if (license.getNotBefore().getTime() < now && license.getNotAfter().getTime() > now) {
        Gson gson = GsonFactory.getInstance().getSimpleGson();
        Map<String, Object> extraData = (Map<String, Object>) license.getExtra();
        String json = gson.toJson(extraData);
        Features features = gson.fromJson(json, Features.class);

        Date end = license.getNotAfter();
        Date start = license.getNotBefore();
        Date issued = license.getIssued();
        String info = license.getInfo();
        String who = license.getIssuer().getName();

        FeatureDetails featureDetails = new FeatureDetails();
        featureDetails.setFeature(features);
        featureDetails.setExpiry(Instant.ofEpochMilli(end.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime());
        featureDetails.setInfo(info);
        licenseList.add(featureDetails);

        logger.log(ServerLogMessages.LICENSE_LOADED, info, who, issued, start, end, gson.toJson(extraData));
        return true;
      } else {
        logger.log(ServerLogMessages.LICENSE_EXPIRED, license.getInfo(), license.getNotBefore(), license.getNotAfter());
        return (license.getNotAfter().getTime() > now);
      }
    }
    return false;
  }

  private boolean fetchLicenseFromServer(File licenseDir, String uniqueId, UUID serverUUID) {
    try {
      LicenseConfig licenseConfig = new LicenseConfig();
      licenseConfig = (LicenseConfig) licenseConfig.load(null);

      String clientSecret = licenseConfig.getClientSecret();
      String clientName = licenseConfig.getClientName();

      LicenseServerClient licenseServerClient = new LicenseServerClient(logger);
      List<LicenseServerResponse> serverLicenses =
          licenseServerClient.fetchLicenses(clientName, clientSecret, uniqueId, serverUUID);

      if (serverLicenses.isEmpty()) {
        return false;
      }

      LicenseFileStore licenseFileStore = new LicenseFileStore(logger);
      boolean savedAny = false;

      for (LicenseServerResponse response : serverLicenses) {
        boolean saved = licenseFileStore.saveLicenseFile(licenseDir, response.getType(), response.getLicenseContent());
        savedAny = savedAny || saved;
      }

      return savedAny;
    } catch (Exception e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_CONTACTING_SERVER, e);
      return false;
    }
  }

  private String extractEdition(String filename) {
    return filename.replace(LICENSE_KEY, "").replace(".lic", "").replace("_installed", "");
  }
}