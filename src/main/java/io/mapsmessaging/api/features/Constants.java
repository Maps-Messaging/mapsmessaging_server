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
  private CompressionMode messageCompression = CompressionMode.NONE;

  @Getter
  @Setter
  private int minimumMessageSize = 1024;

  public void setMessageCompression(String name){
    switch(name.toLowerCase()){
      case "inflator":
        messageCompression = CompressionMode.INFLATOR;
        break;

      case "none":
      default:
        messageCompression = CompressionMode.NONE;
    }
  }

  private Constants(){}

}
