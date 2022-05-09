package io.mapsmessaging.network.io.impl.dtls;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;

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
import java.util.concurrent.FutureTask;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

public class DTLSEndPoint extends EndPoint {

  private final DTLSSessionManager manager;
  private final SocketAddress clientId;
  private final SSLEngine sslEngine;
  private final Packet networkIn;
  private final Packet networkOut;

  public DTLSEndPoint(DTLSSessionManager manager, long id, SocketAddress clientId, EndPointServer server, SSLEngine sslEngine) throws IOException {
    super(id, server);
    this.sslEngine = sslEngine;
    this.manager = manager;
    this.clientId = clientId;
    networkIn = new Packet(1024, false);
    networkOut = new Packet(1024, false);
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
    if(sslEngine.getHandshakeStatus() == FINISHED) {
      SSLEngineResult rs = sslEngine.unwrap(networkIn.getRawBuffer(), packet.getRawBuffer());
      return packet.position();
    }
    else{
      handshake(packet);
      while(sslEngine.getHandshakeStatus() != NEED_UNWRAP) {
        handshake(packet);
      }
    }
    return 0;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
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
      log("Receive DTLS records, handshake status is " + hs);
      ByteBuffer iApp = ByteBuffer.allocate(1024);
      System.err.println("Processing "+packet);
      SSLEngineResult r = sslEngine.unwrap(packet.getRawBuffer(), iApp);
      SSLEngineResult.Status rs = r.getStatus();
      hs = r.getHandshakeStatus();
      if (rs == SSLEngineResult.Status.OK) {
        //
      } else if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
        log("BUFFER_OVERFLOW, handshake status is " + hs);

        // the client maximum fragment size config does not work?
        throw new IOException("Buffer overflow: incorrect client maximum fragment size");
      } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
        log("BUFFER_UNDERFLOW, handshake status is " + hs);

        // bad packet, or the client maximum fragment size
        // config does not work?
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
          throw new IOException("Buffer underflow: incorrect client maximum fragment size");
        } // otherwise, ignore this packet
      } else if (rs == SSLEngineResult.Status.CLOSED) {
        throw new IOException("SSL engine closed, handshake status is " + hs);
      } else {
        throw new IOException("Can't reach here, result is " + rs);
      }

      if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
        log("Handshake status is FINISHED, finish the loop");
      }
    }

    if (hs == NEED_WRAP) {
      List<Packet> packets = new ArrayList<>();
      boolean finished = produceHandshakePackets(packets);

      log("Produced " + packets.size() + " packets");
      for (Packet p : packets) {
        p.setFromAddress(clientId);
        this.manager.sendPacket(p);
        p.clear();
      }
      if (finished) {
        log("Handshake status is FINISHED after producing handshake packets, finish the loop");
      }
      hs = sslEngine.getHandshakeStatus();
    }

    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      runDelegatedTasks();
    } else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
      log("Handshake status is NOT_HANDSHAKING, finish the loop");
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
    log( "----produce handshake packet(" + rs + ", " + hs + ")----");
    if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
      // the client maximum fragment size config does not work?
      throw new IOException("Buffer overflow: incorrect server maximum fragment size");
    } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
      log("Produce handshake packets: BUFFER_UNDERFLOW occured");
      log("Produce handshake packets: Handshake status: " + hs);
      // bad packet, or the client maximum fragment size
      // config does not work?
      if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
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
      log("Produce handshake packets: Handshake status is FINISHED, finish the loop");
      return true;
    }

    boolean endInnerLoop = false;
    SSLEngineResult.HandshakeStatus nhs = hs;
    while (!endInnerLoop) {
      if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
        runDelegatedTasks();
      } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
          nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
          nhs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
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

  private void log(String... msgs) {
    StringBuilder sb = new StringBuilder();
    for(String msg:msgs) {
      sb.append(msg).append(" ");
    }
    System.err.println(sb);
  }
}
