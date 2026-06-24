package io.mapsmessaging.state.n2k.listener;

public final class N2kPgns {

  public static final int ISO_REQUEST = 59904;
  public static final int ISO_ADDRESS_CLAIM = 60928;

  public static final int SYSTEM_TIME = 126992;
  public static final int HEARTBEAT = 126993;

  public static final int VESSEL_HEADING = 127250;
  public static final int RATE_OF_TURN = 127251;
  public static final int ATTITUDE = 127257;
  public static final int MAGNETIC_VARIATION = 127258;

  public static final int BATTERY_STATUS = 127508;
  public static final int INVERTER_STATUS = 127509;

  public static final int POSITION_RAPID_UPDATE = 129025;
  public static final int COG_SOG_RAPID_UPDATE = 129026;
  public static final int TIME_DATE = 129033;
  public static final int GNSS_POSITION_DATA = 129029;
  public static final int GNSS_DOPS = 129539;

  public static final int AIS_CLASS_A_POSITION_REPORT = 129038;
  public static final int AIS_CLASS_B_POSITION_REPORT = 129039;

  public static final int WIND_DATA = 130306;
  public static final int ENVIRONMENTAL_PARAMETERS = 130311;

  private N2kPgns() {
  }
}