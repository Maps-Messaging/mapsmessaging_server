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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import lombok.Getter;

import java.util.StringTokenizer;

public class SubscriptionInfo {

  @Getter
  private final String topicName;
  @Getter
  private final QualityOfService qualityOfService;
  @Getter
  private final RetainHandler retainHandling;
  private final boolean noLocal;
  @Getter
  private final boolean retainAsPublished;
  @Getter
  private final boolean sharedSubscription;
  @Getter
  private final String sharedSubscriptionName;
  @Getter
  private final SubscriptionIdentifier subscriptionIdentifier;

  public SubscriptionInfo(String topicName, QualityOfService qualityOfService) {
    this(topicName, qualityOfService, RetainHandler.SEND_ALWAYS, false, false, null);
  }

  public SubscriptionInfo(String topicName, QualityOfService qualityOfService, RetainHandler retainHandling, boolean noLocal, boolean retainAsPublished,
      SubscriptionIdentifier subscriptionIdentifier) {
    this.qualityOfService = qualityOfService;
    this.retainHandling = retainHandling;
    this.noLocal = noLocal;
    this.retainAsPublished = retainAsPublished;
    this.subscriptionIdentifier = subscriptionIdentifier;
    if (topicName.toLowerCase().startsWith("$share")) {
      sharedSubscription = true;
      StringTokenizer st = new StringTokenizer(topicName, "/");
      st.nextElement(); // Should be $share
      sharedSubscriptionName = st.nextElement().toString();
      this.topicName = topicName.substring(sharedSubscriptionName.length() + 8);
    } else {
      this.topicName = topicName;
      sharedSubscription = false;
      sharedSubscriptionName = null;
    }
  }

  public boolean noLocalMessages() {
    return noLocal;
  }

  public int length() {
    return topicName.length() + 1; // Include QoS
  }

  @Override
  public String toString() {
    return "Topic:" + topicName + " QoS:" + qualityOfService + " RetainHandling:" + retainHandling + " RetainAsPublish:" + retainAsPublished + " NoLocalMessage:" + noLocal
        + " ShareName:" + sharedSubscriptionName;
  }
}
