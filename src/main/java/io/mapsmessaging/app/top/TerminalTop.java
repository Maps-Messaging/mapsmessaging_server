/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.app.top;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import io.mapsmessaging.app.top.network.MqttConnection;
import io.mapsmessaging.app.top.panes.PaneUpdate;
import io.mapsmessaging.app.top.panes.ServerStatusPane;
import io.mapsmessaging.app.top.panes.destination.DestinationPane;
import io.mapsmessaging.app.top.panes.interfaces.InterfacesPane;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.paho.client.mqttv3.MqttException;

public class TerminalTop {

  private final MqttConnection mqttConnection;

  private final ServerStatusPane serverStatusPane;
  private final DestinationPane destinationPane;
  private final InterfacesPane interfacesPane;

  private final Terminal terminal;
  private final  Screen screen;
  private final AtomicBoolean runFlag;
  private boolean disconnected = false;
  private final PaneUpdate[] panels;
  private int idx = 0;
  private long switchDisplay = System.currentTimeMillis() + 3000;


  public TerminalTop(String url, String username, String password) throws IOException, MqttException {
    runFlag = new AtomicBoolean(true);
    mqttConnection = new MqttConnection(url, username, password);

    // Setup terminal and screen layers
    terminal = new DefaultTerminalFactory().createTerminal();
    screen = new TerminalScreen(terminal);
    screen.startScreen();
    screen.clear();
    int rows = terminal.getTerminalSize().getRows();
    TextGraphics normalText = screen.newTextGraphics();
    TextGraphics boldText = screen.newTextGraphics();
    TextGraphics headerText = screen.newTextGraphics();
    headerText.setBackgroundColor(TextColor.ANSI.WHITE);
    headerText.setForegroundColor(TextColor.ANSI.BLACK);
    normalText.setForegroundColor(TextColor.ANSI.WHITE);
    boldText.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
    serverStatusPane = new ServerStatusPane(normalText, boldText);
    destinationPane = new DestinationPane(6, rows,  normalText, boldText, headerText);
    interfacesPane = new InterfacesPane(6, rows, normalText, boldText, headerText);
    panels = new PaneUpdate[2];
    panels[0] = destinationPane;
    panels[1] = interfacesPane;
    panels[0].enable();
    connectAndSubscribeToServer();
    runLoop();
  }

  public void stop(){
    runFlag.set(false);
  }

  private void runLoop() throws IOException {
    Object message;
    long nextUpdate = System.currentTimeMillis()+60000;
    while(runFlag.get()){
      nextUpdate = waitForSomething(nextUpdate);
      message = mqttConnection.getUpdate();
      if (message != null) {
        if (disconnected) {
          disconnected = false;
          screen.clear();
        }
        serverStatusPane.update(message);
        destinationPane.update(message);
        interfacesPane.update(message);
        screen.refresh();
        nextUpdate = System.currentTimeMillis() + 60000;
      }
    }
    try {
      screen.stopScreen(); // Properly stop the screen when done
      mqttConnection.close();
    } catch (IOException | MqttException e) {
      e.printStackTrace();
    }
  }

  private long waitForSomething(long nextUpdate) {
    while (mqttConnection.isQueueEmpty()) {
      if (!runFlag.get()) {
        return 0;
      }
      if (System.currentTimeMillis() > switchDisplay) {
        panels[idx].disable();
        idx = (idx + 1) % panels.length;
        panels[idx].enable();
        switchDisplay = System.currentTimeMillis() + 10000;
      }
      if (!mqttConnection.isConnected() && System.currentTimeMillis() > nextUpdate) {
        disconnectDisplay();
        nextUpdate = System.currentTimeMillis() + 60000;
      }
      try {
        Thread.sleep(10);
        KeyStroke keyStroke = screen.pollInput();
        if (keyStroke != null && keyStroke.getCharacter().equals('q')) {
          runFlag.set(false);
          return 0;
        }
      } catch (IOException e) {
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return nextUpdate;
  }

  public void disconnectDisplay() {
    try {
      screen.clear();
      TextGraphics invertTextGraphics = screen.newTextGraphics();
      invertTextGraphics.setForegroundColor(TextColor.ANSI.BLACK);
      invertTextGraphics.setBackgroundColor(TextColor.ANSI.WHITE);
      invertTextGraphics.putString(0, 0, "No Data received from server                                                  ");
      screen.refresh();
      disconnected = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void connectAndSubscribeToServer() throws MqttException {
    mqttConnection.subscribe("$SYS/server/status");
    mqttConnection.subscribe("$SYS/server/destination/status");
    mqttConnection.subscribe("$SYS/server/interface/status");
  }

}
