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

package org.maps.messaging.api.features;

/**
 * Enum of features that define how a message is transferred to and from the server
 */
public enum QualityOfService {

  AT_MOST_ONCE(
      0,
      "Best Effort, no guarantee of delivery",
      false,
      false,
      ClientAcknowledgement.AUTO
  ),
  AT_LEAST_ONCE(
      1,
      "Guarantees at least once but may be duplicated delivery if connection fails between sending and Ack",
      true,
      true,
      ClientAcknowledgement.INDIVIDUAL
  ),
  EXACTLY_ONCE(
      2,
      "Only once delivery, in that the event is delivered to the client once and once only",
      true,
      true,
      ClientAcknowledgement.INDIVIDUAL
  ),
  MQTT_SN_REGISTERED(3,
      "Used by MQTT-SN to send publish events to a known topic without the need to have a connection established, this is reserved for MQTT-SN",
      true,
      false,
      ClientAcknowledgement.AUTO
  );


  /**
   * Protocol binary format
   */
  private final int level;

  /**
   * Description of what this quality level means
   */
  private final String description;

  /**
   * Should events with this level be stored to disk
   */
  private final boolean storeOffLine;

  /**
   * Does this level require individual packet identification
   */
  private final boolean sendPacketId;

  /**
   *
   */
  private final ClientAcknowledgement clientAcknowledgement;

  // This is a false positive, this constructor IS used by the enums above, but it is recorded as unused
  @java.lang.SuppressWarnings("squid:UnusedPrivateMethod")
  QualityOfService(int level, String description, boolean storeOffLine, boolean sendPacketId, ClientAcknowledgement clientAcknowledgement) {
    this.level = level;
    this.description = description;
    this.storeOffLine = storeOffLine;
    this.sendPacketId = sendPacketId;
    this.clientAcknowledgement = clientAcknowledgement;
  }

  public static QualityOfService getInstance(int value) {
    switch (value) {
      case 0:
        return AT_MOST_ONCE;
      case 1:
        return AT_LEAST_ONCE;
      case 2:
        return EXACTLY_ONCE;
      case 3:
        return MQTT_SN_REGISTERED;

      default:
        throw new IllegalArgumentException("Invalid handler value supplied");
    }
  }

  public int getLevel() {
    return level;
  }

  public boolean storeOffline() {
    return storeOffLine;
  }

  public boolean isSendPacketId() {
    return sendPacketId;
  }

  public String getDescription() {
    return description;
  }

  public ClientAcknowledgement getClientAcknowledgement() {
    return clientAcknowledgement;
  }

}
