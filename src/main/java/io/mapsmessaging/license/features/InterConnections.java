package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class InterConnections {
  private boolean pushSupport;
  private boolean pullSupport;
  private boolean filteringSupport;
}
