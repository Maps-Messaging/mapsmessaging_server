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

package io.mapsmessaging.rest.jolokia;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class JolokiaLogHandler implements org.jolokia.util.LogHandler {
  private final Logger logger = LoggerFactory.getLogger(JolokiaLogHandler.class);

  @Override
  public void error(String message, Throwable t) {
    logger.log(JOLOKIA_ERROR_LOG, message, t);
  }

  @Override
  public void info(String message) {
    logger.log(JOLOKIA_INFO_LOG, message);
  }

  @Override
  public void debug(String message) {
    logger.log(JOLOKIA_DEBUG_LOG, message);
  }

}
