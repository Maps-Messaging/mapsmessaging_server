package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.BaseTestConfig;

public class MQTTBaseTest extends BaseTestConfig {
  public static final String RESTRICTED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final int MQTT_3_1 = 3;
  public static final int MQTT_3_1_1 = 4;
  public static final int MQTT_5_0 = 5;

  String getClientId(String proposed, int version){
    if(version == MQTT_3_1){
      StringBuilder sb = new StringBuilder();
      for(int x=0;x<proposed.length();x++){
        char test = proposed.charAt(x);
        if(RESTRICTED_CHARACTERS.indexOf(test) != -1){
          sb.append(test);
          if(sb.length() == 23){
            break;
          }
        }
      }
      return sb.toString();
    }
    else{
      return proposed;
    }
  }
}
