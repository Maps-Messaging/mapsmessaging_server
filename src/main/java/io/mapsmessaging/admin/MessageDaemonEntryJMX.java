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

package io.mapsmessaging.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Message Daemon Info")
public class MessageDaemonEntryJMX {

  private final ObjectInstance mbean;
  private final MessageDaemon daemon;

  MessageDaemonEntryJMX(MessageDaemon daemon) {
    this.daemon = daemon;
    List<String> typePath = new ArrayList<>();
    typePath.add("type=Broker");
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  //<editor-fold desc="Destination based statistics">
  @JMXBeanAttribute(name = "getName", description = "Returns message daemons name")
  public String getName() {
    return daemon.getId();
  }

}