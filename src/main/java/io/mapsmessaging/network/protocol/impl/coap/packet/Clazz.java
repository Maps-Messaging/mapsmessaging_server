package io.mapsmessaging.network.protocol.impl.coap.packet;

import lombok.Getter;

public enum Clazz {
  REQUEST(0, "Request", "Request packet"),
  SUCCESS_RESPONSE(2, "Success", "Successful response packet"),
  ERROR_RESPONSE(4, "Error", "Error response packet"),
  SERVER_ERROR_RESPONSE(5, "ServerError", "Server error response packet"),
  SIGNAL(7, "Signal", "Signal packet");


  @Getter
  private final int value;
  @Getter
  private final String name;
  @Getter
  private final String description;

  Clazz(int val, String name, String description) {
    value = val;
    this.name = name;
    this.description = description;
  }

  public static Clazz valueOf(final int value) {
    switch (value) {
      case 0:
        return REQUEST;
      case 2:
        return SUCCESS_RESPONSE;
      case 4:
        return ERROR_RESPONSE;
      case 5:
        return SERVER_ERROR_RESPONSE;
      case 7:
        return SIGNAL;
      default:
        return null;
    }
  }

  @Override
  public String toString() {
    return "Clazz:" + value + " " + name + " ( " + description + " )";
  }
}
