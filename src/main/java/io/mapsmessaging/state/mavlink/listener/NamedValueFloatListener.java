/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
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

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.drone.model.DroneContactManager;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.NamedValueFloatPacket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.NAMED_VALUE_FLOAT;

/**
 * Listener for NAMED_VALUE_FLOAT.
 */
public class NamedValueFloatListener implements Listener {

  public static final int LISTENER_ID = NAMED_VALUE_FLOAT;

  private static final double DETECTION_PRESENT = 1.0;
  private static final double DETECTION_LOST = 0.0;
  public static final long CONTACT_TTL_MILLIS = 60_000L;

  private final TwinManager twinManager;

  public NamedValueFloatListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId, MavlinkPacket pkt, TwinUpdateContext context) {
    if (!(pkt instanceof NamedValueFloatPacket packet)) {
      return;
    }

    if (!packet.isValid() || !packet.hasName() || !packet.hasValue()) {
      return;
    }

    Instant now = context != null && context.getReceivedTime() != null
        ? context.getReceivedTime()
        : Instant.now();

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin droneTwin = (DroneTwin) twin;
      DroneContactManager contactManager = droneTwin.getContactManager();

      UUID uuid = UUID.nameUUIDFromBytes(packet.getName().getBytes(StandardCharsets.UTF_8));

      if (Double.compare(packet.getValue(), DETECTION_PRESENT) == 0) {
        if (contactManager.hasContact(uuid)) {
          contactManager.updateContact(uuid, packet.getName(), droneTwin.getGeoPosition(), CONTACT_TTL_MILLIS);
        } else {
          Contact contact = new Contact(packet.getName(), droneTwin.getGeoPosition(), CONTACT_TTL_MILLIS);
          contactManager.addContact(contact);
        }
      } else if (Double.compare(packet.getValue(), DETECTION_LOST) == 0 && contactManager.hasContact(uuid)) {
        contactManager.removeContact(uuid);
      }

      droneTwin.setOperationalUpdatedAt(now);
    }, context);
  }
}