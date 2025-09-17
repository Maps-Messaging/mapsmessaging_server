/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.ConfigHelper;
import io.mapsmessaging.config.network.HmacConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.UdpConfigDTO;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolMode;

import java.util.ArrayList;
import java.util.List;

public class NetworkConfigFactory {

  public static void pack(ConfigurationProperties config, EndPointConfigDTO endPointConfigDTO){
    config.put("proxyProtocol", endPointConfigDTO.getProxyProtocolMode().name());
    config.put("allowedProxyHosts", endPointConfigDTO.getAllowedProxyHosts());
    config.put("type", endPointConfigDTO.getType());
    config.put("selectorThreadCount", endPointConfigDTO.getSelectorThreadCount());
    config.put("discoverable", endPointConfigDTO.isDiscoverable());
    config.put("serverReadBufferSize", ConfigHelper.formatBufferSize(endPointConfigDTO.getServerReadBufferSize()));
    config.put("serverWriteBufferSize", ConfigHelper.formatBufferSize(endPointConfigDTO.getServerWriteBufferSize()));
    if(endPointConfigDTO instanceof TcpConfigDTO){
      TcpConfigDTO tcpConfigDTO = (TcpConfigDTO)endPointConfigDTO;
      config.put("receiveBufferSize", tcpConfigDTO.getReceiveBufferSize());
      config.put("sendBufferSize", tcpConfigDTO.getSendBufferSize());
      config.put("timeout", tcpConfigDTO.getTimeout());
      config.put("backlog", tcpConfigDTO.getBacklog());
      config.put("soLingerDelaySec", tcpConfigDTO.getSoLingerDelaySec());
      config.put("readDelayOnFragmentation", tcpConfigDTO.getReadDelayOnFragmentation());
      config.put("enableReadDelayOnFragmentation", tcpConfigDTO.isEnableReadDelayOnFragmentation());
      config.put("fragmentationLimit", tcpConfigDTO.getFragmentationLimit());
    }
    if(endPointConfigDTO instanceof UdpConfigDTO){
      UdpConfigDTO udpConfigDTO = (UdpConfigDTO)endPointConfigDTO;
      config.put("packetReuseTimeout", udpConfigDTO.getPacketReuseTimeout());
      config.put("idleSessionTimeout", udpConfigDTO.getIdleSessionTimeout());
      config.put("HmacHostLookupCacheExpiry", udpConfigDTO.getHmacHostLookupCacheExpiry());

      List<ConfigurationProperties> nodeConfigs = new ArrayList<>();
      for (HmacConfigDTO hmacConfig : udpConfigDTO.getHmacConfigList()) {
        nodeConfigs.add(((Config)hmacConfig).toConfigurationProperties());
      }
      config.put("nodeConfiguration", nodeConfigs);
    }
  }

  public static void unpack(ConfigurationProperties config, EndPointConfigDTO endPointConfigDTO){
    ProxyProtocolMode mode = ProxyProtocolMode.DISABLED;
    try {
      mode = ProxyProtocolMode.valueOf(config.getProperty("proxyProtocol", "DISABLED").toUpperCase());
    } catch (IllegalArgumentException ignored) {
      // Invalid input; fallback to DISABLED
    }

    endPointConfigDTO.setProxyProtocolMode(mode);
    endPointConfigDTO.setAllowedProxyHosts(config.getProperty("allowedProxyHosts", ""));
    endPointConfigDTO.setSelectorThreadCount(config.getThreadCount("selectorThreadCount", 2));
    endPointConfigDTO.setDiscoverable(config.getBooleanProperty("discoverable", false));
    endPointConfigDTO.setServerReadBufferSize(ConfigHelper.parseBufferSize(config.getProperty("serverReadBufferSize", "10K")));
    endPointConfigDTO.setServerWriteBufferSize(ConfigHelper.parseBufferSize(config.getProperty("serverWriteBufferSize", "10K")));
    endPointConfigDTO.setConnectionTimeout(ConfigHelper.parseBufferSize(config.getProperty("connectionTimeout", "5000")));
    if(endPointConfigDTO instanceof TcpConfigDTO tcpConfigDTO){
      tcpConfigDTO.setReceiveBufferSize(config.getIntProperty("receiveBufferSize", 128000));
      tcpConfigDTO.setSendBufferSize (config.getIntProperty("sendBufferSize", 128000));
      tcpConfigDTO.setTimeout( config.getIntProperty("timeout", 60000));
      tcpConfigDTO.setBacklog( config.getIntProperty("backlog", 100));
      tcpConfigDTO.setSoLingerDelaySec(config.getIntProperty("soLingerDelaySec", 10));
      tcpConfigDTO.setReadDelayOnFragmentation( config.getIntProperty("readDelayOnFragmentation", 100));
      tcpConfigDTO.setFragmentationLimit( config.getIntProperty("fragmentationLimit", 5));
      tcpConfigDTO.setEnableReadDelayOnFragmentation ( config.getBooleanProperty("enableReadDelayOnFragmentation", true));
    }
    else if(endPointConfigDTO instanceof UdpConfigDTO udpConfigDTO){
      udpConfigDTO.setPacketReuseTimeout(config.getLongProperty("packetReuseTimeout", 1000L));
      udpConfigDTO.setIdleSessionTimeout(config.getLongProperty("idleSessionTimeout", 600));
      udpConfigDTO.setHmacHostLookupCacheExpiry(config.getLongProperty("HmacHostLookupCacheExpiry", 600));

      Object t = config.get("nodeConfiguration");
      if (t != null) {
        udpConfigDTO.setHmacConfigList(loadNodeConfig((List<ConfigurationProperties>) t));
      } else {
        udpConfigDTO.setHmacConfigList(new ArrayList<>());
      }

    }
  }

  private static List<HmacConfigDTO> loadNodeConfig(List<ConfigurationProperties> nodes) {
    List<HmacConfigDTO> list = new ArrayList<>();
    for (ConfigurationProperties node : nodes) {
      list.add(new HmacConfig(node));
    }
    return list;
  }


  public static boolean update(EndPointConfigDTO original, EndPointConfigDTO config) {
    boolean hasChanged = false;

    if (original.getSelectorThreadCount() != config.getSelectorThreadCount()) {
      original.setSelectorThreadCount(config.getSelectorThreadCount());
      hasChanged = true;
    }
    if (original.isDiscoverable() != config.isDiscoverable()) {
      original.setDiscoverable(config.isDiscoverable());
      hasChanged = true;
    }
    if (original.getServerReadBufferSize() != config.getServerReadBufferSize()) {
      original.setServerReadBufferSize(config.getServerReadBufferSize());
      hasChanged = true;
    }
    if (original.getServerWriteBufferSize() != config.getServerWriteBufferSize()) {
      original.setServerReadBufferSize(config.getServerWriteBufferSize());
      hasChanged = true;
    }
    if(original.getConnectionTimeout() != config.getConnectionTimeout()){
      original.setConnectionTimeout(config.getConnectionTimeout());
      hasChanged = true;
    }

    if (original instanceof TcpConfigDTO && config instanceof TcpConfigDTO) {
      TcpConfigDTO newConfig = (TcpConfigDTO) config;
      TcpConfigDTO oldConfig = (TcpConfigDTO) original;


      if (oldConfig.getReceiveBufferSize() != newConfig.getReceiveBufferSize()) {
        oldConfig.setReceiveBufferSize(newConfig.getReceiveBufferSize());
        hasChanged = true;
      }
      if (oldConfig.getSendBufferSize() != newConfig.getSendBufferSize()) {
        oldConfig.setSendBufferSize(newConfig.getSendBufferSize());
        hasChanged = true;
      }
      if (oldConfig.getTimeout() != newConfig.getTimeout()) {
        oldConfig.setTimeout(newConfig.getTimeout());
        hasChanged = true;
      }
      if (oldConfig.getBacklog() != newConfig.getBacklog()) {
        oldConfig.setBacklog(newConfig.getBacklog());
        hasChanged = true;
      }
      if (oldConfig.getSoLingerDelaySec() != newConfig.getSoLingerDelaySec()) {
        oldConfig.setSoLingerDelaySec(newConfig.getSoLingerDelaySec());
        hasChanged = true;
      }
      if (oldConfig.getReadDelayOnFragmentation() != newConfig.getReadDelayOnFragmentation()) {
        oldConfig.setReadDelayOnFragmentation(newConfig.getReadDelayOnFragmentation());
        hasChanged = true;
      }
      if (oldConfig.getFragmentationLimit() != newConfig.getFragmentationLimit()) {
        oldConfig.setFragmentationLimit(newConfig.getFragmentationLimit());
        hasChanged = true;
      }
      if (oldConfig.isEnableReadDelayOnFragmentation() != newConfig.isEnableReadDelayOnFragmentation()) {
        oldConfig.setEnableReadDelayOnFragmentation(newConfig.isEnableReadDelayOnFragmentation());
        hasChanged = true;
      }
    }
    if (original instanceof UdpConfigDTO && config instanceof UdpConfigDTO) {
      UdpConfigDTO newConfig = (UdpConfigDTO) config;
      UdpConfigDTO oldConfig = (UdpConfigDTO) original;

      if (oldConfig.getPacketReuseTimeout() != newConfig.getPacketReuseTimeout()) {
        oldConfig.setPacketReuseTimeout(newConfig.getPacketReuseTimeout());
        hasChanged = true;
      }
      if (oldConfig.getIdleSessionTimeout() != newConfig.getIdleSessionTimeout()) {
        oldConfig.setIdleSessionTimeout(newConfig.getIdleSessionTimeout());
        hasChanged = true;
      }
      if (oldConfig.getHmacHostLookupCacheExpiry() != newConfig.getHmacHostLookupCacheExpiry()) {
        oldConfig.setHmacHostLookupCacheExpiry(newConfig.getHmacHostLookupCacheExpiry());
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  private NetworkConfigFactory(){}
}
