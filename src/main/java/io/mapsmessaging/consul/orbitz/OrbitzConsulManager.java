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

package io.mapsmessaging.consul.orbitz;

import com.orbitz.consul.*;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.monitoring.ClientEventCallback;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.consul.Constants;
import io.mapsmessaging.consul.ConsulServerApi;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.rest.RestApiServerManager;

import java.io.IOException;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_CLIENT_LOG;
import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_KEY_VALUE_MANAGER;

public class OrbitzConsulManager extends ConsulServerApi implements ClientEventCallback {

  private final Logger logger = LoggerFactory.getLogger(OrbitzConsulManager.class);

  private final Consul client;
  private final AgentClient agentClient;
  private final KeyValueClient keyValueClient;


  public OrbitzConsulManager(String serverId) throws IOException {
    super(serverId);
    try {
      logger.log(CONSUL_CLIENT_LOG, "Creating client", consulConfiguration);
      client = createBuilder().build();
      logger.log(CONSUL_CLIENT_LOG, "Created client", consulConfiguration);
      agentClient = consulConfiguration.registerAgent() ? client.agentClient() : null;
      keyValueClient = client.keyValueClient();
      logger.log(ServerLogMessages.CONSUL_STARTUP);
    } catch (ConsulException ex) {
      throw new IOException(ex);
    }
  }

  private Consul.Builder createBuilder() throws IOException {
    if (consulConfiguration.getConsulUrl() == null) {
      throw new IOException("No Consul configuration found");
    }
    Consul.Builder builder = Consul.builder();

    //
    // Process a potential token
    //
    if (consulConfiguration.getConsulToken() != null) {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("X-Consul-Token", consulConfiguration.getConsulToken());
      builder = builder.withHeaders(headers)
          .withTokenAuth(consulConfiguration.getConsulToken());
    }

    //
    // Process a potential ACL, they are different to a token
    //
    if (consulConfiguration.getConsulAcl() != null) builder.withAclToken(consulConfiguration.getConsulAcl());
    if (consulConfiguration.registerAgent()) {
      return builder.withUrl(consulConfiguration.getConsulUrl())
          .withWriteTimeoutMillis(60000)
          .withReadTimeoutMillis(60000)
          .withHttps(consulConfiguration.getConsulUrl().toLowerCase().startsWith("https"))
          .withClientEventCallback(this)
          .withPing(true);
    } else {
      return builder.withUrl(consulConfiguration.getConsulUrl())
          .withWriteTimeoutMillis(60000)
          .withReadTimeoutMillis(60000)
          .withHttps(consulConfiguration.getConsulUrl().toLowerCase().startsWith("https"))
          .withClientEventCallback(this)
          .withPing(false);
    }
  }

  @Override
  public void register(Map<String, String> meta) {
    if(!consulConfiguration.registerAgent()){
      return;
    }
    List<String> propertyNames = new ArrayList<>();
    meta.put("version", BuildInfo.getBuildVersion());
    meta.put("build-Date", BuildInfo.getBuildDate());
    logger.log(ServerLogMessages.CONSUL_REGISTER);

    Registration service = ImmutableRegistration.builder()
        .id(uniqueName)
        .name(Constants.NAME)
        .port(Constants.CONSUL_PORT)
        .check(Registration.RegCheck.ttl(Constants.PING_TIME))
        .tags(propertyNames)
        .meta(meta)
        .build();

    agentClient.register(service);
    registerPingTask();
  }

  @Override
  public void register(EndPointServer endPointServer){
    if(!consulConfiguration.registerAgent()){
      return;
    }
    EndPointURL endPointURL = new EndPointURL(endPointServer.getConfig().getUrl());
    String host = endPointURL.getHost();
    if(host.equals("0.0.0.0")){
      return; // Not Yet Supported
    }
    int port = endPointURL.getPort();
    String protocol = endPointServer.getConfig().getProtocols();
    String id = uniqueName+"-"+endPointServer.getConfig().getProperties().getProperty("name");
    Registration service = ImmutableRegistration.builder()
        .id(id)
        .name(Constants.NAME+"-"+protocol)
        .port(port)
        .check(Registration.RegCheck.tcp(host+":"+port, Constants.PING_TIME, Constants.PING_TIME/2))
        .build();
    agentClient.register(service);
    serviceIds.add(id);
  }

  @Override
  public void register(RestApiServerManager restApiServerManager){
    if(!consulConfiguration.registerAgent()){
      return;
    }
    String host = restApiServerManager.getHost();
    if(host.equals("0.0.0.0")){
      return; // Not Yet Supported
    }
    int port = restApiServerManager.getPort();
    String protocol = "http";
    if(restApiServerManager.isSecure()) protocol = "https";
    String url = protocol+"://"+host+":"+port+"/api/v1/ping";
    Registration service = ImmutableRegistration.builder()
        .id(uniqueName+"-RestApi")
        .name(Constants.NAME+"-RestApi")
        .port(restApiServerManager.getPort())
        .check(Registration.RegCheck.http(url, Constants.PING_TIME))
        .build();
    agentClient.register(service);
    serviceIds.add(uniqueName+"-RestApi");
  }

  public void stop() {
    super.stop();
    if (consulConfiguration.registerAgent()) {
      for(String id:serviceIds){
        agentClient.deregister(id);
      }
    }
  }

  protected void pingService() {
    agentClient.ping();
    try {
      agentClient.pass(uniqueName);
    } catch (NotRegisteredException e) {
      logger.log(ServerLogMessages.CONSUL_PING_EXCEPTION, e);
    }
  }

  @Override
  public List<String> getKeys(String key) throws IOException {
    String keyName = validateKey(key);
    try {
      logger.log(CONSUL_KEY_VALUE_MANAGER, "getKeys", keyName);
      List<String> keyList = keyValueClient.getKeys(keyName);
      return keyList;
    } catch (ConsulException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public String getValue(String key) throws IOException {
    try {
      String keyName = validateKey(key);
      logger.log(CONSUL_KEY_VALUE_MANAGER, "GetValues", keyName);
      Optional<Value> value = keyValueClient.getValue(keyName);
      if (value.isPresent()) {
        Optional<String> optionalValue = value.get().getValue();
        if (optionalValue.isPresent()) {
          return new String(Base64.getDecoder().decode(optionalValue.get()));
        }
      }
    } catch (ConsulException ex) {
      throw new IOException(ex);
    }
    return "";
  }

  @Override
  public void putValue(String key, String value) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "putValue", keyName);
    keyValueClient.putValue(keyName, value);
  }

  @Override
  public void deleteKey(String key) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "deleteKey", keyName);
    keyValueClient.deleteKey(key);
  }

}
