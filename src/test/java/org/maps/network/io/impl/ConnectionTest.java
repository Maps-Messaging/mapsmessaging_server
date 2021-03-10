/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package org.maps.network.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.test.BaseTestConfig;
import org.maps.test.WaitForState;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.configuration.ConfigurationProperties;

public abstract class ConnectionTest extends BaseTestConfig {

  protected abstract EndPointConnectionFactory getFactory();
  protected abstract EndPointURL getURL();

  @Test
  void connectToServer() throws IOException {
    SelectorLoadManager selectorLoadManager = new SelectorLoadManager(1);
    EndPointConnectionFactory factory = getFactory();
    EndPointURL url = getURL();
    AtomicBoolean connected = new AtomicBoolean(false);
    EndPoint endPoint = factory.connect(url, selectorLoadManager, endpoint -> connected.set(true),
        new EndPointServerStatus(url) {
          @Override
          public NetworkConfig getConfig() {
            ConfigurationProperties testConfig = ConfigurationManager.getInstance().getProperties("NetworkManager");
            ConfigurationProperties configurationProperties = new ConfigurationProperties();
            configurationProperties.setGlobal(testConfig.getGlobal());
            return new NetworkConfig(configurationProperties);
          }

          @Override
          public void handleNewEndPoint(EndPoint endPoint) throws IOException {
            connected.set(true);
          }

          @Override
          public void handleCloseEndPoint(EndPoint endPoint) {
            connected.set(false);
          }
        }, new ArrayList<String>());

    WaitForState.waitFor(1, TimeUnit.SECONDS, connected::get);
    Assertions.assertTrue(connected.get());

    endPoint.close();
    WaitForState.waitFor(1, TimeUnit.SECONDS, ()->!connected.get());
    selectorLoadManager.close();
  }

}
