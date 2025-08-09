/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  https://commonsclause.com/
 *
 */

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device;

import io.mapsmessaging.network.io.Packet;

import java.util.function.Consumer;

/**
 * IDP/ODI dialect: overrides only what's different from the BaseModem defaults.
 */
public class OdiModem extends BaseModem {

  public OdiModem(Consumer<Packet> sender) {
    super(sender);
  }

  // --- Dialect differences ---

  @Override
  protected String parsedPositionCmd() {
    return "AT%POSR?"; // ODI prefers POSR over GPSPOS
  }

  // Keep BaseModem defaults for GPSP/NETSTATE (will return ERROR if unsupported).

  // --- URC handling ---

  @Override
  protected void handleUnsolicitedLine(String line) {
    if (line == null || line.isEmpty()) return;

    if (line.startsWith("%MGRS:")) {
      onMoProgress(line);
      return;
    }
    if (line.startsWith("%POSR:")) {
      onParsedPosition(line);
      return;
    }
    if (line.startsWith("%GPS:") || line.startsWith("$GP") || line.startsWith("$GN")) {
      onGpsNmea(line);
      return;
    }

    onVendorEvent(line); // %SYSE:, etc.
  }

  // Optional event hooks to wire into your event bus:

  @Override
  protected void onMoProgress(String mgrsLine) { /* no-op */ }

  @Override
  protected void onGpsNmea(String nmeaLine) { /* no-op */ }

  @Override
  protected void onParsedPosition(String line) { /* no-op */ }

  @Override
  protected void onVendorEvent(String line) { /* no-op */ }
}
