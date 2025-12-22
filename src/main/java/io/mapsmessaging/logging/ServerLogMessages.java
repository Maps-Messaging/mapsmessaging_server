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

package io.mapsmessaging.logging;

import lombok.Getter;

/**
 * This enum contains all log messages and the configuration around the log message. This enables log messages to be modified in one place without searching for the log message it
 * also enables the log messages to be translated into other languages if required
 */

public enum ServerLogMessages implements LogMessage {

  //-------------------------------------------------------------------------------------------------------------
  // <editor-fold desc="File Lock Management">
  LOCKFILE_STALE_HEARTBEAT(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Heartbeat is stale, attempting forced takeover"),
  LOCKFILE_DELETED(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Lock file deleted"),
  LOCKFILE_STOP_DETECTED(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Stop signal detected. Shutting down"),
  // </editor-fold>

  // <editor-fold desc="Generic messages">
  PUSH_WRITE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Pushed Packet for write, {}"),
  RECEIVE_PACKET(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Received Packet:{}"),
  RESPONSE_PACKET(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Responding Packet:{}"),
  MALFORMED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Malformed Exception raised {}"),
  END_POINT_CLOSE_EXCEPTION(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "An exception was raised during the close of an end point"),
  SESSION_CLOSE_EXCEPTION(LEVEL.TRACE, SERVER_CATEGORY.ENGINE, "An exception was raised during the of a session"),
  TRANSACTION_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "An exception was raised processing a transaction [ {} ]"),
  // </editor-fold>

  // <editor-fold desc="Main Message Daemon messages">
  MESSAGE_DAEMON_STARTUP(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "Starting Messaging Daemon Version:{} Build Date:{}"),
  MESSAGE_DAEMON_STARTUP_BOOTSTRAP(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "Messaging Daemon Unique Id has been assigned to {}"),
  MESSAGE_DAEMON_WAIT_PREVIOUS_INSTANCE(LEVEL.FATAL, SERVER_CATEGORY.DAEMON, "{}"),
  MAP_ENV_HOME_RESOLVED(LEVEL.INFO, SERVER_CATEGORY.DAEMON, "MAPS_HOME resolved to {} via {}"),
  MAP_ENV_DATA_RESOLVED(LEVEL.INFO, SERVER_CATEGORY.DAEMON, "MAPS_DATA resolved to {} for OS {}"),

  MESSAGE_DAEMON_NO_HOME_DIRECTORY(LEVEL.ERROR, SERVER_CATEGORY.DAEMON, "The supplied home directory, {}, does not exist"),
  MESSAGE_DAEMON_HOME_DIRECTORY(LEVEL.ERROR, SERVER_CATEGORY.DAEMON, "The home directory has been defined as {}"),
  MESSAGE_DAEMON_SERVICE(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "\t\tLoaded service {}, {}"),
  MESSAGE_DAEMON_SERVICE_LOADED(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "Service Manager {} loaded"),
  MESSAGE_DAEMON_PROTOCOL_NOT_AVAILABLE(LEVEL.ERROR, SERVER_CATEGORY.DAEMON, "Protocol not available, see stack trace for more details"),

  MESSAGE_DAEMON_AGENT_STARTING(LEVEL.AUDIT, SERVER_CATEGORY.DAEMON, "Starting {} "),
  MESSAGE_DAEMON_AGENT_STARTED(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "Started {} took {}ms"),

  MESSAGE_DAEMON_AGENT_STOPPING(LEVEL.AUDIT, SERVER_CATEGORY.DAEMON, "Stopping {} "),
  MESSAGE_DAEMON_AGENT_STOPPED(LEVEL.WARN, SERVER_CATEGORY.DAEMON, "Stopped {} took {}ms"),
  // </editor-fold>

  // <editor-fold desc="routing manager messages">
  ROUTING_STARTUP(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Starting Event Routing Manager"),
  ROUTING_SHUTDOWN(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Stopping Event Routing Manager"),
  // </editor-fold>

  // <editor-fold desc="Network Manager log messages">
  NETWORK_MANAGER_STARTUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Starting Network Manager"),
  NETWORK_MANAGER_LOAD_PROPERTIES(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Loading Network Manager Properties"),
  NETWORK_MANAGER_STARTUP_COMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Completed startup Network Manager"),
  NETWORK_MANAGER_START_ALL(LEVEL.AUDIT, SERVER_CATEGORY.ENGINE, "Starting all network interfaces"),
  NETWORK_MANAGER_STOP_ALL(LEVEL.AUDIT, SERVER_CATEGORY.ENGINE, "Stopping all network interfaces"),
  NETWORK_MANAGER_PAUSE_ALL(LEVEL.AUDIT, SERVER_CATEGORY.ENGINE, "Pausing all network interfaces"),
  NETWORK_MANAGER_RESUME_ALL(LEVEL.AUDIT, SERVER_CATEGORY.ENGINE, "Resuming all network interfaces"),
  NETWORK_MANAGER_START_FAILURE(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Unable to start {} due to the following exception"),
  NETWORK_MANAGER_START_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to start interface {}"),
  NETWORK_MANAGER_STOP_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to stop interface {}"),
  NETWORK_MANAGER_PAUSE_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to pause interface {}"),
  NETWORK_MANAGER_RESUME_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to resume interface {}"),
  NETWORK_MANAGER_DEVICE_NOT_LOADED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Device configured, {}, can not be used since it is not loaded"),

  // </editor-fold>

  // <editor-fold desc=" End Point Manager log messages">
  END_POINT_MANAGER_SELECTOR_START(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Creating network Selector thread"),
  END_POINT_MANAGER_START(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Start called on {}"),
  END_POINT_MANAGER_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Close called on {}"),
  END_POINT_MANAGER_PAUSE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Pause called on {}"),
  END_POINT_MANAGER_RESUME(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Resume called on {}"),
  END_POINT_MANAGER_CLOSE_SERVER(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Closing end point on closed server"),
  END_POINT_MANAGER_NEW_SELECTOR(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Created thread safe selector implementation due to the JDK support on interface"),
  END_POINT_MANAGER_ACCEPT_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Exception raised during accept handling of the new connection"),
  END_POINT_MANAGER_CLOSE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Closing end point on accept server raised exception"),
  END_POINT_MANAGER_CLOSE_EXCEPTION_1(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Closing end point on closed server raised exception"),
  // </editor-fold>

  //<editor-fold desc="End Point Connection Management">
  END_POINT_CONNECTION_STARTING(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Starting connection manager"),
  END_POINT_CONNECTION_SUBSCRIPTION_FAILED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Failed to establish a {} subscription in between {} and {} "),
  END_POINT_CONNECTION_CLOSE_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Exception raised while closing end point"),
  END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Established a {} subscription between {} and {}"),
  END_POINT_CONNECTION_PROTOCOL_FAILED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Exception raised while establishing protocol between {} and protocol {}"),
  END_POINT_CONNECTION_FAILED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Exception raised while connecting to remote server {}"),
  END_POINT_CONNECTION_INITIALISED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Initialised connection"),
  END_POINT_CONNECTION_CLOSED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Closing connection"),
  END_POINT_CONNECTION_STATE_CHANGED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Changing state on url {} protocol {} from {} to {}"),
  END_POINT_CONNECTION_STOPPING(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Stopping connection manager"),

  //</editor-fold>

  // <editor-fold desc="Selector and Selector task log messages">
  SELECTOR_OPEN(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Opening Selector"),
  SELECTOR_FAILED_ON_CALL(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Selector failed on select call, exiting select thread"),
  SELECTOR_FIRED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "selection key is fired {}"),
  SELECTOR_CONNECTION_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Connection has been closed"),
  SELECTOR_TASK_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Failed processing selection task {}"),
  SELECTOR_TASK_FAILED_1(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Failed processing selection task {}"),
  SELECTOR_NEW_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Creating new Selector task"),
  SELECTOR_CLOSE_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Closed Selector task"),
  SELECTOR_PUSH_WRITE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Pushed {} for write, Depth:{}"),
  SELECTOR_REGISTERING(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Registering {}"),
  SELECTOR_REGISTER_RESULT(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Registered resulted in {}"),
  SELECTOR_REGISTER_CLOSED_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Registering on closed task {}"),
  SELECTOR_CANCELLING(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Cancelling {} replacing with {}"),
  SELECTOR_CANCEL_FAILED(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Exception raised during cancelling of the selector"),
  SELECTOR_CALLED_BACK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Selected called back for {}"),
  SELECTOR_READ_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Selected calling Read task"),
  SELECTOR_WRITE_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Selected calling Write task"),
  SELECTOR_SPIN_DETECTED(LEVEL.ERROR,  SERVER_CATEGORY.NETWORK, "Selector thread hit empty selector threshold"),
  SELECTOR_REBUILT(LEVEL.ERROR,  SERVER_CATEGORY.NETWORK, "Selector has been rebuilt due to epoll issue"),
  SELECTOR_REBUILD_FAILED(LEVEL.ERROR,  SERVER_CATEGORY.NETWORK, "Selector has failed to be rebuilt due to attached exception"),
  // </editor-fold>

  // <editor-fold desc="Read Task log messages">
  READ_TASK_EXCEPTION(LEVEL.ERROR, SERVER_CATEGORY.NETWORK, "Runtime exception raised during packet handling in protocol"),
  READ_TASK_COMPLETED(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Completed read at Pos:{} Limit:{} Response:{}"),
  READ_TASK_ZERO_BYTE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Zero bytes read from end point Position:{} Limit:{} Capacity:{}"),
  READ_TASK_PACKET_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Packet handling raised an exception, closing end point"),
  READ_TASK_READ_PROCESSING(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Read processing Packet with {} bytes, {}"),
  READ_TASK_POST_PROCESSING(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Packet Post Process:{}"),
  READ_TASK_COMPACT(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Compact for next write {}"),
  READ_TASK_POSITION(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Set the position for next write {}"),
  READ_TASK_NEGATIVE_CLOSE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Negative value returned on read, closing Protocol"),
  // </editor-fold>

  // <editor-fold desc="Write Task log messages">
  WRITE_TASK_RESUMING(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Resuming buffer write Pos:{} Limit:{}"),
  WRITE_TASK_COMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Buffer Completed Write"),
  WRITE_TASK_INCOMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Buffer incomplete write Pos:{} Limit:{}"),
  WRITE_TASK_WRITE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Registered Write"),
  WRITE_TASK_WRITE_CANCEL(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Cancelled Write"),
  WRITE_TASK_WRITE_PACKET(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Write processing Packet {}"),
  WRITE_TASK_BLOCKED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "End Point blocked exiting write and registering with selector"),
  WRITE_TASK_CLOSE_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "failed to close EndPoint during exception handling"),
  WRITE_TASK_UNABLE_TO_ADD_WRITE(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Unable to add write handle to selector"),
  WRITE_TASK_SEND_FAILED(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Failed to send packet"),
  WRITE_TASK_BUFFERSIZE(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Failed to parse write buffer size"),
  // </editor-fold>

  // <editor-fold desc="TCP/IP EndPoint log messages">
  TCP_ACCEPT_START(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Accepting socket {}"),
  TCP_CONNECT_FAILED(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Failed to accept socket {}"),
  TCP_CONNECTION_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Closed socket end point to host {}"),
  TCP_CLOSE_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Exception raised during close {}"),
  TCP_CLOSE_SUCCESS(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Closed socket end point to host {}"),
  TCP_SEND_BUFFER(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Sent {} bytes to socket"),
  TCP_READ_BUFFER(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Read {} bytes from socket"),
  TCP_CLOSE_ERROR(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Exception raised while closing the physical socket"),
  TCP_CONFIGURED_PARAMETER(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Setting socket parameter: {} to {} "),
  // </editor-fold>

  //<editor-fold desc="Local Loop End Point">
  NOOP_END_POINT_CREATE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Created a no-op end point {}"),
  NOOP_END_POINT_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Closed a no-op end point {}"),
  //</editor-fold>

  // <editor-fold desc="TCP/IP Server EndPoint log message">
  TCP_SERVER_ENDPOINT_CREATE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Creating Server Socket on port {} with backlog of {} on interface {}"),
  TCP_SERVER_ENDPOINT_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Closing Server Socket"),
  TCP_SERVER_ENDPOINT_REGISTER(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Registering selector"),
  TCP_SERVER_ENDPOINT_DEREGISTER(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Deregister selector"),
  TCP_SERVER_ENDPOINT_ACCEPT(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Accept failed with "),
  // </editor-fold>

  // <editor-fold desc="SSL End Point log messages">
  SSL_CREATE_ENGINE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Creating SSL engine and configuring for server mode"),
  SSL_ENCRYPTION_BUFFERS(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Setting up encryption buffers, size set to {}"),
  SSL_HANDSHAKE_START(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Creating SSL Handshake manager to establish valid SSL session"),
  SSL_HANDSHAKE_READY(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "SSL Engine ready to start handshaking"),
  SSL_SENT(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "SSL sent {} bytes"),
  SSL_READ(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "SSL read {} bytes"),
  SSL_SEND_ENCRYPTED(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "SSL sent {} encrypted bytes"),
  SSL_READ_ENCRYPTED(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "SSL read {} encrypted bytes, Position:{} Limit:{}"),
  SSL_ENGINE_RESULT(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "SSLEngine Result::{}"),
  SSL_ENGINE_CLIENT_AUTH(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "SSL Engine requires client authentication, however, the peer has not been verified"),
  // </editor-fold>

  // <editor-fold desc="SSL handshake log messages">
  SSL_HANDSHAKE_NEED_UNWRAP(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "SSL handshake state :: NEED UNWRAP"),
  SSL_HANDSHAKE_NEED_WRAP(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "SSL handshake state :: NEED WRAP"),
  SSL_HANDSHAKE_FINISHED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "SSL handshake state :: FINISHED"),
  SSL_HANDSHAKE_ENCRYPTED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Encrypted In Buffer Status : Position:{} Limit:{}"),
  SSL_HANDSHAKE_NEED_TASK(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "SSL handshake state :: NEED_TASK"),
  SSL_HANDSHAKE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "SSL handshake raised exception"),
  // </editor-fold>

  // <editor-fold desc="SSL Server EndPoint log messages">
  SSL_SERVER_START(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Starting to building SSL Context"),
  SSL_SERVER_INITIALISE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "InitialedKey Manager Factory of type {}"),
  SSL_SERVER_TRUST_MANAGER(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Initialised Trust Manager Factory of type {}"),
  SSL_SERVER_CONTEXT_CONSTRUCT(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Constructing SSL Context with the created key and trust stores"),
  SSL_SERVER_SSL_CONTEXT_COMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Completed construction of the SSL Context with the created key and trust stores"),
  SSL_SERVER_COMPLETED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Completed building SSL Context"),
  SSL_SERVER_LOAD_KEY_STORE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Loading Key Store {} of type {}"),
  SSL_SERVER_LOADED_KEY_STORE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Loaded Key Store {} of type {}"),
  SSL_SERVER_ACCEPT_FAILED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Accept failed"),
  // </editor-fold>

  // <editor-fold desc="UDP EndPoint log messages">
  UDP_CREATED(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Created new UDP EndPoint at {}"),
  UDP_SENT_BYTES(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Sent {} byte datagram"),
  UDP_READ_BYTES(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Received a {} byte datagram"),
  UDP_READ_TASK_READ_PACKET(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Processing packet {}"),
  UDP_WRITE_TASK_SENT_PACKET(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "UDP EndPoint sent packet {}"),
  UDP_WRITE_TASK_SEND_PACKET_ERROR(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Exception raised during packet send"),
  UDP_WRITE_TASK_UNABLE_TO_REMOVE_WRITE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Unable to remove WRITE interest from the selector"),

  UDP_READ_TASK_STATE(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Read packet from {} of {} bytes"),
  UDP_READ_TASK_STATE_PREVIOUS(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Previous packet from {} of {} bytes found"),
  UDP_READ_TASK_STATE_RECOMBINED(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Recombined packet from {} of {} bytes found"),
  UDP_READ_TASK_STATE_REMAINING(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Remaining data in packet from {} of {} bytes"),

  // </editor-fold>

  // <editor-fold desc="Security Manager based log messages">
  SECURITY_MANAGER_STARTUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Starting Security Manager"),
  SECURITY_MANAGER_FAILED_LOG_IN(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "User {} failed to logged in, {}"),
  SECURITY_MANAGER_FAILED_LOG_OFF(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "User {} failed to logged off, {}"),
  SECURITY_MANAGER_LOADING(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Loading Security Manager properties"),
  SECURITY_MANAGER_LOADED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Loaded Security Manager Properties"),
  SECURITY_MANAGER_SECURITY_CONTEXT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Created security context for {}"),
  SECURITY_MANAGER_FAILED_TO_CREATE_USER(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to create user {} during initial setup"),
  SECURITY_MANAGER_FAILED_TO_INITIALISE_USER(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to initialise user {} during initial setup"),

  // </editor-fold>

  // <editor-fold desc="Anonymous Login module log messages">
  ANON_LOGIN_MODULE_USERNAME(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] user entered user name: {}"),
  ANON_LOGIN_MODULE_PASSWORD(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] user entered password: {}"),
  ANON_LOGIN_MODULE_SUBJECT(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] added AnonymousPrincipal to Subject"),
  ANON_LOGIN_MODULE_LOG_OUT(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[AnonymousLoginModule] logged out Subject"),
  // </editor-fold>

  // <editor-fold desc="SSL Certificate security log messages">
  SSL_CERTIFICATE_SECURITY_USERNAME(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] user entered user name: {}"),
  SSL_CERTIFICATE_SECURITY_PASSWORD(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] user entered password: {}"),
  SSL_CERTIFICATE_SECURITY_SUBJECT_LOG_IN(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] added AnonymousPrincipal to Subject"),
  SSL_CERTIFICATE_SECURITY_SUBJECT_LOG_OUT(LEVEL.DEBUG, SERVER_CATEGORY.AUTHENTICATION, "\t\t[SSLCertificateLoginModule] logged out Subject"),
  // </editor-fold>

  // <editor-fold desc="Session Manager log messages">
  SESSION_MANAGER_STARTUP(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Session Manager is starting up"),
  SESSION_MANAGER_CREATE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Creating new Session with Context {}"),
  SESSION_MANAGER_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Closing Session {}"),
  SESSION_MANAGER_CREATE_SECURITY_CONTEXT(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Created Security Context"),
  SESSION_MANAGER_FOUND_CLOSED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Found and closed existing session that matched {}"),
  SESSION_MANAGER_KEEP_ALIVE_TASK(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Created new Keep Alive scheduler task"),
  SESSION_ERROR_DURING_CREATION(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Unexpected exception raised during session creation"),

  SESSION_MANAGER_WILL_TASK(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Build WillTask for {} WillTask:{}"),
  SESSION_MANAGER_LOADED_SUBSCRIPTION(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Loaded Subscription Manager {}, containing {}"),
  SESSION_MANAGER_NO_EXISTING(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "No existing Subscription manager found for {}"),
  SESSION_MANAGER_ADDING_SUBSCRIPTION(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Adding Subscription Manager for persistent usage {}"),
  SESSION_MANAGER_FOUND_EXISTING(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Found existing subscription manager {}, resetting due to reset state flag set{}"),
  SESSION_MANAGER_STOPPING(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Session Manager is shutting down"),
  SESSION_MANAGER_LOADING_SESSION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Starting session id {} with {} subscriptions"),
  SESSION_MANAGER_CLOSING_SESSION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Cleaning up session {}"),
  // </editor-fold>

  INSTANCE_STATE_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to load instance state data at {}"),

  //<editor-fold desc="Persistent session errors">
  SESSION_SAVE_STATE(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Saving state for {} at {}"),
  SESSION_SAVE_STATE_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to save state for {} at {}"),
  SESSION_LOAD_STATE_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to load state for file {}"),
  SESSION_INIT_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed create directory for session state at {}"),
  //</editor-fold>

  // <editor-fold desc="Will message processing log messages">
  WILL_TASK_SENDING(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Sending Will Message {}"),
  WILL_TASK_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Exception raised during sending of will message:{}"),
  // </editor-fold>

  // <editor-fold desc="Shared Subscription log messages">
  SHARED_SUBSCRIPTION_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to send message to subscription"),
  // </editor-fold>

  // <editor-fold desc="Wildcard log messages">
  WILDCARD_CONSTRUCTED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Constructed wildcard subscription for {} for {}"),
  WILDCARD_REMOVING_LAST(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Last subscription entry found, removing the physical subscription"),
  WILDCARD_CREATED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Created subscription for {} for {} since matches wildcard {}"),
  WILDCARD_HIBERNATED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Created hibernated subscription for {} since matches wildcard {}"),
  WILDCARD_DELETED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Deleted subscription for {} for {} since matches wildcard {}"),
  // </editor-fold>

  // <editor-fold desc="Subscription log messages">
  SUBSCRIPTION_MGR_OVERLAP_DETECTED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Detected overlapping subscriptions, creating overlapping structure to support it"),
  SUBSCRIPTION_MGR_EXISTING_ADD(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Already have multiple subscriptions on destination, simply add to the list"),
  SUBSCRIPTION_MGR_CANCELLING_SCHEDULER(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Cancelled running timeout scheduler"),
  SUBSCRIPTION_MGR_SCHEDULED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Scheduled timeout scheduler in {} seconds::{}"),
  SUBSCRIPTION_MGR_WAKING_SESSION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Waking Session for {}"),
  SUBSCRIPTION_MGR_TIME_CANCELLED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "marking timeout scheduler as cancelled {}"),
  SUBSCRIPTION_MGR_TIME_OUT_NULL(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Timeout Scheduler is NULL for {}"),
  SUBSCRIPTION_MGR_CANCELLED_TIMEOUT(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Cancelling timeout scheduler::{}"),
  SUBSCRIPTION_MGR_NO_TASK(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "No scheduled tasks for {}"),
  SUBSCRIPTION_MGR_DUPLICATE_SUBSCRIPTION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Duplicate subscriptions detected and overlapping subscriptions are not allowed for destination {}"),
  SUBSCRIPTION_MGR_FAILED_TO_SEND_RETAIN(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to send retained message to new subscription on {}"),
  SUBSCRIPTION_MGR_SESSION_TIMEOUT(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Session timeout reached for {} after {} seconds{}"),
  SUBSCRIPTION_MGR_RELOAD(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Loaded and registered subscription for session id {} for {}, Subscription {} out of {}"),
  SUBSCRIPTION_MGR_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Subscription failed to reload for session id {} for {}"),
  SUBSCRIPTION_MGR_CREATE_SUBSCRIPTION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Creating new subscription on {} with a name of {} using filter of {}"),
  SUBSCRIPTION_MGR_REMOVED(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Removing subscription {} to destination {}"),
  SUBSCRIPTION_MGR_CLEAR_SESSION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Clearing all subscriptions for session {}"),
  SUBSCRIPTION_MGR_CLOSE(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Closing Subscription Manager {}"),
  SUBSCRIPTION_MGR_CLOSED(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Closed Subscription Manager {}"),
  SUBSCRIPTION_MGR_HIBERNATE(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Hibernating subscription {}"),
  SUBSCRIPTION_MGR_CREATE_WILDCARD(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Creating wildcard subscription {} on {}"),
  SUBSCRIPTION_MGR_SELECTOR_EXCEPTION(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Exception raised compiling Selector {}"),
  SUBSCRIPTION_MGR_CLOSE_SUB_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Exception raised while closing subscriptions"),
  //</editor-fold>

  // <editor-fold desc="Echo Protocol log messages">
  ECHO_CLOSED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "EndPoint closed"),
  ECHO_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised during selected function"),
  ECHO_CLOSE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "EndPoint was closing but raised an exception"),
  // </editor-fold>

  // <editor-fold desc="Stomp Protocol log messages">
  STOMP_STATE_ENGINE_FAILED_COMPLETION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed on frame completion callback"),
  STOMP_STARTING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Starting Stomp Protocol Implementation on {}"),
  STOMP_CLOSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Closing Stomp Implementation {}"),
  STOMP_PUSHED_WRITE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Pushed Frame for write, {}"),
  STOMP_FAILED_MAXIMUM_BUFFER(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set maximum buffer size, is not an integer::{}, using default of {}"),
  STOMP_FAILED_CLOSE(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed in close"),
  STOMP_PROCESSING_FRAME(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Processing frame {}"),
  STOMP_PROCESSING_FRAME_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Processing frame raised exception, closing session"),
  STOMP_INVALID_FRAME(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Invalid STOMP frame received.. Unable to process::{}"),
  STOMP_FRAME_HANDLE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised during frame {} processing"),
  // </editor-fold>

  // <editor-fold desc="Nats Protocol log messages">
  NATS_STATE_ENGINE_FAILED_COMPLETION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed on frame completion callback"),
  NATS_STARTING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Starting Nats Protocol Implementation on {}"),
  NATS_CLOSING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Closing Nats Implementation {}"),
  NATS_PUSHED_WRITE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Pushed Frame for write, {}"),
  NATS_FAILED_MAXIMUM_BUFFER(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set maximum buffer size, is not an integer::{}, using default of {}"),
  NATS_FAILED_CLOSE(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed in close"),
  NATS_PROCESSING_FRAME(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Processing frame {}"),
  NATS_PROCESSING_FRAME_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Processing frame raised exception, closing session"),
  NATS_INVALID_FRAME(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Invalid NATS frame received.. Unable to process::{}"),
  NATS_FRAME_HANDLE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised during frame {} processing"),
  // </editor-fold>


  // <editor-fold desc="MQTT 3.1.1 log messages">
  MQTT_CONNECT_LISTENER_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "[MQTT-3.2.2-5] Connection failed with return code, {}, closing connection"),
  MQTT_CONNECT_LISTENER_SECOND_CONNECT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "[MQTT-3.1.0-2] Received a second CONNECT packet"),
  MQTT_CONNECT_LISTENER_SESSION_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to construct a session for {}"),
  MQTT_BAD_USERNAME_PASSWORD(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Invalid username or password combination supplied"),
  MQTT_DUPLICATE_EVENT_RECEIVED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Detected duplicate events from the client, id {}"),
  MQTT_DISCONNECT_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Closing MQTT Session"),
  MQTT_PING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MQTT Ping Request received"),
  MQTT_PUBLISH_EXCEPTION(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Close raised an exception "),
  MQTT_PUBLISH_STORE_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to store message, exception thrown, need to close End Point, [MQTT-3.3.5-2]"),
  MQTT_START(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MQTT protocol instance started"),
  MQTT_FAILED_CLEANUP(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "failed to close EndPoint while cleaning up session"),
  MQTT_ALREADY_CLOSED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Potentially already closed"),
  MQTT_KEEPALIVE_TIMOUT(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Checking for keepalive timeout period of {}"),
  MQTT_DISCONNECT_TIMEOUT(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Disconnecting session since keep alive period has expired with no frames received"),
  MQTT_BUFFER_SIZE_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set maximum buffer size, is not an integer::{}, using default of {}"),
  // </editor-fold>

  // <editor-fold desc="MQTT 5.0 log messages">
  MQTT5_CONNECTION_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Failed connection, {}"),
  MQTT5_SECOND_CONNECT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "[MQTT-3.1.0-2] Received a second CONNECT packet"),
  MQTT5_UNHANDLED_PROPERTY(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Unhandled Message Property :: {}"),
  MQTT5_FAILED_CONSTRUCTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to construct a session for {}"),
  MQTT5_INVALID_USERNAME_PASSWORD(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to send status to client"),
  MQTT5_EXCEED_MAXIMUM(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Client exceeded outstanding events, currently {}, configured for {}"),
  MQTT5_DISCONNECT_REASON(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Disconnect reason:{}"),
  MQTT5_DISCONNECTING_SESSION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Closing session due to disconnect packet:: {}"),
  MQTT5_PING_RECEIVED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MQTTv5 Ping Request received"),
  MQTT5_DUPLICATE_PROPERTIES_DETECTED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Duplicate MQTT 5 properties detected in, this is a protocol error, duplicate ids [ {} ]"),
  MQTT5_HANDLE_EVENT_IO_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "An IOException was raised during the processing of a MQTT5 frame"),
  MQTT5_INITIALISATION(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MQTTv5 protocol instance started"),
  MQTT5_KEEP_ALIVE_CHECK(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Checking for keepalive timeout period of {}"),
  MQTT5_KEEP_ALIVE_DISCONNECT(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Disconnecting session since keep alive period has expired with no frames received"),
  MQTT5_MAXIMUM_SERVER_RECEIVE_FAIL(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set Server Receive Maximum, is not an integer, using default of 65535"),
  MQTT5_MAXIMUM_CLIENT_RECEIVE_FAIL(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set Client Receive Maximum, is not an integer, using default of 65535"),
  MQTT5_CLIENT_TOPIC_ALIAS(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set Client maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_SERVER_TOPIC_ALIAS(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set Server maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_TOPIC_ALIAS_PARSE_ERROR(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to set Server maximum topic alias, is not an integer, using default of 65535"),
  MQTT5_MAX_BUFFER_EXCEEDED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Max Buffer Size:{} prohibits delivery of large events:{}"),
  MQTT5_TOPIC_ALIAS_ADD(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Creating Topic Alias for {} as {}"),
  MQTT5_TOPIC_ALIAS_SET_MAXIMUM(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Setting {} Topic Alias to {}"),
  MQTT5_TOPIC_ALIAS_EXCEEDED_MAXIMUM(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Exceeded maximum number of alias"),
  MQTT5_TOPIC_ALIAS_INVALID_VALUE(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Invalid value supplied for alias, received {}"),
  MQTT5_TOPIC_ALIAS_ALREADY_EXISTS(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Alias already exists for {}"),
  // </editor-fold>

  // <editor-fold desc="Loop connection log messages">
  LOOP_CREATED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "loop protocol connection created"),
  LOOP_CLOSED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "loop protocol connection closed"),
  LOOP_SUBSCRIBED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "loop protocol subscribing to {} and delivering to {}"),
  LOOP_SENT_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Delivered message via loop"),
  LOOP_SEND_MESSAGE_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Delivery of message via loop failed"),
  LOOP_SEND_CONNECT_FAILED(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "Authentication failed"),
  // </editor-fold>

  // <editor-fold desc="Protocol detection log messages">
  PROTOCOL_ACCEPT_REGISTER(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Protocol Accept registered"),
  PROTOCOL_ACCEPT_SELECTOR_FIRED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Selector callback fired bytes"),
  PROTOCOL_ACCEPT_FIRING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Selector callback fired Starting at Packet Pos:{} limit:{}"),
  PROTOCOL_ACCEPT_FIRED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Selector callback fired and read {} bytes, Packet Pos:{} limit:{}"),
  PROTOCOL_ACCEPT_SCANNING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Scanning packet :{}"),
  PROTOCOL_ACCEPT_COMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Selector callback completed"),
  PROTOCOL_ACCEPT_EXCEPTION(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Exception raised during close of end point"),
  PROTOCOL_ACCEPT_FAILED_DETECT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to detect protocol on End Point {}, ip={}"),
  PROTOCOL_ACCEPT_CREATED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Created Protocol {}"),
  PROTOCOL_ACCEPT_CLOSED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "EndPoint closed during protocol negotiation ip={}"),
  // </editor-fold>

  // <editor-fold desc="JMX specific log messages">
  JMX_MANAGER_REGISTER(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Registering MBean with [name={}]"),
  JMX_MANAGER_UNREGISTER(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Unregistering MBean with [name={}]"),
  JMX_MANAGER_REGISTER_FAIL(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Unable to register MBean [name={}] "),
  JMX_MANAGER_UNREGISTER_FAIL(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Unable to unregister MBean [name={}]"),
  // </editor-fold>

  // <editor-fold desc="Configuration log messages">
  PROPERTY_MANAGER_START(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Starting Property Manager"),
  PROPERTY_MANAGER_FOUND(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Found and loaded property {}"),
  PROPERTY_MANAGER_LOOKUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Looking failed for {} config"),
  PROPERTY_MANAGER_LOOKUP_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Looking for {} config, found in {}"),
  PROPERTY_MANAGER_SCANNING(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Scanning property with {} entries"),
  PROPERTY_MANAGER_INDEX_DETECTED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Detected an indexed property file, parsing into different properties"),
  PROPERTY_MANAGER_COMPLETED_INDEX(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Completed indexed property with {} for index {}"),
  PROPERTY_MANAGER_SCAN_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to scan for property files"),
  PROPERTY_MANAGER_LOAD_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Failed to load property {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Lookup for {} found {} in {}"),
  PROPERTY_MANAGER_ENTRY_LOOKUP_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Lookup for {} not found, returning default {}"),
  // </editor-fold>

  //<editor-fold desc="Destination Manager log messages">
  DESTINATION_MANAGER_RELOADED(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Reloaded {} out of {}"),
  DESTINATION_MANAGER_RELOAD_INTERRUPTED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "The reloading of server resources was interrupted during reload"),
  //</editor-fold>

  //<editor-fold desc="Destination Subscription log messages">
  DESTINATION_SUBSCRIPTION_PUT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Adding subscription {} to destination {} for session {}"),
  DESTINATION_SUBSCRIPTION_HIBERNATE(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Hibernating destination subscription {} with session id {}"),
  DESTINATION_SUBSCRIPTION_WAKEUP(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Restoring destination subscription {} with session id {}"),
  DESTINATION_SUBSCRIPTION_SCHEDULED(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Scheduled task for subscription {} to send message to {}"),
  DESTINATION_SUBSCRIPTION_TASK_FAILURE(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Send message task for subscription {} failed to send message to {}"),
  DESTINATION_SUBSCRIPTION_SEND(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Sending message:{} to {} for subscription {}"),
  DESTINATION_SUBSCRIPTION_ACK(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Received ack for message:{} on subscription {} for {}"),
  DESTINATION_SUBSCRIPTION_ROLLBACK(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Received rollback for message:{} on subscription {} for {}"),
  DESTINATION_SUBSCRIPTION_EXCEPTION_ON_CLOSE(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Exception raised during close"),
  DESTINATION_SUBSCRIPTION_EXCEPTION_SELECTOR(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Exception raised while processing messaging selector {}"),
  //</editor-fold>

  //<editor-fold desc="Message State Manager log messages">
  MESSAGE_STATE_MANAGER_REGISTER(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Registering message:{} "),
  MESSAGE_STATE_MANAGER_ALLOCATE(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Allocating message:{} "),
  MESSAGE_STATE_MANAGER_COMMIT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Committing message:{} "),
  MESSAGE_STATE_MANAGER_ROLLBACK(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Rollback message:{} "),
  MESSAGE_STATE_MANAGER_NEXT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Scanning for next message returning:{} "),
  MESSAGE_STATE_MANAGER_ROLLBACK_INFLIGHT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Rolling back all in flight messages {}"),
  MESSAGE_STATE_MANAGER_ROLLED_BACK_INFLIGHT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "{} Rolled back all in flight messages"),
  //</editor-fold>

  //<editor-fold desc="Destination Manager log messages">
  DESTINATION_MANAGER_ADD_SYSTEM_TOPIC(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Added new topic {}"),
  DESTINATION_MANAGER_NOT_LICESNSED(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Not license usage for {}"),
  DESTINATION_MANAGER_EXCEEDED_LICESNSE(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Exceeded license usage for {}, has {} limit is {}"),
  DESTINATION_MANAGER_USER_SYSTEM_TOPIC(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "User attempted to create a system topic, {}, this is prohibited"),
  DESTINATION_MANAGER_CREATED_TOPIC(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "New topic created {}"),
  DESTINATION_MANAGER_DELETED_TOPIC(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Topic, {}, has been deleted"),
  DESTINATION_MANAGER_STARTING(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Destination Manager starting"),
  DESTINATION_MANAGER_STARTED_TOPIC(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Reloaded {}"),
  DESTINATION_MANAGER_EXCEPTION_ON_START(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Exception raised during Destination Manager startup"),
  DESTINATION_MANAGER_EXCEPTION_ON_STOP(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Exception raised during Destination Manager shutdown"),
  DESTINATION_MANAGER_STOPPING(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Shut down started"),
  DESTINATION_MANAGER_CLEARING(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Clear session id {} requested"),
  DESTINATION_MANAGER_DELETING_TEMPORARY_DESTINATION(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "\"Reloaded temp destination {}, now deleting"),
  //</editor-fold>

  //<editor-fold desc="Serial Port Server log Messages">
  SERIAL_SERVER_CREATE_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to construct a Serial End Point"),
  SERIAL_SERVER_BIND_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Failed to bind Serial Port with End Point"),
  //</editor-fold>

  //<editor-fold desc="Serial Port Scanner log messages">
  SERIAL_PORT_SCANNER_SCAN_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised while scanning for Serial Port changes"),
  SERIAL_PORT_SCANNER_BINDING(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Binding server {} to serial port {}"),
  SERIAL_PORT_SCANNER_UNBINDING(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Unbinding server {} from serial port {}"),
  SERIAL_PORT_SCANNER_UNUSED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Found serial port {} but no bound servers"),
  SERIAL_PORT_SCANNER_LOST(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Serial port {} has been disconnected"),
  //</editor-fold>

  //<editor-fold desc="LoRa gateway log messages">
  LORA_GATEWAY_SUCCESS(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Gateway command executed successfully <{}>"),
  LORA_GATEWAY_FAILURE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Gateway command failed to executed <{}>"),
  LORA_GATEWAY_PING(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Gateway Ping received, Radio state is on = {} "),
  LORA_GATEWAY_UNEXPECTED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Unknown command <{}> received from LoRa gateway "),
  LORA_GATEWAY_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised during the processing of the LoRa Gateway request"),
  LORA_GATEWAY_EXCEED_RATE(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exceeded the configured transmission rate of {}"),
  LORA_GATEWAY_LOG(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Gateway Log: {} "),
  LORA_GATEWAY_FRAMING_ERROR(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Stream Framing error detected {}"),
  LORA_GATEWAY_INVALID_COMMAND(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Invalid command <{}> received"),
  //</editor-fold>

  //<editor-fold desc="LoRa Device log messages">
  LORA_DEVICE_LIBRARY_NOT_LOADED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "LoRa device library failed to load {}"),
  LORA_DEVICE_NOT_INITIALISED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "LoRa device for {} on device {} not yet initialised"),
  LORA_DEVICE_INIT_FAILED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "LoRa device for {} on device {} failed to during initialised"),
  LORA_DEVICE_REGISTERED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Registering endPoint {} on {}"),
  LORA_DEVICE_DRIVER_LOG(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Driver message for {} on {} received, {}"),
  LORA_DEVICE_READ_THREAD_ERROR(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "The LoRa read thread on {} failed"),
  LORA_DEVICE_RECEIVED_PACKET(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Received LoRa packet destined to {} from {} with signal strength {}, packet size {}, id {}"),
  LORA_DEVICE_PACKET_READER_EXITED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Packet Reader thread has exited, no more LoRa packets will be processed"),
  LORA_DEVICE_IDLE(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Packet Reader is currently idle"),
  LORA_REGISTER_NETWORK_ACTIVITY(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "LoRa device registering network task for {}"),
  LORA_QUEUED_EVENT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "LoRa message queued, {} outstanding events, has select handler:{} "),
  LORA_DEVICE_NO_REGISTERED_ENDPOINT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "LoRa Device has no registered end points for {} address"),
  //</editor-fold>

  //<editor-fold desc="File operation log messages">
  FILE_RELOAD_PERCENT(LEVEL.TRACE, SERVER_CATEGORY.ENGINE, " File reload currently at {}"),
  FILE_FAILED_TO_CLOSE(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "File close raised exception"),
  FILE_FAILED_TO_DELETE(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "File delete raised exception"),
  //</editor-fold>

  //<editor-fold desc="Jolokia log messages">
  JOLOKIA_SHUTDOWN_FAILURE(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Jolokia failed to shutdown the HTTP server"),
  JOLOKIA_DEBUG_LOG(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Jolokia debug log {}"),
  JOLOKIA_INFO_LOG(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Jolokia info log {}"),
  JOLOKIA_ERROR_LOG(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Jolokia error log {}"),
  JOLOKIA_STARTUP_FAILURE(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Jolokia failed to load the HTTP server"),
  //</editor-fold>

  //<editor-fold desc="AMQP Log messages">
  AMQP_REMOTE_CLIENT_PROPERTIES(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Remote AMQP client property {} - {}"),
  AMQP_RECEIVED_EVENT(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Received event {}"),
  AMQP_DETECTED_JMS_CLIENT(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Detected remote client is a JMS client"),
  AMQP_CREATED_SESSION(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Session {} created"),
  AMQP_CLOSED_SESSION(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Session {} closed"),
  AMQP_CREATED_SUBSCRIPTION(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Created subscription on {} with alias {}"),
  AMQP_DELETED_SUBSCRIPTION(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Deleted subscription with alias {}"),
  AMQP_ENGINE_TRANSPORT_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised on Proton Engine Transport, {}"),
  AMQP_REMOTE_LINK_ERROR(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Remote link closed with error message {}"),
  //</editor-fold>

  //<editor-fold desc="Transaction Manager log messages">
  TRANSACTION_MANAGER_SCANNING(LEVEL.TRACE, SERVER_CATEGORY.ENGINE, "Transaction Manager expiry scan started"),
  TRANSACTION_MANAGER_CLOSE_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Transaction Manager detected exception when closing transaction id:{}"),
  TRANSACTION_MANAGER_TIMEOUT_DETECTED(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Transaction Manager detected expired transaction id:{}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL agent logging">
  CONSUL_STARTUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Agent startup"),
  CONSUL_CLIENT_LOG(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Consul Client state {} with config {}"),
  CONSUL_CLIENT_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Consul Client raised exception {}"),

  CONSUL_SHUTDOWN(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Agent shutdown"),
  CONSUL_REGISTER(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Registering with local agent"),
  CONSUL_PING_EXCEPTION(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Ping failed with exception {}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL management log messages">
  CONSUL_MANAGER_START(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Manager starting up for id {}"),
  CONSUL_MANAGER_STOP(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Manager shutting down"),
  CONSUL_KEY_VALUE_MANAGER(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Consul Key/Value, Action:{}, Key: \"{}\""),
  CONSUL_INVALID_KEY(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Consul Key/Value, invalid key received {}, changed to {}"),
  CONSUL_MANAGER_START_ABORTED(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Startup aborted due to configuration, id {}"),
  CONSUL_MANAGER_START_DELAYED(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Startup delaying server startup due to configuration for id {}"),
  CONSUL_MANAGER_START_SERVER_NOT_FOUND(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Startup aborted since Consul Server is not responding, id {}"),
  //</editor-fold>

  //<editor-fold desc="CONSUL Key/Value management log messages">
  CONSUL_PROPERTY_MANAGER_NO_KEY_VALUES(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "No keys found in Consul Key/Value for id {}"),
  CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Key {}, lookup failed with exception"),
  CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Key {}, lookup success, returned {} bytes"),

  CONSUL_PROPERTY_MANAGER_INVALID_JSON(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Value returned is not valid json for key {}"),
  CONSUL_PROPERTY_MANAGER_SAVE_ALL(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Saving all entries for {}"),
  CONSUL_PROPERTY_MANAGER_STORE(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Storing entry for {}"),
  //</editor-fold>
  //<editor-fold desc="System and Environment property access">
  CONFIG_PROPERTY_ACCESS(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Getting property {} from system resulted in {}"),

  //</editor-fold>

  //<editor-fold desc="NameSpace mapping used to support multi tenancy">
  NAMESPACE_MAPPING(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Mapping {} to namespace {}"),
  NAMESPACE_MAPPING_FOUND(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Found entry for  {} mapping to {}"),
  NAMESPACE_MAPPING_DEFAULT(LEVEL.INFO, SERVER_CATEGORY.ENGINE, "Using default mapping {}"),
  //</editor-fold>

  // <editor-fold desc="MQTT-SN log messages">
  MQTT_SN_INSTANCE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Protocol instance started"),
  MQTT_SN_CLOSE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Failed to close EndPoint while cleaning up session"),
  MQTT_SN_ALREADY_CLOSED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Potentially already closed"),
  MQTT_SN_NON_UDP(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Bound to a non UDP based End Point, this will not work"),
  MQTT_SN_ADVERTISER_SENT_PACKET(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "Sent advertise packet {}"),
  MQTT_SN_ADVERTISE_PACKET_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "An exception occurred while send an advertise packet"),
  MQTT_SN_PACKET_EXCEPTION(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Exception raised processing frame {}"),
  MQTT_SN_GATEWAY_DETECTED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Detected MQTT-SN service advertise packet for Gateway Id {}, from {}"),
  MQTT_SN_REGISTERED_EVENT(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Registered Event processed for {}"),
  MQTT_SN_REGISTERED_EVENT_NOT_FOUND(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Registered Event packet detected but no configuration found for host:{} topic Id:{}"),
  MQTT_SN_INVALID_QOS_PACKET_DETECTED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Publish packet received from {}, but incorrect QoS should be 3 but found {}"),
  MQTT_SN_EXCEPTION_RASIED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "An exception was raised during the processing of an MQTT-SN packet {}"),

  MQTT_SN_START(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MQTT-SN protocol instance started {}"),
  MQTT_SN_CLOSED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "MQTT-SN protocol closed"),
  MQTT_SN_KEEP_ALIVE_SEND(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Sending KeepAlive"),
  MQTT_SN_KEEP_ALIVE_TIMED_OUT(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Keepalive timeout exceeded, disconnecting client"),
  MQTT_SN_STATE_ENGINE_STATE_CHANGE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "MQTT-SN State Engine changing state from {} to {}"),
  //</editor-fold>

  // <editor-fold desc="MQTT-SN message pipeline">
  MQTT_SN_PIPELINE_CREATED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Created new outbound pipeline for {}, DropQoS:{}, Max Inflight:{}, Event Time out:{}"),
  MQTT_SN_PIPELINE_PAUSED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Pipeline has been paused {}"),
  MQTT_SN_PIPELINE_RESUMED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Pipeline has been resumed {}"),
  MQTT_SN_PIPELINE_WOKEN(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Client, {},  has requested delivery of any outstanding messages, maximum:{}, size:{}"),
  MQTT_SN_PIPELINE_EVENT_DROPPED(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Message has been dropped for {} on {}, message id:{}, QoS:{} and client sleeping"),
  MQTT_SN_PIPELINE_EVENT_TIMED_OUT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "Message has timed out for {} on {}, message id:{}, QoS:{} and client sleeping"),
  MQTT_SN_PIPELINE_EVENT_COMPLETED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Message delivery completed {}"),
  MQTT_SN_PIPELINE_EVENT_SENT(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Message has been sent for delivery for {}, on {} message id:{}"),
  MQTT_SN_PIPELINE_EVENT_QUEUED(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Message queued for {}, on {} message id:{}"),
  // </editor-fold>

  // <editor-fold desc="SemTech UDP Protocol Log Messages">
  SEMTECH_RECIEVED_PACKET(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Received packet {}"),
  SEMTECH_SENDING_PACKET(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Sending packet {}"),
  SEMTECH_CLOSE(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Closing Protocol"),
  SEMTECH_QUEUE_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Message queued for delivery {}"),
  // </editor-fold>

  //<editor-fold desc="Server Discovery, mDNS, log messages">
  DISCOVERY_FAILED_TO_START(LEVEL.WARN, SERVER_CATEGORY.DISCOVERY, "The discovery engine failed to start"),
  DISCOVERY_FAILED_TO_REGISTER(LEVEL.WARN, SERVER_CATEGORY.DISCOVERY, "The discovery engine failed to register {}"),
  DISCOVERY_REGISTERED_SERVICE(LEVEL.INFO, SERVER_CATEGORY.DISCOVERY, "Registered new mDNS service {}"),
  DISCOVERY_DEREGISTERED_SERVICE(LEVEL.INFO, SERVER_CATEGORY.DISCOVERY, "Deregistered mDNS service {}"),
  DISCOVERY_DEREGISTERED_ALL(LEVEL.INFO, SERVER_CATEGORY.DISCOVERY, "Removed all registered mDNS services"),

  DISCOVERY_RESOLVED_REMOTE_SERVER(LEVEL.DEBUG, SERVER_CATEGORY.DISCOVERY, "Discovered remote server {} on {} using {}"),
  DISCOVERY_REMOVED_REMOTE_SERVER(LEVEL.DEBUG, SERVER_CATEGORY.DISCOVERY, "Removed remote server {} on {} using {}"),
  //</editor-fold>

  //<editor-fold desc="CoAP, log messages">
  COAP_CREATED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Created new CoAP protocol handler for {}, MTU:{}, MaxBlockSize:{}"),
  COAP_CLOSED(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Closed CoAP protocol handler for {}"),
  COAP_RECEIVED_RESET(LEVEL.INFO, SERVER_CATEGORY.PROTOCOL, "Received CoAP reset packet for {}"),
  COAP_SESSION_TIMED_OUT(LEVEL.WARN, SERVER_CATEGORY.PROTOCOL, "CoAP session {} exceeded idle time of {}"),

  COAP_PACKET_PROCESSED(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "Handling CoAP packet {} for {}"),
  COAP_PACKET_SENT(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "Sent CoAP packet {} for {}"),
  COAP_FAILED_TO_SEND(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "CoAP failed to send packet to {}"),
  COAP_FAILED_TO_PROCESS(LEVEL.ERROR, SERVER_CATEGORY.PROTOCOL, "CoAP failed to process packet from {}"),

  COAP_BERT_NOT_SUPPORTED(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "BERT block not currently supported for {}"),
  COAP_BLOCK2_REQUEST(LEVEL.TRACE, SERVER_CATEGORY.PROTOCOL, "Block2 packet received, Message No: {}, Block Size: {}, has more:{}"),
  //</editor-fold>

  //<editor-fold desc="Device Integration, log messages">
  DEVICE_SELECTOR_PARSER_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Selection {}, failed to parse with the following exception {}"),
  DEVICE_SCHEMA_UPDATED(LEVEL.WARN, SERVER_CATEGORY.DEVICE, "Device {} schema configuration updated"),
  DEVICE_SCHEMA_UPDATE_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Schema failed to be updated {} while applying {}"),
  DEVICE_SUBSCRIPTION_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Subscription failed to be applied to {}"),
  DEVICE_PUBLISH_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Event publish failed {}"),
  DEVICE_START(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Starting device {}"),
  DEVICE_STOP(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Stopping device {}"),

  DEVICE_MANAGER_STARTUP(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Starting Device Manager"),
  DEVICE_MANAGER_STARTUP_FAILED(LEVEL.WARN, SERVER_CATEGORY.ENGINE, "Device Manager failed to start"),
  DEVICE_MANAGER_FAILED_TO_REGISTER(LEVEL.INFO, SERVER_CATEGORY.DEVICE, "Failed to register device"),
  DEVICE_MANAGER_START_ALL(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Starting all registered devices"),
  DEVICE_MANAGER_STOP_ALL(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Stopping all registered devices"),
  DEVICE_MANAGER_PAUSE_ALL(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Pausing all registered devices"),
  DEVICE_MANAGER_RESUME_ALL(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Resuming all registered devices"),
  DEVICE_MANAGER_LOAD_PROPERTIES(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Loading Device Manager Properties"),
  DEVICE_MANAGER_STARTUP_COMPLETE(LEVEL.DEBUG, SERVER_CATEGORY.ENGINE, "Completed startup Device Manager"),
  //</editor-fold>

  //<editor-fold desc="Network Interface status log messages">
  NETWORK_MONITOR_STATE_CHANGE(LEVEL.ERROR, SERVER_CATEGORY.NETWORK, "Network interface {} changed state to {}"),
  NETWORK_MONITOR_DISCOVERED_DEVICES(LEVEL.ERROR, SERVER_CATEGORY.NETWORK, "Discovered {} network device as {}"),
  NETWORK_MONITOR_EXCEPTION(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Network monitor raised exception {}"),
  NETWORK_MONITOR_RESOLVE_ERROR(LEVEL.ERROR, SERVER_CATEGORY.NETWORK, "Failed to resolve host name {} "),
  NETWORK_MONITOR_RESOLVE_SUCCESS(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Successfully resolved host name {} to {}"),
  //</editor-fold>

  //<editor-fold desc="Rest API log messages">
  REST_API_ACCESS(LEVEL.INFO, SERVER_CATEGORY.REST, "Address {} requested {}, returning Status:{} with length {} bytes"),
  REST_API_FAILURE(LEVEL.ERROR, SERVER_CATEGORY.REST, "Rest Server unable to start due to exception"),
  REST_API_SUCCESSFUL_REQUEST(LEVEL.INFO, SERVER_CATEGORY.REST, "Rest request type {} for {} with status {}"),
  REST_CACHE_HIT(LEVEL.INFO, SERVER_CATEGORY.REST,"Cache hit for key: {}"),
  REST_CACHE_MISS(LEVEL.INFO, SERVER_CATEGORY.REST,"Cache miss for key: {}"),
  //</editor-fold>

  SYSTEM_TOPIC_MESSAGE_ERROR(LEVEL.ERROR, SERVER_CATEGORY.ENGINE, "Failed to send update to {}, exception raised"),

  AUTH_STARTUP_FAILED(LEVEL.ERROR, SERVER_CATEGORY.AUTHENTICATION, "Authentication manager failed to start up"),
  AUTH_SAVE_FAILED(LEVEL.ERROR, SERVER_CATEGORY.AUTHENTICATION, "Authentication manager failed to save initial user configuration"),
  AUTH_STOP_FAILED(LEVEL.ERROR, SERVER_CATEGORY.AUTHENTICATION, "Authentication manager raised exception during stop"),
  AUTH_ADDED_USER(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "User: {} added"),
  AUTH_DELETED_USER(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "User: {} deleted"),
  AUTH_MODIFIED_USER(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "User: {} modified"),

  AUTH_ADDED_GROUP(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "Group: {} added"),
  AUTH_DELETED_GROUP(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "Group: {} deleted"),
  AUTH_MODIFIED_GROUP(LEVEL.AUDIT, SERVER_CATEGORY.AUTHENTICATION, "User:{} {} Group: {}"),

  AUTH_STORAGE_FAILED_TO_LOAD(LEVEL.ERROR, SERVER_CATEGORY.AUTHENTICATION, "Authentication storage level failed to load"),
  AUTH_STORAGE_FAILED_ON_UPDATE(LEVEL.ERROR, SERVER_CATEGORY.AUTHENTICATION, "Authentication storage unable to update state"),

  MESSAGE_TRANSFORMATION_EXCEPTION(LEVEL.ERROR, SERVER_CATEGORY.TRANSFORMATION, "Exception raised during transformation"),

  LICENSE_INSTALLING(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Installing license edition {}"),
  LICENSE_FAILED_INSTALLING(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Failed to install license edition {}"),
  LICENSE_MANAGER_NOT_FOUND(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Failed to locate license manager for edition {}"),
  LICENSE_FILE_RENAME_FAILED(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Failed to rename license file from {} to {}"),
  LICENSE_FEATURES_AVAILABLE(LEVEL.WARN, SERVER_CATEGORY.LICENSE, "Loaded the following license {}"),

  LICENSE_LOADING(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Loading license edition {}"),
  LICENSE_EXPIRED(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "License {} has expired Not Before {} and Not After {}"),
  LICENSE_UNINSTALLING(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "License {} is being uninstalled"),

  LICENSE_FAILED_LOADING(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Failed to load license edition {}"),
  LICENSE_CONTACTING_SERVER(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Contacting licensing server for configured license using Client:{}"),
  LICENSE_ERROR_CONTACTING_SERVER(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Error response contacting licensing server"),
  LICENSE_FAILED_CONTACTING_SERVER(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Failed to contact licensing server"),
  LICENSE_SAVED_TO_FILE(LEVEL.INFO, SERVER_CATEGORY.LICENSE, "Saved license file to {}"),
  LICENSE_FAILED_SAVED_TO_FILE(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Failed to save license file to {}"),
  LICENSE_FAILED_DELETE_FILE(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Failed to delete license file {}"),
  LICENSE_LOADED(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Loaded license for {} by {}, created {}, valid after {} and till {} with features {}"),
  LICENSE_UNKNOWN_FEATURE_KEY(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Unknown feature name requested {}"),
  LICENSE_DISABLED_FEATURE_KEY(LEVEL.ERROR, SERVER_CATEGORY.LICENSE, "Feature is not enabled {}"),

  //-------------------------------------------------------------------------------------------------------------
  OGWS_SENDING_REQUEST(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "OGWS request {} returned status {}"),
  OGWS_FAILED_AUTHENTICATION(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Authentication failed, aborting connection"),
  OGWS_FAILED_POLL(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Poll of outstanding messaged failed with error code {}"),
  OGWS_REQUEST_FAILED(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Network based exception raised while communicating with the OGWS server"),
  OGWS_FAILED_TO_SAVE_MESSAGE(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Network based exception raised while communicating with the OGWS server"),
  OGWS_UNPROCESSED_MESSAGE(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Received a message SIN:{} MIN:{} from ClientId:{} but not configured to process it"),
  OGWS_NO_CONFIGURATION_FOUND(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "No configuration found for ogws server"),
  OGWS_EXCEPTION_PROCESSING_MESSAGE(LEVEL.FATAL, SERVER_CATEGORY.ENGINE, "Exception raised processing inbound message"),
  OGWS_WEB_REQUEST_STATS(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Web request for {} took {} ms"),

  //-------------------------------------------------------------------------------------------------------------
  INMARSAT_WEB_REQUEST_FAILED(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Web request failed with error code {},retrying {} more times"),
  INMARSAT_WEB_REQUEST_STATS(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Web request for {} took {} ms"),
  INMARSAT_FAILED_PROCESSING_INCOMING(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Processing incoming message raised exception {}"),
  //-------------------------------------------------------------------------------------------------------------
  STOGI_STARTED_SESSION(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Successfully started modem session, detected modem type {}, read intervals of {} ms and write intervals of {} ms"),
  STOGI_ENCRYPTION_STATUS(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Encryption is {} for {}"),
  STOGI_POLL_FOR_ACTIONS(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Polled for actions took {} ms, next poll in {} ms"),
  STOGI_POLL_RAISED_EXCEPTION(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Polling for action resulted in an exception"),
  STOGI_SEND_AT_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Sending to modem: {} "),
  STOGI_RECEIVED_AT_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Received from modem : {}"),
  STOGI_SATELLITES_STATUS_CHANGE(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Satellite transmission status has changed to {}"),
  STOGI_SENT_MESSAGE_TO_MODEM(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Sent message to be transmitted to satellite of length {} bytes"),
  STOGI_RECEIVED_MESSAGE_TO_MODEM(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Received message from satellite of length {} bytes"),
  STOGI_EXCEPTION_PROCESSING_PACKET(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Exception raised processing inbound packet"),
  STOGI_STORE_EVENT_EXCEPTION(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Exception raised storing event"),
  STOGI_PROCESSING_INBOUND_EVENT(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Processing inbound event for msg no: {}"),
  STOGI_SEND_MESSAGE_TO_MODEM(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Sent message to modem, msg no: {}, {} bytes"),
  STOGI_RECEIVED_PARTIAL_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Received from partial message msg no: {}"),


  STOGI_COMPRESS_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Compressing message for from {} to {} bytes"),
  STOGI_SPLIT_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Splitting message into {} messages"),

  SATELLITE_SENT_RAW_MESSAGE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Sent SIN:{} MIN:{} length {}"),
  SATELLITE_FILTER_FAILED(LEVEL.FATAL, SERVER_CATEGORY.NETWORK, "Filter failed to process {}"),
  SATELLITE_QUEUED_PENDING_MESSAGE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Queued message for {}, current queue depth {}"),
  SATELLITE_SENT_PACKED_MESSAGES(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Sent using SIN:{}, total messages:{}, with total size:{}"),
  SATELLITE_RECEIVED_RAW_MESSAGE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Received raw message SIN:{}, MIN:{}, size:{}, publishing to {}"),
  SATELLITE_RECEIVED_PACKED_MESSAGE(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Received packed messages to {} destinations, for a total of {} messages, from buffer of size:{}"),
  SATELLITE_SCANNING_FOR_INCOMING(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Scanning for incoming messages, returned {} messages"),

  EVALUATION_START(LEVEL.TRACE, SERVER_CATEGORY.NETWORK, "Evaluating link selection"),
  EVALUATION_RESULT(LEVEL.DEBUG, SERVER_CATEGORY.NETWORK, "Selection result: current={}({}), best={}({}), reason={}"),
  SWITCH_SUCCESS(LEVEL.INFO, SERVER_CATEGORY.NETWORK, "Switched from {} to {} due to {}"),
  SWITCH_REJECTED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Switch rejected, reason={}"),
  SWITCH_REQUESTED(LEVEL.WARN, SERVER_CATEGORY.NETWORK, "Switch requested to{}, reason={}"),
  EXCEPTION_DURING_EVALUATION(LEVEL.ERROR, SERVER_CATEGORY.NETWORK, "Exception during link evaluation: {}"),

  STATISTICS_UNKNOWN_NAME(LEVEL.FATAL, SERVER_CATEGORY.PROTOCOL, "Unknown statistics name found {}, defaulting to {}"),

  //-------------------------------------------------------------------------------------------------------------
  LAST_LOG_MESSAGE(LEVEL.DEBUG, SERVER_CATEGORY.PROTOCOL, "Last message to make it simpler to add more");

  private final @Getter String message;
  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter int parameterCount;

  ServerLogMessages(LEVEL level, SERVER_CATEGORY category, String message) {
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
    TEST("Test"),
    AUTHORISATION("Authorisation"),
    AUTHENTICATION("Authentication"),
    NETWORK("Network"),
    PROTOCOL("Protocol"),
    DISCOVERY("Discovery"),
    REST("Rest"),
    TRANSFORMATION("Transformation"),
    DEVICE("Device"),
    LICENSE("License"),
    DAEMON("Daemon"),
    ENGINE("Engine");

    private final @Getter String description;

    public String getDivision() {
      return "Messaging";
    }

    SERVER_CATEGORY(String description) {
      this.description = description;
    }
  }
}
