package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.handlers;

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StreamDeleteHandler extends JetStreamHandler {

  @Override
  public String getName() {
    return "DELETE";
  }

  @Override
  public NatsFrame handle(PayloadFrame frame, JsonObject json, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();
    NatsFrame msg = buildResponse(subject, frame, sessionState);
    if(msg instanceof ErrFrame) {
      return msg;
    }

    String[] parts = subject.split("\\.");
    if (parts.length < 5) {
      return new ErrFrame("Invalid stream delete subject");
    }
    PayloadFrame result = (PayloadFrame) msg;
    if(!sessionState.getProtocol().getNatsConfig().isEnableStreamDelete()){
      result.setPayload(deletionDisabledMessage().getBytes());
      return result;
    }

    String streamName = parts[4];
    StreamInfoList info = NamespaceManager.getInstance().getStream(streamName);
    if(info == null){
      result.setPayload(streamNotFound().getBytes());
      return result;
    }

    List<CompletableFuture<Void>> deletes = new ArrayList<>();

    for (StreamInfo streamInfo : info.getSubjects()) {
      deletes.add(sessionState.getSession().deleteDestinationImpl(streamInfo.getDestination()));
    }

    try {
      CompletableFuture<Void> all = CompletableFuture.allOf(deletes.toArray(new CompletableFuture[0]));
      all.get(20, TimeUnit.SECONDS); // Optional timeout
    } catch (TimeoutException e) {
      return new ErrFrame("Timed out while deleting stream destinations");
    } catch (ExecutionException | InterruptedException e) {
      return new ErrFrame("Error while deleting stream destinations");
    }
    result.setPayload(success().getBytes());

    return result;
  }

  private String success(){
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_delete_response\",\n" +
        "  \"success\": true\n" +
        "}";
  }

  private String streamNotFound(){
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_delete_response\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 404,\n" +
        "    \"description\": \"stream not found\"\n" +
        "  }\n" +
        "}\n";
  }

  private String deletionDisabledMessage(){
    return "{\n" +
        "  \"type\": \"io.nats.jetstream.api.v1.stream_delete_response\",\n" +
        "  \"error\": {\n" +
        "    \"code\": 403,\n" +
        "    \"description\": \"stream deletion not permitted\"\n" +
        "  }\n" +
        "}\n";
  }
}
