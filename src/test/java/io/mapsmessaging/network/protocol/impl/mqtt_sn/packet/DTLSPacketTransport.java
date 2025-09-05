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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.*;

public class DTLSPacketTransport implements PacketTransport {

  private static final int MAXIMUM_PACKET_SIZE = 8192;
  private static final String pathToStores = "";
  private static final String keyStoreFile = "my-keystore.jks";
  private static final String trustStoreFile = "my-truststore.jks";
  private static final String keyFilename = pathToStores + keyStoreFile;
  private static final String trustFilename = pathToStores + trustStoreFile;

  private final UDPPacketTransport udpPacketTransport;
  private final SSLEngine sslEngine;

  public DTLSPacketTransport(InetSocketAddress clientAddress, InetSocketAddress serverAddress) throws Exception {
    udpPacketTransport = new UDPPacketTransport(clientAddress, serverAddress);
    sslEngine = createSSLEngine();
    handshake();
  }


  @Override
  public int readPacket(Packet packet) throws IOException {
    int pos = packet.position();
    Packet pkt = new Packet(MAXIMUM_PACKET_SIZE, false);
    udpPacketTransport.readPacket(pkt);
    pkt.flip();
    SSLEngineResult rs = sslEngine.unwrap(pkt.getRawBuffer(), packet.getRawBuffer());
    return packet.position()- pos;
  }

  @Override
  public int sendPacket(Packet packet) throws Exception {
    List<Packet> packets = produceApplicationPackets(packet);
    for(Packet pkt:packets){
      udpPacketTransport.sendPacket(pkt);
    }
    return 0;
  }

  @Override
  public void close() throws IOException {
    udpPacketTransport.close();
  }

  List<Packet> produceApplicationPackets(Packet source) throws Exception {
    List<Packet> packets = new ArrayList<>();
    ByteBuffer appNet = ByteBuffer.allocate(32768);
    SSLEngineResult r = sslEngine.wrap(source.getRawBuffer(), appNet);
    appNet.flip();

    SSLEngineResult.Status rs = r.getStatus();
    if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
      // the client maximum fragment size config does not work?
      throw new Exception("Buffer overflow: incorrect server maximum fragment size");
    } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
      // unlikely
      throw new Exception("Buffer underflow during wraping");
    } else if (rs == SSLEngineResult.Status.CLOSED) {
      throw new Exception("SSLEngine has closed");
    } else if (rs == SSLEngineResult.Status.OK) {
      // OK
    } else {
      throw new Exception("Can't reach here, result is " + rs);
    }

    // SSLEngineResult.Status.OK:
    if (appNet.hasRemaining()) {
      byte[] ba = new byte[appNet.remaining()];
      appNet.get(ba);
      ByteBuffer bb = ByteBuffer.wrap(ba);
      Packet packet = new Packet(bb);
      packets.add(packet);
    }
    return packets;
  }
  SSLEngine createSSLEngine()
      throws IOException, KeyStoreException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
    SSLContext context = getDTLSContext();
    SSLEngine engine = context.createSSLEngine();

    SSLParameters paras = engine.getSSLParameters();
    paras.setMaximumPacketSize(MAXIMUM_PACKET_SIZE);

    engine.setUseClientMode(true);
    engine.setSSLParameters(paras);

    return engine;
  }

  SSLContext getDTLSContext() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
    KeyStore ks = KeyStore.getInstance("JKS");
    KeyStore ts = KeyStore.getInstance("JKS");

    char[] passphrase = "password".toCharArray();

    try (FileInputStream fis = new FileInputStream(keyFilename)) {
      ks.load(fis, passphrase);
    }

    try (FileInputStream fis = new FileInputStream(trustFilename)) {
      ts.load(fis, passphrase);
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, passphrase);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ts);

    SSLContext sslCtx = SSLContext.getInstance("DTLS");
    sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    return sslCtx;
  }

  void handshake() throws Exception {

    boolean endLoops = false;
    int loops = 100;
    sslEngine.beginHandshake();
    while (!endLoops ) {

      if (--loops < 0) {
        throw new RuntimeException("Too much loops to produce handshake packets");
      }

      SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
      if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP || hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {
        Packet iNet = new Packet(MAXIMUM_PACKET_SIZE, false);
        ByteBuffer iApp;
        if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
          try {
            udpPacketTransport.readPacket(iNet);
          } catch (SocketTimeoutException ste) {
            List<Packet> packets = new ArrayList<>();
            boolean finished = onReceiveTimeout(packets);
            for (Packet p : packets) {
              udpPacketTransport.sendPacket(p);
            }

            if (finished) {
              endLoops = true;
            }
            continue;
          }
          iApp = ByteBuffer.allocate(MAXIMUM_PACKET_SIZE);
        } else {
          iApp = ByteBuffer.allocate(MAXIMUM_PACKET_SIZE);
        }
        iNet.flip();
        SSLEngineResult r = sslEngine.unwrap(iNet.getRawBuffer(), iApp);
        SSLEngineResult.Status rs = r.getStatus();
        hs = r.getHandshakeStatus();
        if (rs == SSLEngineResult.Status.OK) {
          // OK
        } else if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {

          // the client maximum fragment size config does not work?
          throw new Exception("Buffer overflow: incorrect client maximum fragment size");
        } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
          // bad packet, or the client maximum fragment size
          // config does not work?
          if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            throw new Exception("Buffer underflow: incorrect client maximum fragment size");
          } // otherwise, ignore this packet
        } else if (rs == SSLEngineResult.Status.CLOSED) {
          throw new Exception("SSL engine closed, handshake status is " + hs);
        } else {
          throw new Exception("Can't reach here, result is " + rs);
        }

        if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
          endLoops = true;
        }
      } else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
        List<Packet> packets = new ArrayList<>();
        boolean finished = produceHandshakePackets(packets);

        for (Packet p : packets) {
          udpPacketTransport.sendPacket(p);
        }

        if (finished) {
          endLoops = true;
        }
      } else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
        runDelegatedTasks(sslEngine);
      } else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
        endLoops = true;
      } else if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
        throw new Exception("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
      } else {
        throw new Exception("Can't reach here, handshake status is " + hs);
      }
    }

    SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();

    if (sslEngine.getHandshakeSession() != null) {
      throw new Exception("Handshake finished, but handshake session is not null");
    }

    SSLSession session = sslEngine.getSession();
    if (session == null) {
      throw new Exception("Handshake finished, but session is null");
    }

    // handshake status should be NOT_HANDSHAKING
    //
    // According to the spec, SSLEngine.getHandshakeStatus() can't
    // return FINISHED.
    if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
      throw new Exception("Unexpected handshake status " + hs);
    }
  }
  void runDelegatedTasks(SSLEngine engine) throws Exception {
    Runnable runnable;
    while ((runnable = engine.getDelegatedTask()) != null) {
      runnable.run();
    }

    SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      throw new Exception("handshake shouldn't need additional tasks");
    }
  }

  // retransmission if timeout
  boolean onReceiveTimeout(List<Packet> packets) throws Exception {

    SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
      return false;
    } else {
      // retransmission of handshake messages
      return produceHandshakePackets(packets);
    }
  }

  boolean produceHandshakePackets(List<Packet> packets) throws Exception {

    boolean endLoops = false;
    int loops = 10 / 2;
    while (!endLoops) {

      if (--loops < 0) {
        throw new RuntimeException("Too much loops to produce handshake packets");
      }

      ByteBuffer oNet = ByteBuffer.allocate(32768);
      ByteBuffer oApp = ByteBuffer.allocate(0);
      SSLEngineResult r = sslEngine.wrap(oApp, oNet);
      oNet.flip();

      SSLEngineResult.Status rs = r.getStatus();
      SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
      if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
        // the client maximum fragment size config does not work?
        throw new Exception("Buffer overflow: incorrect server maximum fragment size");
      } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
        // bad packet, or the client maximum fragment size
        // config does not work?
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
          throw new Exception("Buffer underflow: incorrect server maximum fragment size");
        } // otherwise, ignore this packet
      } else if (rs == SSLEngineResult.Status.CLOSED) {
        throw new Exception("SSLEngine has closed");
      } else if (rs == SSLEngineResult.Status.OK) {
        // OK
      } else {
        throw new Exception("Can't reach here, result is " + rs);
      }

      // SSLEngineResult.Status.OK:
      if (oNet.hasRemaining()) {
        byte[] ba = new byte[oNet.remaining()];
        oNet.get(ba);
        Packet packet = createHandshakePacket(ba);
        packets.add(packet);
      }

      if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
        return true;
      }

      boolean endInnerLoop = false;
      SSLEngineResult.HandshakeStatus nhs = hs;
      while (!endInnerLoop) {
        if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
          runDelegatedTasks(sslEngine);
        } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
            nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
            nhs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
          endInnerLoop = true;
          endLoops = true;
        } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
          endInnerLoop = true;
        } else if (nhs == SSLEngineResult.HandshakeStatus.FINISHED) {
          throw new Exception("Unexpected status, SSLEngine.getHandshakeStatus() shouldn't return FINISHED");
        } else {
          throw new Exception("Can't reach here, handshake status is " + nhs);
        }
        nhs = sslEngine.getHandshakeStatus();
      }
    }

    return false;
  }

  Packet createHandshakePacket(byte[] ba) {
    ByteBuffer bb = ByteBuffer.wrap(ba);
    return new Packet(bb);
  }

}
