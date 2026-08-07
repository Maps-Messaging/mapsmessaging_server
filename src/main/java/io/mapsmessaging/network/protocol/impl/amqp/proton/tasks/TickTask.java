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

package io.mapsmessaging.network.protocol.impl.amqp.proton.tasks;

import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.engine.EndpointState;

public class TickTask extends PacketTask {

  private final ProtonEngine engine;

  public TickTask(ProtonEngine engine) {
    super(engine);
    this.engine = engine;
  }

  @Override
  public Boolean call() throws Exception {
    long now = System.currentTimeMillis();
    long deadline = transport.tick(now);
    long transactionDeadline = engine.getTransactionCoordinator().expireTransactions(now);
    if (transactionDeadline != 0 && (deadline == 0 || transactionDeadline < deadline)) {
      deadline = transactionDeadline;
    }
    processOutput();
    if (engine.getConnection().getLocalState() == EndpointState.CLOSED) {
      protocol.close();
    } else {
      engine.scheduleTick(deadline, now);
    }
    return true;
  }
}
