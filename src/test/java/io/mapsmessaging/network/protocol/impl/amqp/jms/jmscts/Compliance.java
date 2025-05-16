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

package io.mapsmessaging.network.protocol.impl.amqp.jms.jmscts;

import io.mapsmessaging.test.BaseTestConfig;
import java.io.File;

public class Compliance extends BaseTestConfig {

  void runJmsCts(){
    File work = new File("");
    System.err.println(work.getAbsoluteFile());
    String working = work.getAbsolutePath();
    if(!working.endsWith(File.separator)){
      working = working + File.separator;
    }
    System.setProperty("jmscts.home",working+"src/test/resources/jmscts");
    System.setProperty("POLICY_FILE",working+"src/test/resources/jmscts/config/jmscts.policy");

    System.err.println("Home::"+System.getProperty("jmscts.home"));
    System.err.println("POLICY_FILE::"+System.getProperty("POLICY_FILE"));

    String[] args = {"run", "-filter", "src/test/resources/jmscts/config/filter.xml"};
   // org.exolab.jmscts.test.ComplianceTestSuite.main(args);
  }

}
