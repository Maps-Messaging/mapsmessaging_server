/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.NetworkConnectionManagerConfig;
import io.mapsmessaging.config.protocol.impl.LocalLoopConfig;
import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Checks configured multi-loop discovery without opening endpoints or starting selector threads. */
class LocalLoopConfigurationTest {

  @Test
  void twoLoopConfigurationsProduceIndependentNoOpConnectionsAndKeepTheirIdentities() throws Exception {
    EndPointConnectionServerConfigDTO first = configuration("loop-test-A", "/msg120/a", "/msg120/b");
    EndPointConnectionServerConfigDTO second = configuration("loop-test-B", "/msg120/c", "/msg120/d");
    NetworkConnectionManagerConfig config = mock(NetworkConnectionManagerConfig.class);
    when(config.getEndPointServerConfigList()).thenReturn(List.of(first, second));
    MessageDaemon daemon = mock(MessageDaemon.class);
    when(daemon.getTypePath()).thenReturn(List.of());

    // SelectorLoadManager would otherwise create threads even when the manager is not started.
    try (MockedStatic<NetworkConnectionManagerConfig> configs = mockStatic(NetworkConnectionManagerConfig.class);
         MockedStatic<MessageDaemon> daemons = mockStatic(MessageDaemon.class);
         MockedConstruction<SelectorLoadManager> selectors = mockConstruction(SelectorLoadManager.class)) {
      configs.when(NetworkConnectionManagerConfig::getInstance).thenReturn(config);
      daemons.when(MessageDaemon::getInstance).thenReturn(daemon);
      NetworkConnectionManager manager = new NetworkConnectionManager();
      try {
        manager.initialise();
        List<EndPointConnection> connections = manager.getEndPointConnectionList();
        assertEquals(2, connections.size());
        assertNotSame(connections.get(0), connections.get(1));
        assertEquals("noop", connections.get(0).getUrl().getProtocol());
        assertEquals("noop", connections.get(1).getUrl().getProtocol());
        assertEquals("noop://localhost/", first.getUrl());
        assertEquals("noop://localhost/", second.getUrl());
        assertEquals("loop", first.getProtocolConfigs().getFirst().getType());
        assertEquals("loop", second.getProtocolConfigs().getFirst().getType());
        assertEquals("loop-test-A", first.getAuthConfig().getSessionId());
        assertEquals("loop-test-B", second.getAuthConfig().getSessionId());
        assertNotSame(connections.get(0).getProperties(), connections.get(1).getProperties());
      } finally {
        for (EndPointConnection connection : manager.getEndPointConnectionList()) {
          connection.close();
        }
      }
      assertEquals(1, selectors.constructed().size());
    }
  }

  @Test
  void configurationWithoutLinksDoesNotCreateALocalConnection() throws Exception {
    EndPointConnectionServerConfigDTO unused = configuration("msg120-unused", "/msg120/source", "/msg120/destination");
    unused.setLinkConfigs(List.of());
    NetworkConnectionManagerConfig config = mock(NetworkConnectionManagerConfig.class);
    when(config.getEndPointServerConfigList()).thenReturn(List.of(unused));
    try (MockedStatic<NetworkConnectionManagerConfig> configs = mockStatic(NetworkConnectionManagerConfig.class);
         MockedConstruction<SelectorLoadManager> selectors = mockConstruction(SelectorLoadManager.class)) {
      configs.when(NetworkConnectionManagerConfig::getInstance).thenReturn(config);
      NetworkConnectionManager manager = new NetworkConnectionManager();
      manager.initialise();
      assertTrue(manager.getEndPointConnectionList().isEmpty());
      assertEquals("loop://localhost/", unused.getUrl(), "Unused configuration should not be rewritten");
      assertEquals(1, selectors.constructed().size());
    }
  }

  private static EndPointConnectionServerConfigDTO configuration(String sessionId, String source, String destination) {
    EndPointConnectionServerConfigDTO result = new EndPointConnectionServerConfigDTO();
    result.setName(sessionId);
    result.setUrl("loop://localhost/");
    result.setEndPointConfig(new EndPointConfigDTO("noop"));
    result.setProtocolConfigs(List.of(new LocalLoopConfig()));
    AuthConfigDTO auth = new AuthConfigDTO();
    auth.setSessionId(sessionId);
    auth.setUsername("anonymous");
    auth.setPassword("");
    result.setAuthConfig(auth);
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection("push");
    link.setLocalNamespace(source);
    link.setRemoteNamespace(destination);
    result.setLinkConfigs(List.of(link));
    return result;
  }
}
