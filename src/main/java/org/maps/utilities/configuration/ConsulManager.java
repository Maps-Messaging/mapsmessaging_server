package org.maps.utilities.configuration;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class ConsulManager implements Runnable {

  private final Consul client;
  private final AgentClient agentClient;
  private Registration service;
  private final UUID serviceId;

  public ConsulManager(UUID id) throws NotRegisteredException {
    client = Consul.builder().build(); // connect on localhost
    agentClient = client.agentClient();
    serviceId = id;
  }

  public KeyValueClient getKeyValueManager(){
    return client.keyValueClient();
  }

  public void register(){
    List<String> propertyNames = new ArrayList<>();
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("version", "1.0");

    service = ImmutableRegistration.builder()
        .id(serviceId.toString())
        .name("mapsMessaging")
        .port(8080)
        .check(Registration.RegCheck.ttl(3L)) // registers with a TTL of 3 seconds
        .tags(propertyNames)
        .meta(meta)
        .build();

    agentClient.register(service);
    SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 2,2, TimeUnit.SECONDS);
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
