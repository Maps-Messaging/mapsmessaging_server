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
import org.maps.utilities.threads.SimpleTaskScheduler;

public class ConsulManager implements Runnable {

  private final Consul client;
  private final AgentClient agentClient;
  private final UUID serviceId;
  private Future<Runnable> scheduledTask;

  public ConsulManager(UUID id) {
    client = Consul.builder().build();
    agentClient = client.agentClient();
    serviceId = id;
  }

  public KeyValueClient getKeyValueManager(){
    return client.keyValueClient();
  }

  public void register(Map<String,String> meta){
    List<String> propertyNames = new ArrayList<>();
    meta.put("version", Constants.VERSION);

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
      scheduledTask.cancel(true);
    }
  }

  public void run() {
    agentClient.ping();
    try {
      agentClient.pass(serviceId.toString());
    } catch (NotRegisteredException e) {
      e.printStackTrace();
    }
  }
}
