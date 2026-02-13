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

package io.mapsmessaging.aggregator;

import io.mapsmessaging.api.*;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.*;

public class Aggregator  implements ClientConnection, MessageListener {

  private final AggregatorConfigDTO configDTO;
  private Session session;
  private Map<String, StreamHandler> streamHandlerMap;

  public Aggregator(AggregatorConfigDTO configDTO) {
    this.configDTO = configDTO;
    streamHandlerMap = new ConcurrentHashMap<>();
    for(AggregatorInputConfigDTO input: configDTO.getInputs()){
      streamHandlerMap.put(input.getTopicName(), new StreamHandler(input));
    }
  }

  public void start() throws ExecutionException, IOException, InterruptedException, TimeoutException {
    session = createSession();
    for(StreamHandler handler: streamHandlerMap.values()){
      handler.start(session);
    }

  }

  public void stop() throws IOException {
    for(StreamHandler handler: streamHandlerMap.values()){
      handler.stop(session);
    }
    SessionManager.getInstance().close(session, true);
  }

  private Session createSession() throws ExecutionException, InterruptedException, TimeoutException {
    SessionContextBuilder scb = new SessionContextBuilder(configDTO.getName(), this);
    scb.setResetState(true)
        .setSessionExpiry(0)
        .setPersistentSession(false)
        .setReceiveMaximum(100);
    CompletableFuture<Session> sessionFuture = SessionManager.getInstance().createAsync(scb.build(), this);
    return sessionFuture.get(5, TimeUnit.SECONDS);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    StreamHandler handler = streamHandlerMap.get(messageEvent.getDestinationName());
    if(handler != null){
      handler.handle(messageEvent);
      boolean complete = true;
      for(StreamHandler streamHandler: streamHandlerMap.values()){
        if(!streamHandler.hasMessage()){
          complete = false;
          break;
        }
      }
      if(complete){
        // Combine and publish to the aggregated output
      }
    }
    else{
      if(messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }




  @Override
  public long getTimeOut() {
    return 30000;
  }

  @Override
  public String getName() {
    return "Aggregator-"+configDTO.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // No Op - Nothing to send
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return configDTO.getName();
  }

  @Override
  public String getProtocolName() {
    return "aggregator";
  }

  @Override
  public String getRemoteIp() {
    return "loop";
  }
}
