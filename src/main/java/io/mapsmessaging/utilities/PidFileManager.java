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

package io.mapsmessaging.utilities;

import lombok.Getter;

import java.io.*;

public class PidFileManager {

  @Getter
  private final File pidFile;

  public PidFileManager(File pidFile) {
    this.pidFile = pidFile;
  }

  public boolean deletePidFile() {
    return pidFile.delete();
  }

  public void writeNewFile() throws IOException {
    deletePidFile();
    writeToPid(ProcessHandle.current().pid());
  }

  public boolean isProcessRunning(long pid) {
    return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
  }

  public void writeToPid(long pid) throws IOException {
    try (FileWriter writer = new FileWriter(pidFile)) {
      writer.write(Long.toString(pid));
      writer.flush();
    }
  }

  public long readPidFromFile() throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(pidFile))) {
      String line = reader.readLine();
      return Long.parseLong(line);
    } catch (NumberFormatException e) {
      // Handle exceptions or return a default or error code
      return -1; // Return -1 or any other indication of error
    }
  }

  public boolean exists() {
    return pidFile.exists();
  }
}
