package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.dtls.DTLSSessionManager;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import lombok.Getter;
import lombok.Setter;

public class StateEngine {

  private @Getter final SSLEngine sslEngine;
  private @Getter final SocketAddress clientId;
  private @Getter @Setter Selectable selectableTask;
  private @Getter @Setter Selectable writeTask;
  private @Getter @Setter State currentState;

  private final DTLSSessionManager manager;
  private final Queue<Packet> inboundQueue;

  public StateEngine(SocketAddress clientId, SSLEngine engine, DTLSSessionManager manager){
    this.sslEngine = engine;
    this.manager = manager;
    this.clientId = clientId;
    inboundQueue = new ConcurrentLinkedQueue<>();
    currentState = new HandShakeState(this);
  }

  public void start() throws SSLException {
    sslEngine.setUseClientMode(false);
    sslEngine.beginHandshake();
  }

  public int toNetwork(Packet packet) throws IOException {
    System.err.println(currentState.getClass().getName()+" >> Decrypted >> To Network::"+packet);
    return currentState.outbound(packet);
  }

  public int fromNetwork(Packet packet) throws IOException{
    System.err.println(currentState.getClass().getName()+" >> Encrypted >> From Network::"+packet);
    return currentState.inbound(packet);
  }

  public int send(Packet packet) throws IOException {
    System.err.println(currentState.getClass().getName()+" >> Encrypted >> To Network::"+packet);
    return manager.sendPacket(packet);
  }

  public int read(Packet packet){
    Packet in = inboundQueue.poll();
    if(in != null){
      packet.put(in);
      packet.setFromAddress(in.getFromAddress());
      return in.position();
    }
    return 0;
  }

  void pushToInBoundQueue(Packet packet){
    System.err.println(currentState.getClass().getName()+" >> Decrypted >> From Network >> "+packet);
    inboundQueue.add(packet);
    selectableTask.selected(selectableTask, null, SelectionKey.OP_READ);
  }

}
