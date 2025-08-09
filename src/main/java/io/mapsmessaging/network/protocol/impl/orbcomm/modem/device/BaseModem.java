
package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.ModemMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.ModemMessageFactory;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.PositionMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.SendMessageState;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Queue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.mapsmessaging.logging.ServerLogMessages.STOGI_RECEIVED_AT_MESSAGE;
import static io.mapsmessaging.logging.ServerLogMessages.STOGI_SEND_AT_MESSAGE;

/**
 * Base modem (shared pipeline + typed URC sink). Uses external Command and PacketLineQueue.
 */
public class BaseModem {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Consumer<Packet> packetSender;
  private final PacketLineQueue lineQueue = new PacketLineQueue();

  private final Queue<Command> commandQueue = new ArrayDeque<>();
  private final Map<Command, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();
  private final StringBuilder responseBuffer = new StringBuilder();

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "modem-timeouts");
        t.setDaemon(true);
        return t;
      });

  private final long defaultTimeoutMs;

  private volatile boolean tainted = false;
  private Command currentCommand;

  // Typed event sink for parsed messages/URCs
  @Getter
  @Setter
  private Consumer<ModemMessage> messageSink;

  public BaseModem(Consumer<Packet> packetSender) {
    this(packetSender, 5000);
  }

  public BaseModem(Consumer<Packet> packetSender, long defaultTimeoutMs) {
    this.packetSender = Objects.requireNonNull(packetSender, "packetSender");
    this.defaultTimeoutMs = Math.max(1, defaultTimeoutMs);
  }

  // Transport → lines
  public final void onPacket(Packet packet) {
    if (tainted) return;
    lineQueue.feed(packet);
    while (lineQueue.hasLines()) {
      acceptLine(lineQueue.pollLine());
    }
  }

  // ---------- Shared AT ops ----------

  public CompletableFuture<String> sendMoMessage(String name, int priority, int sin, byte[] payload) {
    Objects.requireNonNull(name, "name");
    String b64 = java.util.Base64.getEncoder().encodeToString(payload == null ? new byte[0] : payload);
    String cmd = String.format("AT%%MGRT=\"%s\",0.0,%d,%d,%d,%s", esc(name), priority, sin, b64.length(), b64);
    return sendATCommand(cmd);
  }

  public CompletableFuture<String> listMtMessages() {
    return sendATCommand("AT%MGLS");
  }

  public CompletableFuture<String> fetchMtMessage(long id) {
    return sendATCommand("AT%MGRD=" + id);
  }

  public CompletableFuture<String> ackMtMessage(long id) {
    return sendATCommand("AT%MGRC=" + id);
  }

  public CompletableFuture<String> getParsedPosition() {
    return sendATCommand(parsedPositionCmd());
  }

  public CompletableFuture<String> getNmeaBurst(int seconds) {
    return sendATCommand("AT%GPS=" + Math.max(1, seconds));
  }

  public CompletableFuture<String> setGnssPower(boolean on) {
    return sendATCommand(gnssPowerSetCmd(on));
  }

  public CompletableFuture<String> getGnssPower() {
    return sendATCommand(gnssPowerGetCmd());
  }

  public CompletableFuture<String> getNetworkState() {
    return sendATCommand(networkStateCmd());
  }

  public CompletableFuture<String> getFirmwareId() {
    return sendATCommand("ATI");
  }

  // ---------- Typed MT helpers ----------

  public CompletableFuture<List<Long>> listMtIds() {
    return listMtMessages().thenApply(this::parseMglsIds);
  }

  public CompletableFuture<byte[]> fetchMtPayload(long id) {
    return fetchMtMessage(id).thenApply(this::parseMgrdPayload);
  }

  public CompletableFuture<Void> ackMt(long id) {
    return ackMtMessage(id).thenApply(s -> null);
  }

  // ---------- URC routing ----------

  protected void handleUnsolicitedLine(String line) {
    if (line == null || line.isEmpty()) return;

    // Position
    if (line.startsWith("%GPSPOS:") || line.startsWith("%POSR:")) {
      System.err.println("Received pos: " + line);
      onParsedPosition(line);
      return;
    }

    // MO progress
    if (line.startsWith("%MGRS:")) {
      if (messageSink != null) {
        var st = new SendMessageState(line);
        messageSink.accept(st);
        return;
      }
      onMoProgress(line);
      return;
    }

    // NMEA passthrough
    if (line.startsWith("%GPS:") || line.startsWith("$GP") || line.startsWith("$GN")) {
      onGpsNmea(line);
      return;
    }

    onVendorEvent(line);
  }


  protected void onMoProgress(String mgrsLine) {
  }

  protected void onGpsNmea(String nmeaLine) {
  }

  protected void onParsedPosition(String line) {
  }

  protected void onVendorEvent(String line) {
  }

  // ---------- Dialect hooks ----------

  protected String parsedPositionCmd() {
    return "AT%GPSPOS?";
  }         // OGx default; ODI overrides to "AT%POSR?"

  protected String gnssPowerSetCmd(boolean on) {
    return "AT%GPSP=" + (on ? "1" : "0");
  }

  protected String gnssPowerGetCmd() {
    return "AT%GPSP?";
  }

  protected String networkStateCmd() {
    return "AT%NETSTATE?";
  }         // May be unsupported on ODI

  // ---------- Parsing helpers ----------

  private static final Pattern MGLS_ID = Pattern.compile("^(?:%MGLS:)?\\s*(\\d+)\\b.*");
  private static final Pattern MGRD_PAYLOAD_B64 = Pattern.compile("(?i)payload[:=]\\s*([A-Za-z0-9+/=]+)");

  protected List<Long> parseMglsIds(String body) {
    List<Long> ids = new ArrayList<>();
    if (body == null || body.isBlank()) return ids;
    for (String line : body.split("\\r?\\n")) {
      Matcher m = MGLS_ID.matcher(line.trim());
      if (m.matches()) {
        try {
          ids.add(Long.parseLong(m.group(1)));
        } catch (NumberFormatException ignore) {
        }
      }
    }
    return ids;
  }

  protected byte[] parseMgrdPayload(String body) {
    if (body == null || body.isBlank()) return null;
    Matcher m = MGRD_PAYLOAD_B64.matcher(body);
    if (m.find()) {
      try {
        return java.util.Base64.getDecoder().decode(m.group(1));
      } catch (IllegalArgumentException ignore) {
      }
    }
    String[] lines = body.split("\\r?\\n");
    for (int i = lines.length - 1; i >= 0; i--) {
      String token = lines[i].trim();
      if (looksBase64(token)) {
        try {
          return java.util.Base64.getDecoder().decode(token);
        } catch (IllegalArgumentException ignore) {
        }
      }
    }
    return null;
  }

  private static boolean looksBase64(String s) {
    if (s.length() < 4) return false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
          (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=';
      if (!ok) return false;
    }
    return true;
  }

  // ---------- AT pipeline with hard timeouts ----------

  protected final CompletableFuture<String> sendATCommand(String cmd) {
    return sendATCommand(cmd, defaultTimeoutMs);
  }

  protected final CompletableFuture<String> sendATCommand(String cmd, long timeoutMs) {
    if (tainted) {
      CompletableFuture<String> f = new CompletableFuture<>();
      f.completeExceptionally(new IOException("Modem not usable after timeout; reconnect required"));
      return f;
    }
    logger.log(STOGI_SEND_AT_MESSAGE, cmd);
    CompletableFuture<String> future = new CompletableFuture<>();
    Command c = new Command(cmd, future);
    synchronized (this) {
      commandQueue.add(c);
      if (currentCommand == null) sendNextCommand();
    }
    scheduleTimeout(c, Math.max(1, timeoutMs));
    return future;
  }

  public void close() {
    tainted = true;
    try {
      scheduler.shutdownNow();
    } catch (Exception ignore) {
    }
    failAllQueued(new IOException("Modem closed"));
  }

  public boolean isTainted() {
    return tainted;
  }

  private synchronized void sendNextCommand() {
    currentCommand = commandQueue.poll();
    if (currentCommand != null) {
      packetSender.accept(packetWith(currentCommand.command));
    }
  }

  private Packet packetWith(String cmd) {
    byte[] data = (cmd + "\r\n").getBytes(StandardCharsets.US_ASCII);
    Packet packet = new Packet(data.length, false);
    packet.put(data).flip();
    return packet;
  }

  private void acceptLine(String line) {
    String stripped = line == null ? "" : line.strip();
    logger.log(STOGI_RECEIVED_AT_MESSAGE, stripped);

    Command inFlight;
    synchronized (this) {
      inFlight = currentCommand;
    }

    if (inFlight == null) {
      handleUnsolicitedLine(stripped);
      return;
    }

    if (stripped.equalsIgnoreCase("OK") || stripped.startsWith("ERROR")) {
      String response;
      synchronized (this) {
        response = responseBuffer.toString().trim();
        if (stripped.startsWith("ERROR")) {
          if (!response.isEmpty()) response += "\r\n";
          response += stripped;
        }
        currentCommand = null;
        responseBuffer.setLength(0);
      }
      cancelTimeout(inFlight);
      inFlight.future.complete(response);
      sendNextCommand();
      return;
    }

    synchronized (this) {
      responseBuffer.append(stripped).append("\r\n");
    }
  }

  private void scheduleTimeout(Command c, long timeoutMs) {
    ScheduledFuture<?> sf = scheduler.schedule(() -> {
      IOException ex = new IOException("AT timeout after " + timeoutMs + " ms for: " + c.command);
      tainted = true;
      Command toFailNow = null;
      synchronized (this) {
        if (currentCommand == c) {
          toFailNow = currentCommand;
          currentCommand = null;
        } else {
          commandQueue.remove(c);
        }
        responseBuffer.setLength(0);
      }
      if (toFailNow != null) toFailNow.future.completeExceptionally(ex);
      failAllQueued(ex);
    }, timeoutMs, TimeUnit.MILLISECONDS);
    timeouts.put(c, sf);
  }

  private void cancelTimeout(Command c) {
    ScheduledFuture<?> sf = timeouts.remove(c);
    if (sf != null) sf.cancel(false);
  }

  private void failAllQueued(IOException ex) {
    Command q;
    while ((q = pollQueue()) != null) {
      cancelTimeout(q);
      q.future.completeExceptionally(ex);
    }
  }

  private synchronized Command pollQueue() {
    return commandQueue.poll();
  }

  private static String esc(String s) {
    return s.replace("\"", "\\\"");
  }
}
