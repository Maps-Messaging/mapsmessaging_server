package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import lombok.Getter;
/*
 +--------------------------+----------+----+------------------------+
   | Media type               | Encoding | ID | Reference              |
   +--------------------------+----------+----+------------------------+
   | text/plain;              | -        |  0 | [RFC2046] [RFC3676]    |
   | charset=utf-8            |          |    | [RFC5147]              |
   | application/link-format  | -        | 40 | [RFC6690]              |
   | application/xml          | -        | 41 | [RFC3023]              |
   | application/octet-stream | -        | 42 | [RFC2045] [RFC2046]    |
   | application/exi          | -        | 47 | [REC-exi-20140211]     |
   | application/json         | -        | 50 | [RFC7159]              |
   +--------------------------+----------+----+------------------------+
 */
public enum Format {
  TEXT_PLAIN(0, "text/plain"),
  LINK_FORMAT(40, "application/link-format"),
  XML(41, "application/xml"),
  OCTET_STREAM(42, "application/octet-stream"),
  EXI(47, "application/exi"),
  JSON(50, "application/json");


  @Getter
  private final int id;

  @Getter
  private final String name;

  Format(int id, String name){
    this.id = id;
    this.name = name;
  }

  public static Format stringValueOf(String value) {
    switch (value) {
      case "text/plain":
        return TEXT_PLAIN;
      case "application/link-format":
        return LINK_FORMAT;
      case "application/xml":
        return XML;
      case "application/exi":
        return EXI;
      case "application/json":
        return JSON;
      case "application/octet-stream":
      default:
        return OCTET_STREAM;
    }
  }

  public static Format valueOf(int value) {
    switch(value){
      case 0:
        return TEXT_PLAIN;
      case 40:
        return LINK_FORMAT;
      case 41:
        return XML;
      case 47:
        return EXI;
      case 50:
        return JSON;
      case 42:
      default:
        return OCTET_STREAM;
    }
  }
}
