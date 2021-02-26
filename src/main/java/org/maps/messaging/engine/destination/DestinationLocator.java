/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.maps.messaging.engine.destination;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.maps.messaging.engine.resources.ResourceFactory;

/**
 * This class is responsible for walking the directory passed in looking for existing destination files.
 *
 * Given the directory layout is flat and the destinations reside in a single directory below the root directory, this
 * class can simply load up the directories under root and see if any of them contain the specific files.
 *
 */
public class DestinationLocator {

  private final String root;
  private final String subDirectory;

  @Getter private final List<File> rejected;
  @Getter private final List<File> valid;

  public DestinationLocator(String root, String subdirectory){
    this.root = root;
    this.subDirectory = subdirectory;
    valid = new ArrayList<>();
    rejected = new ArrayList<>();
  }

  public void parse(){
    File path = new File(root);
    List<File> rootPaths = new ArrayList<>();
    if(path.exists()) {
      if (subDirectory != null) {
        scanSubdirectory(path, rootPaths);
      } else {
        rootPaths.add(path);
      }
    }
    valid.addAll(scanForValidDirectories(rootPaths));
  }

  private List<File> scanForValidDirectories(List<File> rootPaths){
    List<File> validPaths = new ArrayList<>();
    for(File explore:rootPaths){
      if (confirmPath(explore)) {
        validPaths.add(explore);
      }
      else {
        for (File potential : explore.listFiles()) {
          if (confirmPath(potential)) {
            validPaths.add(potential);
          }
        }
      }
    }
    return validPaths;
  }

  private void scanSubdirectory( File path,  List<File> rootPaths){
    for (File sub : path.listFiles()) {
      if (sub.isDirectory()) {
        rootPaths.add(new File(root + File.separator + sub.getName() + File.separator + subDirectory));
      }
    }
  }

  private boolean confirmPath(File directory){
    String name = directory.getAbsolutePath();
    File data = new File(name+File.separator+"data.bin");
    File resource = new File(name+File.separator+ ResourceFactory.RESOURCE_FILE_NAME);
    File delayed = new File(name+File.separator+"state"+File.separator+"delayed.bin");
    File transaction = new File(name+File.separator+"state"+File.separator+"transactions.bin");

    boolean isDestinationDirectory = (data.exists() && resource.exists() && delayed.exists() && transaction.exists());
    if((data.exists() || resource.exists() || delayed.exists()|| transaction.exists()) && !isDestinationDirectory) {
      rejected.add(directory);
    }
    return isDestinationDirectory;
  }
}
