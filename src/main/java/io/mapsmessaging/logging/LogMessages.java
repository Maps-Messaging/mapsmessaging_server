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

package io.mapsmessaging.logging;

/**
 * This enum contains all log messages and the configuration around the log message. This enables log messages to be modified in one place without searching for the log message
 * it also enables the log messages to be translated into other languages if required
 */

public enum LogMessages {
  DEBUG(LEVEL.DEBUG, CATEGORY.TEST, "Debug Testing Only - {}"),
  INFO(LEVEL.INFO, CATEGORY.TEST, "Info Testing Only - {}"),
  WARN(LEVEL.WARN, CATEGORY.TEST, "Warn Testing Only - {}"),

  // <editor-fold desc="Generic messages">
  PUSH_WRITE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Pushed Packet for write, {}"),
  RECEIVE_PACKET(LEVEL.INFO, CATEGORY.PROTOCOL, "Received Packet:{}"),
  RESPONSE_PACKET(LEVEL.INFO, CATEGORY.PROTOCOL, "Responding Packet:{}"),
  MALFORMED(LEVEL.WARN, CATEGORY.PROTOCOL, "Malformed Exception raised {}"),
  END_POINT_CLOSE_EXCEPTION(LEVEL.TRACE, CATEGORY.PROTOCOL, "An exception was raised during the close of an end point"),
  SESSION_CLOSE_EXCEPTION(LEVEL.TRACE, CATEGORY.ENGINE, "An exception was raised during the of a session"),
  TRANSACTION_EXCEPTION(LEVEL.WARN, CATEGORY.ENGINE, "An exception was raised processing a transaction [ {} ]"),
  // </editor-fold>

  // <editor-fold desc="Main Message Daemon messages">
  MESSAGE_DAEMON_STARTUP(LEVEL.WARN, CATEGORY.ENGINE, "Starting Messaging Daemon Version:{} Build Date:{}"),
  MESSAGE_DAEMON_NO_HOME_DIRECTORY(LEVEL.ERROR, CATEGORY.ENGINE, "The supplied home directory, {}, does not exist"),
  MESSAGE_DAEMON_SERVICE(LEVEL.WARN, CATEGORY.ENGINE, "\t\tLoaded service {}, {}" ),
  MESSAGE_DAEMON_SERVICE_LOADED(LEVEL.WARN, CATEGORY.ENGINE, "Service Manager {} loaded" ),

  // </editor-fold>

  // <editor-fold desc="hawtio messages">
  HAWTIO_STARTUP(LEVEL.INFO, CATEGORY.ENGINE, "Starting Hawtio interface"),
  HAWTIO_STARTUP_FAILURE(LEVEL.WARN, CATEGORY.ENGINE, "Hawtio failed to start"),
  HAWTIO_WAR_FILE_NOT_FOUND(LEVEL.WARN, CATEGORY.ENGINE, "Hawtio WAR file not found, at location {}"),
  HAWTIO_NOT_CONFIGURED_TO_RUN(LEVEL.INFO, CATEGORY.ENGINE, "Hawtio interface not configured to run"),
  HAWTIO_INITIALISATION(LEVEL.INFO, CATEGORY.ENGINE, "Hawtio initialisation started using file {} "),
  // </editor-fold>

  // <editor-fold desc="Network Manager log messages">
  NETWORK_MANAGER_STARTUP(LEVEL.DEBUG, CATEGORY.ENGINE, "Starting Network Manager"),
  NETWORK_MANAGER_LOAD_PROPERTIES(LEVEL.DEBUG, CATEGORY.ENGINE, "Loading Network Manager Properties"),
  NETWORK_MANAGER_STARTUP_COMPLETE(LEVEL.DEBUG, CATEGORY.ENGINE, "Completed startup Network Manager"),
  NETWORK_MANAGER_START_ALL(LEVEL.DEBUG, CATEGORY.ENGINE, "Starting all network interfaces"),
  NETWORK_MANAGER_STOP_ALL(LEVEL.DEBUG, CATEGORY.ENGINE, "Stopping all network interfaces"),
  NETWORK_MANAGER_PAUSE_ALL(LEVEL.DEBUG, CATEGORY.ENGINE, "Pausing all network interfaces"),
  NETWORK_MANAGER_RESUME_ALL(LEVEL.DEBUG, CATEGORY.ENGINE, "Resuming all network interfaces"),
  NETWORK_MANAGER_START_FAILURE(LEVEL.WARN, CATEGORY.ENGINE, "Unable to start {} due to the following exception"),
  NETWORK_MANAGER_START_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to start interface {}"),
  NETWORK_MANAGER_STOP_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to stop interface {}"),
  NETWORK_MANAGER_PAUSE_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to pause interface {}"),
  NETWORK_MANAGER_RESUME_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to resume interface {}"),
  NETWORK_MANAGER_DEVICE_NOT_LOADED(LEVEL.WARN, CATEGORY.ENGINE, "Device configured, {}, can not be used since it is not loaded"),

  // </editor-fold>

  // <editor-fold desc=" End Point Manager log messages">
  END_POINT_MANAGER_SELECTOR_START(LEVEL.DEBUG, CATEGORY.ENGINE, "Creating network Selector thread"),
  END_POINT_MANAGER_START(LEVEL.DEBUG, CATEGORY.ENGINE, "Start called on {}"),
  END_POINT_MANAGER_CLOSE(LEVEL.DEBUG, CATEGORY.ENGINE, "Close called on {}"),
  END_POINT_MANAGER_PAUSE(LEVEL.DEBUG, CATEGORY.ENGINE, "Pause called on {}"),
  END_POINT_MANAGER_RESUME(LEVEL.DEBUG, CATEGORY.ENGINE, "Resume called on {}"),
  END_POINT_MANAGER_CLOSE_SERVER(LEVEL.DEBUG, CATEGORY.ENGINE, "Closing end point on closed server"),
  END_POINT_MANAGER_NEW_SELECTOR(LEVEL.INFO, CATEGORY.ENGINE, "Created thread safe selector implementation due to the JDK support on interface"),
  END_POINT_MANAGER_OLD_SELECTOR(LEVEL.INFO, CATEGORY.ENGINE, "Created queue based selector implementation due to the JDK not supporting thread safe java.nio.Selector on interface"),
  END_POINT_MANAGER_ACCEPT_EXCEPTION(LEVEL.WARN, CATEGORY.ENGINE, "Exception raised during accept handling of the new connection"),
  END_POINT_MANAGER_CLOSE_EXCEPTION(LEVEL.WARN, CATEGORY.ENGINE, "Closing end point on accept server raised exception"),
  END_POINT_MANAGER_CLOSE_EXCEPTION_1(LEVEL.WARN, CATEGORY.ENGINE, "Closing end point on closed server raised exception"),
  // </editor-fold>

  // <editor-fold desc="Selector and Selector task log messages">
  SELECTOR_OPEN(LEVEL.DEBUG, CATEGORY.NETWORK, "Opening Selector"),
  SELECTOR_FAILED_ON_CALL(LEVEL.DEBUG, CATEGORY.NETWORK, "Selector failed on select call, exiting select thread"),
  SELECTOR_FIRED(LEVEL.DEBUG, CATEGORY.NETWORK, "selection key is fired {}"),
  SELECTOR_CONNECTION_CLOSE(LEVEL.DEBUG, CATEGORY.NETWORK, "Connection has been closed"),
  SELECTOR_TASK_FAILED(LEVEL.DEBUG, CATEGORY.NETWORK, "Failed processing selection task {}"),
  SELECTOR_TASK_FAILED_1(LEVEL.WARN, CATEGORY.NETWORK, "Failed processing selection task {}"),
  SELECTOR_NEW_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "Creating new Selector task"),
  SELECTOR_CLOSE_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "Closed Selector task"),
  SELECTOR_PUSH_WRITE(LEVEL.DEBUG, CATEGORY.NETWORK, "Pushed {} for write, Depth:{}"),
  SELECTOR_REGISTERING(LEVEL.DEBUG, CATEGORY.NETWORK, "Registering {}"),
  SELECTOR_REGISTER_RESULT(LEVEL.DEBUG, CATEGORY.NETWORK, "Registered resulted in {}"),
  SELECTOR_REGISTER_CLOSED_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "Registering on closed task {}"),
  SELECTOR_CANCELLING(LEVEL.DEBUG, CATEGORY.NETWORK, "Cancelling {} replacing with {}"),
  SELECTOR_CANCEL_FAILED(LEVEL.INFO, CATEGORY.NETWORK, "Exception raised during cancelling of the selector"),
  SELECTOR_CALLED_BACK(LEVEL.DEBUG, CATEGORY.NETWORK, "Selected called back for {}"),
  SELECTOR_READ_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "Selected calling Read task"),
  SELECTOR_WRITE_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "Selected calling Write task"),
  // </editor-fold>

  // <editor-fold desc="Read Task log messages">
  READ_TASK_EXCEPTION(LEVEL.ERROR, CATEGORY.NETWORK, "Runtime exception raised during packet handling in protocol"),
  READ_TASK_COMPLETED(LEVEL.TRACE, CATEGORY.NETWORK, "Completed read at Pos:{} Limit:{} Response:{}"),
  READ_TASK_ZERO_BYTE(LEVEL.DEBUG, CATEGORY.NETWORK, "Zero bytes read from end point Position:{} Limit:{} Capacity:{}"),
  READ_TASK_PACKET_EXCEPTION(LEVEL.INFO, CATEGORY.NETWORK, "Packet handling raised an exception, closing end point"),
  READ_TASK_READ_PROCESSING(LEVEL.DEBUG, CATEGORY.NETWORK, "Read processing Packet with {} bytes, {}"),
  READ_TASK_POST_PROCESSING(LEVEL.DEBUG, CATEGORY.NETWORK, "Packet Post Process:{}"),
  READ_TASK_COMPACT(LEVEL.TRACE, CATEGORY.NETWORK, "Compact for next write {}"),
  READ_TASK_POSITION(LEVEL.TRACE, CATEGORY.NETWORK, "Set the position for next write {}"),
  READ_TASK_NEGATIVE_CLOSE(LEVEL.INFO, CATEGORY.NETWORK, "Negative value returned on read, closing Protocol"),
  // </editor-fold>

  // <editor-fold desc="Write Task log messages">
  WRITE_TASK_RESUMING(LEVEL.DEBUG, CATEGORY.NETWORK, "Resuming buffer write Pos:{} Limit:{}"),
  WRITE_TASK_COMPLETE(LEVEL.DEBUG, CATEGORY.NETWORK, "Buffer Completed Write"),
  WRITE_TASK_INCOMPLETE(LEVEL.DEBUG, CATEGORY.NETWORK, "Buffer incomplete write Pos:{} Limit:{}"),
  WRITE_TASK_WRITE(LEVEL.DEBUG, CATEGORY.NETWORK, "Registered Write"),
  WRITE_TASK_WRITE_CANCEL(LEVEL.DEBUG, CATEGORY.NETWORK, "Cancelled Write"),
  WRITE_TASK_WRITE_PACKET(LEVEL.DEBUG, CATEGORY.NETWORK, "Write processing Packet {}"),
  WRITE_TASK_BLOCKED(LEVEL.DEBUG, CATEGORY.NETWORK, "End Point blocked exiting write and registering with selector"),
  WRITE_TASK_CLOSE_FAILED(LEVEL.DEBUG, CATEGORY.NETWORK, "failed to close EndPoint during exception handling"),
  WRITE_TASK_UNABLE_TO_ADD_WRITE(LEVEL.WARN, CATEGORY.NETWORK, "Unable to add write handle to selector"),
  WRITE_TASK_SEND_FAILED(LEVEL.INFO, CATEGORY.NETWORK, "Failed to send packet"),
  WRITE_TASK_BUFFERSIZE(LEVEL.WARN, CATEGORY.NETWORK, "Failed to parse write buffer size"),
  // </editor-fold>

  // <editor-fold desc="TCP/IP EndPoint log messages">
  TCP_ACCEPT_START(LEVEL.INFO, CATEGORY.NETWORK, "Accepting socket {}"),
  TCP_CONNECT_FAILED(LEVEL.INFO, CATEGORY.NETWORK, "Failed to accept socket {}"),
  TCP_CONNECTION_CLOSE(LEVEL.DEBUG, CATEGORY.NETWORK, "Closed socket end point to host {}"),
  TCP_CLOSE_EXCEPTION(LEVEL.INFO, CATEGORY.NETWORK, "Exception raised during close {}"),
  TCP_CLOSE_SUCCESS(LEVEL.DEBUG, CATEGORY.NETWORK, "Closed socket end point to host {}"),
  TCP_SEND_BUFFER(LEVEL.TRACE, CATEGORY.NETWORK, "Sent {} bytes to socket"),
  TCP_READ_BUFFER(LEVEL.TRACE, CATEGORY.NETWORK, "Read {} bytes from socket"),
  TCP_CLOSE_ERROR(LEVEL.TRACE, CATEGORY.NETWORK, "Exception raised while closing the physical socket"),
  TCP_CONFIGURED_PARAMETER(LEVEL.TRACE, CATEGORY.NETWORK, "Setting socket parameter: {} to {} "),
  // </editor-fold>

  //<editor-fold desc="Local Loop End Point">
  NOOP_END_POINT_CREATE(LEVEL.DEBUG, CATEGORY.NETWORK, "Created a no-op end point {}"),
  NOOP_END_POINT_CLOSE(LEVEL.DEBUG, CATEGORY.NETWORK, "Closed a no-op end point {}"),
  //</editor-fold>


  // <editor-fold desc="TCP/IP Server EndPoint log message">
  TCP_SERVER_ENDPOINT_CREATE(LEVEL.DEBUG, CATEGORY.NETWORK, "Creating Server Socket on port {} with backlog of {} on interface {}"),
  TCP_SERVER_ENDPOINT_CLOSE(LEVEL.DEBUG, CATEGORY.NETWORK, "Closing Server Socket"),
  TCP_SERVER_ENDPOINT_REGISTER(LEVEL.DEBUG, CATEGORY.NETWORK, "Registering selector"),
  TCP_SERVER_ENDPOINT_DEREGISTER(LEVEL.DEBUG, CATEGORY.NETWORK, "Deregister selector"),
  TCP_SERVER_ENDPOINT_ACCEPT(LEVEL.WARN, CATEGORY.NETWORK, "Accept failed with "),

  // </editor-fold>

  // <editor-fold desc="SSL End Point log messages">
  SSL_CREATE_ENGINE(LEVEL.DEBUG, CATEGORY.NETWORK, "Creating SSL engine and configuring for server mode"),
  SSL_ENCRYPTION_BUFFERS(LEVEL.DEBUG, CATEGORY.NETWORK, "Setting up encryption buffers, size set to {}"),
  SSL_HANDSHAKE_START(LEVEL.DEBUG, CATEGORY.NETWORK, "Creating SSL Handshake manager to establish valid SSL session"),
  SSL_HANDSHAKE_READY(LEVEL.DEBUG, CATEGORY.NETWORK, "SSL Engine ready to start handshaking"),
  SSL_SENT(LEVEL.TRACE, CATEGORY.NETWORK, "SSL sent {} bytes"),
  SSL_READ(LEVEL.TRACE, CATEGORY.NETWORK, "SSL read {} bytes"),
  SSL_SEND_ENCRYPTED(LEVEL.TRACE, CATEGORY.NETWORK, "SSL sent {} encrypted bytes"),
  SSL_READ_ENCRYPTED(LEVEL.TRACE, CATEGORY.NETWORK, "SSL read {} encrypted bytes, Position:{} Limit:{}"),
  SSL_ENGINE_RESULT(LEVEL.TRACE, CATEGORY.NETWORK, "SSLEngine Result::{}"),
  SSL_ENGINE_CLIENT_AUTH(LEVEL.WARN, CATEGORY.NETWORK, "SSL Engine requires client authentication, however, the peer has not been verified"),
  // </editor-fold>

  // <editor-fold desc="SSL handshake log messages">
  SSL_HANDSHAKE_NEED_UNWRAP(LEVEL.DEBUG, CATEGORY.NETWORK, "SSL handshake state :: NEED UNWRAP"),
  SSL_HANDSHAKE_NEED_WRAP(LEVEL.DEBUG, CATEGORY.NETWORK, "SSL handshake state :: NEED WRAP"),
  SSL_HANDSHAKE_FINISHED(LEVEL.DEBUG, CATEGORY.NETWORK, "SSL handshake state :: FINISHED"),
  SSL_HANDSHAKE_ENCRYPTED(LEVEL.DEBUG, CATEGORY.NETWORK, "Encrypted In Buffer Status : Position:{} Limit:{}"),
  SSL_HANDSHAKE_NEED_TASK(LEVEL.DEBUG, CATEGORY.NETWORK, "SSL handshake state :: NEED_TASK"),
  SSL_HANDSHAKE_EXCEPTION(LEVEL.WARN, CATEGORY.NETWORK, "SSL handshake raised exception"),
  // </editor-fold>

  // <editor-fold desc="SSL Server EndPoint log messages">
  SSL_SERVER_START(LEVEL.DEBUG, CATEGORY.NETWORK, "Starting to building SSL Context"),
  SSL_SERVER_INITIALISE(LEVEL.DEBUG, CATEGORY.NETWORK, "InitialedKey Manager Factory of type {}"),
  SSL_SERVER_TRUST_MANAGER(LEVEL.DEBUG, CATEGORY.NETWORK, "Initialised Trust Manager Factory of type {}"),
  SSL_SERVER_CONTEXT_CONSTRUCT(LEVEL.DEBUG, CATEGORY.NETWORK, "Constructing SSL Context with the created key and trust stores"),
  SSL_SERVER_SSL_CONTEXT_COMPLETE(LEVEL.DEBUG, CATEGORY.NETWORK, "Completed construction of the SSL Context with the created key and trust stores"),
  SSL_SERVER_COMPLETED(LEVEL.DEBUG, CATEGORY.NETWORK, "Completed building SSL Context"),
  SSL_SERVER_LOAD_KEY_STORE(LEVEL.DEBUG, CATEGORY.NETWORK, "Loading Key Store {} of type {}"),
  SSL_SERVER_LOADED_KEY_STORE(LEVEL.DEBUG, CATEGORY.NETWORK, "Loaded Key Store {} of type {}"),
  SSL_SERVER_ACCEPT_FAILED(LEVEL.WARN, CATEGORY.NETWORK, "Accept failed"),
  // </editor-fold>

  // <editor-fold desc="UDP EndPoint log messages">
  UDP_CREATED(LEVEL.TRACE, CATEGORY.NETWORK, "Created new UDP EndPoint at {}"),
  UDP_SENT_BYTES(LEVEL.TRACE, CATEGORY.NETWORK, "Sent {} byte datagram"),
  UDP_READ_BYTES(LEVEL.TRACE, CATEGORY.NETWORK, "Received a {} byte datagram"),
  UDP_READ_TASK_READ_PACKET(LEVEL.TRACE, CATEGORY.NETWORK, "Processing packet {}"),
  UDP_WRITE_TASK_SENT_PACKET(LEVEL.TRACE, CATEGORY.NETWORK, "UDP EndPoint sent packet {}"),
  UDP_WRITE_TASK_SEND_PACKET_ERROR(LEVEL.WARN, CATEGORY.NETWORK, "Exception raised during packet send"),
  UDP_WRITE_TASK_UNABLE_TO_REMOVE_WRITE(LEVEL.INFO, CATEGORY.NETWORK, "Unable to remove WRITE interest from the selector"),
  // </editor-fold>

  // <editor-fold desc="Security Manager based log messages">
  SECURITY_MANAGER_STARTUP(LEVEL.DEBUG, CATEGORY.ENGINE, "Starting Security Manager"),
  SECURITY_MANAGER_LOG_IN(LEVEL.ERROR, CATEGORY.ENGINE, "User {} successfully logged in"),
  SECURITY_MANAGER_LOG_OFF(LEVEL.ERROR, CATEGORY.ENGINE, "User {} successfully logged off"),
  SECURITY_MANAGER_FAILED_LOG_IN(LEVEL.WARN, CATEGORY.ENGINE, "User {} failed to logged in, {}"),
  SECURITY_MANAGER_FAILED_LOG_OFF(LEVEL.WARN, CATEGORY.ENGINE, "User {} failed to logged off, {}"),
  SECURITY_MANAGER_LOADING(LEVEL.DEBUG, CATEGORY.ENGINE, "Loading Security Manager properties"),
  SECURITY_MANAGER_LOADED(LEVEL.DEBUG, CATEGORY.ENGINE, "Loaded Security Manager Properties"),
  // </editor-fold>

  // <editor-fold desc="Anonymous Login module log messages">
  ANON_LOGIN_MODULE_USERNAME(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] user entered user name: {}"),
  ANON_LOGIN_MODULE_PASSWORD(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] user entered password: {}"),
  ANON_LOGIN_MODULE_SUBJECT(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] added AnonymousPrincipal to Subject"),
  ANON_LOGIN_MODULE_LOG_OUT(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] logged out Subject"),
  // </editor-fold>

  // <editor-fold desc="Anonymous Security log messages">
  ANONYMOUS_SECURITY_LOG_IN(LEVEL.ERROR, CATEGORY.AUTHENTICATION, "User {} successfully logged in"),
  ANONYMOUS_SECURITY_LOG_OFF(LEVEL.ERROR, CATEGORY.AUTHENTICATION, "User {} successfully logged off"),
  // </editor-fold>

  // <editor-fold desc="SSL Certificate security log messages">
  SSL_CERTIFICATE_SECURITY_USERNAME(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] user entered user name: {}"),
  SSL_CERTIFICATE_SECURITY_PASSWORD(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] user entered password: {}"),
  SSL_CERTIFICATE_SECURITY_SUBJECT_LOG_IN(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] added AnonymousPrincipal to Subject"),
  SSL_CERTIFICATE_SECURITY_SUBJECT_LOG_OUT(LEVEL.DEBUG, CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] logged out Subject"),
  // </editor-fold>

  // <editor-fold desc="Session Manager log messages">
  SESSION_MANAGER_STARTUP(LEVEL.INFO, CATEGORY.ENGINE, "Session Manager is starting up"),
  SESSION_MANAGER_CREATE(LEVEL.DEBUG, CATEGORY.ENGINE, "Creating new Session with Context {}"),
  SESSION_MANAGER_CREATE_SECURITY_CONTEXT(LEVEL.DEBUG, CATEGORY.ENGINE, "Created Security Context"),
  SESSION_MANAGER_FOUND_CLOSED(LEVEL.DEBUG, CATEGORY.ENGINE, "Found and closed existing session that matched {}"),
  SESSION_MANAGER_KEEP_ALIVE_TASK(LEVEL.DEBUG, CATEGORY.ENGINE, "Created new Keep Alive scheduler task"),
  SESSION_MANAGER_WILL_TASK(LEVEL.DEBUG, CATEGORY.ENGINE, "Build WillTask for {} WillTask:{}"),
  SESSION_MANAGER_LOADED_SUBSCRIPTION(LEVEL.DEBUG, CATEGORY.ENGINE, "Loaded Subscription Manager {}, containing {}"),
  SESSION_MANAGER_NO_EXISTING(LEVEL.DEBUG, CATEGORY.ENGINE, "No existing Subscription manager found for {}"),
  SESSION_MANAGER_ADDING_SUBSCRIPTION(LEVEL.DEBUG, CATEGORY.ENGINE, "Adding Subscription Manager for persistent usage {}"),
  SESSION_MANAGER_FOUND_EXISTING(LEVEL.DEBUG, CATEGORY.ENGINE, "Found existing subscription manager {}, resetting due to reset state flag set{}"),
  SESSION_MANAGER_STOPPING(LEVEL.INFO, CATEGORY.ENGINE, "Session Manager is shutting down"),
  SESSION_MANAGER_LOADING_SESSION(LEVEL.INFO, CATEGORY.ENGINE, "Starting session id {} with {} subscriptions"),
  SESSION_MANAGER_CLOSING_SESSION(LEVEL.INFO, CATEGORY.ENGINE, "Cleaning up session {}"),
  // </editor-fold>

  // <editor-fold desc="Will message processing log messages">
  WILL_TASK_SENDING(LEVEL.DEBUG, CATEGORY.ENGINE, "Sending Will Message {}"),
  WILL_TASK_EXCEPTION(LEVEL.INFO, CATEGORY.ENGINE, "Exception raised during sending of will message:{}"),
  // </editor-fold>

  // <editor-fold desc="Shared Subscription log messages">
  SHARED_SUBSCRIPTION_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to send message to subscription"),
  // </editor-fold>

  // <editor-fold desc="Wildcard log messages">
  WILDCARD_CONSTRUCTED(LEVEL.DEBUG, CATEGORY.ENGINE, "Constructed wildcard subscription for {} for {}"),
  WILDCARD_REMOVING_LAST(LEVEL.DEBUG, CATEGORY.ENGINE, "Last subscription entry found, removing the physical subscription"),
  WILDCARD_CREATED(LEVEL.DEBUG, CATEGORY.ENGINE, "Created subscription for {} for {} since matches wildcard {}"),
  WILDCARD_HIBERNATED(LEVEL.DEBUG, CATEGORY.ENGINE, "Created hibernated subscription for {} since matches wildcard {}"),
  WILDCARD_DELETED(LEVEL.DEBUG, CATEGORY.ENGINE, "Deleted subscription for {} for {} since matches wildcard {}"),
  // </editor-fold>

  // <editor-fold desc="Subscription log messages">
  SUBSCRIPTION_MGR_OVERLAP_DETECTED(LEVEL.DEBUG, CATEGORY.ENGINE, "Detected overlapping subscriptions, creating overlapping structure to support it"),
  SUBSCRIPTION_MGR_EXISTING_ADD(LEVEL.DEBUG, CATEGORY.ENGINE, "Already have multiple subscriptions on destination, simply add to the list"),
  SUBSCRIPTION_MGR_CANCELLING_SCHEDULER(LEVEL.WARN, CATEGORY.ENGINE, "Cancelled running timeout scheduler"),
  SUBSCRIPTION_MGR_SCHEDULED(LEVEL.WARN, CATEGORY.ENGINE, "Scheduled timeout scheduler in {} seconds::{}"),
  SUBSCRIPTION_MGR_WAKING_SESSION(LEVEL.WARN, CATEGORY.ENGINE, "Waking Session for {}"),
  SUBSCRIPTION_MGR_TIME_CANCELLED(LEVEL.WARN, CATEGORY.ENGINE, "marking timeout scheduler as cancelled {}"),
  SUBSCRIPTION_MGR_TIME_OUT_NULL(LEVEL.WARN, CATEGORY.ENGINE, "Timeout Scheduler is NULL for {}"),
  SUBSCRIPTION_MGR_CANCELLED_TIMEOUT(LEVEL.WARN, CATEGORY.ENGINE, "Cancelling timeout scheduler::{}"),
  SUBSCRIPTION_MGR_NO_TASK(LEVEL.WARN, CATEGORY.ENGINE, "No scheduled tasks for {}"),
  SUBSCRIPTION_MGR_DUPLICATE_SUBSCRIPTION(LEVEL.WARN, CATEGORY.ENGINE, "Duplicate subscriptions detected and overlapping subscriptions are not allowed for destination {}"),
  SUBSCRIPTION_MGR_FAILED_TO_SEND_RETAIN(LEVEL.WARN, CATEGORY.ENGINE, "Failed to send retained message to new subscription on {}"),
  SUBSCRIPTION_MGR_SESSION_TIMEOUT(LEVEL.WARN, CATEGORY.ENGINE, "Session timeout reached for {} after {} seconds{}"),
  SUBSCRIPTION_MGR_RELOAD(LEVEL.INFO, CATEGORY.ENGINE, "Loaded and registered subscription for session id {} for {}, Subscription {} out of {}"),
  SUBSCRIPTION_MGR_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Subscription failed to reload for session id {} for {}"),
  SUBSCRIPTION_MGR_CREATE_SUBSCRIPTION(LEVEL.INFO, CATEGORY.ENGINE, "Creating new subscription on {} with a name of {} using filter of {}"),
  SUBSCRIPTION_MGR_REMOVED(LEVEL.INFO, CATEGORY.ENGINE, "Removing subscription {} to destination {}"),
  SUBSCRIPTION_MGR_CLEAR_SESSION(LEVEL.INFO, CATEGORY.ENGINE, "Clearing all subscriptions for session {}"),
  SUBSCRIPTION_MGR_CLOSE(LEVEL.INFO, CATEGORY.ENGINE, "Closing Subscription Manager {}"),
  SUBSCRIPTION_MGR_CLOSED(LEVEL.INFO, CATEGORY.ENGINE, "Closed Subscription Manager {}"),
  SUBSCRIPTION_MGR_HIBERNATE(LEVEL.INFO, CATEGORY.ENGINE, "Hibernating subscription {}"),
  SUBSCRIPTION_MGR_CREATE_WILDCARD(LEVEL.INFO, CATEGORY.ENGINE, "Creating wildcard subscription {} on {}"),
  SUBSCRIPTION_MGR_SELECTOR_EXCEPTION(LEVEL.ERROR, CATEGORY.ENGINE, "Exception raised compiling Selector {}"),
  SUBSCRIPTION_MGR_CLOSE_SUB_ERROR(LEVEL.ERROR, CATEGORY.ENGINE, "Exception raised while closing subscriptions"),
  //</editor-fold>

  // <editor-fold desc="Echo Protocol log messages">
  ECHO_CLOSED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "EndPoint closed"),
  ECHO_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised during selected function"),
  ECHO_CLOSE_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "EndPoint was closing but raised an exception"),
  // </editor-fold>

  // <editor-fold desc="Stomp Protocol log messages">
  STOMP_STATE_ENGINE_FAILED_COMPLETION(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed on frame completion callback"),
  STOMP_STARTING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Starting Stomp Protocol Implementation on {}"),
  STOMP_CLOSING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Closing Stomp Implementation {}"),
  STOMP_PUSHED_WRITE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Pushed Frame for write, {}"),
  STOMP_FAILED_MAXIMUM_BUFFER(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set maximum buffer size, is not an integer::{}, using default of {}"),
  STOMP_FAILED_CLOSE(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed in close"),
  STOMP_PROCESSING_FRAME(LEVEL.INFO, CATEGORY.PROTOCOL, "Processing frame {}"),
  STOMP_PROCESSING_FRAME_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Processing frame raised exception, closing session"),
  STOMP_INVALID_FRAME(LEVEL.WARN, CATEGORY.PROTOCOL, "Invalid STOMP frame received.. Unable to process::{}"),
  STOMP_FRAME_HANDLE_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised during frame {} processing"),
  // </editor-fold>

  // <editor-fold desc="MQTT 3.1.1 log messages">
  MQTT_CONNECT_LISTENER_FAILED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "[MQTT-3.2.2-5] Connection failed with return code, {}, closing connection"),
  MQTT_CONNECT_LISTENER_SECOND_CONNECT(LEVEL.WARN, CATEGORY.PROTOCOL, "[MQTT-3.1.0-2] Received a second CONNECT packet"),
  MQTT_CONNECT_LISTENER_SESSION_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to construct a session for {}"),
  MQTT_BAD_USERNAME_PASSWORD(LEVEL.INFO, CATEGORY.PROTOCOL, "Invalid username or password combination supplied"),
  MQTT_DUPLICATE_EVENT_RECEIVED(LEVEL.WARN, CATEGORY.PROTOCOL, "Detected duplicate events from the client, id {}" ),
  MQTT_DISCONNECT_CLOSE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Closing MQTT Session"),
  MQTT_PING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "MQTT Ping Request received"),
  MQTT_PUBLISH_EXCEPTION(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Close raised an exception "),
  MQTT_PUBLISH_STORE_FAILED(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to store message, exception thrown, need to close End Point, [MQTT-3.3.5-2]"),
  MQTT_START(LEVEL.DEBUG, CATEGORY.PROTOCOL, "MQTT protocol instance started"),
  MQTT_FAILED_CLEANUP(LEVEL.DEBUG, CATEGORY.PROTOCOL, "failed to close EndPoint while cleaning up session"),
  MQTT_ALREADY_CLOSED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Potentially already closed"),
  MQTT_KEEPALIVE_TIMOUT(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Checking for keepalive timeout period of {}"),
  MQTT_DISCONNECT_TIMEOUT(LEVEL.INFO, CATEGORY.PROTOCOL, "Disconnecting session since keep alive period has expired with no frames received"),
  MQTT_BUFFER_SIZE_FAILED(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set maximum buffer size, is not an integer::{}, using default of {}"),
  // </editor-fold>

  // <editor-fold desc="MQTT 5.0 log messages">
  MQTT5_CONNECTION_FAILED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Failed connection, {}"),
  MQTT5_SECOND_CONNECT(LEVEL.WARN, CATEGORY.PROTOCOL, "[MQTT-3.1.0-2] Received a second CONNECT packet"),
  MQTT5_UNHANDLED_PROPERTY(LEVEL.WARN, CATEGORY.PROTOCOL, "Unhandled Message Property :: {}"),
  MQTT5_FAILED_CONSTRUCTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to construct a session for {}"),
  MQTT5_INVALID_USERNAME_PASSWORD(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to send status to client"),
  MQTT5_EXCEED_MAXIMUM(LEVEL.WARN, CATEGORY.PROTOCOL, "Client exceeded outstanding events, currently {}, configured for {}"),
  MQTT5_DISCONNECT_REASON(LEVEL.WARN, CATEGORY.PROTOCOL, "Disconnect reason:{}"),
  MQTT5_DISCONNECTING_SESSION(LEVEL.WARN, CATEGORY.PROTOCOL, "Closing session due to disconnect packet:: {}"),
  MQTT5_PING_RECEIVED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "MQTTv5 Ping Request received"),
  MQTT5_DUPLICATE_PROPERTIES_DETECTED(LEVEL.WARN, CATEGORY.PROTOCOL, "Duplicate MQTT 5 properties detected in, this is a protocol error, duplicate ids [ {} ]"),
  MQTT5_HANDLE_EVENT_IO_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "An IOException was raised during the processing of a MQTT5 frame"),
  MQTT5_INITIALISATION(LEVEL.DEBUG, CATEGORY.PROTOCOL, "MQTTv5 protocol instance started"),
  MQTT5_KEEP_ALIVE_CHECK(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Checking for keepalive timeout period of {}"),
  MQTT5_KEEP_ALIVE_DISCONNECT(LEVEL.INFO, CATEGORY.PROTOCOL, "Disconnecting session since keep alive period has expired with no frames received"),
  MQTT5_MAXIMUM_SERVER_RECEIVE_FAIL(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set Server Receive Maximum, is not an integer, using default of 65535"),
  MQTT5_MAXIMUM_CLIENT_RECEIVE_FAIL(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set Client Receive Maximum, is not an integer, using default of 65535"),
  MQTT5_CLIENT_TOPIC_ALIAS(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set Client maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_SERVER_TOPIC_ALIAS(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set Server maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_TOPIC_ALIAS_PARSE_ERROR(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to set Server maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_MAX_BUFFER_EXCEEDED(LEVEL.WARN, CATEGORY.PROTOCOL, "Max Buffer Size:{} prohibits delivery of large events:{}"),
  MQTT5_TOPIC_ALIAS_ADD(LEVEL.INFO, CATEGORY.PROTOCOL, "Creating Topic Alias for {} as {}"),
  MQTT5_TOPIC_ALIAS_SET_MAXIMUM(LEVEL.INFO, CATEGORY.PROTOCOL, "Setting {} Topic Alias to {}"),
  MQTT5_TOPIC_ALIAS_EXCEEDED_MAXIMUM(LEVEL.ERROR, CATEGORY.PROTOCOL, "Exceeded maximum number of alias"),
  MQTT5_TOPIC_ALIAS_INVALID_VALUE(LEVEL.ERROR, CATEGORY.PROTOCOL, "Invalid value supplied for alias, received {}"),
  MQTT5_TOPIC_ALIAS_ALREADY_EXISTS(LEVEL.ERROR, CATEGORY.PROTOCOL, "Alias already exists for {}"),
  // </editor-fold>

  // <editor-fold desc="MQTT-SN log messages">
  MQTT_SN_INSTANCE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Protocol instance started"),
  MQTT_SN_CLOSE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Failed to close EndPoint while cleaning up session"),
  MQTT_SN_ALREADY_CLOSED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Potentially already closed"),
  MQTT_SN_NON_UDP(LEVEL.WARN, CATEGORY.PROTOCOL, "Bound to a non UDP based End Point, this will not work"),
  MQTT_SN_ADVERTISER_SENT_PACKET(LEVEL.TRACE, CATEGORY.PROTOCOL, "Sent advertise packet {}"),
  MQTT_SN_ADVERTISE_PACKET_EXCEPTION(LEVEL.INFO, CATEGORY.PROTOCOL, "An exception occurred while send an advertise packet"),
  MQTT_SN_PACKET_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised processing frame {}"),
  MQTT_SN_GATEWAY_DETECTED(LEVEL.WARN, CATEGORY.PROTOCOL, "Detected MQTT-SN service advertise packet for Gateway Id {}, from {}" ),
  MQTT_SN_REGISTERED_EVENT(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Registered Event processed for {}"),
  MQTT_SN_REGISTERED_EVENT_NOT_FOUND(LEVEL.WARN, CATEGORY.PROTOCOL, "Registered Event packet detected but no configuration found for host:{} topic Id:{}"),
  MQTT_SN_INVALID_QOS_PACKET_DETECTED(LEVEL.WARN, CATEGORY.PROTOCOL, "Publish packet received from {}, but incorrect QoS should be 3 but found {}"),
  // </editor-fold>

  // <editor-fold desc="MQTT-SN log messages">
  LOOP_CREATED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "loop protocol connection created"),
  LOOP_CLOSED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "loop protocol connection closed"),
  LOOP_SUBSCRIBED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "loop protocol subscribing to {} and delivering to {}"),
  LOOP_SENT_MESSAGE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Delivered message via loop"),
  LOOP_SEND_MESSAGE_FAILED(LEVEL.ERROR, CATEGORY.PROTOCOL, "Delivery of message via loop failed"),
  LOOP_SEND_CONNECT_FAILED(LEVEL.ERROR, CATEGORY.PROTOCOL, "Authentication failed"),
  // </editor-fold>

  // <editor-fold desc="Protocol detection log messages">
  PROTOCOL_ACCEPT_REGISTER(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Protocol Accept registered"),
  PROTOCOL_ACCEPT_SELECTOR_FIRED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Selector callback fired bytes"),
  PROTOCOL_ACCEPT_FIRING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Selector callback fired Starting at Packet Pos:{} limit:{}"),
  PROTOCOL_ACCEPT_FIRED(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Selector callback fired and read {} bytes, Packet Pos:{} limit:{}"),
  PROTOCOL_ACCEPT_SCANNING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Scanning packet :{}"),
  PROTOCOL_ACCEPT_COMPLETE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Selector callback completed"),
  PROTOCOL_ACCEPT_EXCEPTION(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Exception raised during close of end point"),
  PROTOCOL_ACCEPT_FAILED_DETECT(LEVEL.INFO, CATEGORY.PROTOCOL, "Failed to detect protocol on End Point {}"),
  PROTOCOL_ACCEPT_CREATED(LEVEL.WARN, CATEGORY.PROTOCOL, "Created Protocol {}"),
  PROTOCOL_ACCEPT_CLOSED(LEVEL.WARN, CATEGORY.PROTOCOL, "EndPoint closed during protocol negotiation"),
  // </editor-fold>

  // <editor-fold desc="JMX specific log messages">
  JMX_MANAGER_REGISTER(LEVEL.DEBUG, CATEGORY.ENGINE, "Registering MBean with [name={}]"),
  JMX_MANAGER_UNREGISTER(LEVEL.DEBUG, CATEGORY.ENGINE, "Unregistering MBean with [name={}]"),
  JMX_MANAGER_REGISTER_FAIL(LEVEL.WARN, CATEGORY.ENGINE, "Unable to register MBean [name={}] "),
  JMX_MANAGER_UNREGISTER_FAIL(LEVEL.WARN, CATEGORY.ENGINE, "Unable to unregister MBean [name={}]"),
  // </editor-fold>

  // <editor-fold desc="Configuration log messages">
  PROPERTY_MANAGER_START(LEVEL.DEBUG, CATEGORY.ENGINE, "Starting Property Manager"),
  PROPERTY_MANAGER_FOUND(LEVEL.DEBUG, CATEGORY.ENGINE, "Found and loaded property {}"),
  PROPERTY_MANAGER_SCANNING(LEVEL.DEBUG, CATEGORY.ENGINE, "Scanning property with {} entries"),
  PROPERTY_MANAGER_INDEX_DETECTED(LEVEL.DEBUG, CATEGORY.ENGINE, "Detected an indexed property file, parsing into different properties"),
  PROPERTY_MANAGER_COMPLETED_INDEX(LEVEL.DEBUG, CATEGORY.ENGINE, "Completed indexed property with {} for index {}"),
  PROPERTY_MANAGER_SCAN_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to scan for property files"),
  PROPERTY_MANAGER_LOAD_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Failed to load property {}"),
  DESTINATION_MANAGER_RELOADED(LEVEL.INFO, CATEGORY.ENGINE, "Reloaded {} out of {}"),
  // </editor-fold>

  //<editor-fold desc="Destination Subscription log messages">
  DESTINATION_SUBSCRIPTION_PUT(LEVEL.INFO, CATEGORY.ENGINE, "Adding subscription {} to destination {} for session {}"),
  DESTINATION_SUBSCRIPTION_HIBERNATE(LEVEL.INFO, CATEGORY.ENGINE, "Hibernating destination subscription {} with session id {}"),
  DESTINATION_SUBSCRIPTION_WAKEUP(LEVEL.INFO, CATEGORY.ENGINE, "Restoring destination subscription {} with session id {}"),
  DESTINATION_SUBSCRIPTION_SCHEDULED(LEVEL.INFO, CATEGORY.ENGINE, "Scheduled task for subscription {} to send message to {}"),
  DESTINATION_SUBSCRIPTION_TASK_FAILURE(LEVEL.ERROR, CATEGORY.ENGINE, "Send message task for subscription {} failed to send message to {}"),
  DESTINATION_SUBSCRIPTION_SEND(LEVEL.INFO, CATEGORY.ENGINE, "Sending message:{} to {} for subscription {}"),
  DESTINATION_SUBSCRIPTION_ACK(LEVEL.INFO, CATEGORY.ENGINE, "Received ack for message:{} on subscription {} for {}"),
  DESTINATION_SUBSCRIPTION_ROLLBACK(LEVEL.INFO, CATEGORY.ENGINE, "Received rollback for message:{} on subscription {} for {}"),
  DESTINATION_SUBSCRIPTION_EXCEPTION_ON_CLOSE(LEVEL.WARN, CATEGORY.ENGINE, "Exception raised during close"),
  DESTINATION_SUBSCRIPTION_EXCEPTION_SELECTOR(LEVEL.WARN, CATEGORY.ENGINE, "Exception raised while processing messaging selector {}"),
  //</editor-fold>

  //<editor-fold desc="Message State Manager log messages">
  MESSAGE_STATE_MANAGER_REGISTER(LEVEL.INFO, CATEGORY.ENGINE, "{} Registering message:{} "),
  MESSAGE_STATE_MANAGER_ALLOCATE(LEVEL.INFO, CATEGORY.ENGINE, "{} Allocating message:{} "),
  MESSAGE_STATE_MANAGER_COMMIT(LEVEL.INFO, CATEGORY.ENGINE, "{} Committing message:{} "),
  MESSAGE_STATE_MANAGER_ROLLBACK(LEVEL.INFO, CATEGORY.ENGINE, "{} Rollback message:{} "),
  MESSAGE_STATE_MANAGER_NEXT(LEVEL.INFO, CATEGORY.ENGINE, "{} Scanning for next message returning:{} "),
  MESSAGE_STATE_MANAGER_ROLLBACK_INFLIGHT(LEVEL.INFO, CATEGORY.ENGINE, "{} Rolling back all in flight messages {}"),
  MESSAGE_STATE_MANAGER_ROLLED_BACK_INFLIGHT(LEVEL.INFO, CATEGORY.ENGINE, "{} Rolled back all in flight messages"),
  //</editor-fold>

  //<editor-fold desc="Destination Manager log messages">
  DESTINATION_MANAGER_ADD_SYSTEM_TOPIC(LEVEL.INFO, CATEGORY.ENGINE, "Added new topic {}"),
  DESTINATION_MANAGER_USER_SYSTEM_TOPIC(LEVEL.INFO, CATEGORY.ENGINE, "User attempted to create a system topic, {}, this is prohibited"),
  DESTINATION_MANAGER_CREATED_TOPIC(LEVEL.INFO, CATEGORY.ENGINE, "New topic created {}"),
  DESTINATION_MANAGER_DELETED_TOPIC(LEVEL.INFO, CATEGORY.ENGINE, "Topic, {}, hasbeen deleted"),
  DESTINATION_MANAGER_STARTING(LEVEL.INFO, CATEGORY.ENGINE, "Destination Manager starting"),
  DESTINATION_MANAGER_STARTED_TOPIC(LEVEL.INFO, CATEGORY.ENGINE, "Reloaded {}"),
  DESTINATION_MANAGER_EXCEPTION_ON_START(LEVEL.ERROR, CATEGORY.ENGINE, "Exception raised during Destination Manager startup"),
  DESTINATION_MANAGER_STOPPING(LEVEL.INFO, CATEGORY.ENGINE, "Shut down started"),
  DESTINATION_MANAGER_CLEARING(LEVEL.INFO, CATEGORY.ENGINE, "Clear session id {} requested"),
  DESTINATION_MANAGER_DELETING_TEMPORARY_DESTINATION(LEVEL.INFO, CATEGORY.ENGINE, "\"Reloaded temp destination {}, now deleting"),
  //</editor-fold>

  //<editor-fold desc="Serial Port Server log Messages">
  SERIAL_SERVER_CREATE_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to construct a Serial End Point"),
  SERIAL_SERVER_BIND_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Failed to bind Serial Port with End Point"),
  //</editor-fold>

  //<editor-fold desc="Serial Port Scanner log messages">
  SERIAL_PORT_SCANNER_SCAN_FAILED(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised while scanning for Serial Port changes"),
  SERIAL_PORT_SCANNER_BINDING(LEVEL.WARN, CATEGORY.PROTOCOL, "Binding server {} to serial port {}"),
  SERIAL_PORT_SCANNER_UNBINDING(LEVEL.WARN, CATEGORY.PROTOCOL, "Unbinding server {} from serial port {}"),
  SERIAL_PORT_SCANNER_UNUSED(LEVEL.WARN, CATEGORY.PROTOCOL, "Found serial port {} but no bound servers"),
  SERIAL_PORT_SCANNER_LOST(LEVEL.WARN, CATEGORY.PROTOCOL, "Serial port {} has been disconnected"),
  //</editor-fold>

  //<editor-fold desc="LoRa gateway log messages">
  LORA_GATEWAY_SUCCESS(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Gateway command executed successfully <{}>"),
  LORA_GATEWAY_FAILURE(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Gateway command failed to executed <{}>"),
  LORA_GATEWAY_PING(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Gateway Ping received, Radio state is on = {} "),
  LORA_GATEWAY_UNEXPECTED(LEVEL.WARN, CATEGORY.PROTOCOL, "Unknown command <{}> received from LoRa gateway "),
  LORA_GATEWAY_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised during the processing of the LoRa Gateway request"),
  LORA_GATEWAY_EXCEED_RATE(LEVEL.WARN, CATEGORY.PROTOCOL, "Exceeded the configured transmission rate of {}"),
  LORA_GATEWAY_LOG(LEVEL.DEBUG, CATEGORY.PROTOCOL, "Gateway Log: {} "),
  LORA_GATEWAY_FRAMING_ERROR(LEVEL.WARN, CATEGORY.PROTOCOL, "Stream Framing error detected {}"),
  LORA_GATEWAY_INVALID_COMMAND(LEVEL.WARN, CATEGORY.PROTOCOL, "Invalid command <{}> received"),
  //</editor-fold>

  //<editor-fold desc="LoRa Device log messages">
  LORA_DEVICE_LIBRARY_NOT_LOADED(LEVEL.WARN, CATEGORY.NETWORK, "LoRa device library failed to load {}"),
  LORA_DEVICE_NOT_INITIALISED(LEVEL.WARN, CATEGORY.PROTOCOL, "LoRa device for {} on device {} not yet initialised"),
  LORA_DEVICE_INIT_FAILED(LEVEL.WARN, CATEGORY.PROTOCOL, "LoRa device for {} on device {} failed to during initialised"),
  LORA_DEVICE_REGISTERED(LEVEL.INFO, CATEGORY.PROTOCOL, "Registering endPoint {} on {}"),
  LORA_DEVICE_DRIVER_LOG(LEVEL.INFO, CATEGORY.PROTOCOL, "Driver message for {} on {} received, {}"),
  LORA_DEVICE_READ_THREAD_ERROR(LEVEL.ERROR, CATEGORY.PROTOCOL, "The LoRa read thread on {} failed"),
  //</editor-fold>

  //<editor-fold desc="File operation log messages">
  FILE_RELOAD_PERCENT(LEVEL.TRACE, CATEGORY.ENGINE, " File reload currently at {}"),
  FILE_FAILED_TO_CLOSE(LEVEL.WARN, CATEGORY.ENGINE, "File close raised exception"),
  FILE_FAILED_TO_DELETE(LEVEL.WARN, CATEGORY.ENGINE, "File delete raised exception"),
  //</editor-fold>

  JOLOKIA_SHUTDOWN_FAILURE(LEVEL.ERROR, CATEGORY.ENGINE , "Jolokia failed to shutdown the HTTP server"),
  JOLOKIA_STARTUP_FAILURE(LEVEL.ERROR, CATEGORY.ENGINE , "Jolokia failed to load the HTTP server"),

  //<editor-fold desc="AMQP Log messages">
  AMQP_REMOTE_CLIENT_PROPERTIES(LEVEL.INFO, CATEGORY.PROTOCOL, "Remote AMQP client property {} - {}"),
  AMQP_RECEIVED_EVENT(LEVEL.INFO, CATEGORY.PROTOCOL, "Received event {}"),
  AMQP_DETECTED_JMS_CLIENT(LEVEL.INFO, CATEGORY.PROTOCOL, "Detected remote client is a JMS client"),
  AMQP_CREATED_SESSION(LEVEL.INFO, CATEGORY.PROTOCOL , "Session {} created"),
  AMQP_CLOSED_SESSION(LEVEL.INFO, CATEGORY.PROTOCOL , "Session {} closed"),
  AMQP_CREATED_SUBSCRIPTION(LEVEL.INFO, CATEGORY.PROTOCOL , "Created subscription on {} with alias {}" ),
  AMQP_DELETED_SUBSCRIPTION(LEVEL.INFO, CATEGORY.PROTOCOL , "Deleted subscription with alias {}" ),
  AMQP_ENGINE_TRANSPORT_EXCEPTION(LEVEL.WARN, CATEGORY.PROTOCOL, "Exception raised on Proton Engine Transport, {}"),
  AMQP_REMOTE_LINK_ERROR(LEVEL.WARN, CATEGORY.PROTOCOL, "Remote link closed with error message {}"),
  //</editor-fold>

  //<editor-fold desc="Transaction Manager log messages">
  TRANSACTION_MANAGER_SCANNING(LEVEL.TRACE,CATEGORY.ENGINE , "Transaction Manager expiry scan started"),
  TRANSACTION_MANAGER_CLOSE_FAILED(LEVEL.WARN, CATEGORY.ENGINE, "Transaction Manager detected exception when closing transaction id:{}"),
  TRANSACTION_MANAGER_TIMEOUT_DETECTED(LEVEL.INFO, CATEGORY.ENGINE, "Transaction Manager detected expired transaction id:{}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL agent logging">
  CONSUL_STARTUP(LEVEL.DEBUG, CATEGORY.ENGINE, "Agent startup"),
  CONSUL_SHUTDOWN(LEVEL.DEBUG, CATEGORY.ENGINE, "Agent shutdown"),
  CONSUL_REGISTER(LEVEL.DEBUG, CATEGORY.ENGINE, "Registering with local agent"),
  CONSUL_PING_EXCEPTION(LEVEL.DEBUG, CATEGORY.ENGINE, "Ping failed with exception {}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL management log messages">
  CONSUL_MANAGER_START(LEVEL.DEBUG, CATEGORY.ENGINE, "Manager starting up for id {}"),
  CONSUL_MANAGER_STOP(LEVEL.DEBUG, CATEGORY.ENGINE, "Manager shutting down"),
  CONSUL_MANAGER_START_ABORTED(LEVEL.ERROR, CATEGORY.ENGINE, "Startup aborted due to configuration, id {}"),
  CONSUL_MANAGER_START_DELAYED(LEVEL.ERROR, CATEGORY.ENGINE, "Startup delaying server startup due to configuration for id {}"),
  //</editor-fold>


  //<editor-fold desc="CONSUL Key/Value management log messages">
  CONSUL_PROPERTY_MANAGER_NO_KEY_VALUES(LEVEL.ERROR, CATEGORY.ENGINE, "No keys found in Consul Key/Value for id {}"),
  CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION(LEVEL.ERROR, CATEGORY.ENGINE, "Key {}, lookup failed with exception"),
  CONSUL_PROPERTY_MANAGER_INVALID_JSON(LEVEL.ERROR, CATEGORY.ENGINE, "Value returned is not valid json for key {}"),
  CONSUL_PROPERTY_MANAGER_SAVE_ALL(LEVEL.ERROR, CATEGORY.ENGINE, "Saving all entries for {}"),
  CONSUL_PROPERTY_MANAGER_STORE(LEVEL.ERROR, CATEGORY.ENGINE, "Storing entry for {}"),
  //</editor-fold>

  //<editor-fold desc="NameSpace mapping used to support multi tenancy">
  NAMESPACE_MAPPING(LEVEL.INFO, CATEGORY.ENGINE,"Mapping {} to namespace {}"),
  NAMESPACE_MAPPING_FOUND(LEVEL.INFO, CATEGORY.ENGINE,    "Found entry for  {} mapping to {}"),
  NAMESPACE_MAPPING_DEFAULT(LEVEL.INFO, CATEGORY.ENGINE,    "Using default mapping {}"),
  //</editor-fold>

  //<editor-fold desc="End Point Connection Management">
  END_POINT_CONNECTION_STARTING(LEVEL.INFO, CATEGORY.NETWORK, "Starting connection manager"),
  END_POINT_CONNECTION_SUBSCRIPTION_FAILED(LEVEL.WARN, CATEGORY.NETWORK, "Failed to establish a {} subscription in between {} and {} "),
  END_POINT_CONNECTION_CLOSE_EXCEPTION(LEVEL.INFO, CATEGORY.NETWORK, "Exception raised while closing end point"),
  END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED(LEVEL.INFO, CATEGORY.NETWORK, "Established a {} subscription between {} and {}"),
  END_POINT_CONNECTION_PROTOCOL_FAILED(LEVEL.WARN, CATEGORY.NETWORK, "Exception raised while establishing protocol between {} and protocol {}"),
  END_POINT_CONNECTION_FAILED(LEVEL.WARN, CATEGORY.NETWORK, "Exception raised while connecting to remote server {}"),
  END_POINT_CONNECTION_INITIALISED(LEVEL.INFO, CATEGORY.NETWORK, "Initialised connection"),
  END_POINT_CONNECTION_CLOSED(LEVEL.INFO, CATEGORY.NETWORK, "Closing connection"),
  END_POINT_CONNECTION_STATE_CHANGED(LEVEL.INFO, CATEGORY.NETWORK, "Changing state from {} to {}"),
  END_POINT_CONNECTION_STOPPING(LEVEL.INFO, CATEGORY.NETWORK, "Stopping connection manager"),
  //</editor-fold>

  ;


  public final String message;
  public final LEVEL level;
  public final CATEGORY category;
  public final int parameters;

  LogMessages(LEVEL level, CATEGORY category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameters = count;
  }

  public enum LEVEL {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }

  public enum CATEGORY {
    TEST("Test"),
    AUTHORISATION("Authorisation"),
    AUTHENTICATION("Authentication"),
    NETWORK("Network"),
    PROTOCOL("Protocol"),
    ENGINE("Engine");

    public final String description;

    CATEGORY(String description) {
      this.description = description;
    }
  }
}
