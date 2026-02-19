/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.tak.transport;

import io.mapsmessaging.network.protocol.impl.tak.TakExtensionConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class TakMulticastTransport {

  private final TakExtensionConfig config;
  private final ReentrantLock sendLock;

  private MulticastSocket socket;
  private InetAddress groupAddress;

  public TakMulticastTransport(TakExtensionConfig config) {
    this.config = config;
    this.sendLock = new ReentrantLock();
  }

  public synchronized void start() throws IOException {
    close();
    groupAddress = InetAddress.getByName(config.getMulticastGroup());
    socket = new MulticastSocket(config.getMulticastPort());
    socket.setReuseAddress(true);
    socket.setSoTimeout(1000);
    socket.setTimeToLive(config.getMulticastTtl());

    NetworkInterface networkInterface = resolveInterface();
    if (networkInterface != null) {
      socket.setNetworkInterface(networkInterface);
      socket.joinGroup(new InetSocketAddress(groupAddress, config.getMulticastPort()), networkInterface);
    } else {
      socket.joinGroup(groupAddress);
    }
  }

  public Optional<byte[]> read() throws IOException {
    MulticastSocket current = getSocket();
    byte[] buffer = new byte[config.getMulticastReadBufferBytes()];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    try {
      current.receive(packet);
      if (packet.getLength() > config.getMaxPayloadBytes()) {
        return Optional.empty();
      }
      byte[] copy = new byte[packet.getLength()];
      System.arraycopy(packet.getData(), packet.getOffset(), copy, 0, packet.getLength());
      return Optional.of(copy);
    } catch (SocketTimeoutException ignored) {
      return Optional.empty();
    }
  }

  public void send(byte[] payload) throws IOException {
    MulticastSocket current = getSocket();
    DatagramPacket packet = new DatagramPacket(payload, payload.length, groupAddress, config.getMulticastPort());
    sendLock.lock();
    try {
      current.send(packet);
    } finally {
      sendLock.unlock();
    }
  }

  public synchronized void close() throws IOException {
    if (socket == null) {
      return;
    }
    IOException deferred = null;
    try {
      NetworkInterface networkInterface = resolveInterface();
      if (networkInterface != null && groupAddress != null) {
        socket.leaveGroup(new InetSocketAddress(groupAddress, config.getMulticastPort()), networkInterface);
      } else if (groupAddress != null) {
        socket.leaveGroup(groupAddress);
      }
    } catch (IOException ex) {
      deferred = ex;
    }
    socket.close();
    socket = null;
    groupAddress = null;
    if (deferred != null) {
      throw deferred;
    }
  }

  private synchronized MulticastSocket getSocket() throws IOException {
    if (socket == null || socket.isClosed()) {
      throw new IOException("TAK multicast transport is not started");
    }
    return socket;
  }

  private NetworkInterface resolveInterface() throws IOException {
    String iface = config.getMulticastInterface();
    if (iface == null || iface.isBlank()) {
      return null;
    }
    NetworkInterface named = NetworkInterface.getByName(iface);
    if (named != null) {
      return named;
    }
    InetAddress address = InetAddress.getByName(iface);
    return NetworkInterface.getByInetAddress(address);
  }
}
