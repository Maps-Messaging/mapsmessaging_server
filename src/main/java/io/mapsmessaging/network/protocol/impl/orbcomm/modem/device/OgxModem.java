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
 * OGx dialect: minimal overrides; uses BaseModem defaults where identical.
 */
public class OgxModem extends BaseModem {

  public OgxModem(Consumer<Packet> sender) {
    super(sender);
  }

  @Override
  protected String parsedPositionCmd() {
    return "AT%GPSPOS?"; // explicit, though BaseModem already defaults to this
  }

  @Override
  protected void handleUnsolicitedLine(String line) {
    if (line == null || line.isEmpty()) return;

    if (line.startsWith("%MGRS:")) {
      onMoProgress(line);
      return;
    }
    if (line.startsWith("%GPSPOS:")) {
      onParsedPosition(line);
      return;
    }
    if (line.startsWith("%GPS:") || line.startsWith("$GP") || line.startsWith("$GN")) {
      onGpsNmea(line);
      return;
    }

    onVendorEvent(line); // %NETSTATE:, %WAKE:, %TIME:, %SYSE:, etc.
  }

  @Override
  protected void onMoProgress(String mgrsLine) { /* no-op */ }

  @Override
  protected void onGpsNmea(String nmeaLine) { /* no-op */ }

  @Override
  protected void onParsedPosition(String line) { /* no-op */ }

  @Override
  protected void onVendorEvent(String line) { /* no-op */ }
}
