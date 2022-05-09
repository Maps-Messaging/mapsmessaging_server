package io.mapsmessaging.network.io.impl.dtls;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DTLSEndPoint extends EndPoint {

  private final DTLSSessionManager manager;
  private final SocketAddress clientId;
  private final SSLEngine sslEngine;
  private final Queue<Packet> inboundQueue;

  private volatile Selectable selectableTask;

  public DTLSEndPoint(DTLSSessionManager manager, long id, SocketAddress clientId, EndPointServer server, SSLEngine sslEngine) throws IOException {
    super(id, server);
    this.sslEngine = sslEngine;
    this.manager = manager;
    this.clientId = clientId;
    inboundQueue = new ConcurrentLinkedQueue<>();
    sslEngine.setUseClientMode(false);

    sslEngine.beginHandshake();
  }

  public void close(){
    manager.close(clientId.toString());
  }

  public String getClientId(){
    return clientId.toString();
  }

  @Override
  public String getProtocol() {
    return "DTLS";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    packet.setFromAddress(clientId);
    return manager.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    Packet in = inboundQueue.poll();
    if(in != null){
      packet.put(in);
      packet.setFromAddress(in.getFromAddress());
      return in.position();
    }
    return 0;
  }

  protected int processPacket(@NonNull @NotNull Packet packet) throws IOException{
    if(sslEngine.getHandshakeStatus() == NOT_HANDSHAKING) {
      Packet networkOut = new Packet(2048, false);
      SSLEngineResult rs = sslEngine.unwrap(packet.getRawBuffer(), networkOut.getRawBuffer());
      if(rs.getStatus() == Status.OK) {
        networkOut.flip();
        networkOut.setFromAddress(packet.getFromAddress());
        inboundQueue.add(networkOut);
        selectableTask.selected(selectableTask, null, 1);
      }
      return packet.position();
    }
    else{
      handshake(packet);
      while(sslEngine.getHandshakeStatus() != NEED_UNWRAP) {
        handshake(packet);
        if(sslEngine.getHandshakeStatus() == NOT_HANDSHAKING){
          return 0;
        }
      }
    }
    return 0;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    if((selectionKey & SelectionKey.OP_READ) != 0) selectableTask = runner;
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return server.getConfig().getAuthConfig();
  }

  @Override
  public String getName() {
    return "DTLS";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(DTLSEndPoint.class.getName() + "_" + getId());
  }

  void runDelegatedTasks() throws IOException {
    Runnable runnable;
    while ((runnable = sslEngine.getDelegatedTask()) != null) {
      runnable.run();
    }
    SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      throw new IOException("handshake shouldn't need additional tasks");
    }
  }

  void handshake(Packet packet) throws IOException {
    SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
        hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {
      ByteBuffer iApp = ByteBuffer.allocate(1024);
      SSLEngineResult r = sslEngine.unwrap(packet.getRawBuffer(), iApp);
      SSLEngineResult.Status rs = r.getStatus();
      hs = r.getHandshakeStatus();
      if (rs == SSLEngineResult.Status.OK) {
        //
      } else if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
        // the client maximum fragment size config does not work?
        throw new IOException("Buffer overflow: incorrect client maximum fragment size");
      } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
        // bad packet, or the client maximum fragment size
        // config does not work?
        if (hs != NOT_HANDSHAKING) {
          throw new IOException("Buffer underflow: incorrect client maximum fragment size");
        } // otherwise, ignore this packet
      } else if (rs == SSLEngineResult.Status.CLOSED) {
        throw new IOException("SSL engine closed, handshake status is " + hs);
      } else {
        throw new IOException("Can't reach here, result is " + rs);
      }

      if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
        return;
      }
    }

    if (hs == NEED_WRAP) {
      List<Packet> packets = new ArrayList<>();
      boolean finished = produceHandshakePackets(packets);
      for (Packet p : packets) {
        p.setFromAddress(clientId);
        this.manager.sendPacket(p);
        p.clear();
      }
      hs = sslEngine.getHandshakeStatus();
    }

    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      runDelegatedTasks();
    } else if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
      throw new IOException("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
    }
  }

  // produce handshake packets
  boolean produceHandshakePackets( List<Packet> packets) throws IOException {
    ByteBuffer oNet = ByteBuffer.allocate(32768);
    ByteBuffer oApp = ByteBuffer.allocate(0);
    SSLEngineResult r = sslEngine.wrap(oApp, oNet);
    oNet.flip();

    SSLEngineResult.Status rs = r.getStatus();
    SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
    if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
      // the client maximum fragment size config does not work?
      throw new IOException("Buffer overflow: incorrect server maximum fragment size");
    } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
      // bad packet, or the client maximum fragment size
      // config does not work?
      if (hs != NOT_HANDSHAKING) {
        throw new IOException("Buffer underflow: incorrect server maximum fragment size");
      } // otherwise, ignore this packet
    } else if (rs == SSLEngineResult.Status.CLOSED) {
      throw new IOException("SSLEngine has closed");
    } else if (rs == SSLEngineResult.Status.OK) {
      // OK
    } else {
      throw new IOException("Can't reach here, result is " + rs);
    }

    // SSLEngineResult.Status.OK:
    if (oNet.hasRemaining()) {
      Packet packet = new Packet(oNet);
      packets.add(packet);
    }

    if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
      return true;
    }

    boolean endInnerLoop = false;
    SSLEngineResult.HandshakeStatus nhs = hs;
    while (!endInnerLoop) {
      if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
        runDelegatedTasks();
      } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
          nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
          nhs == NOT_HANDSHAKING) {
        endInnerLoop = true;
      } else if (nhs == NEED_WRAP) {
        endInnerLoop = true;
      } else if (nhs == SSLEngineResult.HandshakeStatus.FINISHED) {
        throw new IOException("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
      } else {
        throw new IOException("Can't reach here, handshake status is " + nhs);
      }
      nhs = sslEngine.getHandshakeStatus();
    }
    return false;
  }

}
