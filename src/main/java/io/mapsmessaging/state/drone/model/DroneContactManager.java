/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.drone.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DroneContactManager {

  private final ConcurrentMap<UUID, Contact> contacts;

  public DroneContactManager() {
    this.contacts = new ConcurrentHashMap<>();
  }

  public Contact addContact(String description, GeoPosition position, long ttlMillis) {
    Contact contact = new Contact(description, position, ttlMillis);

    contacts.put(contact.getId(), contact);

    return contact;
  }

  public Contact addContact(Contact contact) {
    if (contact.getId() == null) {
      contact.setId(UUID.randomUUID());
    }

    long nowMs = System.currentTimeMillis();

    if (contact.getCreatedTimeMs() == 0) {
      contact.setCreatedTimeMs(nowMs);
    }

    if (contact.getUpdatedTimeMs() == 0) {
      contact.setUpdatedTimeMs(nowMs);
    }

    contacts.put(contact.getId(), contact);

    return contact;
  }

  public Contact updateContact(UUID id, String description, GeoPosition position, long ttlMillis) {
    long nowMs = System.currentTimeMillis();

    return contacts.compute(id, (contactId, existingContact) -> {
      if (existingContact == null) {
        Contact contact = new Contact(description, position, ttlMillis);
        contact.setId(contactId);
        return contact;
      }

      existingContact.update(description, position, ttlMillis, nowMs);
      return existingContact;
    });
  }

  public Contact removeContact(UUID id) {
    return contacts.remove(id);
  }

  public void expireContacts() {
    long nowMs = System.currentTimeMillis();

    contacts.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMs));
  }

  public List<Contact> getContactList() {
    expireContacts();

    return Collections.unmodifiableList(new ArrayList<>(contacts.values()));
  }

  public int size() {
    expireContacts();

    return contacts.size();
  }

  public void clear() {
    contacts.clear();
  }

  public boolean hasContact(UUID uuid) {
    return contacts.containsKey(uuid);
  }
}