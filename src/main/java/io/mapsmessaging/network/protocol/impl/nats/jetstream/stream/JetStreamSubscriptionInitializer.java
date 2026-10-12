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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.NatsArchiveStateCopy;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NATS-only subscription initialisation. The target is a normal MAPS subscription
 * registered for live updates before this task is queued. The task is ordered with
 * destination subscription updates; priority-aware addAll unions any overlapping
 * messages registered between target creation and archive seeding.
 */
public final class JetStreamSubscriptionInitializer {

  private JetStreamSubscriptionInitializer() {
  }

  public static int seedFromArchive(DestinationImpl destination, String archiveSessionId,
                                    String consumerSessionId) throws IOException {
    Objects.requireNonNull(destination, "destination");
    if (archiveSessionId == null || archiveSessionId.equals(consumerSessionId)) {
      throw new IOException("A JetStream consumer must use a separate MAPS session from its archive");
    }
    Future<Response> future = destination.submit(() -> {
      DestinationSubscription archive = destination.getSubscription(archiveSessionId);
      DestinationSubscription consumer = destination.getSubscription(consumerSessionId);
      if (archive == null || consumer == null) {
        throw new IOException("JetStream archive or consumer subscription is missing for "
            + destination.getFullyQualifiedNamespace());
      }
      if (!archive.isSync()) {
        throw new IOException("JetStream archive must be synchronous and unconsumed");
      }
      return new LongResponse(NatsArchiveStateCopy.copyAtRest(
          archive.getMessageStateManager(), consumer.getMessageStateManager()));
    });
    try {
      Response result = future.get(30, TimeUnit.SECONDS);
      if (result instanceof LongResponse count) {
        return Math.toIntExact(count.getResponse());
      }
      throw new IOException("Invalid MAPS response from JetStream archive initialisation");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted initialising JetStream consumer", interrupted);
    } catch (ExecutionException failure) {
      if (failure.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      throw new IOException("Could not initialise JetStream consumer", failure.getCause());
    } catch (TimeoutException failure) {
      throw new IOException("Timed out initialising JetStream consumer", failure);
    }
  }
}
