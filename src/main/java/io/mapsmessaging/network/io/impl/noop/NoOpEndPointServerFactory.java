/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.io.impl.noop;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import java.io.IOException;

public class NoOpEndPointServerFactory implements EndPointServerFactory {

  @Override
  public EndPointServer instance(EndPointURL url, SelectorLoadManager selector, AcceptHandler acceptHandler, NetworkConfig config, EndPointManagerJMX managerMBean)
      throws IOException {
    return new NoOpEndPointServer(acceptHandler, url, config);
  }

  @Override
  public boolean active() {
    return true;
  }

  @Override
  public String getName() {
    return "noop";
  }

  @Override
  public String getDescription() {
    return "Provides a EndPoint object for a loop based protocol";
  }
}