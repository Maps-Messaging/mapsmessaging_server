package io.mapsmessaging.network.protocol.impl.n2k.msg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationInformationFieldValueSourceTest {

  @Test
  void defaultsExposeServerBridgeAndManufacturerDescriptions() {
    ConfigurationInformationFieldValueSource source = new ConfigurationInformationFieldValueSource();

    assertEquals("Maps Messaging Server", source.getString("installationDescriptionField1"));
    assertEquals("AIS Source Bridge", source.getString("installationDescriptionField2"));
    assertEquals("MapsMessaging B.V.", source.getString("manufacturerInformationField3"));
  }

  @Test
  void explicitValuesArePreserved() {
    ConfigurationInformationFieldValueSource source =
        new ConfigurationInformationFieldValueSource("Bridge A", "Port feed", "Example Ltd");

    assertEquals("Bridge A", source.getString("installationDescriptionField1"));
    assertEquals("Port feed", source.getString("installationDescriptionField2"));
    assertEquals("Example Ltd", source.getString("manufacturerInformationField3"));
  }

  @Test
  void nullAndBlankValuesFallBackIndependently() {
    ConfigurationInformationFieldValueSource source =
        new ConfigurationInformationFieldValueSource(null, " ", "");

    assertEquals("Maps Messaging Server", source.getString("installationDescriptionField1"));
    assertEquals("AIS Source Bridge", source.getString("installationDescriptionField2"));
    assertEquals("MapsMessaging B.V.", source.getString("manufacturerInformationField3"));
  }
}
