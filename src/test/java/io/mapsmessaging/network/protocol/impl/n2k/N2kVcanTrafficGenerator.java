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

package io.mapsmessaging.network.protocol.impl.n2k;

import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class N2kVcanTrafficGenerator {

  private static final String DEFAULT_INTERFACE_NAME = "vcan0";

  private static final int DEFAULT_SOURCE_ADDRESS = 150;
  private static final int DEFAULT_PRIORITY = 2;
  private static final int BROADCAST_DESTINATION = 255;

  private static final long RAPID_INTERVAL_MILLIS = 500;
  private static final long VESSEL_INTERVAL_MILLIS = 500;
  private static final long UNKNOWN_INTERVAL_MILLIS = 1000;

  private final String interfaceName;

  private long sequenceId;

  public N2kVcanTrafficGenerator(String interfaceName) {
    this.interfaceName = interfaceName;
  }

  public static void main(String[] args) throws IOException {
    String interfaceName = DEFAULT_INTERFACE_NAME;

    if (args.length > 0) {
      interfaceName = args[0];
    }

    N2kVcanTrafficGenerator generator = new N2kVcanTrafficGenerator(interfaceName);
    generator.run(Duration.ofMinutes(5));
  }

  public void run(Duration duration) throws IOException {
    long endTimeMillis = System.currentTimeMillis() + duration.toMillis();

    long nextRapidTimeMillis = 0;
    long nextVesselTimeMillis = 0;
    long nextUnknownTimeMillis = 0;

    try (SocketCanDevice socketCanDevice = new SocketCanDevice(interfaceName)) {
      while (System.currentTimeMillis() < endTimeMillis) {
        long nowMillis = System.currentTimeMillis();

        if (nowMillis >= nextRapidTimeMillis) {
          writeFrames(socketCanDevice, createRapidFrames());
          nextRapidTimeMillis = nowMillis + RAPID_INTERVAL_MILLIS;
        }

        if (nowMillis >= nextVesselTimeMillis) {
          writeFrames(socketCanDevice, createVesselFrames());
          nextVesselTimeMillis = nowMillis + VESSEL_INTERVAL_MILLIS;
        }

        if (nowMillis >= nextUnknownTimeMillis) {
          writeFrames(socketCanDevice, createUnknownFrames());
          nextUnknownTimeMillis = nowMillis + UNKNOWN_INTERVAL_MILLIS;
        }

        delay(10);
      }
    }
  }

  private List<CanFrame> createRapidFrames() {
    List<CanFrame> frames = new ArrayList<>();

    frames.add(createPositionRapidUpdateFrame());
    frames.add(createCogSogRapidUpdateFrame());

    return frames;
  }

  private List<CanFrame> createVesselFrames() {
    List<CanFrame> frames = new ArrayList<>();

    frames.add(createRudderFrame());
    frames.add(createVesselHeadingFrame());
    frames.add(createRateOfTurnFrame());
    frames.add(createTimeDateFrame());
    frames.addAll(createHeadingTrackControlFrames());

    return frames;
  }

  private List<CanFrame> createUnknownFrames() {
    List<CanFrame> frames = new ArrayList<>();

    frames.add(createFrame(233898243, payload(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)));
    frames.add(createFrame(150929416, payload(0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80)));
    frames.add(createFrame(486482183, payload(0x11, 0x21, 0x31, 0x41, 0x51, 0x61, 0x71, 0x81)));
    frames.add(createFrame(352265479, payload(0x12, 0x22, 0x32, 0x42, 0x52, 0x62, 0x72, 0x82)));
    frames.add(createFrame(503254280, payload(0x13, 0x23, 0x33, 0x43, 0x53, 0x63, 0x73, 0x83)));
    frames.add(createFrame(234817032, payload(0x14, 0x24, 0x34, 0x44, 0x54, 0x64, 0x74, 0x84)));
    frames.add(createFrame(167715336, payload(0x15, 0x25, 0x35, 0x45, 0x55, 0x65, 0x75, 0x85)));
    frames.add(createFrame(436148739, payload(0x16, 0x26, 0x36, 0x46, 0x56, 0x66, 0x76, 0x86)));
    frames.add(createFrame(234821640, payload(0x17, 0x27, 0x37, 0x47, 0x57, 0x67, 0x77, 0x87)));
    frames.add(createFrame(417859684, payload(0x18, 0x28, 0x38, 0x48, 0x58, 0x68, 0x78, 0x88)));
    frames.add(createFrame(233701128, payload(0x19, 0x29, 0x39, 0x49, 0x59, 0x69, 0x79, 0x89)));
    frames.add(createFrame(234823176, payload(0x1A, 0x2A, 0x3A, 0x4A, 0x5A, 0x6A, 0x7A, 0x8A)));

    return frames;
  }

  private CanFrame createPositionRapidUpdateFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 129025, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int latitude = (int) Math.round(-33.8600000 * 10_000_000);
    int longitude = (int) Math.round(151.2094000 * 10_000_000);

    byte[] data = new byte[8];

    writeInt32LittleEndian(data, 0, latitude);
    writeInt32LittleEndian(data, 4, longitude);

    return createFrame(canIdentifier, data);
  }

  private CanFrame createCogSogRapidUpdateFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 129026, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int sid = nextSequenceId();
    int cogRadiansScaled = 15708;
    int speedMetresPerSecondScaled = 650;

    byte[] data = new byte[8];

    data[0] = (byte) sid;
    data[1] = 0;
    writeUInt16LittleEndian(data, 2, cogRadiansScaled);
    writeUInt16LittleEndian(data, 4, speedMetresPerSecondScaled);
    data[6] = (byte) 0xFF;
    data[7] = (byte) 0xFF;

    return createFrame(canIdentifier, data);
  }

  private CanFrame createRudderFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 127245, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int sid = nextSequenceId();
    int rudderPositionScaled = 500;

    byte[] data = new byte[8];

    data[0] = (byte) sid;
    data[1] = (byte) 0xFF;
    writeInt16LittleEndian(data, 2, rudderPositionScaled);
    writeInt16LittleEndian(data, 4, 0);
    data[6] = (byte) 0xFF;
    data[7] = (byte) 0xFF;

    return createFrame(canIdentifier, data);
  }

  private CanFrame createVesselHeadingFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 127250, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int sid = nextSequenceId();
    int headingScaled = 15708;

    byte[] data = new byte[8];

    data[0] = (byte) sid;
    writeUInt16LittleEndian(data, 1, headingScaled);
    writeUInt16LittleEndian(data, 3, 0xFFFF);
    writeUInt16LittleEndian(data, 5, 0xFFFF);
    data[7] = 0;

    return createFrame(canIdentifier, data);
  }

  private CanFrame createRateOfTurnFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 127251, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int sid = nextSequenceId();
    int rateOfTurnScaled = 1000;

    byte[] data = new byte[8];

    data[0] = (byte) sid;
    writeInt32LittleEndian(data, 1, rateOfTurnScaled);
    data[5] = (byte) 0xFF;
    data[6] = (byte) 0xFF;
    data[7] = (byte) 0xFF;

    return createFrame(canIdentifier, data);
  }

  private CanFrame createTimeDateFrame() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 129033, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    int daysSinceEpoch = 20500;
    int timeOfDayScaled = 43_200_000;

    byte[] data = new byte[8];

    writeUInt16LittleEndian(data, 0, daysSinceEpoch);
    writeUInt32LittleEndian(data, 2, timeOfDayScaled);
    data[6] = (byte) 0xFF;
    data[7] = (byte) 0xFF;

    return createFrame(canIdentifier, data);
  }

  private List<CanFrame> createHeadingTrackControlFrames() {
    int canIdentifier = buildCanIdentifier(DEFAULT_PRIORITY, 127237, DEFAULT_SOURCE_ADDRESS, BROADCAST_DESTINATION);

    byte[] fastPacketPayload = payload(
        0x00, 0x00, 0x00, 0x00,
        0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF
    );

    return createFastPacketFrames(canIdentifier, fastPacketPayload);
  }

  private List<CanFrame> createFastPacketFrames(int canIdentifier, byte[] payload) {
    List<CanFrame> frames = new ArrayList<>();

    int fastPacketSequence = nextSequenceId() & 0x07;
    int frameIndex = 0;
    int payloadIndex = 0;

    byte[] firstFrameData = new byte[8];

    firstFrameData[0] = (byte) ((fastPacketSequence << 5) | frameIndex);
    firstFrameData[1] = (byte) payload.length;

    int firstFramePayloadLength = Math.min(6, payload.length);

    for (int index = 0; index < firstFramePayloadLength; index++) {
      firstFrameData[index + 2] = payload[payloadIndex];
      payloadIndex++;
    }

    fillUnusedBytes(firstFrameData, firstFramePayloadLength + 2);
    frames.add(createFrame(canIdentifier, firstFrameData));

    frameIndex++;

    while (payloadIndex < payload.length) {
      byte[] frameData = new byte[8];

      frameData[0] = (byte) ((fastPacketSequence << 5) | frameIndex);

      int framePayloadLength = Math.min(7, payload.length - payloadIndex);

      for (int index = 0; index < framePayloadLength; index++) {
        frameData[index + 1] = payload[payloadIndex];
        payloadIndex++;
      }

      fillUnusedBytes(frameData, framePayloadLength + 1);
      frames.add(createFrame(canIdentifier, frameData));

      frameIndex++;
    }

    return frames;
  }

  private CanFrame createFrame(int canIdentifier, byte[] data) {
    return new CanFrame(canIdentifier, true, data.length, data);
  }

  private int buildCanIdentifier(int priority, int pgn, int sourceAddress, int destinationAddress) {
    int protocolDataUnitFormat = (pgn >> 8) & 0xFF;
    int canIdentifier = (priority & 0x07) << 26;

    if (protocolDataUnitFormat < 240) {
      canIdentifier |= (pgn & 0x1FF00) << 8;
      canIdentifier |= (destinationAddress & 0xFF) << 8;
    } else {
      canIdentifier |= (pgn & 0x1FFFF) << 8;
    }

    canIdentifier |= sourceAddress & 0xFF;

    return canIdentifier;
  }

  private void writeFrames(SocketCanDevice socketCanDevice, List<CanFrame> frames) throws IOException {
    for (CanFrame frame : frames) {
      socketCanDevice.writeFrame(frame);
      delay(10);
    }
  }

  private void delay(long delayMillis) {
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  private int nextSequenceId() {
    int nextSequenceId = (int) (sequenceId & 0xFF);
    sequenceId++;
    return nextSequenceId;
  }

  private byte[] payload(int... values) {
    byte[] data = new byte[values.length];

    for (int index = 0; index < values.length; index++) {
      data[index] = (byte) values[index];
    }

    return data;
  }

  private void fillUnusedBytes(byte[] data, int fromIndex) {
    for (int index = fromIndex; index < data.length; index++) {
      data[index] = (byte) 0xFF;
    }
  }

  private void writeUInt16LittleEndian(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }

  private void writeInt16LittleEndian(byte[] data, int offset, int value) {
    writeUInt16LittleEndian(data, offset, value);
  }

  private void writeUInt32LittleEndian(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private void writeInt32LittleEndian(byte[] data, int offset, int value) {
    writeUInt32LittleEndian(data, offset, value);
  }
}