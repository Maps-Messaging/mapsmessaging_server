/*
 *
 *  Copyright [ 2020 - 2025 ] Matthew Buckton
 *  MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device;

import io.mapsmessaging.network.io.Packet;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Creates a modem implementation (OGX or ODI) after a short AT probe.
 * Assumes the transport will route inbound data to the same BaseModem instance used during probing.
 */
public final class ModemFactory {

  private ModemFactory() {
  }

  public enum Mode {OGX, ODI, UNKNOWN}

  public static final class DetectionOptions {
    public final long atiTimeoutMs;
    public final long gpsposTimeoutMs;
    public final long posrTimeoutMs;
    public final long gpsBurstTimeoutMs;
    public final Pattern ogxHint;
    public final Pattern odiHint;
    public final Mode forceMode;

    private DetectionOptions(long atiTimeoutMs, long gpsposTimeoutMs, long posrTimeoutMs, long gpsBurstTimeoutMs,
                             Pattern ogxHint, Pattern odiHint, Mode forceMode) {
      this.atiTimeoutMs = atiTimeoutMs;
      this.gpsposTimeoutMs = gpsposTimeoutMs;
      this.posrTimeoutMs = posrTimeoutMs;
      this.gpsBurstTimeoutMs = gpsBurstTimeoutMs;
      this.ogxHint = ogxHint;
      this.odiHint = odiHint;
      this.forceMode = forceMode;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static DetectionOptions defaults() {
      return builder().build();
    }

    public static final class Builder {
      private long atiTimeoutMs = 1500;
      private long gpsposTimeoutMs = 1200;
      private long posrTimeoutMs = 1200;
      private long gpsBurstTimeoutMs = 2000;
      private Pattern ogxHint = Pattern.compile(".*\\bogx\\b|\\b6\\.[0-9].*", Pattern.CASE_INSENSITIVE);
      private Pattern odiHint = Pattern.compile(".*\\b(idp|odi)\\b|\\b3\\.[0-9].*", Pattern.CASE_INSENSITIVE);
      private Mode forceMode = Mode.UNKNOWN;

      public Builder atiTimeoutMs(long v) {
        atiTimeoutMs = v;
        return this;
      }

      public Builder gpsposTimeoutMs(long v) {
        gpsposTimeoutMs = v;
        return this;
      }

      public Builder posrTimeoutMs(long v) {
        posrTimeoutMs = v;
        return this;
      }

      public Builder gpsBurstTimeoutMs(long v) {
        gpsBurstTimeoutMs = v;
        return this;
      }

      public Builder ogxHintRegex(String regex) {
        ogxHint = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return this;
      }

      public Builder odiHintRegex(String regex) {
        odiHint = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return this;
      }

      public Builder forceMode(Mode v) {
        forceMode = v;
        return this;
      }

      public DetectionOptions build() {
        return new DetectionOptions(atiTimeoutMs, gpsposTimeoutMs, posrTimeoutMs, gpsBurstTimeoutMs, ogxHint, odiHint, forceMode);
      }
    }
  }

  /**
   * Result of detection for logging/metrics if needed.
   */
  public static final class DetectionResult {
    public final Mode mode;
    public final String firmwareInfo;

    public DetectionResult(Mode mode, String firmwareInfo) {
      this.mode = mode;
      this.firmwareInfo = firmwareInfo;
    }
  }

  /**
   * One-shot creation with default probe options.
   */
  public static BaseModem create(Consumer<Packet> sender) {
    return create(sender, DetectionOptions.defaults());
  }

  /**
   * One-shot creation with custom probe options.
   */
  public static BaseModem create(Consumer<Packet> sender, DetectionOptions opts) {
    DetectionResult dr = detect(sender, opts);
    return dr.mode == Mode.OGX ? new OgxModem(sender) : new OdiModem(sender);
  }

  /**
   * Detect only; returns mode + firmware string (if any).
   */
  public static DetectionResult detect(Consumer<Packet> sender, DetectionOptions opts) {
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(opts, "opts");

    if (opts.forceMode == Mode.OGX) return new DetectionResult(Mode.OGX, "forced");
    if (opts.forceMode == Mode.ODI) return new DetectionResult(Mode.ODI, "forced");

    BaseModem probe = new BaseModem(sender, Math.max(1, Math.min(opts.atiTimeoutMs, 5000)));
    try {
      // ATI
      String ati = safeGet(probe.getFirmwareId(), opts.atiTimeoutMs);
      Mode mode = inferFromAti(ati, opts);
      if (mode != Mode.UNKNOWN) return new DetectionResult(mode, nullToEmpty(ati));

      // Capability probes
      String gpspos = safeGet(probe.sendATCommand("AT%GPSPOS?"), opts.gpsposTimeoutMs);
      if (isOk(gpspos)) return new DetectionResult(Mode.OGX, nullToEmpty(ati));

      String posr = safeGet(probe.sendATCommand("AT%POSR?"), opts.posrTimeoutMs);
      if (isOk(posr)) return new DetectionResult(Mode.ODI, nullToEmpty(ati));

      // Last resort: short NMEA burst
      String gps = safeGet(probe.sendATCommand("AT%GPS=1"), opts.gpsBurstTimeoutMs);
      Mode fallback = containsNmea(gps) ? Mode.ODI : Mode.OGX;
      return new DetectionResult(fallback, nullToEmpty(ati));
    } finally {
      probe.close(); // ensure scheduler/resources are torn down
    }
  }

  // ---------- helpers ----------

  private static Mode inferFromAti(String ati, DetectionOptions opts) {
    if (ati == null || ati.isBlank()) return Mode.UNKNOWN;
    if (opts.ogxHint.matcher(ati).matches()) return Mode.OGX;
    if (opts.odiHint.matcher(ati).matches()) return Mode.ODI;
    return Mode.UNKNOWN;
  }

  private static String safeGet(CompletableFuture<String> f, long timeoutMs) {
    if (f == null) return null;
    try {
      return f.orTimeout(Math.max(1, timeoutMs), TimeUnit.MILLISECONDS).join();
    } catch (Throwable ignore) {
      return null;
    }
  }

  private static boolean isOk(String resp) {
    return resp != null && !resp.trim().isEmpty() && !resp.contains("ERROR");
  }

  private static boolean containsNmea(String resp) {
    if (resp == null) return false;
    return resp.contains("$GP") || resp.contains("$GN") || resp.contains("%GPS:");
    // Note: both OGX and ODI can emit NMEA; this is only a fallback discriminator.
  }

  private static String nullToEmpty(String s) {
    return Optional.ofNullable(s).orElse("");
  }
}
