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

package io.mapsmessaging.consul;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConsulManager implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(ConsulManager.class);
  private final Consul client;
  private final AgentClient agentClient;
  private final String serviceId;
  private Future<?> scheduledTask;

  public ConsulManager(String serverId) {
    client = Consul.builder().build();
    agentClient = client.agentClient();
    serviceId = serverId;
    logger.log(ServerLogMessages.CONSUL_STARTUP);
  }

  public KeyValueClient getKeyValueManager(){
    return client.keyValueClient();
  }

  public void register(Map<String,String> meta){
    List<String> propertyNames = new ArrayList<>();
    meta.put("version", Constants.VERSION);
    logger.log(ServerLogMessages.CONSUL_REGISTER);

    Registration service = ImmutableRegistration.builder()
        .id(serviceId)
        .name(Constants.NAME)
        .port(Constants.CONSUL_PORT)
        .check(Registration.RegCheck.ttl(Constants.PING_TIME))
        .tags(propertyNames)
        .meta(meta)
        .build();

    agentClient.register(service);
    scheduledTask = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, Constants.HEALTH_TIME, Constants.HEALTH_TIME, TimeUnit.SECONDS);
  }

  public void stop(){
    if(scheduledTask != null){
      logger.log(ServerLogMessages.CONSUL_SHUTDOWN);
      scheduledTask.cancel(false);
    }
  }

  public void run() {
    agentClient.ping();
    try {
      agentClient.pass(serviceId.toString());
    } catch (NotRegisteredException e) {
      logger.log(ServerLogMessages.CONSUL_PING_EXCEPTION, e);
    }
  }
}
