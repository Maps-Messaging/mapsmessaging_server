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

package io.mapsmessaging.network.protocol.impl.satellite;

import io.mapsmessaging.network.protocol.impl.satellite.protocol.CipherManager;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.MessageQueue;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.MessageQueuePacker;
import io.mapsmessaging.network.protocol.impl.satellite.protocol.MessageQueueUnpacker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

class PackingTest {

  @AfterAll
  static void tearDown() {
    MessageQueue.LENGTH_BYTE_SIZE = 3;
  }

  @Test
  void validatePackSmallUnpack() throws IOException {
    MessageQueue.LENGTH_BYTE_SIZE = 2;
    Map<String, List<byte[]>> queuedMessages = new LinkedHashMap<>();
    for(int x=0;x<20;x++){
      List<byte[]> messages = new ArrayList<>();
      for(int y=0;y<1;y++){
        byte[] buff = new byte[100];
        Arrays.fill(buff, (byte)x);
        messages.add(buff);
      }
      queuedMessages.put(String.valueOf(x), messages);
    }
    queuedMessages.put("debug", List.of("we should not see plain text in the resultant pack".getBytes()));

    int minCompressSize = 100;
    CipherManager cipherManager = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(queuedMessages, minCompressSize, cipherManager);
    String t = new String(packed.data());
    Assertions.assertFalse(t.contains("we should not see plain text in the resultant pack"));
    Assertions.assertFalse(t.contains("debug"));
    Map<String, List<byte[]>> unqueued = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cipherManager);

    Assertions.assertEquals(queuedMessages.size(), unqueued.size(), "Map sizes differ");

    for (Map.Entry<String, List<byte[]>> entry : queuedMessages.entrySet()) {
      String key = entry.getKey();
      List<byte[]> expectedList = entry.getValue();
      List<byte[]> actualList   = unqueued.get(key);

      Assertions.assertNotNull(actualList, "Missing key: " + key);
      Assertions.assertEquals(expectedList.size(), actualList.size(), "List size differs for key: " + key);
      for (int i = 0; i < expectedList.size(); i++) {
        Assertions.assertArrayEquals(expectedList.get(i), actualList.get(i), "Mismatch at key=" + key + ", index=" + i);
      }
    }
  }


  @Test
  void validatePackUnpack() throws IOException {
    MessageQueue.LENGTH_BYTE_SIZE = 3;
    Map<String, List<byte[]>> queuedMessages = new LinkedHashMap<>();
    for(int x=0;x<200;x++){
      List<byte[]> messages = new ArrayList<>();
      for(int y=0;y<100;y++){
        byte[] buff = new byte[1024];
        Arrays.fill(buff, (byte)x);
        messages.add(buff);
      }
      queuedMessages.put(String.valueOf(x), messages);
    }
    queuedMessages.put("debug", List.of("we should not see plain text in the resultant pack".getBytes()));

    int minCompressSize = 100;
    CipherManager cipherManager = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(queuedMessages, minCompressSize, cipherManager);
    String t = new String(packed.data());
    Assertions.assertFalse(t.contains("we should not see plain text in the resultant pack"));
    Assertions.assertFalse(t.contains("debug"));
    Map<String, List<byte[]>> unqueued = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cipherManager);

    Assertions.assertEquals(queuedMessages.size(), unqueued.size(), "Map sizes differ");

    for (Map.Entry<String, List<byte[]>> entry : queuedMessages.entrySet()) {
      String key = entry.getKey();
      List<byte[]> expectedList = entry.getValue();
      List<byte[]> actualList   = unqueued.get(key);

      Assertions.assertNotNull(actualList, "Missing key: " + key);
      Assertions.assertEquals(expectedList.size(), actualList.size(), "List size differs for key: " + key);
      for (int i = 0; i < expectedList.size(); i++) {
        Assertions.assertArrayEquals(expectedList.get(i), actualList.get(i), "Mismatch at key=" + key + ", index=" + i);
      }
    }
  }

  @Test
  void validatePackUnpackLarger() throws IOException {
    MessageQueue.LENGTH_BYTE_SIZE = 4;
    Map<String, List<byte[]>> queuedMessages = new LinkedHashMap<>();
    for(int x=0;x<200;x++){
      List<byte[]> messages = new ArrayList<>();
      for(int y=0;y<100;y++){
        byte[] buff = new byte[1024];
        Arrays.fill(buff, (byte)x);
        messages.add(buff);
      }
      queuedMessages.put(String.valueOf(x), messages);
    }
    queuedMessages.put("debug", List.of("we should not see plain text in the resultant pack".getBytes()));

    int minCompressSize = 100;
    CipherManager cipherManager = new CipherManager("testing".getBytes());
    MessageQueuePacker.Packed packed = MessageQueuePacker.pack(queuedMessages, minCompressSize, cipherManager);
    String t = new String(packed.data());
    Assertions.assertFalse(t.contains("we should not see plain text in the resultant pack"));
    Assertions.assertFalse(t.contains("debug"));
    Map<String, List<byte[]>> unqueued = MessageQueueUnpacker.unpack(packed.data(), packed.compressed(), cipherManager);

    Assertions.assertEquals(queuedMessages.size(), unqueued.size(), "Map sizes differ");

    for (Map.Entry<String, List<byte[]>> entry : queuedMessages.entrySet()) {
      String key = entry.getKey();
      List<byte[]> expectedList = entry.getValue();
      List<byte[]> actualList   = unqueued.get(key);

      Assertions.assertNotNull(actualList, "Missing key: " + key);
      Assertions.assertEquals(expectedList.size(), actualList.size(), "List size differs for key: " + key);
      for (int i = 0; i < expectedList.size(); i++) {
        Assertions.assertArrayEquals(expectedList.get(i), actualList.get(i), "Mismatch at key=" + key + ", index=" + i);
      }
    }
  }
}
