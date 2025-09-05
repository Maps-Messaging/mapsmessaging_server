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
  private RollbackPriority rollbackPriority = RollbackPriority.MAINTAIN;

  @Getter
  @Setter
  private int minimumMessageSize = 1024;

  public void setMessageCompression(String name){
    if(name == null){
      name = "none";
    }

    switch(name.toLowerCase()){
      case "inflator":
        messageCompression = CompressionMode.INFLATOR;
        break;

      case "none":
      default:
        messageCompression = CompressionMode.NONE;
    }
  }

  public void setRollbackPriority(String name){
    if(name == null){
      name = "maintain";
    }

    switch(name.toLowerCase()){
      case "increment":
        rollbackPriority = RollbackPriority.INCREMENT;
        break;

      case "maintain":
      default:
        rollbackPriority = RollbackPriority.MAINTAIN;
        break;
    }
  }


  private Constants(){}

}
