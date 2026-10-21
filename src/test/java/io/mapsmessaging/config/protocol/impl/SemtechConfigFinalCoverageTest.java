package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechTransmitDefaultsDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SemtechConfigFinalCoverageTest {

  @Test
  void transmitBlockParsesBooleanNumericAndStringFields() {
    ConfigurationProperties tx = new ConfigurationProperties();
    tx.put("imme", true);
    tx.put("freq", 868.1);
    tx.put("rfch", 1);
    tx.put("powe", 14);
    tx.put("modu", "LORA");
    tx.put("datr", "SF7BW125");
    tx.put("codr", "4/5");
    tx.put("ipol", true);
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("tx", tx);

    SemtechConfig config = new SemtechConfig(root);

    assertTrue(config.getTransmitDefaults().isImme());
    assertEquals(868.1, config.getTransmitDefaults().getFreq(), 0.0);
    assertEquals(1, config.getTransmitDefaults().getRfch());
    assertEquals(14, config.getTransmitDefaults().getPowe());
    assertEquals("LORA", config.getTransmitDefaults().getModu());
    assertTrue(config.getTransmitDefaults().isIpol());
  }

  @Test
  void packedTransmitDefaultsCanBeParsedBackWithoutLosingValues() {
    SemtechConfig config = new SemtechConfig(new ConfigurationProperties());
    config.getTransmitDefaults().setImme(true);
    config.getTransmitDefaults().setFreq(915.5);
    config.getTransmitDefaults().setRfch(2);
    config.getTransmitDefaults().setPowe(20);
    config.getTransmitDefaults().setModu("LORA");
    config.getTransmitDefaults().setDatr("SF8BW125");
    config.getTransmitDefaults().setCodr("4/6");
    config.getTransmitDefaults().setIpol(true);

    SemtechConfig restored = new SemtechConfig(config.toConfigurationProperties());

    assertTrue(restored.getTransmitDefaults().isImme());
    assertEquals(915.5, restored.getTransmitDefaults().getFreq(), 0.0);
    assertEquals(2, restored.getTransmitDefaults().getRfch());
    assertEquals(20, restored.getTransmitDefaults().getPowe());
    assertEquals("SF8BW125", restored.getTransmitDefaults().getDatr());
    assertEquals("4/6", restored.getTransmitDefaults().getCodr());
  }

  @Test
  void safeEqualsHandlesNullEqualAndDifferentStrings() throws Exception {
    SemtechConfig config = new SemtechConfig(new ConfigurationProperties());
    Method method = SemtechConfig.class.getDeclaredMethod(
        "safeEquals", String.class, String.class);
    method.setAccessible(true);

    assertEquals(true, method.invoke(config, null, null));
    assertEquals(false, method.invoke(config, null, "x"));
    assertEquals(true, method.invoke(config, "x", "x"));
    assertEquals(false, method.invoke(config, "x", "y"));
  }

  @Test
  void transmitDefaultUpdaterHandlesNullAndInitializesMissingCurrentDefaults() throws Exception {
    SemtechConfig config = new SemtechConfig(new ConfigurationProperties());
    Method method = SemtechConfig.class.getDeclaredMethod(
        "updateTransmitDefaults", SemtechTransmitDefaultsDTO.class);
    method.setAccessible(true);

    assertEquals(false, method.invoke(config, new Object[]{null}));

    config.setTransmitDefaults(null);
    SemtechTransmitDefaultsDTO defaults = new SemtechTransmitDefaultsDTO();
    defaults.setImme(true);
    defaults.setFreq(100.0);
    defaults.setRfch(3);
    defaults.setPowe(9);
    defaults.setModu("FSK");
    defaults.setDatr("50000");
    defaults.setCodr("");
    defaults.setIpol(false);

    assertEquals(true, method.invoke(config, defaults));
    assertNotNull(config.getTransmitDefaults());
    assertEquals("FSK", config.getTransmitDefaults().getModu());
    assertEquals(false, method.invoke(config, defaults));
  }
}