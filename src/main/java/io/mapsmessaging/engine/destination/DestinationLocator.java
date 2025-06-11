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
package io.mapsmessaging.engine.destination;

import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.engine.resources.ResourceFactory;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for walking the directory passed in looking for existing destination files.
 *
 * Given the directory layout is flat and the destinations reside in a single directory below the root directory, this class can simply load up the directories under root and see
 * if any of them contain the specific files.
 */
public class DestinationLocator {

  private final DestinationConfigDTO pathManager;
  private final String subDirectory;

  @Getter
  private final List<File> rejected;
  @Getter
  private final List<File> valid;

  public DestinationLocator(DestinationConfigDTO pathManager, String subdirectory) {
    this.pathManager = pathManager;
    this.subDirectory = subdirectory;
    valid = new ArrayList<>();
    rejected = new ArrayList<>();
  }

  public void parse() {
    File path = new File(pathManager.getDirectory());
    List<File> rootPaths = new ArrayList<>();
    if (path.exists()) {
      if (subDirectory != null) {
        scanSubdirectory(path, rootPaths);
      } else {
        rootPaths.add(path);
      }
    }
    valid.addAll(scanForValidDirectories(rootPaths));
  }

  private List<File> scanForValidDirectories(List<File> rootPaths) {
    List<File> validPaths = new ArrayList<>();
    for (File explore : rootPaths) {
      if (explore != null) {
        testFile(explore, validPaths);
      }
    }
    return validPaths;
  }

  private void testFile(File explore, List<File> validPaths) {
    if (confirmPath(explore)) {
      validPaths.add(explore);
    } else {
      File[] fileList = explore.listFiles();
      if (fileList != null) {
        for (File potential : fileList) {
          testFile(potential, validPaths);
        }
      }
    }
  }

  private void scanSubdirectory(File path, List<File> rootPaths) {
    File[] listFiles = path.listFiles();
    if (listFiles != null) {
      for (File sub : listFiles) {
        if (sub.isDirectory()) {
          rootPaths.add(new File(pathManager.getDirectory() + File.separator + sub.getName() + File.separator + subDirectory));
        }
      }
    }
  }

  private boolean confirmPath(File directory) {
    String name = directory.getAbsolutePath();
    File data = new File(name + File.separator + "message.data");
    File resource = new File(name + File.separator + ResourceFactory.RESOURCE_FILE_NAME);

    boolean isDestinationDirectory = (data.exists() && resource.exists());
    if (!isDestinationDirectory) {
      rejected.add(directory);
    }
    return isDestinationDirectory;
  }
}
