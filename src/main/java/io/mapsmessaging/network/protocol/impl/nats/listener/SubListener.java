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

package io.mapsmessaging.network.protocol.impl.nats.listener;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.OkFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.SubFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;

import java.io.IOException;

public class SubListener implements FrameListener {

  @Override
  public void frameEvent(NatsFrame frame, SessionState engine, boolean endOfBuffer) throws IOException {

    SubFrame subscribe = (SubFrame) frame;
    if (subscribe.getSubject().startsWith("_INBOX.") && subscribe.getSubscriptionId() != null) {
      engine.getJetStreamRequestManager().setJetSubject(subscribe.getSubject());
      String reply = engine.getJetStreamRequestManager().getJetSubject();

      String key = reply.substring(0, reply.indexOf(".*"));
      engine.getJetStreamRequestManager().registerSid(key, subscribe.getSubscriptionId());
      if (engine.isVerbose()) engine.send(new OkFrame());
      return;
    }
    engine.subscribe(subscribe.getSubject(), subscribe.getSubscriptionId(), subscribe.getShareName(), ClientAcknowledgement.AUTO, 0, false );
  }
}

