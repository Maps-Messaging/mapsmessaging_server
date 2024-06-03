/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/*
    logger = LoggerFactory.getLogger(MessagePipeline.class);
    maxInFlightEvents = props.getIntProperty("maxInFlightEvents", 1);
    dropQoS0 = props.getBooleanProperty("dropQoS0Events", false);

    long t = TimeUnit.SECONDS.toMillis(props.getIntProperty("eventQueueTimeout", 0));
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class MqttSnConfig extends ProtocolConfig {
  private String gatewayId;
  private int receiveMaximum;
  private long idleSessionTimeout;
  private long maximumSessionExpiry;
  private boolean enablePortChanges;
  private boolean enableAddressChanges;
  private boolean advertiseGateway;
  private boolean dropQoS0;
  private String registeredTopics;
  private int advertiseInterval;
  private int maxRegisteredSize;
  private int maxInFlightEvents;
  private int eventQueueTimeout;

  private List<PredefinedTopics> predefinedTopicsList;

  public MqttSnConfig(ConfigurationProperties config) {
    super(config);
    gatewayId = config.getProperty("gatewayId", "1");
    idleSessionTimeout = config.getLongProperty("idleSessionTimeout", 600);
    receiveMaximum = config.getIntProperty("receiveMaximum", 10);
    maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", 86400);
    enablePortChanges = config.getBooleanProperty("enablePortChanges", true);
    enableAddressChanges = config.getBooleanProperty("enableAddressChanges", false);
    advertiseGateway = config.getBooleanProperty("advertiseGateway", false);
    registeredTopics = config.getProperty("registered", "");
    advertiseInterval = config.getIntProperty("advertiseInterval", 30);
    maxRegisteredSize = config.getIntProperty("maxRegisteredSize", ((1 << 15) - 1));
    maxInFlightEvents = config.getIntProperty("maxInFlightEvents", 1);
    dropQoS0 = config.getBooleanProperty("dropQoS0Events", false);
    eventQueueTimeout = config.getIntProperty("eventQueueTimeout", 0);
    predefinedTopicsList = new ArrayList<>();
    Object predefined = config.get("preDefinedTopics");
    if (predefined instanceof List) {
      List<ConfigurationProperties> predefinedList = (List<ConfigurationProperties>) predefined;
      for (ConfigurationProperties props : predefinedList) {
        predefinedTopicsList.add(new PredefinedTopics(props));
      }
    }
  }

  @Override
  public String getType() {
    return "mqtt-sn";
  }

  public ConfigurationProperties toConfigurationProperties() {
    return super.toConfigurationProperties();
  }
}
