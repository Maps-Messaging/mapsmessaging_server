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

package io.mapsmessaging.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;

public class SimpleBufferBasedTest extends BaseTestConfig {

  private static final long END_FRAME_DELAY = 1000;
  public boolean recordResponses = false;

  public void frameCharacter(String filename, String host, int port, int[] frameCharacter, int totalFrames) throws IOException, URISyntaxException {
    try (TestClient testClient = new TestClient(host, port, 0, frameCharacter[0])) {
      delay(1000); // Pause for a second while things start up
      List<byte[]> list = parseToBytes(filename);
      OutputStream outputStream = testClient.getOutputStream();
      for(byte[] source: list) {
        for (byte b : source) {
          boolean end = false;
          for (int i : frameCharacter) {
            if (b == i) {
              end = true;
            }
          }
          if (end) {
            outputStream.flush(); // Flush the buffer
            delay(10); // Wait for server to process
            outputStream.write(b); // send the frame byte
            outputStream.flush();
          } else {
            outputStream.write(b);
          }
        }
      }
      outputStream.flush();
      waitForCompletion(testClient, totalFrames);
    }
  }

  protected void simpleByteWrite(String filename, int frameChar, int size, String host, int port, int totalFrames) throws IOException, URISyntaxException {
    simpleByteWrite(filename, frameChar, size, host, port, totalFrames, null);
  }

  protected void simpleByteWrite(String filename, int frameChar, int size, String host, int port, int totalFrames, byte[] endFrame) throws IOException, URISyntaxException {
    simpleByteWrite(filename, frameChar, size, host, port, totalFrames, endFrame, size == 1);
  }

  protected void simpleByteWrite(String filename, int frameChar, int size, String host, int port, int totalFrames, byte[] endFrame, boolean fast) throws IOException, URISyntaxException {
    try (TestClient testClient = new TestClient(host, port, 0, frameChar)) {
      Random rdm = new Random();
      delay(1000); // Pause for a second while things start up
      byte[] write = new byte[size];
      List<byte[]> list = parseToBytes(filename);
      int idx = 0;
      OutputStream outputStream = testClient.getOutputStream();
      for (byte[] source: list) {
        while (idx < source.length) {
          int x = 0;
          while (x < write.length && idx < source.length) {
            write[x] = source[idx];
            if (write[x] == 0xe0) {
              outputStream.flush();
              delay(1000);
            }
            idx++;
            x++;
          }

          outputStream.write(write, 0, x);
          outputStream.flush();
          if (!fast) {
            long delay = Math.abs(rdm.nextInt(2));
            delay(delay);
          }
        }
      }
      if(endFrame != null) {
        delay(END_FRAME_DELAY);
        outputStream.write(endFrame);
        outputStream.flush();
      }
      waitForCompletion(testClient, totalFrames);
    }
  }
  protected void slowSubscriberTest( String filename, String host, int port, int totalFrames, int frameCharacter, int delay )throws IOException, URISyntaxException {
    slowSubscriberTest(filename, host, port, totalFrames, frameCharacter, delay, null);
  }

  protected void slowSubscriberTest( String filename, String host, int port, int totalFrames, int frameCharacter, int delay, byte[] endFrame )throws IOException, URISyntaxException {
    try (TestClient testClient = new TestClient(host, port, delay, frameCharacter)) {
        delay(1000); // Pause for a second while things start up
        List<byte[]> list = parseToBytes(filename);
        byte[] write = new byte[1024];
        int idx = 0;
        OutputStream outputStream = testClient.getOutputStream();
        for(byte[] source: list) {
          while (idx < source.length) {
            int x = 0;
            while (x < write.length && idx < source.length) {
              write[x] = source[idx];
              idx++;
              x++;
            }
            outputStream.write(write, 0, x);
            outputStream.flush();
          }
          if (endFrame != null) {
            delay(END_FRAME_DELAY);
            outputStream.write(endFrame);
            outputStream.flush();
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        waitForCompletion(testClient, totalFrames);
      }
  }

  private void waitForCompletion(TestClient testClient, int noOfEvents){
    long end = System.currentTimeMillis() + 60000;
    int startCount = testClient.getFrameCounter();
    if(noOfEvents > 0) {
      while (testClient.getFrameCounter() < noOfEvents && System.currentTimeMillis() < end) {
        delay(100);
        if (startCount != testClient.getFrameCounter()) {
          startCount = testClient.getFrameCounter();
          end = System.currentTimeMillis() + 60000;
        }
      }
      Assertions.assertEquals(noOfEvents, testClient.getFrameCounter());
    }
  }

  private Socket createSocket(String host, int port) throws IOException {
    return new Socket(host, port);
  }

  private List<byte[]> parseToBytes(String filename) throws IOException, URISyntaxException {
    URI filePath = getClass().getResource(filename).toURI();
    Path path = Paths.get(filePath);
    byte[] buffer = Files.readAllBytes(path);
    String input = new String(buffer);
    String[] lines = input.split("\n");
    List<byte[]> bytes = new ArrayList<>();
    for (String line : lines) {
      StringTokenizer st = new StringTokenizer(line, ",");

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      while (st.hasMoreElements()) {
        String nextChar = st.nextElement().toString().trim();
        if (!nextChar.isEmpty()) {
          int val = Integer.parseInt(nextChar, 16);
          char tchar = (char) (val & 0xff);
          byteArrayOutputStream.write(tchar);
        }
      }
      bytes.add(byteArrayOutputStream.toByteArray());
    }
    return bytes;
  }

  private class TestClient implements AutoCloseable {
    final Socket connection;
    final Thread readThread;
    @Getter
    final OutputStream outputStream;
    final InputStream inputStream;
    final int frameCharacter;
    AtomicInteger frameCounter = new AtomicInteger(0);
    ByteArrayOutputStream recordedResponses;

    public TestClient(String host, int port, long msDelay, int frameCharacter) throws IOException {
      if(recordResponses){
        recordedResponses = new ByteArrayOutputStream(1024*1024);
      }
      else{
        recordedResponses = null;
      }
      this.frameCharacter = frameCharacter;
      connection = createSocket(host, port);
      connection.setReceiveBufferSize(1024);
      connection.setTcpNoDelay(true);
      outputStream = connection.getOutputStream();
      inputStream = connection.getInputStream();
      readThread = new Thread(() -> {
        byte[] tmp = new byte[1024 * 1024];
        while (true) {
          try {
            int len = inputStream.read(tmp);
            if (len > 0) {
              if(recordedResponses != null){
                recordedResponses.write(tmp, 0, len);
              }
              for (int x = 0; x < len; x++) {
                if (tmp[x] == frameCharacter) {
                  frameCounter.incrementAndGet();
                  if (msDelay > 0) {
                    delay(msDelay);
                  }
                }
              }
            } else {
              dumpResponseBuffer();
              break;
            }
          } catch (IOException e) {
            dumpResponseBuffer();
            break;
          }
        }
      });
      readThread.setDaemon(true);
      readThread.start();
    }

    public void dumpResponseBuffer(){
      if(recordedResponses != null){
        byte[] dump = recordedResponses.toByteArray();
        int counter = 0;
        StringBuilder sb = new StringBuilder(1024);
        for(byte entry:dump){
          sb.append(Integer.toHexString(entry)).append(",");
          counter++;
          if(counter > 132){
            counter = 0;
            sb.append("\n");
          }
        }
        System.err.println(sb.toString());
      }
    }

    public void close() throws IOException {
      outputStream.close();
      inputStream.close();
      connection.close();
    }

    public int getFrameCounter(){
      return frameCounter.get();
    }
  }
}
