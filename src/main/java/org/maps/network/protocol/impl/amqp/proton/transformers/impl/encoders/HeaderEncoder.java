/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.proton.transformers.impl.encoders;

import java.util.concurrent.TimeUnit;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;

public class HeaderEncoder {

  public static void unpackHeader( MessageBuilder messageBuilder, Header header){
    if(header != null){
      // Get the priority of the message
      if(header.getPriority() != null) {
        messageBuilder.setPriority(Priority.getInstance(header.getPriority().intValue()));
      }
      else{
        // This is the default value for JMS messages and if not present should be set to 4
        messageBuilder.setPriority(Priority.NORMAL);
      }

      // Process the TTL of the message
      if(header.getTtl() != null) {
        // Convert to seconds
        messageBuilder.setMessageExpiryInterval(header.getTtl().longValue(), TimeUnit.MILLISECONDS);
      }

      // Process the durable flag
      if(header.getDurable()){
        messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      }
      else{
        messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
      }
    }
  }


  public static boolean packHeader(Message message, Header header){
    boolean addHeader = false;
    if(!message.getPriority().equals(Priority.NORMAL)) {
      header.setPriority(new UnsignedByte((byte) message.getPriority().getValue()));
      addHeader = true;
    }

    if(message.getExpiry() != 0){
      long expiry = (message.getExpiry()) - System.currentTimeMillis();
      header.setTtl(new UnsignedInteger((int)(expiry)));
      addHeader = true;
    }

    if(!message.getQualityOfService().equals(QualityOfService.AT_MOST_ONCE)){
      header.setDurable(true);
      addHeader = true;
    }
    return addHeader;
  }

  private HeaderEncoder(){}
}
