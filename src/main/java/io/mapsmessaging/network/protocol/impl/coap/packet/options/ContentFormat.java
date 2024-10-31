/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ContentFormat extends BinaryOption {

  @Getter
  @Setter
  private Format format;

  public ContentFormat() {
    super(Constants.CONTENT_FORMAT);
  }

  public ContentFormat(Format format) {
    super(Constants.CONTENT_FORMAT);
    this.format = format;
    value = format.getId();
  }

  @Override
  public void update(byte[] data) throws IOException {
    super.update(data);
    format = Format.valueOf((int)getValue());
  }
}
