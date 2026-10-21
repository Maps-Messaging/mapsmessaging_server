package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.UdpConfigDTO;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigFactoryTest {

  @Test
  void invalidProxyModeFallsBackToDisabled() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("proxyProtocol", "definitely-not-valid");

    TcpConfigDTO config = new TcpConfigDTO();
    NetworkConfigFactory.unpack(properties, config);

    assertEquals(ProxyProtocolMode.DISABLED, config.getProxyProtocolMode());
  }

  @Test
  void tcpPackAndUnpackPreserveTransportSettings() {
    TcpConfigDTO source = new TcpConfigDTO();
    source.setProxyProtocolMode(ProxyProtocolMode.REQUIRED);
    source.setAllowedProxyHosts("10.0.0.0/8");
    source.setSelectorThreadCount(4);
    source.setDiscoverable(true);
    source.setServerReadBufferSize(16384);
    source.setServerWriteBufferSize(32768);
    source.setConnectionTimeout(7000);
    source.setReceiveBufferSize(20000);
    source.setSendBufferSize(30000);
    source.setTimeout(40000);
    source.setBacklog(42);
    source.setSoLingerDelaySec(3);
    source.setReadDelayOnFragmentation(55);
    source.setEnableReadDelayOnFragmentation(false);
    source.setFragmentationLimit(9);

    ConfigurationProperties packed = new ConfigurationProperties();
    NetworkConfigFactory.pack(packed, source);
    TcpConfigDTO restored = new TcpConfigDTO();
    NetworkConfigFactory.unpack(packed, restored);

    assertEquals(ProxyProtocolMode.REQUIRED, restored.getProxyProtocolMode());
    assertEquals(16384, restored.getServerReadBufferSize());
    assertEquals(32768, restored.getServerWriteBufferSize());
    assertEquals(20000, restored.getReceiveBufferSize());
    assertEquals(30000, restored.getSendBufferSize());
    assertEquals(9, restored.getFragmentationLimit());
  }

  @Test
  void updateChangesWriteBufferWithoutCorruptingReadBuffer() {
    TcpConfigDTO original = new TcpConfigDTO();
    original.setServerReadBufferSize(1000);
    original.setServerWriteBufferSize(2000);

    TcpConfigDTO updated = new TcpConfigDTO();
    updated.setServerReadBufferSize(1000);
    updated.setServerWriteBufferSize(9000);

    assertTrue(NetworkConfigFactory.update(original, updated));
    assertEquals(1000, original.getServerReadBufferSize());
    assertEquals(9000, original.getServerWriteBufferSize());
  }

  @Test
  void udpPackAndUnpackPreserveTimeoutsWithEmptyHmacList() {
    UdpConfigDTO source = new UdpConfigDTO();
    source.setHmacConfigList(new ArrayList<>());
    source.setPacketReuseTimeout(111);
    source.setIdleSessionTimeout(222);
    source.setHmacHostLookupCacheExpiry(333);

    ConfigurationProperties packed = new ConfigurationProperties();
    NetworkConfigFactory.pack(packed, source);
    UdpConfigDTO restored = new UdpConfigDTO();
    NetworkConfigFactory.unpack(packed, restored);

    assertEquals(111, restored.getPacketReuseTimeout());
    assertEquals(222, restored.getIdleSessionTimeout());
    assertEquals(333, restored.getHmacHostLookupCacheExpiry());
    assertNotNull(restored.getHmacConfigList());
    assertTrue(restored.getHmacConfigList().isEmpty());
  }
}