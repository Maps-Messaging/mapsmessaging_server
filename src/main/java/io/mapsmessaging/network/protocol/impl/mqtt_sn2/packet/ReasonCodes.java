package io.mapsmessaging.network.protocol.impl.mqtt_sn2.packet;

import lombok.Getter;

public enum ReasonCodes {

  Success(0x0),
  Congestion(0x1),
  InvalidTopicAlias(0x2),
  NotSupported(0x3),
  NoSession(0x5),
  ContinueAuthentication(0x18),
  ReAuthenticate(0x19),
  BadAuth(0x8C),
  NoAuth(0x87),
  PacketTooLarge(0x95),
  PayloadFormatInvalid(0x99),
  Unsupported(0x84);

  @Getter private final int value;
  ReasonCodes(int val){
    value = val;
  }

  static ReasonCodes lookup(int val){
    switch(val){
      case 0x0:
        return Success;
      case 0x1:
        return Congestion;
      case 0x2:
        return InvalidTopicAlias;
      case 0x3:
        return NotSupported;
      case 0x5:
        return NoSession;
      case 0x18:
        return ContinueAuthentication;
      case 0x19:
        return ReAuthenticate;
      case 0x8C:
        return BadAuth;
      case 0x87:
        return NoAuth;
      case 0x95:
        return PacketTooLarge;
      case 0x99:
        return PayloadFormatInvalid;
      case 0x84:
        return Unsupported;
      default:
        return Unsupported;
    }
  }
}

