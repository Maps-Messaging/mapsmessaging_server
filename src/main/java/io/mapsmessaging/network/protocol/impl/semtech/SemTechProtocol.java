package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
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
  private final EndPoint endPoint;
  private final PacketFactory packetFactory;
  private final ProtocolMessageTransformation transformation;
  private final Session session;

  @Getter
  private final Destination inbound;
  @Getter
  private final SubscribedEventManager outbound;

  private final Queue<MessageEvent> waitingMessages;

  protected SemTechProtocol(@NonNull @NotNull EndPoint endPoint) throws IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger("SemTech Protocol on " + endPoint.getName());
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    packetFactory = new PacketFactory();
    this.endPoint = endPoint;
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
    waitingMessages = new ConcurrentLinkedQueue<>();
    SessionContext sessionContext = new SessionContext("SemTech-Gateway", this);
    sessionContext.setPersistentSession(false);
    sessionContext.setResetState(true);
    sessionContext.setReceiveMaximum(10);
    try {
      String inboundTopicName = endPoint.getConfig().getProperties().getProperty("InBound", "/semtech/inbound");
      String outboundTopicName = endPoint.getConfig().getProperties().getProperty("OutBound", "/semtech/outbound");
      session = SessionManager.getInstance().createAsync(sessionContext, this).get();
      inbound = session.findDestination(inboundTopicName, DestinationType.TOPIC).get();

      SubscriptionContext subscriptionContext = new SubscriptionContext();
      subscriptionContext.setAlias(outboundTopicName);
      subscriptionContext.setAcknowledgementController(ClientAcknowledgement.AUTO);
      subscriptionContext.setReceiveMaximum(10);
      outbound = session.addSubscription(subscriptionContext);
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e.getMessage());
    }
  }

  public void close() throws IOException {
    SessionManager.getInstance().close(session,true);
    endPoint.close();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    waitingMessages.offer(messageEvent);
    System.err.println("Received outbound message : "+messageEvent.getMessage());
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    SemTechPacket semTechPacket = packetFactory.parse(packet);
    PacketHandler.getInstance().handle(this, semTechPacket);
    return true;
  }

  public void sendPacket(@NotNull @NonNull SemTechPacket packet) {
    sentMessageAverages.increment();
    selectorTask.push(packet);
    logger.log(ServerLogMessages.PUSH_WRITE, packet);
    sentMessage();
  }

  public @NotNull @NonNull Queue<MessageEvent> getWaitingMessages() {
    return waitingMessages;
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
