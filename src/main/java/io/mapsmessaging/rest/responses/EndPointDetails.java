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

package io.mapsmessaging.rest.responses;

import io.mapsmessaging.network.io.EndPoint;
import lombok.Getter;

@Getter
public class EndPointDetails {

  private long id;
  private String adapter;

  private String name;
  private String user;
  private String protocolName;
  private String protocolVersion;
  private long connectedTimeMs;
  private long lastRead;
  private long lastWrite;
  private long totalBytesRead;
  private long totalBytesWritten;
  private long totalOverflow;
  private long totalUnderflow;
  private long bytesRead;
  private long bytesWritten;
  private long overFlow;
  private long underFlow;

  public EndPointDetails(){
  }

  public EndPointDetails(String adapterName, EndPoint endPoint){
    id = endPoint.getId();
    adapter = adapterName;
    name = endPoint.getName();
    connectedTimeMs = System.currentTimeMillis() - endPoint.getConnected();
    protocolName = endPoint.getBoundProtocol().getName();
    protocolVersion = endPoint.getBoundProtocol().getVersion();
    if(endPoint.getEndPointPrincipal() != null) {
      user = endPoint.getEndPointPrincipal().getName();
    }
    else{
      user = "anonymous";
    }

    lastRead = endPoint.getLastRead();
    lastWrite = endPoint.getLastWrite();

    bytesRead = endPoint.getReadBytes().getCurrent();
    totalBytesRead = endPoint.getReadBytes().getTotal();

    bytesWritten = endPoint.getWriteBytes().getCurrent();
    totalBytesWritten = endPoint.getWriteBytes().getTotal();

    overFlow = endPoint.getOverFlow().getCurrent();
    totalOverflow = endPoint.getOverFlow().getTotal();

    underFlow = endPoint.getUnderFlow().getCurrent();
    totalUnderflow = endPoint.getUnderFlow().getTotal();

  }
}
