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

package io.mapsmessaging.state.logging;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum StateLogMessages implements LogMessage {

  TWIN_JSON_SERIALISATION_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to serialise twin '{}' of type '{}' to JSON: {}"),
  CONTACT_JSON_SERIALISATION_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to serialise contact for twin '{}' to JSON: {}"),
  TWIN_PUBLISH_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to publish twin '{}' to topic '{}': {}"),
  CONTACT_PUBLISH_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to publish contact for twin '{}' to topic '{}': {}"),

  // <editor-fold desc="Twin Manager">
  TWIN_REGISTERED(LEVEL.INFO, SERVER_CATEGORY.STATE, "Registered twin {} of type {}"),
  TWIN_REGISTER_EXISTING(LEVEL.DEBUG, SERVER_CATEGORY.STATE, "Twin {} already exists, returning existing instance"),
  TWIN_UPDATED(LEVEL.DEBUG, SERVER_CATEGORY.STATE, "Updated twin {}"),
  TWIN_REMOVED(LEVEL.INFO, SERVER_CATEGORY.STATE, "Removed twin {}"),
  TWIN_STATUS_CHANGED(LEVEL.INFO, SERVER_CATEGORY.STATE, "Twin {} status changed from {} to {}"),
  TWIN_RELATIONSHIP_UPSERTED(LEVEL.DEBUG, SERVER_CATEGORY.STATE, "Upserted relationship {} -> {} type {} for twin {}"),
  TWIN_RELATIONSHIP_REMOVED(LEVEL.DEBUG, SERVER_CATEGORY.STATE, "Removed relationship {} -> {} type {} for twin {}"),
  TWIN_PURGED(LEVEL.INFO, SERVER_CATEGORY.STATE, "Purged expired twin {}"),
  TWIN_OBSERVER_CALLBACK_FAILED(LEVEL.ERROR, SERVER_CATEGORY.STATE, "Twin observer callback failed for twin {} during {}"),

  MAVLINK_STATE_DIALECT_DEFAULTED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "No MAVLink dialect specified for state subscriber, using default dialect {}"),
  MAVLINK_STATE_DIALECT_LOADED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Loaded MAVLink dialect {} for state subscriber"),
  MAVLINK_STATE_DIALECT_LOAD_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to load MAVLink dialect {}, falling back to default dialect {}"),
  MAVLINK_STATE_RAW_PACKET_DETECTED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Detected raw MAVLink packet from {}"),
  MAVLINK_STATE_JSON_PACKET_DETECTED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Detected JSON MAVLink packet from {}"),
  MAVLINK_STATE_JSON_PARSE_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Failed to parse JSON MAVLink state packet from {}"),
  MAVLINK_STATE_PACKET_UNPACK_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Failed to unpack MAVLink state packet from {}"),
  MAVLINK_STATE_PACKET_UNPACK_EMPTY(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MAVLink state packet from {} did not produce a processed frame"),
  MAVLINK_STATE_UNSUPPORTED_PACKET_IGNORED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring unsupported MAVLink message {} from {}"),
  MAVLINK_STATE_TWIN_CREATED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Created MAVLink twin {} for system {} component {}"),

  MAVLINK_STATE_SUBSCRIBER_STARTING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Starting MAVLink state subscriber on topic '{}'"),
  MAVLINK_STATE_SUBSCRIBER_STARTED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Started MAVLink state subscriber on topic '{}'"),
  MAVLINK_STATE_SUBSCRIBER_START_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to start MAVLink state subscriber on topic '{}'"),
  MAVLINK_STATE_SUBSCRIBER_STOPPING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Stopping MAVLink state subscriber on topic '{}'"),
  MAVLINK_STATE_SUBSCRIBER_STOPPED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Stopped MAVLink state subscriber on topic '{}'"),
  MAVLINK_STATE_SUBSCRIBER_STOP_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to stop MAVLink state subscriber on topic '{}'"),

  MAVLINK_STATE_EMPTY_MESSAGE_IGNORED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring empty MAVLink state message from '{}'"),
  MAVLINK_STATE_MAVLINK_OBJECT_MISSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring JSON state message from '{}' because it contains no MAVLink object"),
  MAVLINK_STATE_PAYLOAD_OBJECT_MISSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring MAVLink message {} from '{}' because it contains no payload object"),
  MAVLINK_STATE_SOURCE_NOT_CONFIGURED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring MAVLink message {} from system {} component {} because no known source is configured"),
  MAVLINK_STATE_DRONE_NOT_CONFIGURED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Ignoring MAVLink message {} from known source '{}' because drone '{}' is not configured"),
  MAVLINK_STATE_CORRELATION_DATA_MISSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MAVLink message {} from '{}' contains no correlation data"),
  MAVLINK_STATE_PROCESSING_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to process MAVLink state message from '{}'"),
  MAVLINK_STATE_TWIN_UPDATE_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to update drone '{}' from MAVLink message {} received from '{}'"),
  MAVLINK_BOOTSTRAP_REQUEST_SKIPPED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Skipping MAVLink bootstrap request for twin '{}': {}"),
  MAVLINK_BOOTSTRAP_REQUEST_SENT(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Sent MAVLink bootstrap request for message {} to system {} component {} for twin '{}'"),
  MAVLINK_BOOTSTRAP_REQUEST_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to send MAVLink bootstrap request for message {} to twin '{}'"),
  // </editor-fold>

  // <editor-fold desc="State Manager">
  STATE_MANAGER_START(LEVEL.INFO, SERVER_CATEGORY.STATE, "StateManagerAgent starting"),
  STATE_MANAGER_STARTED(LEVEL.INFO, SERVER_CATEGORY.STATE, "StateManagerAgent started"),
  STATE_MANAGER_STOP(LEVEL.INFO, SERVER_CATEGORY.STATE, "StateManagerAgent stopping"),
  STATE_MANAGER_STOPPED(LEVEL.INFO, SERVER_CATEGORY.STATE, "StateManagerAgent stopped"),
  STATE_MANAGER_TAK_ENABLED(LEVEL.INFO, SERVER_CATEGORY.STATE, "TAK observer enabled"),
  STATE_MANAGER_PUBLISH_ENABLED(LEVEL.INFO, SERVER_CATEGORY.STATE, "Twin JSON publisher enabled with topic {}"),
  STATE_MANAGER_PUBLISH_FAILED(LEVEL.ERROR, SERVER_CATEGORY.STATE, "Failed to start Twin JSON publisher"),
  STATE_MANAGER_SCHEDULER_ERROR(LEVEL.ERROR, SERVER_CATEGORY.STATE, "Scheduler task failed"),
  STATE_MANAGER_AUDIT_INIT_FAILED(LEVEL.ERROR, SERVER_CATEGORY.STATE, "Failed to initialize audit context - auditing will be disabled"),
  // </editor-fold>


  N2K_DRONE_CONFIG_RESOLVED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Resolved drone configuration '{}' for N2K topic '{}'"),
  N2K_DRONE_CONFIG_MISSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "No drone configuration found for N2K source '{}' using topic '{}'"),
  N2K_SESSION_START_SKIPPED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "N2K session '{}' was not started for topic '{}' because no matching drone is configured"),
  N2K_SESSION_STARTING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Starting N2K session '{}' using topic '{}'"),
  N2K_SESSION_STARTED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Started N2K session '{}' using topic '{}'"),
  N2K_SESSION_START_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to start N2K session '{}' using topic '{}': {}"),
  N2K_SESSION_STOP_SKIPPED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "N2K session '{}' does not require stopping because no matching drone is configured"),
  N2K_SESSION_STOPPING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Stopping N2K session '{}' using topic '{}'"),
  N2K_SESSION_STOPPED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Stopped N2K session '{}' using topic '{}'"),
  N2K_SESSION_STOP_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to stop N2K session '{}' using topic '{}': {}"),
  N2K_MESSAGE_RECEIVED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Received N2K message from '{}'"),
  N2K_MESSAGE_IGNORED_NO_DRONE(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Ignoring N2K message from '{}' because no drone is configured for N2K source '{}'"),
  N2K_MESSAGE_IGNORED_EMPTY(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring empty N2K message from '{}'"),
  N2K_MESSAGE_IGNORED_NOT_JSON(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring non-JSON N2K message from '{}'"),
  N2K_MESSAGE_IGNORED_NO_J1939(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring N2K message from '{}' because it contains no J1939 object"),
  N2K_MESSAGE_IGNORED_NO_PGN(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring N2K message from '{}' because the J1939 PGN is missing or invalid"),
  N2K_MESSAGE_IGNORED_NO_N2K(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring N2K message from '{}' because it contains no N2K object"),
  N2K_MESSAGE_IGNORED_NO_PACKET(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring N2K PGN '{}' from '{}' because it contains no packet object"),
  N2K_MESSAGE_PROCESSING_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to process N2K message from '{}': {}"),
  N2K_TWIN_UPDATE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Updating drone twin from N2K PGN '{}', source '{}', N2K message '{}'"),
  N2K_TWIN_UPDATED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Updated drone twin from N2K PGN '{}', source '{}'"),

  MAVLINK_EVENT_LIST_SENDER_CREATED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Created MAVLink event list sender '{}', messages {}"),
  MAVLINK_EVENT_LIST_SENDER_STARTING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Starting MAVLink event list sender '{}'"),
  MAVLINK_EVENT_LIST_SENDER_START_IGNORED_TERMINAL(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring start for MAVLink event list sender '{}' because it is terminal"),
  MAVLINK_EVENT_LIST_SENDER_START_IGNORED_STARTED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring start for MAVLink event list sender '{}' because it has already started"),

  MAVLINK_EVENT_LIST_SENDER_INBOUND_IGNORED_TERMINAL(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring inbound MAVLink message for sender '{}' because it is terminal"),
  MAVLINK_EVENT_LIST_SENDER_INBOUND_IGNORED_NOT_WAITING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring inbound MAVLink message for sender '{}' because it is not waiting for acknowledgement"),
  MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_UNRELATED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring unrelated MAVLink acknowledgement for sender '{}', index {}"),
  MAVLINK_EVENT_LIST_SENDER_ACK_PENDING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink acknowledgement pending for sender '{}', index {}"),
  MAVLINK_EVENT_LIST_SENDER_ACK_SUCCESS(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink acknowledgement accepted for sender '{}', index {}, advancing"),
  MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_STATE_CHANGED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring MAVLink acknowledgement for sender '{}' because waiting message changed"),
  MAVLINK_EVENT_LIST_SENDER_ACK_IGNORED_TERMINAL(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring MAVLink acknowledgement for sender '{}' because it is terminal"),

  MAVLINK_EVENT_LIST_SENDER_SENDING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Sending MAVLink message for sender '{}', index {}/{}, message '{}', requires acknowledgement {}"),
  MAVLINK_EVENT_LIST_SENDER_WAITING_FOR_ACK(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' waiting for acknowledgement at index {}"),
  MAVLINK_EVENT_LIST_SENDER_NO_ACK_ADVANCING(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink message for sender '{}', index {}, does not require acknowledgement, advancing"),

  MAVLINK_EVENT_LIST_SENDER_TERMINAL_TRANSITION_IGNORED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Ignoring terminal transition for MAVLink event list sender '{}', requested status '{}'"),
  MAVLINK_EVENT_LIST_SENDER_COMPLETED_SUCCESS(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' completed successfully, messages {}"),
  MAVLINK_EVENT_LIST_SENDER_COMPLETED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' completed with status '{}', index {}/{}, message '{}', reason '{}'"),
  MAVLINK_EVENT_LIST_SENDER_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' failed with status '{}', index {}/{}, message '{}', reason '{}'"),
  MAVLINK_EVENT_LIST_SENDER_FAILED_EXCEPTION(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' failed with exception: {}"),
  MAVLINK_EVENT_LIST_SENDER_COMPLETION_HANDLER_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "MAVLink event list sender '{}' completion handler failed: {}"),

  MAVLINK_TWIN_MANAGER_START_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to start MAVLink twin manager subscriber: {}"),
  MAVLINK_TWIN_MANAGER_STOP_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Failed to stop MAVLink twin manager subscriber: {}");
  //-------------------------------------------------------------------------------------------------------------

  private final @Getter String message;
  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter int parameterCount;

  StateLogMessages(LEVEL level, StateLogMessages.SERVER_CATEGORY category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  public enum SERVER_CATEGORY implements Category {
    NETWORK("Network"),
    PROTOCOL("Protocol"),
    STATE("State");

    private final @Getter String description;

    public String getDivision() {
      return "State";
    }

    SERVER_CATEGORY(String description) {
      this.description = description;
    }
  }
}
