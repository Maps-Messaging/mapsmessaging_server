package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackedOption extends Option {


  private final List<String> path;
  private final String delimiter;


  public PackedOption(int id, String delimiter) {
    super(id);
    path = new ArrayList<>();
    this.delimiter = delimiter;
  }

  public void add(String part) {
    path.add(part);
  }

  public void setPath(String completePath) {
    path.clear();
    path.addAll(Arrays.asList(completePath.split(delimiter)));
  }

  public void update(byte[] data) {
    path.add(new String(data));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String uri : path) {
      if (!first) {
        sb.append(delimiter);
      }
      first = false;
      sb.append(uri);
    }
    return sb.toString();
  }
}

