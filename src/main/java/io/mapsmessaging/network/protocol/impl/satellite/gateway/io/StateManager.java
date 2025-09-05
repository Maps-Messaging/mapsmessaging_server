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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.MapsEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StateManager {

  public static String loadLastMessageUtc(String username, String password) {
    String fileName = generate(username, password);
    Path path = Path.of(fileName);
    if (Files.exists(path)) {
      try {
        return Files.readString(path, StandardCharsets.UTF_8).trim();
      } catch (IOException e) {
        throw new RuntimeException("Failed to read file", e);
      }
    }
    return null; // no state yet
  }

  public static void saveLastMessageUtc(String username, String password, String utc) {
    String fileName = generate(username, password);
    Path path = Path.of(fileName);
    try {
      Files.createDirectories(path.getParent());
      Files.writeString(path, utc, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write file", e);
    }
  }

  private static String generate(String username, String password) {

    try {
      String input = username + ":" + password;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      String file = hex.toString();

      String directory = MapsEnvironment.getMapsData()+File.separator+"satellite";
      return directory + File.separator +   file+".state";
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
