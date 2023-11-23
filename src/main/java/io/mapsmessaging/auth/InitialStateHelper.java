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

package io.mapsmessaging.auth;

import io.mapsmessaging.security.identity.IdentityEntry;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.parsers.PasswordParser;
import io.mapsmessaging.security.identity.parsers.sha.Sha512PasswordParser;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Map;

public class InitialStateHelper {

  private InitialStateHelper() {
  }

  public static String checkForFirstInstall(ConfigurationProperties properties) {
    String password = null;
    Map<String, Object> config = ((ConfigurationProperties) properties.get("config")).getMap();
    String authProvider = config.get("identityProvider").toString().trim();
    if (authProvider.equalsIgnoreCase("Apache-Basic-Auth")) {
      String directory = (String) config.get("configDirectory");
      if (directory != null) {
        File file = new File(directory);
        if (!file.exists()) {
          file.mkdirs();
        }
        File passwordFile = new File(file, ".htpassword");
        File groupFile = new File(file, ".htgroup");
        if (!passwordFile.exists()) {
          password = PasswordGenerator.generateRandomPassword(12);
          String salt = PasswordGenerator.generateSalt(16);
          PasswordParser passwordParser = new Sha512PasswordParser();
          salt = passwordParser.getKey() + salt;
          byte[] hash = passwordParser.computeHash(password.getBytes(), salt.getBytes(), passwordParser.getCost());
          try (FileOutputStream fileOutputStream = new FileOutputStream(passwordFile)) {
            fileOutputStream.write(("admin" + ":" + new String(hash)).getBytes());
          } catch (Exception e) {
            //throw new RuntimeException(e);
          }

        }
        if (!groupFile.exists()) {
          try (FileOutputStream fileOutputStream = new FileOutputStream(groupFile)) {
            fileOutputStream.write(("admin:admin").getBytes());
          } catch (Exception e) {
            //throw new RuntimeException(e);
          }
        }
      }
    }
    return password;
  }

  public static boolean validate(String username, String password, IdentityLookup identityLookup) {
    IdentityEntry identityEntry = identityLookup.findEntry(username);
    if (identityEntry != null) {
      PasswordParser passwordParser = identityEntry.getPasswordParser();
      byte[] hash = passwordParser.computeHash(password.getBytes(), passwordParser.getSalt(), passwordParser.getCost());
      return Arrays.equals(hash, identityEntry.getPassword().getBytes());
    }
    return false;
  }
}
