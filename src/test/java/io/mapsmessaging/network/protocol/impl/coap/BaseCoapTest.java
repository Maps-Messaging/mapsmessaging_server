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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.test.BaseTestConfig;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.junit.jupiter.api.BeforeAll;

public class BaseCoapTest extends BaseTestConfig {

  private static final String uri = "coap://127.0.0.1:5683/fred-";
  private static final AtomicLong counter = new AtomicLong(0);

  protected static String getUri() {
    return uri + counter.incrementAndGet();
  }

  private static final File CONFIG_FILE = new File("Californium3.properties");
  private static final String CONFIG_HEADER = "Californium CoAP Properties file for client";
  private static final int DEFAULT_MAX_RESOURCE_SIZE = 2 * 1024 * 1024; // 2 MB
  private static final int DEFAULT_BLOCK_SIZE = 2048;
  private static Configuration.DefinitionsProvider DEFAULTS = config -> {
    config.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, DEFAULT_MAX_RESOURCE_SIZE);
    config.set(CoapConfig.MAX_MESSAGE_SIZE, DEFAULT_BLOCK_SIZE);
    config.set(CoapConfig.PREFERRED_BLOCK_SIZE, DEFAULT_BLOCK_SIZE);
  };

  @BeforeAll
  public static void buildClient(){
    Configuration config = Configuration.createWithFile(CONFIG_FILE, CONFIG_HEADER, DEFAULTS);
    Configuration.setStandard(config);
  }
}
