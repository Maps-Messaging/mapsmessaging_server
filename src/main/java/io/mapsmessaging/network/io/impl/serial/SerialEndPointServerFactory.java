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

package io.mapsmessaging.network.io.impl.serial;

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

public class SerialEndPointServerFactory implements EndPointServerFactory {


  @Override
  public EndPointServer instance(
      EndPointURL url,
      SelectorLoadManager selector,
      AcceptHandler acceptHandler,
      EndPointServerConfig endPointServerConfig,
      EndPointManagerJMX managerMBean) {
    return new SerialEndPointServer(acceptHandler, url, endPointServerConfig, managerMBean);
  }

  @Override
  public String getName() {
    return "serial";
  }

  @Override
  public String getDescription() {
    return "Serial Port End Point Server Factory";
  }

  @Override
  public boolean active() {
    return true;
  }

}
