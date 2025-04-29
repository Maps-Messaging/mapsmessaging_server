package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PubFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PubListener implements FrameListener {
  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {
    PubFrame msgFrame = (PubFrame) frame;
    String destName = convertSubject(msgFrame.getSubject());
    String lookup = engine.getMapping(destName);
    CompletableFuture<Destination> future = engine.getSession().findDestination(lookup, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          if (destination != null) {
            handleMessageStoreToDestination(destination, engine, msgFrame);
            if (engine.isVerbose()) engine.send(new OkFrame());
          } else {
            ErrFrame errFrame = new ErrFrame();
            errFrame.setError("No such destination");
            engine.send(errFrame);
          }
        } catch (IOException e) {
          ErrFrame errFrame = new ErrFrame();
          errFrame.setError(e.getMessage());
          engine.send(errFrame);
          future.completeExceptionally(e);
          throw new RuntimeException(e);
        }
        return destination;
      });
    }
  }

  protected void handleMessageStoreToDestination(Destination destination, SessionState engine, PubFrame msgFrame) throws IOException {
    if (destination != null) {
      Map<String, TypedData> dataMap = new HashMap<>();
      Map<String, String> metaData = new HashMap<>();
      metaData.put("protocol", "NATS");
      metaData.put("version", engine.getProtocol().getVersion());
      metaData.put("sessionId", engine.getSession().getName());

      MessageBuilder mb = new MessageBuilder();
      Message message = mb.setDataMap(dataMap)
          .setOpaqueData(msgFrame.getPayload())
          .setMeta(metaData)
          .setCorrelationData(msgFrame.getReplyTo())
          .setQoS(QualityOfService.AT_LEAST_ONCE)
          .setTransformation(engine.getProtocol().getTransformation())
          .build();
      destination.storeMessage(message);
    }
  }
}
