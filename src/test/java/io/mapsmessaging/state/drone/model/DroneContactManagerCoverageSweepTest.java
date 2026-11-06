package io.mapsmessaging.state.drone.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DroneContactManagerCoverageSweepTest {
  @Test
  void contactsCanBeAddedUpdatedExpiredAndRemoved() {
    DroneContactManager manager = new DroneContactManager();
    UUID id = UUID.randomUUID();
    GeoPosition firstPosition = new GeoPosition(1.0, 2.0, null, null);

    Contact created = manager.updateContact(id, "first", firstPosition, 1000);
    assertEquals(id, created.getId());
    assertTrue(manager.hasContact(id));
    assertEquals(1, manager.size());

    GeoPosition secondPosition = new GeoPosition(3.0, 4.0, null, null);
    Contact updated = manager.updateContact(id, "second", secondPosition, 2000);
    assertSame(created, updated);
    assertEquals("second", updated.getDescription());
    assertEquals(secondPosition, updated.getPosition());

    Contact expired = new Contact("expired", firstPosition, 1);
    expired.setId(UUID.randomUUID());
    expired.setUpdatedTimeMs(System.currentTimeMillis() - 100);
    manager.addContact(expired);
    manager.expireContacts();
    assertFalse(manager.hasContact(expired.getId()));

    assertSame(created, manager.removeContact(id));
    assertEquals(0, manager.size());
  }
}
