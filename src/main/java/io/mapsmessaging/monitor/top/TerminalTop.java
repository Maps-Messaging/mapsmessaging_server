/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.monitor.top;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalTop {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;
  private static final long TB = GB * 1024;
  private final MqttConnection mqttConnection;

  private final Terminal terminal;
  private final  Screen screen;
  private final LinkedList<StatusMessage> queue;
  private final AtomicBoolean runFlag;


  public TerminalTop(String url, String username, String password) throws IOException, MqttException, InterruptedException {
    queue = new LinkedList<>();
    runFlag = new AtomicBoolean(true);
    mqttConnection = new MqttConnection(url, username, password);

    // Setup terminal and screen layers
    terminal = new DefaultTerminalFactory().createTerminal();
    screen = new TerminalScreen(terminal);
    screen.startScreen();
    connectAndSubscribeToServer();
    runLoop();
  }

  public void stop(){
    runFlag.set(false);
  }

  private void runLoop() throws InterruptedException {
    StatusMessage message;
    long nextUpdate = System.currentTimeMillis()+60000;
    while(runFlag.get()){
      synchronized (queue){
        while(queue.isEmpty()){
          if(System.currentTimeMillis() > nextUpdate){
            disconnectDisplay();
            nextUpdate = System.currentTimeMillis()+60000;
          }
          queue.wait(100);
          try {
            KeyStroke keyStroke = screen.pollInput();
            if(keyStroke != null && keyStroke.getCharacter().equals('q')){
              System.exit(1);
            }
          } catch (IOException e) {
          }
        }
        message = queue.removeFirst();
      }
      if(message != null){
        nextUpdate = System.currentTimeMillis()+60000;
        updateWindow(message);
      }
    }
    try {
      screen.stopScreen(); // Properly stop the screen when done
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void disconnectDisplay() {
    try {
      screen.clear();
      TextGraphics invertTextGraphics = screen.newTextGraphics();
      invertTextGraphics.setForegroundColor(TextColor.ANSI.BLACK);
      invertTextGraphics.setBackgroundColor(TextColor.ANSI.WHITE);
      invertTextGraphics.putString(0, 0, "No Data received from server                                                  ");
      screen.refresh();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void updateWindow(StatusMessage serverStatus) {
    try {

      String messageState = String.format("Pub: %d, Sub: %d, No Int: %d, Received: %d, Disk: %d",
          serverStatus.getServerStatistics().getTotalPublishedMessages(),
          serverStatus.getServerStatistics().getTotalSubscribedMessages(),
          serverStatus.getServerStatistics().getTotalNoInterestMessages(),
          serverStatus.getServerStatistics().getTotalDeliveredMessages(),
          serverStatus.getServerStatistics().getTotalRetrievedMessages());

      String tasksText = String.format(serverStatus.getNumberOfThreads()+" threads, Running: %d, Waiting: %d, Timed_Waiting: %d",
          serverStatus.getThreadState().getOrDefault("RUNNABLE", 0),
          serverStatus.getThreadState().getOrDefault("WAITING", 0),
          serverStatus.getThreadState().getOrDefault("TIMED_WAITING", 0));

      String networkStats = String.format("Pkts Out: %s, In: %s | Bytes Out: %s, In: %s",
          formatSize(serverStatus.getServerStatistics().getPacketsSent()),
          formatSize(serverStatus.getServerStatistics().getPacketsReceived()),
          formatSize(serverStatus.getServerStatistics().getTotalWriteBytes()),
          formatSize(serverStatus.getServerStatistics().getTotalReadBytes())
      );

      long usedMemory = serverStatus.getTotalMemory() - serverStatus.getFreeMemory();

      String connectionsString = String.format("Connections: %d, Destinations: %d", serverStatus.getConnections(), serverStatus.getDestinations());
      String memoryString = String.format("Memory Total: %s, Free: %s, Used: %s", formatSize(serverStatus.getTotalMemory()), formatSize(serverStatus.getFreeMemory()), formatSize(usedMemory));
      screen.clear();
      TextGraphics textGraphics = screen.newTextGraphics();
      TextGraphics invertTextGraphics = screen.newTextGraphics();
      invertTextGraphics.setForegroundColor(TextColor.ANSI.BLACK);
      invertTextGraphics.setBackgroundColor(TextColor.ANSI.WHITE);

      // Drawing directly to the screen using textGraphics
      textGraphics.putString(0, 0, "Messaging Server Top - Server: "+serverStatus.getServerName());
      textGraphics.putString(0, 1, "Uptime: " + formatUptime(serverStatus.getUptime()) + ",  Version : "+ serverStatus.getVersion()+" - " + serverStatus.getBuildDate());
      textGraphics.putString(0, 2, tasksText);
      textGraphics.putString(0, 3, connectionsString);
      textGraphics.putString(0, 4,memoryString);
      textGraphics.putString(0, 5, messageState);
      textGraphics.putString(0, 6, networkStats);
      invertTextGraphics.putString(0, 7, "Header line for the top 10 connections                                        ");
      screen.refresh();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String formatUptime(long uptimeMillis) {
    long uptimeSeconds = uptimeMillis / 1000;
    long hours = uptimeSeconds / 3600;
    long minutes = (uptimeSeconds % 3600) / 60;
    long seconds = uptimeSeconds % 60;

    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }


  public static String formatSize(long bytes) {
    if (bytes >= TB) {
      return String.format("%.2f TB", bytes / (double) TB);
    } else if (bytes >= GB) {
      return String.format("%.2f GB", bytes / (double) GB);
    } else if (bytes >= MB) {
      return String.format("%.2f MB", bytes / (double) MB);
    } else if (bytes >= KB) {
      return String.format("%.2f KB", bytes / (double) KB);
    } else {
      return bytes + " bytes";
    }
  }
  public void connectAndSubscribeToServer() throws MqttException {
    mqttConnection.subscribe("$SYS/server/status", new ServerStatusListener());
  }

  private class ServerStatusListener implements IMqttMessageListener{
    ObjectMapper mapper = new ObjectMapper();

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
      String jsonString = new String(message.getPayload());
      try {
        synchronized (queue){
          queue.add(mapper.readValue(jsonString, StatusMessage.class));
          queue.notify();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
