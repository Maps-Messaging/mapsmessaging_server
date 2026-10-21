package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationParserTest {

  @Test
  void parsesConfiguredGpggaSentence() {
    LocationParser parser = new LocationParser();

    Sentence sentence = parser.parseLocation(
        "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,"
    );

    assertNotNull(sentence);
    assertEquals("GPGGA", sentence.getName());
    assertNotNull(sentence.get("latitude"));
    assertNotNull(sentence.get("longitude"));
  }
}
