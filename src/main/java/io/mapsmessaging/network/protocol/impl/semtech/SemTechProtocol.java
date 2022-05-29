package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.semtech.handlers.PacketHandler;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class SemTechProtocol extends ProtocolImpl {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final PacketFactory packetFactory;
  private final Session session;


  private final Queue<MessageEvent> waitingMessages;

  @Getter
  private final GatewayManager gatewayManager;

  protected SemTechProtocol(@NonNull @NotNull EndPoint endPoint) throws IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger("SemTech Protocol on " + endPoint.getName());
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    packetFactory = new PacketFactory();
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
    waitingMessages = new ConcurrentLinkedQueue<>();

    int maxQueued = endPoint.getConfig().getProperties().getIntProperty("MaxQueueSize", 10);
    SessionContext sessionContext = new SessionContext("SemTech-Gateway:"+endPoint.getName(), this);
    sessionContext.setPersistentSession(false);
    sessionContext.setResetState(true);
    sessionContext.setReceiveMaximum(maxQueued);
    try {
      session = SessionManager.getInstance().createAsync(sessionContext, this).get();
      String inboundTopicName = endPoint.getConfig().getProperties().getProperty("inbound", "/semtech/inbound");
      String outboundTopicName = endPoint.getConfig().getProperties().getProperty("outbound", "/semtech/outbound");
      gatewayManager = new GatewayManager(session, inboundTopicName, outboundTopicName, maxQueued);
    } catch (InterruptedException | ExecutionException e) {
      if(Thread.currentThread().isInterrupted()){
        endPoint.close();
        Thread.currentThread().interrupt();
      }
      throw new IOException(e.getMessage());
    }
  }

  public void close() throws IOException {
    logger.log(ServerLogMessages.SEMTECH_CLOSE, endPoint.toString());
    SessionManager.getInstance().close(session,true);
    endPoint.close();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    logger.log(ServerLogMessages.SEMTECH_QUEUE_MESSAGE, messageEvent.getMessage());
    waitingMessages.offer(messageEvent);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    SemTechPacket semTechPacket = packetFactory.parse(packet);
    if(semTechPacket != null) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, semTechPacket);
      PacketHandler.getInstance().handle(this, semTechPacket);
    }
    return true;
  }

  public void sendPacket(@NotNull @NonNull SemTechPacket semTechPacket) {
    sentMessageAverages.increment();
    selectorTask.push(semTechPacket);
    logger.log(ServerLogMessages.SEMTECH_SENDING_PACKET, semTechPacket);
    sentMessage();
  }

  public @NotNull @NonNull Queue<MessageEvent> getWaitingMessages() {
    return waitingMessages;
  }

  public Logger getLogger(){
    return logger;
  }
  @Override
  public String getName() {
    return "semtech";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "2";
  }


}
