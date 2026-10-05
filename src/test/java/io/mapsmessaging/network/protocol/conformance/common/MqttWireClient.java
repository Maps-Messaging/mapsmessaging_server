/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.conformance.common;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MqttWireClient implements AutoCloseable {

  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;

  public MqttWireClient(String host, int port) throws IOException {
    socket = new Socket(host, port);
    socket.setSoTimeout(5000);
    socket.setTcpNoDelay(true);
    input = socket.getInputStream();
    output = socket.getOutputStream();
  }

  public void send(byte[] packet) throws IOException {
    output.write(packet);
    output.flush();
  }

  public void sendByteByByte(byte[] packet) throws IOException {
    for (byte value : packet) {
      output.write(value);
      output.flush();
    }
  }

  public void setReadTimeoutMillis(int timeoutMillis) throws IOException {
    socket.setSoTimeout(timeoutMillis);
  }

  public int readRawByte() throws IOException {
    return input.read();
  }

  public WirePacket readPacket() throws IOException {
    int header = input.read();
    if (header < 0) {
      throw new EOFException("Connection closed before MQTT fixed header");
    }
    int remainingLength = readVariableByteInteger(input);
    byte[] body = input.readNBytes(remainingLength);
    if (body.length != remainingLength) {
      throw new EOFException("Connection closed inside MQTT packet");
    }
    return new WirePacket(header, body);
  }

  public static byte[] connect311(String clientId, boolean cleanSession) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, "MQTT");
    body.write(4);
    body.write(cleanSession ? 0x02 : 0x00);
    writeUnsignedShort(body, 30);
    writeUtf8(body, clientId);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] connect5(String clientId, boolean cleanStart) {
    return connect5(clientId, cleanStart, 30, new byte[0]);
  }

  public static byte[] connect5(String clientId, boolean cleanStart, int keepAlive, byte[] properties) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, "MQTT");
    body.write(5);
    body.write(cleanStart ? 0x02 : 0x00);
    writeUnsignedShort(body, keepAlive);
    writeVariableByteInteger(body, properties.length);
    body.writeBytes(properties);
    writeUtf8(body, clientId);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] connectWithRawClientId(
      int level,
      boolean cleanStart,
      byte[] clientIdBytes) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, "MQTT");
    body.write(level);
    body.write(cleanStart ? 0x02 : 0x00);
    writeUnsignedShort(body, 30);
    if (level == 5) {
      writeVariableByteInteger(body, 0);
    }
    writeUnsignedShort(body, clientIdBytes.length);
    body.writeBytes(clientIdBytes);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] connect311WithWill(
      String clientId,
      int keepAlive,
      String willTopic,
      byte[] willPayload) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, "MQTT");
    body.write(4);
    body.write(0x0E); // clean session + Will flag + Will QoS 1
    writeUnsignedShort(body, keepAlive);
    writeUtf8(body, clientId);
    writeUtf8(body, willTopic);
    writeBinary(body, willPayload);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] connect5WithWill(
      String clientId,
      int keepAlive,
      long sessionExpiry,
      long willDelay,
      String willTopic,
      byte[] willPayload) {
    ByteArrayOutputStream connectProperties = new ByteArrayOutputStream();
    connectProperties.write(0x11); // Session Expiry Interval
    writeUnsignedInt(connectProperties, sessionExpiry);

    ByteArrayOutputStream willProperties = new ByteArrayOutputStream();
    willProperties.write(0x18); // Will Delay Interval
    writeUnsignedInt(willProperties, willDelay);

    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, "MQTT");
    body.write(5);
    body.write(0x0E); // clean start + Will flag + Will QoS 1
    writeUnsignedShort(body, keepAlive);
    writeVariableByteInteger(body, connectProperties.size());
    body.writeBytes(connectProperties.toByteArray());
    writeUtf8(body, clientId);
    writeVariableByteInteger(body, willProperties.size());
    body.writeBytes(willProperties.toByteArray());
    writeUtf8(body, willTopic);
    writeBinary(body, willPayload);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] connectWithProtocolLevel(String clientId, int level) {
    return connectPacket("MQTT", level, 0x02, 30, new byte[0], clientId);
  }

  public static byte[] connectPacket(
      String protocolName,
      int level,
      int connectFlags,
      int keepAlive,
      byte[] properties,
      String clientId) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, protocolName);
    body.write(level);
    body.write(connectFlags);
    writeUnsignedShort(body, keepAlive);
    if (level == 5) {
      writeVariableByteInteger(body, properties.length);
      body.writeBytes(properties);
    }
    writeUtf8(body, clientId);
    return packet(0x10, body.toByteArray());
  }

  public static byte[] pingReq() {
    return new byte[]{(byte) 0xC0, 0x00};
  }

  public static byte[] disconnect() {
    return new byte[]{(byte) 0xE0, 0x00};
  }

  public static byte[] pubAck(int packetId) {
    return acknowledgement(0x40, packetId);
  }

  public static byte[] pubRec(int packetId) {
    return acknowledgement(0x50, packetId);
  }

  public static byte[] pubRel(int packetId) {
    return acknowledgement(0x62, packetId);
  }

  public static byte[] pubComp(int packetId) {
    return acknowledgement(0x70, packetId);
  }

  private static byte[] acknowledgement(int fixedHeader, int packetId) {
    return new byte[]{
        (byte) fixedHeader,
        0x02,
        (byte) ((packetId >>> 8) & 0xff),
        (byte) (packetId & 0xff)
    };
  }

  public static int publishPacketIdentifier(WirePacket packet) throws IOException {
    if (packet.type() != 3) {
      throw new IOException("Expected PUBLISH but received packet type " + packet.type());
    }
    byte[] body = packet.body();
    if (body.length < 4) {
      throw new IOException("PUBLISH packet too short");
    }
    int topicLength = unsignedShort(body, 0);
    int packetIdOffset = 2 + topicLength;
    if (packetIdOffset + 1 >= body.length) {
      throw new IOException("PUBLISH packet does not contain a packet identifier");
    }
    return unsignedShort(body, packetIdOffset);
  }

  public static byte[] subscribe311(int packetId, String topicFilter, int qos) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUnsignedShort(body, packetId);
    writeUtf8(body, topicFilter);
    body.write(qos & 0x03);
    return packet(0x82, body.toByteArray());
  }

  public static byte[] unsubscribe311(int packetId, String topicFilter) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUnsignedShort(body, packetId);
    writeUtf8(body, topicFilter);
    return packet(0xA2, body.toByteArray());
  }

  public static byte[] publishQos1_311(int packetId, String topic, byte[] payload) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, topic);
    writeUnsignedShort(body, packetId);
    body.writeBytes(payload);
    return packet(0x32, body.toByteArray());
  }

  public static byte[] subscribe5(int packetId, String topicFilter, int qos) {
    return subscribe5(packetId, new byte[0], topicFilter, qos & 0x03);
  }

  public static byte[] subscribe5(
      int packetId,
      byte[] properties,
      String topicFilter,
      int subscriptionOptions) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUnsignedShort(body, packetId);
    writeVariableByteInteger(body, properties.length);
    body.writeBytes(properties);
    writeUtf8(body, topicFilter);
    body.write(subscriptionOptions & 0xff);
    return packet(0x82, body.toByteArray());
  }

  public static byte[] unsubscribe5(int packetId, String topicFilter) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUnsignedShort(body, packetId);
    writeVariableByteInteger(body, 0);
    writeUtf8(body, topicFilter);
    return packet(0xA2, body.toByteArray());
  }

  public static byte[] publishQos1_5(int packetId, String topic, byte[] payload) {
    return publish5(0x32, packetId, topic, new byte[0], payload);
  }

  public static byte[] publishQos2_5(int packetId, String topic, byte[] payload) {
    return publish5(0x34, packetId, topic, new byte[0], payload);
  }

  public static byte[] publishQos1_5(
      int packetId,
      String topic,
      byte[] properties,
      byte[] payload) {
    return publish5(0x32, packetId, topic, properties, payload);
  }

  private static byte[] publish5(
      int fixedHeader,
      int packetId,
      String topic,
      byte[] properties,
      byte[] payload) {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeUtf8(body, topic);
    writeUnsignedShort(body, packetId);
    writeVariableByteInteger(body, properties.length);
    body.writeBytes(properties);
    body.writeBytes(payload);
    return packet(fixedHeader, body.toByteArray());
  }

  public static byte[] packet(int fixedHeader, byte[] body) {
    ByteArrayOutputStream packet = new ByteArrayOutputStream();
    packet.write(fixedHeader);
    writeVariableByteInteger(packet, body.length);
    packet.writeBytes(body);
    return packet.toByteArray();
  }

  public static int unsignedShort(byte[] data, int offset) {
    return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
  }

  public static int variableByteIntegerSize(int value) {
    if (value < 0 || value > 268_435_455) {
      throw new IllegalArgumentException("MQTT Variable Byte Integer out of range: " + value);
    }
    if (value <= 127) {
      return 1;
    }
    if (value <= 16_383) {
      return 2;
    }
    if (value <= 2_097_151) {
      return 3;
    }
    return 4;
  }

  public static byte[] encodeVariableByteInteger(int value) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writeVariableByteInteger(out, value);
    return out.toByteArray();
  }

  public static int readVariableByteInteger(byte[] data, int offset) throws IOException {
    int multiplier = 1;
    int value = 0;
    int index = offset;
    int count = 0;
    while (true) {
      if (index >= data.length || count == 4) {
        throw new IOException("Malformed MQTT Variable Byte Integer");
      }
      int encoded = data[index++] & 0xff;
      value += (encoded & 0x7f) * multiplier;
      count++;
      if ((encoded & 0x80) == 0) {
        return value;
      }
      multiplier *= 128;
    }
  }

  private static int readVariableByteInteger(InputStream input) throws IOException {
    int multiplier = 1;
    int value = 0;
    int count = 0;
    while (true) {
      int encoded = input.read();
      if (encoded < 0) {
        throw new EOFException("Connection closed while reading MQTT Remaining Length");
      }
      value += (encoded & 0x7f) * multiplier;
      count++;
      if ((encoded & 0x80) == 0) {
        return value;
      }
      if (count == 4) {
        throw new IOException("Malformed MQTT Remaining Length");
      }
      multiplier *= 128;
    }
  }

  private static void writeVariableByteInteger(ByteArrayOutputStream out, int value) {
    if (value < 0 || value > 268_435_455) {
      throw new IllegalArgumentException("MQTT Variable Byte Integer out of range: " + value);
    }
    do {
      int digit = value % 128;
      value /= 128;
      if (value > 0) {
        digit |= 0x80;
      }
      out.write(digit);
    } while (value > 0);
  }

  private static void writeUtf8(ByteArrayOutputStream out, String value) {
    byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
    writeUnsignedShort(out, encoded.length);
    out.writeBytes(encoded);
  }

  private static void writeUnsignedShort(ByteArrayOutputStream out, int value) {
    out.write((value >>> 8) & 0xff);
    out.write(value & 0xff);
  }

  private static void writeUnsignedInt(ByteArrayOutputStream out, long value) {
    out.write((int) ((value >>> 24) & 0xff));
    out.write((int) ((value >>> 16) & 0xff));
    out.write((int) ((value >>> 8) & 0xff));
    out.write((int) (value & 0xff));
  }

  private static void writeBinary(ByteArrayOutputStream out, byte[] value) {
    writeUnsignedShort(out, value.length);
    out.writeBytes(value);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  public record WirePacket(int fixedHeader, byte[] body) {
    public int type() {
      return (fixedHeader >>> 4) & 0x0f;
    }

    public int flags() {
      return fixedHeader & 0x0f;
    }

    @Override
    public String toString() {
      return "WirePacket{header=0x" + Integer.toHexString(fixedHeader)
          + ", body=" + Arrays.toString(body) + "}";
    }
  }
}
