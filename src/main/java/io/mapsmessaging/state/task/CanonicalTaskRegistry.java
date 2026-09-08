/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CanonicalTaskRegistry {

  private final Map<UUID, CanonicalTask> tasks = new ConcurrentHashMap<>();
  private final Map<String, UUID> aliases = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<CanonicalTaskObserver> observers = new CopyOnWriteArrayList<>();
  private final CanonicalTaskStore store;

  public CanonicalTaskRegistry() {
    this.store = null;
  }

  public CanonicalTaskRegistry(Path storageRoot) throws IOException {
    this.store = new CanonicalTaskStore(storageRoot);
    for (CanonicalTask task : store.load()) {
      index(task);
    }
  }

  public synchronized CanonicalTask submit(CanonicalTask submitted) throws IOException {
    validate(submitted);
    Optional<CanonicalTask> existing = findByAnyAlias(submitted);
    if (existing.isPresent()) {
      return existing.get().copy();
    }
    CanonicalTask stored = submitted.copy();
    if (stored.getCanonicalTaskId() == null) {
      stored.setCanonicalTaskId(UUID.randomUUID());
    }
    index(stored);
    persist(stored);
    notifyObservers(stored, null);
    return stored.copy();
  }

  public synchronized Optional<CanonicalTask> updateState(
      UUID taskId, CanonicalTaskState state, String reason) throws IOException {
    return updateState(taskId, state, reason, null);
  }

  public synchronized Optional<CanonicalTask> updateState(
      UUID taskId, CanonicalTaskState state, String reason, String sourceProtocol) throws IOException {
    CanonicalTask task = tasks.get(taskId);
    if (task == null) {
      return Optional.empty();
    }
    if (task.getState() != null && !task.getState().canTransitionTo(state)) {
      return Optional.of(task.copy());
    }
    CanonicalTask previous = task.copy();
    task.setState(state);
    task.setResultReason(reason);
    task.setLastUpdateProtocol(sourceProtocol);
    persist(task);
    notifyObservers(task, previous);
    return Optional.of(task.copy());
  }

  public Optional<CanonicalTask> get(UUID taskId) {
    CanonicalTask task = tasks.get(taskId);
    return task == null ? Optional.empty() : Optional.of(task.copy());
  }

  public Optional<CanonicalTask> find(String protocol, String externalTaskId) {
    UUID taskId = aliases.get(alias(protocol, externalTaskId));
    return taskId == null ? Optional.empty() : get(taskId);
  }

  public synchronized Optional<CanonicalTask> bindProtocolId(
      UUID taskId, String protocol, String externalTaskId) throws IOException {
    CanonicalTask task = tasks.get(taskId);
    if (task == null) {
      return Optional.empty();
    }
    String alias = alias(protocol, externalTaskId);
    UUID existing = aliases.putIfAbsent(alias, taskId);
    if (existing != null && !existing.equals(taskId)) {
      throw new IllegalArgumentException("Protocol task ID is already bound to another canonical task");
    }
    task.getProtocolTaskIds().put(protocol, externalTaskId);
    persist(task);
    return Optional.of(task.copy());
  }

  public Collection<CanonicalTask> list() {
    return Collections.unmodifiableList(tasks.values().stream().map(CanonicalTask::copy).toList());
  }

  public void addObserver(CanonicalTaskObserver observer) {
    observers.addIfAbsent(observer);
  }

  public void removeObserver(CanonicalTaskObserver observer) {
    observers.remove(observer);
  }

  private Optional<CanonicalTask> findByAnyAlias(CanonicalTask task) {
    for (Map.Entry<String, String> entry : task.getProtocolTaskIds().entrySet()) {
      Optional<CanonicalTask> existing = find(entry.getKey(), entry.getValue());
      if (existing.isPresent()) {
        return existing;
      }
    }
    return Optional.empty();
  }

  private void index(CanonicalTask task) {
    tasks.put(task.getCanonicalTaskId(), task);
    task.getProtocolTaskIds().forEach((protocol, externalId) ->
        aliases.put(alias(protocol, externalId), task.getCanonicalTaskId()));
  }

  private String alias(String protocol, String externalTaskId) {
    return protocol.trim().toLowerCase() + ":" + externalTaskId.trim();
  }

  private void validate(CanonicalTask task) {
    if (task == null
        || task.getOriginProtocol() == null
        || task.getOriginProtocol().isBlank()
        || task.getTwinId() == null
        || task.getTwinId().isBlank()
        || task.getTaskType() == null
        || task.getAction() == null
        || task.getProtocolTaskIds().isEmpty()) {
      throw new IllegalArgumentException("Canonical task requires origin, twin, type, action and protocol ID");
    }
  }

  private void persist(CanonicalTask task) throws IOException {
    if (store != null) {
      store.save(task);
    }
  }

  private void notifyObservers(CanonicalTask current, CanonicalTask previous) {
    for (CanonicalTaskObserver observer : observers) {
      observer.onTaskChanged(current.copy(), previous == null ? null : previous.copy());
    }
  }
}
