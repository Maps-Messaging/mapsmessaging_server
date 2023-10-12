/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.api.features;

import lombok.Getter;
import lombok.Setter;

public class Constants {

  private static final Constants instance;
  static{
    instance = new Constants();
  }

  public static Constants getInstance(){
    return instance;
  }

  @Getter
  private CompressionMode messageCompression = CompressionMode.NONE;

  @Getter
  @Setter
  private int minimumMessageSize = 1024;

  public void setMessageCompression(String name){
    switch(name.toLowerCase()){
      case "inflator":
        messageCompression = CompressionMode.INFLATOR;
        break;

      case "none":
      default:
        messageCompression = CompressionMode.NONE;
    }
  }

  private Constants(){}

}
