/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class PacketIdStoreTest {
  @TempDir(factory = TargetDataTempDirFactory.class) Path directory;

  @Test void allocation_is_bidirectional_and_existing_mapping_signals_replay() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      var first = store.allocate("topic/a", Long.MAX_VALUE);
      assertFalse(first.existing());
      assertEquals(1, first.packetId());
      assertEquals(first.packetId(), store.getPacketId("topic/a", Long.MAX_VALUE));
      assertEquals(new PacketIdStore.Delivery("topic/a", Long.MAX_VALUE), store.getDelivery(first.packetId()));
      var replay = store.allocate("topic/a", Long.MAX_VALUE);
      assertTrue(replay.existing());
      assertEquals(first.packetId(), replay.packetId());
      assertEquals(1, store.size());
    }
  }

  @Test void same_message_on_different_topics_gets_distinct_ids() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      assertNotEquals(store.allocate("a", -1).packetId(), store.allocate("b", -1).packetId());
    }
  }

  @Test void wrap_skips_used_ids_and_exhaustion_preserves_existing_entries() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      store.allocate("a", 1); store.allocate("b", 2); store.allocate("c", 3);
      assertThrows(IOException.class, () -> store.allocate("d", 4));
      assertTrue(store.allocate("a", 1).existing());
      assertTrue(store.clear(2));
      assertEquals(2, store.allocate("d", 4).packetId());
      assertNull(store.getPacketId("b", 2));
      assertEquals(3, store.size());
    }
  }

  @Test void clear_by_delivery_and_unknown_clear_are_idempotent() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      int id = store.allocate("a", 7).packetId();
      assertTrue(store.clear("a", 7));
      assertNull(store.getDelivery(id));
      assertFalse(store.clear(id));
      assertFalse(store.clear("a", 7));
      assertFalse(store.allocate("a", 7).existing());
    }
  }

  @Test void persistent_restart_preserves_mappings_and_clears() throws Exception {
    Path file = directory.resolve("packets");
    int retained;
    try (PacketIdStore store = PacketIdStore.open(file, 5, PacketIdStore.Durability.BATCHED)) {
      retained = store.allocate("temperature/é", Long.MIN_VALUE).packetId();
      store.allocate("removed", 2); store.clear("removed", 2); store.flush();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 5, PacketIdStore.Durability.BATCHED)) {
      assertTrue(store.allocate("temperature/é", Long.MIN_VALUE).existing());
      assertEquals(retained, store.getPacketId("temperature/é", Long.MIN_VALUE));
      assertNull(store.getPacketId("removed", 2));
      assertEquals(1, store.size());
    }
  }

  @Test void compaction_reclaims_history_without_losing_active_mappings() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      int stable = store.allocate("stable", 9).packetId();
      for (int i = 0; i < 100; i++) { int id = store.allocate("churn", i).packetId(); store.clear(id); }
      long before = Files.size(file);
      store.compact();
      assertTrue(Files.size(file) < before);
      assertEquals(stable, store.getPacketId("stable", 9));
      store.allocate("after", 99);
    }
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(2, store.size());
      assertTrue(store.allocate("after", 99).existing());
    }
  }

  @Test void owner_lock_is_held_until_close() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore first = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
      first.allocate("a", 1);
      first.compact();
      assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
    }
    try (PacketIdStore reopened = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { assertEquals(1, reopened.size()); }
  }

  @Test void checksum_corruption_fails_closed() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { store.allocate("a", 1); }
    byte[] bytes = Files.readAllBytes(file); bytes[bytes.length - 1] ^= 1; Files.write(file, bytes);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
  }

  @Test void partial_final_append_is_removed_and_prior_records_recover() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { store.allocate("a", 1); }
    long length = Files.size(file);
    Files.write(file, new byte[]{0,0}, java.nio.file.StandardOpenOption.APPEND);
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(length, Files.size(file)); assertTrue(store.allocate("a", 1).existing());
    }
  }

  @Test void wrong_capacity_and_truncated_header_fail_closed() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { store.allocate("a", 1); }
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 4, PacketIdStore.Durability.SYNC));
    Files.write(file, new byte[]{1,2});
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
  }

  @Test void concurrent_allocations_are_unique_and_same_key_is_idempotent() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(1000); var executor = Executors.newFixedThreadPool(8)) {
      var tasks = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
      for (int i = 0; i < 200; i++) { final int message = i; tasks.add(executor.submit(() -> store.allocate("a", message).packetId())); }
      var ids = new HashSet<Integer>(); for (var task : tasks) assertTrue(ids.add(task.get()));
      tasks.clear();
      for (int i = 0; i < 100; i++) tasks.add(executor.submit(() -> store.allocate("same", 1).packetId()));
      int expected = tasks.getFirst().get(); for (var task : tasks) assertEquals(expected, task.get());
      assertEquals(201, store.size());
    }
  }

  @Test void invalid_inputs_and_closed_store_are_rejected() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> PacketIdStore.memory(0));
    assertThrows(IllegalArgumentException.class, () -> PacketIdStore.memory(65536));
    PacketIdStore store = PacketIdStore.memory(3);
    assertThrows(IllegalArgumentException.class, () -> store.allocate("", 1));
    assertThrows(NullPointerException.class, () -> store.allocate(null, 1));
    assertThrows(IllegalArgumentException.class, () -> store.clear(0));
    assertThrows(IllegalArgumentException.class, () -> store.getStage(0));
    assertThrows(IllegalArgumentException.class, () -> store.setStage(4, 1));
    assertThrows(IllegalArgumentException.class, () -> store.releaseReservedPacketId(0));
    store.close(); store.close();
    assertThrows(IOException.class, () -> store.allocate("a", 1));
    assertThrows(IOException.class, () -> store.getDelivery(1));
    assertThrows(IOException.class, store::reservePacketId);
    assertThrows(IOException.class, () -> store.getStage(1));
    assertThrows(IOException.class, () -> store.setStage(1, 1));
    assertThrows(IllegalStateException.class, () -> store.releaseReservedPacketId(1));
  }
  @Test void lookups_and_existing_allocations_do_not_append_and_ack_is_small() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 10, PacketIdStore.Durability.BATCHED)) {
      int id = store.allocate("a", 1).packetId();
      long length = Files.size(file);
      for (int i = 0; i < 100; i++) {
        assertTrue(store.allocate("a", 1).existing());
        assertEquals(id, store.getPacketId("a", 1));
        assertEquals(1, store.getDelivery(id).messageId());
      }
      assertEquals(length, Files.size(file));
      store.clear(id);
      assertEquals(length + 17, Files.size(file));
    }
  }

  @Test void maximum_wire_id_is_usable_and_snapshot_is_immutable() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      for (int i = 1; i <= 65535; i++) assertEquals(i, store.allocate("a", i).packetId());
      assertThrows(IOException.class, () -> store.allocate("a", 65536));
      var snapshot = store.outstanding();
      assertThrows(UnsupportedOperationException.class, snapshot::clear);
      store.clear(65535);
      assertEquals(65535, snapshot.size());
      assertEquals(65535, store.allocate("b", 1).packetId());
    }
  }

  @Test void partial_record_body_recovers_only_complete_records() throws Exception {
    Path file = directory.resolve("packets");
    long completeLength;
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      store.allocate("a", 1); completeLength = Files.size(file); store.allocate("b", 2);
    }
    byte[] bytes = Files.readAllBytes(file);
    Files.write(file, java.util.Arrays.copyOf(bytes, bytes.length - 3));
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(completeLength, Files.size(file));
      assertEquals(1, store.size());
      assertNull(store.getPacketId("b", 2));
      assertEquals(2, store.allocate("b", 2).packetId());
    }
  }

  @Test void persistence_write_failure_poison_prevents_untracked_delivery() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      var journalField = PacketIdStore.class.getDeclaredField("journal"); journalField.setAccessible(true);
      Object journal = journalField.get(store);
      var channelField = PacketIdJournal.class.getDeclaredField("channel"); channelField.setAccessible(true);
      ((java.nio.channels.FileChannel) channelField.get(journal)).close();
      assertThrows(IOException.class, () -> store.allocate("a", 1));
      assertThrows(IOException.class, () -> store.getPacketId("a", 1));
      assertThrows(IOException.class, () -> store.clear(1));
    }
    try (PacketIdStore reopened = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { assertEquals(0, reopened.size()); }
  }

  @Test void invalid_unicode_and_oversized_topics_are_rejected_without_mutation() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      assertThrows(IllegalArgumentException.class, () -> store.allocate("\uD800", 1));
      assertThrows(IllegalArgumentException.class, () -> store.allocate("a".repeat(65536), 1));
      assertEquals(0, store.size());
    }
  }

  @Test void corrupt_length_fails_closed_without_truncating_durable_records() throws Exception {
    Path file = directory.resolve("packets");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { store.allocate("a", 1); }
    byte[] bytes = Files.readAllBytes(file); bytes[19] ^= 64; Files.write(file, bytes);
    long length = Files.size(file);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
    assertEquals(length, Files.size(file));
  }

  @Test void symbolic_link_cannot_bypass_journal_ownership() throws Exception {
    Path file = directory.resolve("packets");
    Path alias = directory.resolve("alias");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      try { Files.createSymbolicLink(alias, file); }
      catch (java.nio.file.FileSystemException unavailable) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) throw unavailable;
        org.junit.jupiter.api.Assumptions.abort("Windows user lacks symbolic-link capability: " + unavailable.getReason());
      }
      assertThrows(IOException.class, () -> PacketIdStore.open(alias, 3, PacketIdStore.Durability.SYNC));
      store.allocate("a", 1);
    }
  }

  @Test void forced_allocation_survives_process_exit_without_close() throws Exception {
    Path file = directory.resolve("packets");
    String classpath = Path.of(PacketIdStore.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString()
        + java.io.File.pathSeparator + Path.of(PacketIdStoreTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp", classpath, CrashWriter.class.getName(), file.toString()).redirectErrorStream(true).start();
    try {
      process.onExit().get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes()));
    } finally { if (process.isAlive()) process.destroyForcibly(); }
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(1, store.getPacketId("a", 7));
      assertTrue(store.allocate("a", 7).existing());
    }
  }

  @Test void stage_update_is_monotonic_and_retained_after_compaction_and_restart() throws Exception {
    Path file = directory.resolve("stages");
    int awaiting;
    int publishing;
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.BATCHED)) {
      awaiting = store.allocate("qos2", 1).packetId();
      publishing = store.allocate("qos1", 2).packetId();
      assertEquals(0, store.getStage(awaiting));
      long initialLength = Files.size(file);
      store.setStage(awaiting, 1);
      assertEquals(initialLength + 18, Files.size(file));
      store.setStage(awaiting, 1);
      assertEquals(initialLength + 18, Files.size(file));
      assertThrows(IllegalArgumentException.class, () -> store.setStage(awaiting, 0));
      assertThrows(IllegalArgumentException.class, () -> store.setStage(awaiting, 2));
      assertThrows(IllegalArgumentException.class, () -> store.setStage(3, 1));
      store.flush();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, store.getStage(awaiting));
      assertEquals(2, store.getQos(awaiting));
      assertEquals(0, store.getQos(publishing));
      assertEquals(0, store.getStage(publishing));
      store.compact();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, store.getStage(awaiting));
      assertEquals(2, store.getQos(awaiting));
      assertEquals(0, store.getQos(publishing));
      assertEquals(0, store.getStage(publishing));
      store.allocate("third", 3);
      store.clear(awaiting);
      assertEquals(awaiting, store.allocate("reused", 4).packetId());
      assertEquals(0, store.getStage(awaiting));
    }
  }

  @Test void corrupt_stage_update_fails_closed() throws Exception {
    Path file = directory.resolve("stages");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      store.setStage(store.allocate("qos2", 1).packetId(), 1);
    }
    byte[] bytes = Files.readAllBytes(file);
    bytes[bytes.length - 5] ^= 1;
    Files.write(file, bytes);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
  }

  @Test void partial_stage_update_recovers_publish_stage_and_can_advance_again() throws Exception {
    Path file = directory.resolve("partial-stage");
    long allocationLength;
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      int packetId = store.allocate("qos2", 1).packetId();
      allocationLength = Files.size(file);
      store.setStage(packetId, 1);
    }
    byte[] bytes = Files.readAllBytes(file);
    Files.write(file, java.util.Arrays.copyOf(bytes, bytes.length - 1));
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(allocationLength, Files.size(file));
      assertEquals(0, store.getStage(1));
      store.setStage(1, 1);
    }
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      assertEquals(1, store.getStage(1));
    }
  }

  @Test void checksummed_invalid_stage_update_is_rejected() throws Exception {
    Path file = directory.resolve("invalid-stage");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      store.setStage(store.allocate("qos2", 1).packetId(), 1);
    }
    byte[] valid = Files.readAllBytes(file);
    int bodyOffset = valid.length - 10;
    for (int invalidStage : new int[]{0, 2, 255}) {
      byte[] bytes = valid.clone();
      bytes[bodyOffset + 5] = (byte) invalidStage;
      java.util.zip.CRC32C crc = new java.util.zip.CRC32C();
      crc.update(bytes, bodyOffset, 6);
      java.nio.ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
      Files.write(file, bytes);
      assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
    }
    byte[] missingDelivery = valid.clone();
    java.nio.ByteBuffer.wrap(missingDelivery).putInt(bodyOffset + 1, 2);
    java.util.zip.CRC32C crc = new java.util.zip.CRC32C();
    crc.update(missingDelivery, bodyOffset, 6);
    java.nio.ByteBuffer.wrap(missingDelivery).putInt(missingDelivery.length - 4, (int) crc.getValue());
    Files.write(file, missingDelivery);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
    Files.write(file, valid);
    Files.write(file, java.util.Arrays.copyOfRange(valid, bodyOffset - 8, valid.length),
        java.nio.file.StandardOpenOption.APPEND);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC));
  }

  @Test void control_reservations_share_capacity_and_release_without_delivery_mutation() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      int delivery = store.allocate("a", 1).packetId();
      int control = store.reservePacketId();
      int secondDelivery = store.allocate("b", 2).packetId();
      assertNotEquals(delivery, control);
      assertNotEquals(secondDelivery, control);
      assertEquals(2, store.size());
      assertNull(store.getDelivery(control));
      assertFalse(store.clear(control));
      assertThrows(IOException.class, store::reservePacketId);
      assertThrows(IOException.class, () -> store.allocate("c", 3));
      assertTrue(store.allocate("a", 1).existing());
      assertFalse(store.releaseReservedPacketId(delivery));
      assertTrue(store.releaseReservedPacketId(control));
      assertFalse(store.releaseReservedPacketId(control));
      assertEquals(control, store.reservePacketId());
    }
  }

  @Test void control_reservations_are_never_written_or_restored() throws Exception {
    Path file = directory.resolve("control");
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      store.allocate("a", 1);
      long length = Files.size(file);
      assertEquals(2, store.reservePacketId());
      assertEquals(length, Files.size(file));
      store.compact();
      assertThrows(IOException.class, store::reservePacketId);
    }
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, store.size());
      assertEquals(2, store.reservePacketId());
    }
  }

  @Test void qos_is_immutable_and_survives_restart_compaction_and_id_reuse() throws Exception {
    Path file = directory.resolve("qos");
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      int qos2 = store.allocate("qos2", 1).packetId();
      int qos1 = store.allocate("qos1", 2).packetId();
      assertEquals(0, store.getQos(qos2));
      long before = Files.size(file);
      store.setQos(qos2, 2);
      store.setQos(qos1, 1);
      assertEquals(before + 36, Files.size(file));
      store.setQos(qos2, 2);
      assertEquals(before + 36, Files.size(file));
      assertThrows(IllegalArgumentException.class, () -> store.setQos(qos2, 1));
      assertThrows(IllegalArgumentException.class, () -> store.setStage(qos1, 1));
      assertThrows(IllegalArgumentException.class, () -> store.setQos(qos1, 0));
      store.flush();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      assertEquals(2, store.getQos(1));
      assertEquals(1, store.getQos(2));
      assertThrows(IllegalArgumentException.class, () -> store.setQos(1, 1));
      store.compact();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      assertEquals(2, store.getQos(1));
      assertEquals(1, store.getQos(2));
      store.clear(1);
      assertEquals(1, store.allocate("new", 3).packetId());
      assertEquals(0, store.getQos(1));
      store.setQos(1, 1);
    }
  }

  @Test void qos_update_corruption_and_invalid_checksummed_value_fail_closed() throws Exception {
    Path file = directory.resolve("qos-corrupt");
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.SYNC)) {
      store.setQos(store.allocate("qos2", 1).packetId(), 2);
    }
    byte[] valid = Files.readAllBytes(file);
    byte[] corrupt = valid.clone();
    corrupt[corrupt.length - 1] ^= 1;
    Files.write(file, corrupt);
    assertThrows(IOException.class, () -> PacketIdStore.open(file, 2, PacketIdStore.Durability.SYNC));
    for (int invalidQos : new int[]{0, 3, 255}) {
      byte[] bytes = valid.clone();
      int bodyOffset = bytes.length - 10;
      bytes[bodyOffset + 5] = (byte) invalidQos;
      java.util.zip.CRC32C crc = new java.util.zip.CRC32C();
      crc.update(bytes, bodyOffset, 6);
      java.nio.ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
      Files.write(file, bytes);
      assertThrows(IOException.class, () -> PacketIdStore.open(file, 2, PacketIdStore.Durability.SYNC));
    }
    Files.write(file, java.util.Arrays.copyOf(valid, valid.length - 1));
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.SYNC)) {
      assertEquals(0, store.getQos(1));
      store.setQos(1, 2);
      assertEquals(2, store.getQos(1));
    }
  }

  @Test void legacy_stage_implies_qos2_and_pending_clear_preserves_pubrel() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      int pubrel = store.allocate("key", 1).packetId();
      store.setStage(pubrel, 1);
      assertEquals(2, store.getQos(pubrel));
      assertThrows(IllegalArgumentException.class, () -> store.setQos(pubrel, 1));
      assertFalse(store.clearPendingDelivery("key", 1));
      assertFalse(store.clearPendingDelivery("other", 1));
      int publishing = store.allocate("key", 2).packetId();
      assertTrue(store.clearPendingDelivery("key", 2));
      assertNull(store.getDelivery(publishing));
      assertFalse(store.clearPendingDelivery("key", 2));
      assertEquals(1, store.size());
    }
  }

  @Test void bulk_clear_matches_exact_key_and_all_stages_without_touching_reservations() throws Exception {
    Path file = directory.resolve("bulk");
    try (PacketIdStore store = PacketIdStore.open(file, 4, PacketIdStore.Durability.BATCHED)) {
      store.setStage(store.allocate("key", 1).packetId(), 1);
      store.allocate("key", 2);
      int retained = store.allocate("key/other", 3).packetId();
      store.setQos(retained, 1);
      int control = store.reservePacketId();
      assertEquals(2, store.clearDeliveries("key"));
      assertEquals(0, store.clearDeliveries("key"));
      assertEquals(1, store.size());
      assertEquals(retained, store.getPacketId("key/other", 3));
      assertTrue(store.releaseReservedPacketId(control));
      assertThrows(IllegalArgumentException.class, () -> store.setQos(1, 2));
    }
    try (PacketIdStore store = PacketIdStore.open(file, 4, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, store.size());
      assertEquals(1, store.getQos(store.getPacketId("key/other", 3)));
    }
  }

  @Test void flush_automatically_compacts_large_dead_history_preserving_delivery_state() throws Exception {
    Path file = directory.resolve("automatic-compaction");
    int retained;
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      retained = store.allocate("held", 1).packetId();
      store.setQos(retained, 2);
      store.setStage(retained, 1);
      for (int message = 0; message < 24000; message++) {
        int packetId = store.allocate("churn", message).packetId();
        store.clear(packetId);
      }
      assertTrue(Files.size(file) >= 1024 * 1024);
      store.flush();
      assertTrue(Files.size(file) < 1024);
      assertEquals(retained, store.getPacketId("held", 1));
      assertEquals(2, store.getQos(retained));
      assertEquals(1, store.getStage(retained));
      long compactedLength = Files.size(file);
      store.flush();
      assertEquals(compactedLength, Files.size(file));
    }
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, store.size());
      assertEquals(retained, store.getPacketId("held", 1));
      assertEquals(2, store.getQos(retained));
      assertEquals(1, store.getStage(retained));
    }
  }

  @Test void flush_keeps_small_dead_history_append_only() throws Exception {
    Path file = directory.resolve("below-compaction-threshold");
    try (PacketIdStore store = PacketIdStore.open(file, 2, PacketIdStore.Durability.BATCHED)) {
      for (int message = 0; message < 100; message++) {
        store.clear(store.allocate("churn", message).packetId());
      }
      long before = Files.size(file);
      store.flush();
      assertEquals(before, Files.size(file));
    }
  }

  @Test void flush_keeps_large_journal_when_live_records_dominate() throws Exception {
    Path file = directory.resolve("mostly-live");
    try (PacketIdStore store = PacketIdStore.open(file, 12000, PacketIdStore.Durability.BATCHED)) {
      String topic = "long-live-key/" + "a".repeat(100);
      for (int message = 0; message < 11000; message++) store.allocate(topic, message);
      for (int message = 0; message < 100; message++) store.clear(topic, message);
      long before = Files.size(file);
      assertTrue(before >= 1024 * 1024);
      store.flush();
      assertEquals(before, Files.size(file));
      assertEquals(10900, store.size());
    }
  }

  @Test void automatic_compaction_counts_recovered_records() throws Exception {
    Path file = directory.resolve("recovered-history");
    try (PacketIdStore store = PacketIdStore.open(file, 1, PacketIdStore.Durability.BATCHED)) {
      for (int message = 0; message < 24000; message++) {
        store.clear(store.allocate("churn", message).packetId());
      }
    }
    assertTrue(Files.size(file) >= 1024 * 1024);
    try (PacketIdStore store = PacketIdStore.open(file, 1, PacketIdStore.Durability.BATCHED)) {
      store.flush();
      assertEquals(16, Files.size(file));
      assertEquals(0, store.size());
    }
  }

  public static class CrashWriter {
    public static void main(String[] arguments) throws Exception {
      PacketIdStore store = PacketIdStore.open(Path.of(arguments[0]), 3, PacketIdStore.Durability.SYNC);
      store.allocate("a", 7);
      Runtime.getRuntime().halt(0);
    }
  }

  @Test void failed_compaction_poison_retains_original_records_and_releases_lock() throws Exception {
    Path file = directory.resolve("packets");
    Path blocked = directory.resolve("blocked");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      store.allocate("a", 1);
      Files.createDirectory(blocked); Files.writeString(blocked.resolve("blocker"), "block");
      var journalField = PacketIdStore.class.getDeclaredField("journal"); journalField.setAccessible(true);
      Object journal = journalField.get(store);
      var fileField = PacketIdJournal.class.getDeclaredField("file"); fileField.setAccessible(true);
      fileField.set(journal, blocked);
      assertThrows(IOException.class, store::compact);
      assertThrows(IOException.class, () -> store.allocate("b", 2));
    }

    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) { assertTrue(store.allocate("a", 1).existing()); }
  }

  @Test void hard_link_cannot_bypass_active_journal_ownership() throws Exception {
    Path file = directory.resolve("packets");
    Path alias = directory.resolve("hard-alias");
    try (PacketIdStore store = PacketIdStore.open(file, 3, PacketIdStore.Durability.SYNC)) {
      Files.createLink(alias, file);
      assertThrows(IOException.class, () -> PacketIdStore.open(alias, 3, PacketIdStore.Durability.SYNC));
      store.allocate("a", 1);
    }
  }

}
