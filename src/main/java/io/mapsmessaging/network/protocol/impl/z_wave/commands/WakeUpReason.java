package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import lombok.Getter;

public enum WakeUpReason {

  RESET(0, "The Z-Wave API Module has been woken up by reset or external interrupt."),
  TIMER(1, "The Z-Wave API Module has been woken up by a timer."),
  BEAM(2, "The Z-Wave API Module has been woken up by a Wake Up Beam."),
  WATCH_DOG_RESET(3, "The Z-Wave API Module has been woken up by a reset triggered by the watch-dog"),
  EXTERNAL_INTERRUPT(4, "The Z-Wave API Module has been woken up by an external interrupt."),
  POWER_UP(5, "The Z-Wave API Module has been woken up by a powering up."),
  USB_SUSPEND(6, "The Z-Wave API Module has been woken up by USB Suspend."),
  SOFTWARE_RESET(7, "The Z-Wave API Module has been woken up by a reset triggered by software."),
  EMERGENCY_WATCH_DOG_RESET(8,"The Z-Wave API Module has been woken up by an emergency watchdog reset." ),
  BROWNOUT(9, "The Z-Wave API Module has been woken up by a reset triggered by brownout circuit."),
  UNKNOWN(10, "Unknown state");


  @Getter
  private final int id;
  @Getter
  private final String description;

  WakeUpReason(int id, String description){
    this.id = id;
    this.description = description;
  }

  public static WakeUpReason fromId(int id){
    switch (id){
      case 0:
        return RESET;
      case 1:
        return TIMER;
      case 2:
        return BEAM;
      case 3:
        return WATCH_DOG_RESET;
      case 4:
        return EXTERNAL_INTERRUPT;
      case 5:
        return POWER_UP;
      case 6:
        return USB_SUSPEND;
      case 7:
        return SOFTWARE_RESET;
      case 8:
        return EMERGENCY_WATCH_DOG_RESET;
      case 9:
        return BROWNOUT;
    }
    return UNKNOWN;
  }
}
/*
0x00 Reset
The Z-Wave API Module has been woken up by reset or external interrupt.
0x01 Wake Up Timer
The Z-Wave API Module has been woken up by a timer.
0x02 Wake Up Beam

0x03 Watchdog reset
The Z-Wave API Module has been woken up by a reset triggered by the watch-
dog.
0x04 External interrupt

0x05 Power Up

0x06 USB Suspend

0x07 Software reset

0x08 Emergency Watchdog Reset

0x09 Brownout circuit
The Z-Wave API Module has been woken up by a reset triggered by brownout
circuit.

 */
