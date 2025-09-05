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

package io.mapsmessaging.app.top;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import io.mapsmessaging.app.top.network.RestRequestManager;
import io.mapsmessaging.app.top.panes.PaneUpdate;
import io.mapsmessaging.app.top.panes.ServerDetailsPane;
import io.mapsmessaging.app.top.panes.ServerStatusPane;
import io.mapsmessaging.app.top.panes.destination.DestinationPane;
import io.mapsmessaging.app.top.panes.interfaces.InterfacesPane;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalTop {

  private final RestRequestManager restConnection;

  private final ServerDetailsPane serverDetailsPane;
  private final ServerStatusPane serverStatusPane;
  private final DestinationPane destinationPane;
  private final InterfacesPane interfacesPane;

  private final  Screen screen;
  private final AtomicBoolean runFlag;
  private boolean disconnected = false;
  private final PaneUpdate[] panels;
  private int idx = 0;
  private long switchDisplay = System.currentTimeMillis() + 3000;

  @SuppressWarnings("java:S2095") // Yes we create a resource for the terminal, but will close on exit
  public TerminalTop(String url, String username, String password) throws IOException {
    runFlag = new AtomicBoolean(true);
    restConnection = RestRequestManager.getInstance();
    restConnection.initialize(url, username, password);

    // Setup terminal and screen layers
    Terminal terminal = new DefaultTerminalFactory().createTerminal();
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
    serverDetailsPane = new ServerDetailsPane(normalText, boldText);
    serverStatusPane = new ServerStatusPane(normalText, boldText);
    destinationPane = new DestinationPane(6, rows,  normalText, boldText, headerText);
    interfacesPane = new InterfacesPane(6, rows, normalText, boldText, headerText);
    panels = new PaneUpdate[2];
    panels[0] = destinationPane;
    panels[1] = interfacesPane;
    panels[0].enable();
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
      message = restConnection.getUpdate();
      if (message != null) {
        if (disconnected) {
          disconnected = false;
          screen.clear();
        }
        serverDetailsPane.update(message);
        serverStatusPane.update(message);
        destinationPane.update(message);
        interfacesPane.update(message);
        screen.refresh();
        nextUpdate = System.currentTimeMillis() + 60000;
      }
    }
    try {
      screen.stopScreen(); // Properly stop the screen when done
      restConnection.close();
    } catch (IOException e) {
      // ignore since it is an expected exceptio that we handle
    }
  }

  private long waitForSomething(long nextUpdate) {
    while (restConnection.isQueueEmpty()) {
      if (!runFlag.get()) {
        return 0;
      }
      if (System.currentTimeMillis() > switchDisplay) {
        panels[idx].disable();
        idx = (idx + 1) % panels.length;
        panels[idx].enable();
        switchDisplay = System.currentTimeMillis() + 10000;
      }
      if (!restConnection.isConnected() && System.currentTimeMillis() > nextUpdate) {
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
        // ignore
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return nextUpdate;
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
      // Ignore
    }
  }

}
