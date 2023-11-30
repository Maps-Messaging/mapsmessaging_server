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

package io.mapsmessaging.rest.hawtio;

import java.util.Map;

public class JolokiaLogHandler implements org.jolokia.util.LogHandler {
  @Override
  public void error(String message, Throwable t) {
    System.err.println(message);
    t.printStackTrace();
  }

  @Override
  public void info(String message) {
    System.out.println(message);
  }

  @Override
  public void debug(String message) {
    System.out.println(message);
  }

  public void configure(Map<String, String> config) {
    for (Map.Entry<String, String> entry : config.entrySet()) {
      System.setProperty(entry.getKey(), entry.getValue());
    }
  }
}
