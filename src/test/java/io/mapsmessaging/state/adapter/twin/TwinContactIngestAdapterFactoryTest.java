/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.adapter.twin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.state.adapter.StateMessageAdapter;
import io.mapsmessaging.state.adapter.StateMessageAdapterContext;
import io.mapsmessaging.state.adapter.StateMessageAdapterFactory;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.drone.core.TwinManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

/** The adapter exists only where it is configured: an edge node must not ingest its own relay. */
class TwinContactIngestAdapterFactoryTest {

  private final TwinContactIngestAdapterFactory factory = new TwinContactIngestAdapterFactory();

  private static StateMessageAdapterContext context(Map<String, ConfigurationProperties> adapters) {
    TwinManagerConfig config = new TwinManagerConfig();
    config.setAdapterConfig(adapters);
    return new StateMessageAdapterContext(new TwinManager(), config);
  }

  @Test
  void withoutTheKeyNoAdapterIsCreated() {
    assertTrue(factory.create(context(new LinkedHashMap<>())).isEmpty());
    assertTrue(factory.create(context(null)).isEmpty());
  }

  @Test
  void theKeyCreatesItOnTheDefaultTree() {
    Map<String, ConfigurationProperties> adapters = new LinkedHashMap<>();
    adapters.put("twinContactIngest", new ConfigurationProperties(new LinkedHashMap<>()));

    Optional<StateMessageAdapter> adapter = factory.create(context(adapters));

    assertTrue(adapter.isPresent());
    assertEquals("twin-contact-ingest", adapter.get().getName());
  }

  @Test
  void theTopicIsConfigurable() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("topic", "/relayed/twins/+/contacts");
    Map<String, ConfigurationProperties> adapters = new LinkedHashMap<>();
    adapters.put("twinContactIngest", new ConfigurationProperties(values));

    assertTrue(factory.create(context(adapters)).isPresent());
  }

  @Test
  void theFactoryIsOnTheServiceLoader() {
    boolean found = false;
    for (StateMessageAdapterFactory candidate : ServiceLoader.load(StateMessageAdapterFactory.class)) {
      found |= "twin-contact-ingest".equals(candidate.getName());
    }
    assertTrue(found, "the adapter is not registered in META-INF/services, so it can never start");
    assertFalse(factory.getName().isBlank());
  }
}
