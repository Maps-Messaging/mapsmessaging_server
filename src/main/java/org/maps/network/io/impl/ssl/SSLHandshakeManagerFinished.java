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

package org.maps.network.io.impl.ssl;

import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;

public class SSLHandshakeManagerFinished implements SSLHandshakeManager {

  @Override
  public boolean handleSSLHandshakeStatus(){
    return false;
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // The SSL handshake has completed, we don't care about selected operations now since this just tells anybody that it is now done
  }
}
