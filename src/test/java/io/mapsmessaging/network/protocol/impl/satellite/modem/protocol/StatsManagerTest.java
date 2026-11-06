package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatsManagerTest {

  @Test
  void disabledLocationPublishingDoesNotPollModem() {
    Modem modem = mock(Modem.class);

    new StatsManager(modem, 0, mock(Destination.class))
        .processLocationRequest(NetworkStatus.parse("5"));

    verifyNoInteractions(modem);
  }

  @Test
  void publishedStatsContainLocationJammingNetworkAndTemperature() throws Exception {
    Modem modem = mock(Modem.class);
    Destination destination = mock(Destination.class);
    when(modem.getJammingIndicator())
        .thenReturn(CompletableFuture.completedFuture(17));
    when(modem.getJammingStatus())
        .thenReturn(CompletableFuture.completedFuture(0x87));
    when(modem.getTemperature())
        .thenReturn(CompletableFuture.completedFuture("00250"));
    when(modem.getModemProtocol()).thenReturn(null);

    StatsManager manager = new StatsManager(modem, 1000, destination);
    Method publish = StatsManager.class.getDeclaredMethod(
        "publishStats", NetworkStatus.class, double.class, double.class, String.class);
    publish.setAccessible(true);
    publish.invoke(manager, NetworkStatus.parse("1"), 38.5, -9.25, "12");

    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(message.capture());

    JsonObject json = JsonParser.parseString(
        new String(message.getValue().getOpaqueData(), StandardCharsets.UTF_8))
        .getAsJsonObject();

    assertEquals("38° 30' 00.000 N",
        json.getAsJsonObject("location").get("latitude").getAsString());
    assertEquals("9° 15' 00.000 W",
        json.getAsJsonObject("location").get("longitude").getAsString());
    assertEquals("Antenna Cut",
        json.getAsJsonObject("jamming").get("status").getAsString());
    assertFalse(json.getAsJsonObject("satelliteTxStatus").get("canSend").getAsBoolean());
    assertEquals(25.0f, json.get("temperature").getAsFloat(), 0.0f);
  }

  @Test
  void malformedTemperatureConvertsToNaN() throws Exception {
    StatsManager manager =
        new StatsManager(mock(Modem.class), 1000, mock(Destination.class));
    Method method = StatsManager.class.getDeclaredMethod("toFloat", String.class);
    method.setAccessible(true);

    assertTrue(Float.isNaN((Float) method.invoke(manager, "abcde")));
  }
}
