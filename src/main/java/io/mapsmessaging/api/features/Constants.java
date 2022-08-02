package io.mapsmessaging.api.features;

import lombok.Getter;
import lombok.Setter;

public class Constants {

  private static final Constants instance;
  static{
    instance = new Constants();
  }

  public static Constants getInstance(){
    return instance;
  }

  @Getter
  @Setter
  private boolean enableMessageStoreCompression = false;

  @Getter
  @Setter
  private int minimumMessageSize = 1024;

  private Constants(){}

}
