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

package io.mapsmessaging.consul.ecwid;

import io.mapsmessaging.consul.ConsulServerApi;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.rest.RestApiServerManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_CLIENT_LOG;

public class EcwidConsulManager extends ConsulServerApi {

  private final Logger logger = LoggerFactory.getLogger(EcwidConsulManager.class);

  public EcwidConsulManager(String name) throws IOException {
    super(name);
    try {
      logger.log(CONSUL_CLIENT_LOG, "Creating client", consulConfiguration);
      //    client = createBuilder().build();
      logger.log(CONSUL_CLIENT_LOG, "Created client", consulConfiguration);
      //  agentClient = consulConfiguration.registerAgent() ? client.agentClient() : null;
      //  keyValueClient = client.keyValueClient();
      logger.log(ServerLogMessages.CONSUL_STARTUP);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  protected void pingService() {

  }

  @Override
  public String scanForDefaultConfig(String namespace) {
    return null;
  }

  @Override
  public void register(Map<String, String> meta) {

  }

  @Override
  public void register(EndPointServer endPointServer) {

  }

  @Override
  public void register(RestApiServerManager restApiServerManager) {

  }

  @Override
  public String getUrlPath() {
    return null;
  }

  @Override
  public List<String> getKeys(String key) throws IOException {
    return null;
  }

  @Override
  public String getValue(String key) throws IOException {
    return null;
  }

  @Override
  public void putValue(String key, String value) {

  }

  @Override
  public void deleteKey(String key) {

  }
}
