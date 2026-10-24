/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.dto.rest.config.network.impl.MemoryConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.RdmaConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.ShmConfigDTO;
import io.mapsmessaging.memory.MemoryTransportType;
import io.mapsmessaging.network.EndPointURL;
import org.junit.jupiter.api.Test;

import java.util.List;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class MemoryEndpointSettingsTest {
  @Test
  void rdma_and_explicit_legacy_shm_map_to_managed_configs() throws Exception {
    for (String type : List.of("shm", "rdma")) {
      MemoryConfigDTO config = type.equals("shm") ? new ShmConfigDTO() : new RdmaConfigDTO();
      config.setNativeSharedMemory(false);
      config.setMaxConnections(3);
      config.setHandshakeTimeoutMillis(1234);
      EndPointURL url = new EndPointURL(type + "://127.0.0.1:7471");
      assertEquals(new InetSocketAddress("127.0.0.1", 7471), MemoryEndpointSettings.listener(url, config).address());
      assertEquals(3, MemoryEndpointSettings.listener(url, config).maxConnections());
      assertEquals(1234, MemoryEndpointSettings.listener(url, config).handshakeTimeoutMillis());
      assertNull(MemoryEndpointSettings.client(url, config).security());
      assertEquals(type.equals("shm") ? MemoryTransportType.SHARED_MEMORY : MemoryTransportType.RDMA,
          MemoryEndpointSettings.client(url, config).transport());
    }
  }

  @Test
  void native_address_requires_explicit_local_name() throws Exception {
    ShmConfigDTO config = new ShmConfigDTO();
    EndPointURL url = new EndPointURL("shm://localhost:0");
    assertThrows(IOException.class, () -> MemoryEndpointSettings.listener(url, config));
    config.setLocalAddress("target/data/memory.sock");
    if (!System.getProperty("os.name").startsWith("Windows")) {
      assertInstanceOf(UnixDomainSocketAddress.class, MemoryEndpointSettings.client(url, config).address());
    }
  }

  @Test
  void secure_configuration_never_downgrades_on_missing_identity() {
    ShmConfigDTO config = new ShmConfigDTO();
    config.setLocalAddress("target/data/memory.sock");
    config.getMemorySecurity().setEnabled(true);
    assertThrows(IOException.class, () -> MemoryEndpointSettings.listener(new EndPointURL("shm://localhost:0"), config));
    assertThrows(IOException.class, () -> MemoryEndpointSettings.client(new EndPointURL("shm://localhost:0"), config));
  }

  @Test
  void invalid_limits_and_transport_mismatch_fail_before_connecting() {
    RdmaConfigDTO config = new RdmaConfigDTO();
    config.setSlotSize(1);
    assertThrows(IOException.class, () -> MemoryEndpointSettings.listener(new EndPointURL("rdma://127.0.0.1:7471"), config));
    config.setSlotSize(65536);
    config.setReadinessPollMicros(0);
    assertThrows(IOException.class, () -> MemoryEndpointSettings.client(new EndPointURL("rdma://127.0.0.1:7471"), config));
    config.setReadinessPollMicros(1000);
    assertThrows(IOException.class, () -> MemoryEndpointSettings.listener(new EndPointURL("shm://localhost:0"), config));
  }
}
