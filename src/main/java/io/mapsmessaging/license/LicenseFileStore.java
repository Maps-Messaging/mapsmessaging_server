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
 *  distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.license;

import io.mapsmessaging.MapsEnvironment;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class LicenseFileStore {

  private static final String LICENSE_KEY = "license_";
  private static final String FALLBACK_LICENSE_FILE_NAME = LICENSE_KEY+"community.lic";
  private static final String FALLBACK_EDITION = "community";

  private final Logger logger;

  public LicenseFileStore(Logger logger) {
    this.logger = logger;
  }

  public boolean saveLicenseFile(File licenseDir, String edition, byte[] licenseContent) {
    File licenseFile = new File(licenseDir, LICENSE_KEY + edition + ".lic");
    try (FileOutputStream fileOutputStream = new FileOutputStream(licenseFile)) {
      fileOutputStream.write(licenseContent);
      logger.log(ServerLogMessages.LICENSE_SAVED_TO_FILE, licenseFile.getAbsolutePath());
      return true;
    } catch (IOException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_SAVED_TO_FILE, licenseFile.getAbsolutePath(), e);
      return false;
    }
  }

  /**
   * Ensures a fallback bundled license exists in the expected "license_*.lic" form inside licenseDir.
   *
   * Source order:
   *  1) Classpath: "/conf/community.lic"
   *  2) Classpath: "/community.lic"
   *  3) Filesystem: MAPS_HOME/conf/community.lic
   *
   * Target:
   *  - licenseDir/license_community.lic
   */
  public void ensureFallbackLicensePresent(File licenseDir) {
    if (licenseDir == null || !licenseDir.exists() || !licenseDir.isDirectory()) {
      return;
    }

    File[] existingLicenses = licenseDir.listFiles((dir, name) -> name.startsWith(LICENSE_KEY) && name.endsWith(".lic"));
    if (existingLicenses != null && existingLicenses.length > 0) {
      return;
    }

    File preferred = new File(licenseDir, LICENSE_KEY + FALLBACK_EDITION + ".lic");
    if (preferred.exists()) {
      return;
    }

    if (copyFromClasspath(preferred)) {
      return;
    }

    copyFromMapsHome(preferred);
  }

  private boolean copyFromClasspath(File destination) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = LicenseFileStore.class.getClassLoader();
    }

    InputStream inputStream = classLoader.getResourceAsStream( FALLBACK_LICENSE_FILE_NAME);
    if (inputStream == null) {
      inputStream = classLoader.getResourceAsStream(FALLBACK_LICENSE_FILE_NAME);
    }

    if (inputStream == null) {
      return false;
    }

    try (InputStream sourceStream = inputStream) {
      writeStreamToFile(sourceStream, destination);
      logger.log(ServerLogMessages.LICENSE_SAVED_TO_FILE, destination.getAbsolutePath());
      return true;
    } catch (IOException e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_SAVED_TO_FILE, destination.getAbsolutePath(), e);
      return false;
    }
  }

  private void copyFromMapsHome(File destination) {
    try {
      String mapsHome = MapsEnvironment.getMapsHome();
      if (mapsHome == null || mapsHome.isEmpty()) {
        return;
      }

      Path sourcePath = Path.of(mapsHome, "conf", FALLBACK_LICENSE_FILE_NAME);
      if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
        return;
      }

      copyFile(sourcePath.toFile(), destination);
      logger.log(ServerLogMessages.LICENSE_SAVED_TO_FILE, destination.getAbsolutePath());
    } catch (Exception e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_SAVED_TO_FILE, destination.getAbsolutePath(), e);
    }
  }

  private void copyFile(File source, File destination) throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(source);
         FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = fileInputStream.read(buffer)) != -1) {
        fileOutputStream.write(buffer, 0, read);
      }
    }
  }

  private void writeStreamToFile(InputStream sourceStream, File destination) throws IOException {
    try (FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = sourceStream.read(buffer)) != -1) {
        fileOutputStream.write(buffer, 0, read);
      }
    }
  }
}