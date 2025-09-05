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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers;

import com.google.gson.JsonObject;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamFrameHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamConfig;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamInfoResponse;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamState;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StreamInfoHandler extends JetStreamFrameHandler {

  @Override
  public String getName() {
    return "STREAM.INFO";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    NatsFrame response = buildResponse(subject, frame, sessionState);
    if (response instanceof ErrFrame) {
      return response;
    }
    int idx = frame.getSubject().indexOf("INFO.");
    if (idx < 0) {
      return new ErrFrame("No stream name supplied");
    }
    String streamName = frame.getSubject().substring(idx + "INFO.".length());
    ((PayloadFrame) response).setPayload(buildInfo(streamName).getBytes());
    return response;
  }

  private String buildInfo(String subject) {
    StreamInfoList streamInfoList = NamespaceManager.getInstance().getStream(subject);
    if (streamInfoList == null) {
      return noStreamsError();
    }
    StreamInfoResponse streamInfoResponse = new StreamInfoResponse();
    packResponse(streamInfoResponse, streamInfoList);
    return gson.toJson(streamInfoResponse);
  }

  private void packResponse(StreamInfoResponse response, StreamInfoList streamInfoList) {
    buildState(response, streamInfoList);
    response.setConfig(buildConfig(streamInfoList));
  }

  private StreamConfig buildConfig(StreamInfoList streamInfoList) {
    StreamConfig streamConfig = new StreamConfig();
    streamConfig.setName(streamInfoList.getName());
    List<String> subjects = new ArrayList<>();
    for (StreamInfo info : streamInfoList.getSubjects()) {
      subjects.add(info.getSubject());
    }
    streamConfig.setSubjects(subjects);
    streamConfig.setStorage("maps");
    return streamConfig;
  }

  private StreamState buildState(StreamInfoResponse response, StreamInfoList list) {
    StreamState aggregateState = new StreamState();

    long totalMessages = 0;
    long firstSeq = Long.MAX_VALUE;
    long lastSeq = 0;
    int totalConsumers = 0;

    Date created = new Date(System.currentTimeMillis());
    for (StreamInfo info : list.getSubjects()) {
      DestinationImpl destination = info.getDestination();
      DestinationStats stats = destination.getStats();
      totalMessages += stats.getStoredMessages();
      totalConsumers += destination.getSubscriptionStates().size();
      Date destDate = destination.getResourceProperties().getDate();
      if (destDate != null && destDate.before(created)) {
        created = destDate;
      }
    }

    response.setCreated(created.toInstant());
    aggregateState.setMessages(totalMessages);
    aggregateState.setBytes(-1);
    aggregateState.setFirst_seq(firstSeq == Long.MAX_VALUE ? 1 : firstSeq);
    aggregateState.setLast_seq(lastSeq);
    aggregateState.setConsumer_count(totalConsumers);
    return aggregateState;
  }

  private String noStreamsError() {
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_info_response\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 404,\n" +
        "    \"err_code\": 2,\n" +
        "    \"description\": \"stream not found\"\n" +
        "  }\n" +
        "}\n";
  }


}
