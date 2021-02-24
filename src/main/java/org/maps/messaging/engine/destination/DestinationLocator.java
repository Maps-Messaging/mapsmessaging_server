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

  private final List<File> rejected;
  private final List<File> valid;

  public DestinationLocator(String root, String subdirectory){
    this.root = root;
    this.subDirectory = subdirectory;
    valid = new ArrayList<>();
    rejected = new ArrayList<>();
  }

  public List<File> parse(){
    File path = new File(root);
    List<File> rootPaths = new ArrayList<>();
    if(path.exists()) {
      if (subDirectory != null) {
        for (File sub : path.listFiles()) {
          if (sub.isDirectory()) {
            rootPaths.add(new File(root + File.separator + sub.getName() + File.separator + subDirectory));
          }
        }
      } else {
        rootPaths.add(path);
      }
    }
    for(File explore:rootPaths){
      if (confirmPath(explore)) {
        valid.add(explore);
      }
      else {
        for (File potential : explore.listFiles()) {
          if (confirmPath(potential)) {
            valid.add(potential);
          }
        }
      }
    }

    return valid;
  }

  public List<File> getRejected() {
    return rejected;
  }

  public List<File> getValid() {
    return valid;
  }

  public boolean confirmPath(File directory){
    String name = directory.getAbsolutePath();
    File data = new File(name+File.separator+"data.bin");
    File resource = new File(name+File.separator+ ResourceFactory.ResourceFileName);
    File delayed = new File(name+File.separator+"state"+File.separator+"delayed.bin");
    File transaction = new File(name+File.separator+"state"+File.separator+"transactions.bin");

    boolean isDestinationDirectory = (data.exists() && resource.exists() && delayed.exists() && transaction.exists());
    if(data.exists() || resource.exists() || delayed.exists()||transaction.exists()){
      if(!isDestinationDirectory){
        rejected.add(directory);
      }
    }

    return isDestinationDirectory;
  }
}
