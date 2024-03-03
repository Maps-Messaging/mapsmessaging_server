package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

public class BuildDatePanel extends ServerStatusUpdate {

  public BuildDatePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, " - ", labelText, valueText, new StringFormatter(20, true));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getBuildDate());
  }
}
