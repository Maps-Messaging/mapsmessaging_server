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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

public enum StatusCode {
  SUCCESS(0, "Operation completed successfully"),
  SUCCESS_QOS_1(1, "Successful subscription at QoS:1"),
  SUCCESS_QOS_2(2, "Successful subscription at QoS:2"),
  NO_MATCHING_SUBSCRIBERS(0x10, "There are currently no matching subscribers on this topic"),
  CONTINUE_AUTHENTICATION(0x18, "Continue authentication"),
  UNSPECIFIED_ERROR(0x80, "Unknown error or unexpected error occurred"),
  MALFORMED_PACKET(0x81, "Received a malformed packet"),
  PROTOCOL_ERROR(0x82, "Received a packet which data broke the agreed protocol"),
  IMPLEMENTATION_SPECIFIC_ERROR(0x83, "An error occurred specific to this implementation"),
  UNSUPPORTED_PROTOCOL_VERSION(0x84, "Unsupported protocol version received"),
  CLIENT_IDENTIFIER_NOT_VALID(0x85, "Invalid client identifier received"),
  BAD_USERNAME_PASSWORD(0x86, "Bad username and password combination"),
  NOT_AUTHORISED(0x87, "Username not authorised on this server"),
  SERVER_UNAVAILABLE(0x88, "Server is currently not available"),
  SERVER_BUSY(0x89, "Server is currently busy"),
  BANNED(0x8A, "Client is currently banned on this server"),
  BAD_AUTHENTICATION_METHOD(0x8C, "Bad authentication method, request method is not supported"),
  TOPIC_FILTER_INVALID(0x8F, "The supplied topic filter is not standard or valid"),
  TOPIC_NAME_INVALID(0x90, "Topic name is invalid"),
  PACKET_IDENTIFIER_INUSE(0x91, "The supplied packet identifier is already in use"),
  PACKET_IDENTIFIER_NOT_FOUND(0x92, "Packet Identifier not found"),
  RECEIVE_MAXIMUM_EXCEEDED(0x93, "Receive Maximum has been exceeded"),
  TOPIC_ALIAS_INVALID(0x94, "Topic Alias invalid"),
  PACKET_TOO_LARGE(0x95, "Packet size is too large for the current configuration"),
  QUOTA_EXCEEDED(0x97, "Client quota has been exceeded"),
  PAYLOAD_FORMAT_INVALID(0x99, "Payload format is invalid"),
  RETAIN_NOT_SUPPORTED(0x9A, "Retain is not supported on this server"),
  QOS_NOT_SUPPORTED(0x9B, "Quality Of Server is not supported on this server"),
  USE_ANOTHER_SERVER(0x9C, "Try a different server"),
  SERVER_MOVED(0x9D, "Server has moved"),
  SHARED_SUBSCRIPTION_NOT_SUPPORTED(0x9E, "Shared subscription is not supported"),
  CONNECTION_RATE_EXCEEDED(0x9F, "Connection rate has been exceeded, please try again later"),
  SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED(0xA1, "Subscription identifiers are not supported"),
  WILDCARD_SUBSCRIPTION_NOT_SUPPORTED(0xA2, "Wildcard subscription is not supported");


  private final byte value;
  private final String description;

  StatusCode(int value, String description) {
    this.value = (byte) value;
    this.description = description;
  }

  public static StatusCode getInstance(byte status) {
    StatusCode[] statusCodes = StatusCode.values();
    for (StatusCode statusCode : statusCodes) {
      if (statusCode.value == status) {
        return statusCode;
      }
    }
    throw new IllegalArgumentException("Invalid handler value supplied");
  }

  public byte getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }
}
