/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.auth.registry;

import java.io.*;

public abstract class FileManager {
  private final File file;

  protected FileManager(String filename) {
    file = new File(filename);
  }

  public boolean exists() {
    return file.exists();
  }

  protected void add(String line) throws IOException {
    if (!file.exists()) {
      file.createNewFile();
    }
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
      bw.write(line);
      bw.newLine(); // Add a newline character after each line
    }
  }

  protected void delete(String name) throws IOException {
    File tempFile = new File(file.getAbsolutePath() + ".tmp");

    try (BufferedReader reader = new BufferedReader(new FileReader(file));
         BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

      String lineToRemove = name + ":";
      String currentLine;

      while ((currentLine = reader.readLine()) != null) {
        if (!currentLine.startsWith(lineToRemove)) {
          writer.write(currentLine + System.lineSeparator());
        }
      }
    }

    if (!file.delete()) {
      throw new IOException("Could not delete original file");
    }

    if (!tempFile.renameTo(file)) {
      throw new IOException("Could not rename temporary file");
    }
  }

}
