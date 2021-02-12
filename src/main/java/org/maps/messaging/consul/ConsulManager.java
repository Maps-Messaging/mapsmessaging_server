/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.consul;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class ConsulManager implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(ConsulManager.class);
  private final Consul client;
  private final AgentClient agentClient;
  private final UUID serviceId;
  private Future<Runnable> scheduledTask;

  public ConsulManager(UUID id) {
    client = Consul.builder().build();
    agentClient = client.agentClient();
    serviceId = id;
    logger.log(LogMessages.CONSUL_STARTUP);
  }

  public KeyValueClient getKeyValueManager(){
    return client.keyValueClient();
  }

  public void register(Map<String,String> meta){
    List<String> propertyNames = new ArrayList<>();
    meta.put("version", Constants.VERSION);
    logger.log(LogMessages.CONSUL_REGISTER);

    Registration service = ImmutableRegistration.builder()
        .id(serviceId.toString())
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
      logger.log(LogMessages.CONSUL_SHUTDOWN);
      scheduledTask.cancel(true);
    }
  }

  public void run() {
    agentClient.ping();
    try {
      agentClient.pass(serviceId.toString());
    } catch (NotRegisteredException e) {
      logger.log(LogMessages.CONSUL_PING_EXCEPTION, e);
    }
  }
}
