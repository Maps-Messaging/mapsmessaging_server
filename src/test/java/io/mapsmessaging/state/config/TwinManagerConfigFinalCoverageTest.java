package io.mapsmessaging.state.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TwinManagerConfigFinalCoverageTest {

  @Test
  void altitudeModeParserUsesDefaultForMissingValuesAndNormalizesCase() throws Exception {
    TwinManagerConfig config = new TwinManagerConfig();
    Method method = TwinManagerConfig.class.getDeclaredMethod(
        "parseAltitudeMode", String.class, AltitudeMode.class);
    method.setAccessible(true);

    assertEquals(AltitudeMode.CURRENT, method.invoke(config, null, AltitudeMode.CURRENT));
    assertEquals(AltitudeMode.CURRENT, method.invoke(config, " ", AltitudeMode.CURRENT));
    assertEquals(AltitudeMode.FIXED, method.invoke(config, " fixed ", AltitudeMode.CURRENT));
    assertEquals(AltitudeMode.TASK, method.invoke(config, "task", AltitudeMode.CURRENT));
  }

  @Test
  void messageEncodingParserUsesDefaultAndAcceptsLowercaseNames() throws Exception {
    TwinManagerConfig config = new TwinManagerConfig();
    Method method = TwinManagerConfig.class.getDeclaredMethod(
        "parseMessageEncoding", String.class, MessageEncodingEnum.class);
    method.setAccessible(true);

    assertEquals(
        MessageEncodingEnum.JSON,
        method.invoke(config, null, MessageEncodingEnum.JSON));
    assertEquals(
        MessageEncodingEnum.PROTOBUF,
        method.invoke(config, " protobuf ", MessageEncodingEnum.JSON));
  }

  @Test
  void terminalActionParserUsesDefaultAndParsesLegacyAndCurrentValues() throws Exception {
    TwinManagerConfig config = new TwinManagerConfig();
    Method method = TwinManagerConfig.class.getDeclaredMethod(
        "parseTerminalAction", String.class, StopActionEnum.class);
    method.setAccessible(true);

    assertEquals(
        StopActionEnum.HOLD_POSITION,
        method.invoke(config, "", StopActionEnum.HOLD_POSITION));
    assertEquals(
        StopActionEnum.RETURN_TO_HOME,
        method.invoke(config, " return_to_home ", StopActionEnum.HOLD_POSITION));
    assertEquals(
        StopActionEnum.STOP,
        method.invoke(config, "stop", StopActionEnum.HOLD_POSITION));
  }

  @Test
  void optionalPositiveDoubleAndVehicleClassHelpersRejectInvalidValues() throws Exception {
    TwinManagerConfig config = new TwinManagerConfig();
    Method read = TwinManagerConfig.class.getDeclaredMethod(
        "readOptionalPositiveDouble", ConfigurationProperties.class, String.class);
    read.setAccessible(true);
    Method vehicle = TwinManagerConfig.class.getDeclaredMethod(
        "parseVehicleClass", String.class, VehicleClass.class);
    vehicle.setAccessible(true);

    ConfigurationProperties properties = new ConfigurationProperties();
    assertNull(read.invoke(config, properties, "range"));

    properties.put("range", 0.0);
    assertNull(read.invoke(config, properties, "range"));
    properties.put("range", -1.0);
    assertNull(read.invoke(config, properties, "range"));
    properties.put("range", 12.5);
    assertEquals(12.5, (Double) read.invoke(config, properties, "range"), 0.0);

    assertEquals(VehicleClass.USV, vehicle.invoke(config, null, VehicleClass.USV));
    assertEquals(VehicleClass.UAV, vehicle.invoke(config, " uav ", VehicleClass.USV));
  }
}