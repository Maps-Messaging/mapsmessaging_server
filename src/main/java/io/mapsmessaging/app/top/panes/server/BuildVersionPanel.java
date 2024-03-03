package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

public class BuildVersionPanel extends ServerStatusUpdate {

  public BuildVersionPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Version : ", labelText, valueText, new StringFormatter(10, true));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getVersion());
  }
}