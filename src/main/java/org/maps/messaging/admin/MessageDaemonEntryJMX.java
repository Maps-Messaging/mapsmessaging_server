/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;
import org.maps.messaging.MessageDaemon;
import org.maps.utilities.admin.JMXManager;

@JMXBean(description = "Message Daemon Info")
public class MessageDaemonEntryJMX {

  private final List<String> typePath;
  private final ObjectInstance mbean;
  private final MessageDaemon daemon;

  MessageDaemonEntryJMX(MessageDaemon daemon) {
    this.daemon = daemon;
    typePath = new ArrayList<>();
    typePath.add("type=Broker");
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  //<editor-fold desc="Destination based statistics">
  @JMXBeanAttribute(name = "getName", description = "Returns message daemons name")
  public String getTotalNoInterest() {
    return daemon.getId();
  }

}