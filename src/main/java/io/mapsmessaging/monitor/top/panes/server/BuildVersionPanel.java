package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.StringFormatter;

public class BuildVersionPanel extends ServerStatusUpdate {

  public BuildVersionPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Version : ", labelText, valueText, new StringFormatter(10, true));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getVersion());
  }
}