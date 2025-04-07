package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Management {
  private boolean jolokia;
  private boolean restApi;
  private boolean jmx;
  private boolean sysTopics;
}
