/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.network.io.impl.tcp;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.impl.ConnectionTest;

public class TcpConnectionTest  extends ConnectionTest {

  @Override
  protected EndPointConnectionFactory getFactory() {
    return new TCPEndPointConnectionFactory();
  }

  @Override
  protected EndPointURL getURL() {
    return new EndPointURL("tcp://localhost:2001/");
  }
}
