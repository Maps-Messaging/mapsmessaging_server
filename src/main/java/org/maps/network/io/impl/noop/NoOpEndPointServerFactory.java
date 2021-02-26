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

package org.maps.network.io.impl.noop;

import java.io.IOException;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointManagerJMX;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.EndPointServer;
import org.maps.network.io.EndPointServerFactory;
import org.maps.network.io.impl.SelectorLoadManager;

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
