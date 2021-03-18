/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import java.util.StringTokenizer;

public class SubscriptionInfo {

  private final String topicName;
  private final QualityOfService qualityOfService;
  private final RetainHandler retainHandling;
  private final boolean noLocal;
  private final boolean retainAsPublished;
  private final boolean isShared;
  private final String sharedName;
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
      isShared = true;
      StringTokenizer st = new StringTokenizer(topicName, "/");
      st.nextElement(); // Should be $share
      sharedName = st.nextElement().toString();
      this.topicName = topicName.substring(sharedName.length() + 8);
    } else {
      this.topicName = topicName;
      isShared = false;
      sharedName = null;
    }
  }

  public boolean isSharedSubscription() {
    return isShared;
  }

  public String getSharedSubscriptionName() {
    return sharedName;
  }

  public String getTopicName() {
    return topicName;
  }

  public QualityOfService getQualityOfService() {
    return qualityOfService;
  }

  public RetainHandler getRetainHandling() {
    return retainHandling;
  }

  public boolean isRetainAsPublished() {
    return retainAsPublished;
  }

  public boolean noLocalMessages() {
    return noLocal;
  }

  public int length() {
    return topicName.length() + 1; // Include QoS
  }

  public SubscriptionIdentifier getSubscriptionIdentifier() {
    return subscriptionIdentifier;
  }

  @Override
  public String toString() {
    return "Topic:" + topicName + " QoS:" + qualityOfService + " RetainHandling:" + retainHandling + " RetainAsPublish:" + retainAsPublished + " NoLocalMessage:" + noLocal
        + " ShareName:" + sharedName;
  }
}
