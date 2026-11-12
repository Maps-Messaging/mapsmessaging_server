package io.mapsmessaging.network.protocol.impl.mavlink.messages;

/** Geodetic coordinates and the explicit MAVLink altitude frame. */
public final class MavlinkPosition {
  public static final int GLOBAL = 0;
  public static final int GLOBAL_RELATIVE_ALT = 3;
  public static final int GLOBAL_INT = 5;
  public static final int GLOBAL_RELATIVE_ALT_INT = 6;
  public static final int GLOBAL_TERRAIN_ALT = 10;
  public static final int GLOBAL_TERRAIN_ALT_INT = 11;

  private final Double latitude;
  private final Double longitude;
  private final Double altitudeMeters;
  private final int frame;

  public MavlinkPosition(Double latitude, Double longitude, Double altitudeMeters, int frame) {
    if (frame != GLOBAL && frame != GLOBAL_RELATIVE_ALT && frame != GLOBAL_INT
        && frame != GLOBAL_RELATIVE_ALT_INT && frame != GLOBAL_TERRAIN_ALT && frame != GLOBAL_TERRAIN_ALT_INT) {
      throw new IllegalArgumentException("Unsupported MAVLink position frame: " + frame);
    }
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitudeMeters = altitudeMeters;
    this.frame = frame;
  }

  public Double getLatitude() { return latitude; }
  public Double getLongitude() { return longitude; }
  public Double getAltitudeMeters() { return altitudeMeters; }
  public int getFrame() { return frame; }
  public Double getAltitudeMslMeters() { return frame == GLOBAL || frame == GLOBAL_INT ? altitudeMeters : null; }
  public Double getAltitudeAglMeters() { return frame == GLOBAL_TERRAIN_ALT || frame == GLOBAL_TERRAIN_ALT_INT ? altitudeMeters : null; }
  public Double getAltitudeRelativeMeters() { return frame == GLOBAL_RELATIVE_ALT || frame == GLOBAL_RELATIVE_ALT_INT ? altitudeMeters : null; }
  public Double getPreferredAltitudeMeters() { return altitudeMeters; }
}
