package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data.StreamEntry;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data.StreamInfoResponse;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers.data.StreamListResponse;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.util.List;

public class StreamListHandler extends JetStreamHandler {

  @Override
  public String getName() {
    return "LIST";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    NatsFrame response = buildResponse(subject, frame, sessionState);
    if(response instanceof ErrFrame) {
      return response;
    }
    ((PayloadFrame)response).setPayload(buildInfo(frame, json).getBytes());
    return response;
  }

  private String buildInfo(PayloadFrame frame, JsonObject json) {
    List<StreamEntry> entries = NamespaceManager.getInstance().getStreamEntries();

    // Extract pagination values
    int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
    int limit = json.has("limit") ? json.get("limit").getAsInt() : 1024;

    // Clamp values to avoid index errors
    offset = Math.max(0, Math.min(offset, entries.size()));
    int toIndex = Math.min(offset + limit, entries.size());

    List<StreamEntry> page = entries.subList(offset, toIndex);

    // Build response
    StreamListResponse streamListResponse = new StreamListResponse();
    streamListResponse.setStreams(page);
    streamListResponse.setOffset(offset);
    streamListResponse.setLimit(limit);
    streamListResponse.setTotal(entries.size());
    Gson gson = new Gson();
    return gson.toJson(streamListResponse);
  }
}
