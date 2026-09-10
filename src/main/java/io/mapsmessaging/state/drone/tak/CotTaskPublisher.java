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
package io.mapsmessaging.state.drone.tak;

import static io.mapsmessaging.state.logging.StateLogMessages.COT_TASK_PUBLISH_FAILED;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.task.CanonicalTask;
import io.mapsmessaging.state.task.CanonicalTaskObserver;
import io.mapsmessaging.state.task.CanonicalTaskRegistry;
import java.io.IOException;
import java.time.Instant;

public class CotTaskPublisher implements CanonicalTaskObserver, AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(CotTaskPublisher.class);
  private final CanonicalTaskRegistry tasks;
  private final CotIdentityRegistry identities;
  private final CotTaskProfile profile;
  private final TakXmlSerialiser serialiser;
  private final EventPublisher publisher;

  public CotTaskPublisher(
      CanonicalTaskRegistry tasks,
      CotIdentityRegistry identities,
      CotTaskProfile profile,
      String outboundTopic) throws IOException {
    this.tasks = tasks;
    this.identities = identities;
    this.profile = profile;
    this.serialiser = new TakXmlSerialiser();
    try {
      this.publisher = new EventPublisher(outboundTopic);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating CoT task publisher", exception);
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
      throw new IOException("Unable to create CoT task publisher", exception);
    }
    tasks.addObserver(this);
  }

  @Override
  public void onTaskChanged(CanonicalTask current, CanonicalTask previous) {
    if (previous != null && "cot".equalsIgnoreCase(current.getLastUpdateProtocol())) {
      return;
    }
    CotIdentityBinding binding = identities.findByTwinId(current.getTwinId()).orElse(null);
    if (binding == null) {
      return;
    }
    try {
      CanonicalTask mappedTask = current;
      if (!current.getProtocolTaskIds().containsKey("cot")) {
        mappedTask = tasks.bindProtocolId(
            current.getCanonicalTaskId(),
            "cot",
            "maps-" + current.getCanonicalTaskId()).orElse(current);
      }
      publisher.publish(serialiser.toXml(profile.map(mappedTask, binding, Instant.now())));
    } catch (IOException | RuntimeException exception) {
      logger.log(COT_TASK_PUBLISH_FAILED, exception, current.getCanonicalTaskId(), exception.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    tasks.removeObserver(this);
    publisher.close();
  }
}
