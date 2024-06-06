/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.test.WaitForState;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class ConnectionTest extends BaseTestConfig {

  protected abstract EndPointConnectionFactory getFactory();
  protected abstract EndPointURL getURL();

  @Test
  void connectToServer() throws IOException {
    EndPointConnectionFactory factory = getFactory();
    EndPointURL url = getURL();
    SelectorLoadManager selectorLoadManager = new SelectorLoadManager(1, url.toString());
    AtomicBoolean connected = new AtomicBoolean(false);
    EndPoint endPoint = factory.connect(url, selectorLoadManager, endpoint -> connected.set(true),
        new EndPointServerStatus(url) {
          @Override
          public EndPointServerConfig getConfig() {
            ConfigurationProperties testConfig = ConfigurationManager.getInstance().getProperties("NetworkManager");
            ConfigurationProperties localTest = new ConfigurationProperties();
            localTest.setGlobal(testConfig.getGlobal());
            localTest.put("name", "testInterface");
            localTest.put("url", "ssl://localhost:1892/");
            return new EndPointServerConfig(localTest);
          }

          @Override
          public void handleNewEndPoint(EndPoint endPoint) {
            connected.set(true);
          }

          @Override
          public void handleCloseEndPoint(EndPoint endPoint) {
            connected.set(false);
          }
        }, new ArrayList<>());

    WaitForState.waitFor(5, TimeUnit.SECONDS, connected::get);
    Assertions.assertTrue(connected.get());

    endPoint.close();
    WaitForState.waitFor(5, TimeUnit.SECONDS, ()->!connected.get());
    selectorLoadManager.close();
  }

}
