/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.configuration;

import java.util.ArrayList;

/**
 * This class detects the JDK version and will determine which features are or are not supported based on the Major version of the JDK.
 *
 * <p>Currently only java.nio.Selector has changed in JDK11 and up to be thread safe and hence makes
 * the code cleaner
 */
public class EnvironmentConfig {

  private static final EnvironmentConfig instance = new EnvironmentConfig();
  private final int major;

  private EnvironmentConfig() {
    major = getVersion(System.getProperty("java.version"));
  }

  public static EnvironmentConfig getInstance() {
    return instance;
  }

  /**
   * For JDK versions from 11 onwards the java.nio.selector supports multithreaded access, prior to this the selector collection did not support multithreaded access and would lock
   * on the same thread as the select() call which would result in live lock conditions.
   *
   * @return true if this version of Java is thread safe in the selector
   */
  public boolean supportThreadSafeSelector() {
    return (major >= 11);
  }

  public int getMajor() {
    return major;
  }

  int getVersion(String versionTest) {
    ArrayList<Integer> results = parseVersion(versionTest);
    if (!results.isEmpty()) {
      int testMajor = results.get(0);
      // Ignore this since 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8 are all major 1
      if (testMajor == 1) {
        testMajor = results.get(1);
      }
      return testMajor;
    }
    return -1;
  }

  ArrayList<Integer> parseVersion(String versionTest) {
    ArrayList<Integer> results = new ArrayList<>();
    if (versionTest != null) {
      StringBuilder valueString = new StringBuilder();
      for (char ch : versionTest.toCharArray()) {
        if (Character.isDigit(ch)) {
          valueString.append(ch);
        } else {
          if (valueString.length() > 0) {
            results.add(parse(valueString.toString()));
            valueString = new StringBuilder();
          } else {
            break;
          }
        }
      }
      if (valueString.length() > 0) {
        results.add(parse(valueString.toString()));
      }
    }
    return results;
  }

  private int parse(Object value) {
    String txt = value.toString().trim();
    return Integer.parseInt(txt);
  }
}
