/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import io.mapsmessaging.network.protocol.impl.nmea.types.LongType;
import io.mapsmessaging.network.protocol.impl.nmea.types.PositionType;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.BaseModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;

import java.io.IOException;
import java.util.List;

public class StatsManager {
  private final Modem modem;
  private final long locationPollInterval;
  private final LocationParser locationParser;
  private final Destination destination;

  private long lastLocationPoll;

  public StatsManager(Modem modem, long locationPollInterval, Destination destination) {
    this.modem = modem;
    this.destination = destination;
    this.locationPollInterval = locationPollInterval;
    locationParser = new LocationParser();
    lastLocationPoll = System.currentTimeMillis();
  }

  public void processLocationRequest(NetworkStatus networkStatus) {
    if(destination == null || locationPollInterval == 0) {
      return; // nothing to do
    }
    if (lastLocationPoll + locationPollInterval < System.currentTimeMillis()) {
      lastLocationPoll = System.currentTimeMillis();
      List<String> location = modem.getLocation().join();
      for (String loc : location) {
        Sentence sentence = locationParser.parseLocation(loc);
        if (sentence != null && sentence.getName().equalsIgnoreCase("GPGGA")) {
          PositionType latitude = (PositionType) sentence.get("latitude");
          PositionType longitude = (PositionType) sentence.get("longitude");
          LongType satellites = (LongType) sentence.get("satellites");
          LocationManager.getInstance().setPosition(latitude.getPosition(), longitude.getPosition());
          publishStats(networkStatus, latitude.getPosition(), longitude.getPosition(), satellites.toString());
        }
      }
    }
  }


  private void publishStats(NetworkStatus networkStatus, double lat, double lon, String satellites) {
    Integer jamIndicator = modem.getJammingIndicator().join();
    int jamStatus = modem.getJammingStatus().join();
    int status = jamStatus & 0x3;
    String jammingStatus = switch (status) {
      case 0 -> "unknown";
      case 1 -> "OK";
      case 2 -> "Warning - fix OK";
      case 3 -> "Critical - NO FIX WARNING";
      default -> "";
    };

    boolean jammed = (jamStatus & 0x04) != 0;
    boolean antennaCut = (jamStatus & 0x80) != 0;

    JsonObject obj = new JsonObject();
    BaseModemProtocol baseModemProtocol = modem.getModemProtocol();
    if(baseModemProtocol != null) {
      JsonObject stats = new JsonObject();
      stats.addProperty("receivedMsgs", baseModemProtocol.getReceivedPackets().get());
      stats.addProperty("sentMsgs", baseModemProtocol.getSentPackets().get());
      stats.addProperty("bytesSent", baseModemProtocol.getSentBytes().get());
      stats.addProperty("bytesRead", baseModemProtocol.getReceivedBytes().get());
      obj.add("satellite", stats);
    }

    JsonObject location = new JsonObject();
    location.addProperty("latitude", toDMS(lat, false));
    location.addProperty("longitude", toDMS(lon, true));
    location.addProperty("satellites", satellites);
    obj.add("location", location);

    JsonObject jamming = new JsonObject();
    jamming.addProperty("jammingIndicator", jamIndicator);
    jamming.addProperty("jammingStatus", jammingStatus);
    if (jammed) {
      jamming.addProperty("status", "JAMMED");
    }
    if (antennaCut) {
      jamming.addProperty("status", "Antenna Cut");
    }
    obj.add("jamming", jamming);

    JsonObject network = new JsonObject();
    network.addProperty("canSend", networkStatus.canSend());
    if(!networkStatus.canSend()) {
      network.addProperty("reason", networkStatus.getReason());
    }
    obj.add("satelliteTxStatus", network);

    String temp = modem.getTemperature().join();
    if (temp != null) {
      float f = toFloat(temp);
      obj.addProperty("temperature", f);
    }
    String payload = obj.toString();
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(payload.getBytes());
    try {
      destination.storeMessage(messageBuilder.build());
    } catch (IOException e) {
      // Log this
    }
  }

  private float toFloat(String temp) {
    try {
      temp = temp.substring(0, 5);
      return Float.parseFloat(temp.trim()) / 10.0f;
    } catch (NumberFormatException e) {
      return Float.NaN;
    }
  }
  private String toDMS(double value, boolean isLongitude) {
    String hemisphere;
    if (isLongitude) {
      hemisphere = (value >= 0) ? "E" : "W";
    } else {
      hemisphere = (value >= 0) ? "N" : "S";
    }
    double absVal = Math.abs(value);
    int degrees = (int) absVal;
    double minutesFull = (absVal - degrees) * 60.0;
    int minutes = (int) minutesFull;
    double seconds = (minutesFull - minutes) * 60.0;  // fractional seconds

    return String.format("%d° %02d' %06.3f %s", degrees, minutes, seconds, hemisphere);
  }

}
