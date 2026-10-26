/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.test.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Test-only, protocol-independent UDP forwarder with deterministic packet faults.
 *
 * <p>Rules are FIFO within each direction and shared by all clients of this proxy.
 * Each datagram consumes at most one rule. Reverse batches retain the original
 * destination and socket for each packet, so reordering never mixes responses
 * between clients. A partial reverse batch is forwarded in arrival order after
 * its deadline; the rule is then retired. The deadline starts on the first match.
 *
 * <p>All sockets and rules are owned by this instance. A JUnit class can create
 * the proxy in {@code @BeforeAll} and close it in {@code @AfterAll}, clearing
 * fault rules between tests using {@link #resetRules()}. The proxy listens only
 * on the loopback interface; no global test-suite startup is required.
 */
public final class UdpFaultProxy implements AutoCloseable {

  public enum Direction {
    CLIENT_TO_SERVER,
    SERVER_TO_CLIENT
  }

  public record Stats(
      long clientToServerReceived,
      long serverToClientReceived,
      long clientToServerForwarded,
      long serverToClientForwarded,
      long dropped,
      long modified,
      long reversed,
      long partialReverseFlushes,
      long invalidMutations,
      long ioFailures,
      int activePeers,
      int pendingRules) {
  }

  private enum FaultType {
    DROP,
    REVERSE_PACKETS,
    INSERT_BYTES,
    REVERSE_BYTES
  }

  private static final int MAX_DATAGRAM_SIZE = 65_507;
  private static final int MAX_BATCH_PACKETS = 1_024;
  private static final int MAX_CLIENT_PEERS = 64;
  private static final int MAX_PENDING_RULES_PER_DIRECTION = 32;
  private static final int SOCKET_POLL_MILLIS = 200;

  private final Object lock = new Object();
  private final InetSocketAddress target;
  private final DatagramSocket listener;
  private final Thread listenerThread;
  private final ScheduledExecutorService scheduler;
  private final Map<SocketAddress, Peer> peers = new HashMap<>();
  private final EnumMap<Direction, ArrayDeque<Rule>> rules = new EnumMap<>(Direction.class);
  private boolean closed;
  private long c2sReceived;
  private long s2cReceived;
  private long c2sForwarded;
  private long s2cForwarded;
  private long dropped;
  private long modified;
  private long reversed;
  private long partialReverseFlushes;
  private long invalidMutations;
  private long ioFailures;

  /** Create a proxy on a known port; port 0 requests an ephemeral test port. */
  public UdpFaultProxy(int listenPort, InetSocketAddress target) throws SocketException {
    if (listenPort < 0 || listenPort > 65_535) {
      throw new IllegalArgumentException("Invalid listen port: " + listenPort);
    }
    this.target = Objects.requireNonNull(target, "target");
    if (target.isUnresolved() || target.getPort() == 0 || !target.getAddress().isLoopbackAddress()) {
      throw new IllegalArgumentException("Target must be a resolved, nonzero loopback UDP endpoint");
    }
    listener = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), listenPort));
    try {
      listener.setSoTimeout(SOCKET_POLL_MILLIS);
    } catch (SocketException exception) {
      listener.close();
      throw exception;
    }
    for (Direction direction : Direction.values()) {
      rules.put(direction, new ArrayDeque<>());
    }
    ThreadFactory schedulerThreads = runnable -> {
      Thread thread = new Thread(runnable, "udp-fault-proxy-timer-" + listener.getLocalPort());
      thread.setDaemon(true);
      return thread;
    };
    scheduler = Executors.newSingleThreadScheduledExecutor(schedulerThreads);
    listenerThread = new Thread(this::readClients, "udp-fault-proxy-listener-" + listener.getLocalPort());
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  public int getListenPort() {
    return listener.getLocalPort();
  }

  public InetSocketAddress getListenAddress() {
    return new InetSocketAddress(listener.getLocalAddress(), listener.getLocalPort());
  }

  public void dropNext(Direction direction, int count) {
    addRule(direction, new Rule(FaultType.DROP, count, 0, 0, null, null));
  }

  /** Reverse arrival order for a batch, or flush a partial batch unchanged at timeout. */
  public void reverseNext(Direction direction, int count, Duration timeout) {
    Objects.requireNonNull(timeout, "timeout");
    if (timeout.isNegative() || timeout.isZero() || timeout.toMillis() <= 0) {
      throw new IllegalArgumentException("Reverse timeout must be at least 1 ms");
    }
    addRule(direction, new Rule(FaultType.REVERSE_PACKETS, count, 0, 0, null, timeout));
  }

  /** Insert bytes before the byte at offset (offset == datagram length appends). */
  public void injectNext(Direction direction, int count, int offset, byte[] bytes) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset cannot be negative");
    }
    byte[] copy = Objects.requireNonNull(bytes, "bytes").clone();
    if (copy.length == 0) {
      throw new IllegalArgumentException("Inserted bytes must not be empty");
    }
    addRule(direction, new Rule(FaultType.INSERT_BYTES, count, offset, 0, copy, null));
  }

  /** Reverse a zero-based, start-inclusive, end-exclusive payload byte range. */
  public void reverseRangeNext(Direction direction, int count, int startInclusive, int endExclusive) {
    if (startInclusive < 0 || endExclusive <= startInclusive) {
      throw new IllegalArgumentException("Invalid byte range");
    }
    addRule(direction, new Rule(FaultType.REVERSE_BYTES, count, startInclusive, endExclusive, null, null));
  }

  /** Stop applying queued rules. Buffered reverse packets are forwarded unchanged. */
  public void resetRules() {
    synchronized (lock) {
      ensureOpen();
      for (ArrayDeque<Rule> queue : rules.values()) {
        while (!queue.isEmpty()) {
          Rule rule = queue.removeFirst();
          cancelTimer(rule);
          forwardBuffered(rule, false);
        }
      }
    }
  }

  public Stats stats() {
    synchronized (lock) {
      int ruleCount = 0;
      for (ArrayDeque<Rule> queue : rules.values()) {
        ruleCount += queue.size();
      }
      return new Stats(c2sReceived, s2cReceived, c2sForwarded, s2cForwarded,
          dropped, modified, reversed, partialReverseFlushes, invalidMutations,
          ioFailures, peers.size(), ruleCount);
    }
  }

  private void addRule(Direction direction, Rule rule) {
    Objects.requireNonNull(direction, "direction");
    if (rule.remaining <= 0 || rule.remaining > MAX_BATCH_PACKETS) {
      throw new IllegalArgumentException("Rule count must be 1.." + MAX_BATCH_PACKETS);
    }
    synchronized (lock) {
      ensureOpen();
      ArrayDeque<Rule> queue = rules.get(direction);
      if (queue.size() >= MAX_PENDING_RULES_PER_DIRECTION) {
        throw new IllegalStateException("Too many pending UDP fault rules");
      }
      queue.addLast(rule);
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("UDP proxy has been closed");
    }
  }

  private void readClients() {
    receiveLoop(listener, null);
  }

  private void receiveLoop(DatagramSocket socket, Peer fixedPeer) {
    while (!socket.isClosed()) {
      byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
      DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
      try {
        socket.receive(datagram);
        byte[] data = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getOffset() + datagram.getLength());
        synchronized (lock) {
          if (closed) {
            return;
          }
          if (fixedPeer == null) {
            c2sReceived++;
            Peer peer = peers.get(datagram.getSocketAddress());
            if (peer == null) {
              if (peers.size() >= MAX_CLIENT_PEERS) {
                dropped++;
                continue;
              }
              peer = createPeer(datagram.getSocketAddress());
              peers.put(datagram.getSocketAddress(), peer);
              peer.thread.start();
            }
            process(new Envelope(data, peer.socket, target, Direction.CLIENT_TO_SERVER));
          } else {
            s2cReceived++;
            process(new Envelope(data, listener, fixedPeer.clientAddress, Direction.SERVER_TO_CLIENT));
          }
        }
      } catch (SocketTimeoutException ignored) {
        // Periodic poll permits deterministic shutdown.
      } catch (SocketException exception) {
        if (!socket.isClosed()) {
          countIoFailure();
        }
        return;
      } catch (IOException exception) {
        countIoFailure();
      }
    }
  }

  private Peer createPeer(SocketAddress client) throws SocketException {
    DatagramSocket upstream = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
    try {
      upstream.connect(target);
      upstream.setSoTimeout(SOCKET_POLL_MILLIS);
    } catch (SocketException exception) {
      upstream.close();
      throw exception;
    }
    Peer peer = new Peer(client, upstream);
    peer.thread = new Thread(() -> receiveLoop(upstream, peer), "udp-fault-proxy-peer-" + upstream.getLocalPort());
    peer.thread.setDaemon(true);
    return peer;
  }

  private void process(Envelope packet) {
    ArrayDeque<Rule> queue = rules.get(packet.direction);
    Rule rule = queue.peekFirst();
    if (rule == null) {
      forward(packet);
      return;
    }

    rule.remaining--;
    switch (rule.type) {
      case DROP:
        dropped++;
        break;
      case INSERT_BYTES:
        if (rule.start > packet.data.length || packet.data.length + rule.bytes.length > MAX_DATAGRAM_SIZE) {
          invalidMutations++;
          dropped++;
        } else {
          byte[] result = new byte[packet.data.length + rule.bytes.length];
          System.arraycopy(packet.data, 0, result, 0, rule.start);
          System.arraycopy(rule.bytes, 0, result, rule.start, rule.bytes.length);
          System.arraycopy(packet.data, rule.start, result, rule.start + rule.bytes.length, packet.data.length - rule.start);
          modified++;
          forward(packet.withData(result));
        }
        break;
      case REVERSE_BYTES:
        if (rule.end > packet.data.length) {
          invalidMutations++;
          dropped++;
        } else {
          byte[] result = packet.data.clone();
          for (int first = rule.start, last = rule.end - 1; first < last; first++, last--) {
            byte value = result[first];
            result[first] = result[last];
            result[last] = value;
          }
          modified++;
          forward(packet.withData(result));
        }
        break;
      case REVERSE_PACKETS:
        rule.buffer.add(packet);
        if (rule.timer == null) {
          rule.timer = scheduler.schedule(() -> expireReverse(packet.direction, rule), rule.timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (rule.remaining == 0) {
          cancelTimer(rule);
          forwardBuffered(rule, true);
        }
        break;
    }

    if (rule.remaining == 0) {
      queue.removeFirst();
    }
  }

  private void expireReverse(Direction direction, Rule rule) {
    synchronized (lock) {
      if (closed || rules.get(direction).peekFirst() != rule) {
        return;
      }
      rules.get(direction).removeFirst();
      partialReverseFlushes++;
      forwardBuffered(rule, false);
    }
  }

  private void forwardBuffered(Rule rule, boolean reverse) {
    if (reverse && rule.buffer.size() > 1) {
      reversed += rule.buffer.size();
    }
    if (reverse) {
      for (int index = rule.buffer.size() - 1; index >= 0; index--) {
        forward(rule.buffer.get(index));
      }
    } else {
      for (Envelope packet : rule.buffer) {
        forward(packet);
      }
    }
    rule.buffer.clear();
  }

  private void forward(Envelope packet) {
    try {
      DatagramPacket datagram = new DatagramPacket(packet.data, packet.data.length, packet.destination);
      packet.socket.send(datagram);
      if (packet.direction == Direction.CLIENT_TO_SERVER) {
        c2sForwarded++;
      } else {
        s2cForwarded++;
      }
    } catch (IOException exception) {
      ioFailures++;
    }
  }

  private void countIoFailure() {
    synchronized (lock) {
      if (!closed) {
        ioFailures++;
      }
    }
  }

  private void cancelTimer(Rule rule) {
    if (rule.timer != null) {
      rule.timer.cancel(false);
      rule.timer = null;
    }
  }

  @Override
  public void close() {
    List<Thread> threads = new ArrayList<>();
    synchronized (lock) {
      if (closed) {
        return;
      }
      closed = true;
      listener.close();
      threads.add(listenerThread);
      for (Peer peer : peers.values()) {
        peer.socket.close();
        threads.add(peer.thread);
      }
      for (ArrayDeque<Rule> queue : rules.values()) {
        for (Rule rule : queue) {
          cancelTimer(rule);
          dropped += rule.buffer.size();
          rule.buffer.clear();
        }
        queue.clear();
      }
    }
    scheduler.shutdownNow();
    for (Thread thread : threads) {
      if (Thread.currentThread() == thread) {
        continue;
      }
      try {
        thread.join(1_000);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private record Envelope(byte[] data, DatagramSocket socket, SocketAddress destination, Direction direction) {
    private Envelope withData(byte[] replacement) {
      return new Envelope(replacement, socket, destination, direction);
    }
  }

  private static final class Peer {
    private final SocketAddress clientAddress;
    private final DatagramSocket socket;
    private Thread thread;

    private Peer(SocketAddress clientAddress, DatagramSocket socket) {
      this.clientAddress = clientAddress;
      this.socket = socket;
    }
  }

  private static final class Rule {
    private final FaultType type;
    private final int start;
    private final int end;
    private final byte[] bytes;
    private final Duration timeout;
    private final List<Envelope> buffer = new ArrayList<>();
    private ScheduledFuture<?> timer;
    private int remaining;

    private Rule(FaultType type, int remaining, int start, int end, byte[] bytes, Duration timeout) {
      this.type = type;
      this.remaining = remaining;
      this.start = start;
      this.end = end;
      this.bytes = bytes;
      this.timeout = timeout;
    }
  }
}
