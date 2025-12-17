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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.satellite.TaskManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import lombok.Getter;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.OGWS_FAILED_AUTHENTICATION;
import static io.mapsmessaging.logging.ServerLogMessages.SATELLITE_SCANNING_FOR_INCOMING;

public class GatewayManager {

  private final Logger logger = LoggerFactory.getLogger(GatewayManager.class);
  private final SatelliteClient satelliteClient;
  private final IncomingMessageHandler handler;
  private final int pollInterval;
  private final List<MessageData> pendingMessages;
  private final Map<String, RemoteDeviceInfo> knownTerminals;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private ScheduledFuture<?> scheduledFuture;


  @Getter
  private boolean authenticated;

  public GatewayManager(SatelliteConfigDTO satelliteConfigDTO, IncomingMessageHandler handler) {
    this.satelliteClient = ClientFactory.createSatelliteClient(satelliteConfigDTO);
    this.pollInterval = (int)satelliteConfigDTO.getIncomingMessagePollInterval();
    this.handler = handler;
    authenticated = false;
    knownTerminals = new ConcurrentHashMap<>();
    pendingMessages = new ArrayList<>();
  }

  public void start() {
    TaskManager.getInstance().schedule(this::initSession, 10, TimeUnit.SECONDS);
  }

  public void stop(){
    closed.set(true);
    if(scheduledFuture != null){
      scheduledFuture.cancel(true);
      scheduledFuture = null;
    }
  }

  protected void initSession(){
    if(closed.get()){
      return;
    }
    try {
      authenticated = satelliteClient.authenticate();
      if (authenticated) {
        List<RemoteDeviceInfo> response = satelliteClient.getTerminals(null);
        for (RemoteDeviceInfo terminal : response) {
          if (DeviceIdUtil.isValidDeviceId(terminal.getUniqueId())) {
            knownTerminals.put(terminal.getUniqueId(), terminal);
          }
        }
        for (RemoteDeviceInfo terminal : knownTerminals.values()) {
          handler.registerTerminal(terminal);
        }
        scheduledFuture = TaskManager.getInstance().schedule(this::pollGateway, pollInterval, TimeUnit.SECONDS);
      } else {
        logger.log(OGWS_FAILED_AUTHENTICATION);
      }
    }
    catch (RuntimeException| IOException e) {
      // Todo log this
      // we can try again
      TaskManager.getInstance().schedule(this::initSession, 1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (LoginException e) {
      // We need to log the fact that this session is not allowed
    }

  }

  public void muteClient(String id){
    satelliteClient.mute(id);
  }

  public void unmuteClient(String id){
    satelliteClient.unmute(id);
  }

  public RemoteDeviceInfo getTerminal(String id) {
    return knownTerminals.get(id);
  }


  public void sendClientMessage(String primeId, MessageData submitMessage) {
    submitMessage.setUniqueId(primeId);
    synchronized (pendingMessages) {
      pendingMessages.add(submitMessage);
    }
  }

  // timed task to read /write to the remote server
  protected void pollGateway() {
    try {
      Queue<MessageData> queue = satelliteClient.scanForIncoming();
      logger.log(SATELLITE_SCANNING_FOR_INCOMING, queue.size());
      handler.handleIncomingMessage(queue);
      List<MessageData> tmp;
      synchronized (pendingMessages) {
        tmp = new ArrayList<>(pendingMessages);
        pendingMessages.clear();
      }
      if (!tmp.isEmpty()) {
        satelliteClient.processPendingMessages(tmp);
      }
    }
    finally {
      scheduledFuture = TaskManager.getInstance().schedule(this::pollGateway, pollInterval, TimeUnit.SECONDS);
    }
  }

  public RemoteDeviceInfo updateTerminalInfo(String uniqueId) throws IOException, InterruptedException {
    List<RemoteDeviceInfo> response = satelliteClient.getTerminals(uniqueId);
    for (RemoteDeviceInfo terminal : response) {
      if(DeviceIdUtil.isValidDeviceId(terminal.getUniqueId()) && uniqueId.equals(terminal.getUniqueId())) {
        return terminal;
      }
    }
    return null;
  }
}
