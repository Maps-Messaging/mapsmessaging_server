package io.mapsmessaging.config.network;

import io.mapsmessaging.config.network.impl.TcpConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndPointConfigFactoryTest {

  @Test
  void packAndUnpackPreserveSelectorTaskWait() {
    EndPointServerConfigDTO source = server("tcp://localhost:1883");
    source.setSelectorTaskWait(73);

    ConfigurationProperties packed = new ConfigurationProperties();
    EndPointConfigFactory.pack(packed, source);

    EndPointServerConfigDTO restored = new EndPointServerConfigDTO();
    EndPointConfigFactory.unpack(packed, restored);

    assertEquals(73, restored.getSelectorTaskWait());
  }

  @Test
  void allProtocolAliasExpandsAccordingToTransport() {
    ConfigurationProperties udp = new ConfigurationProperties();
    udp.put("name", "udp");
    udp.put("url", "udp://localhost:1884");
    udp.put("auth", "");
    udp.put("protocol", "all");

    EndPointServerConfigDTO server = new EndPointServerConfigDTO();
    EndPointConfigFactory.unpack(udp, server);

    assertEquals(List.of("coap", "mqtt-sn"), server.getProtocolConfigs().stream().map(p -> p.getType()).toList());
  }

  @Test
  void updateAppliesServerAndNestedEndpointChangesExactlyOnce() {
    EndPointServerConfigDTO original = server("tcp://localhost:1883");
    original.setEndPointConfig(new TcpConfig(new ConfigurationProperties()));

    EndPointServerConfigDTO updated = server("tcp://localhost:1883");
    updated.setName("updated");
    updated.setBacklog(321);
    updated.setSelectorTaskWait(44);
    TcpConfigDTO tcp = new TcpConfigDTO();
    tcp.setTimeout(12345);
    updated.setEndPointConfig(tcp);

    assertTrue(EndPointConfigFactory.update(original, updated));
    assertEquals("updated", original.getName());
    assertEquals(321, original.getBacklog());
    assertEquals(44, original.getSelectorTaskWait());
    assertEquals(12345, ((TcpConfigDTO) original.getEndPointConfig()).getTimeout());
    assertFalse(EndPointConfigFactory.update(original, updated));
  }

  private static EndPointServerConfigDTO server(String url) {
    EndPointServerConfigDTO server = new EndPointServerConfigDTO();
    server.setName("server");
    server.setUrl(url);
    server.setAuthenticationRealm("");
    server.setProtocolConfigs(List.of());
    return server;
  }
}
