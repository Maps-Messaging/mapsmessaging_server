/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.security.SubjectHelper;
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
    if(endPoint.getEndPointSubject() != null) {
      user = SubjectHelper.getUsername(endPoint.getEndPointSubject());
    }
    else{
      user = "anonymous";
    }

    lastRead = endPoint.getLastRead();
    lastWrite = endPoint.getLastWrite();

    EndPointStatus status = endPoint.getEndPointStatus();
    totalBytesRead = status.getReadBytesTotal();
    totalBytesWritten = status.getWriteBytesTotal();

    if (status.supportsMovingAverages()) {
      bytesRead = status.getReadByteAverages().getCurrent();
      bytesWritten = status.getWriteByteAverages().getCurrent();

      overFlow = status.getBufferOverFlow().getCurrent();
      totalOverflow = status.getBufferOverFlow().getTotal();

      underFlow = status.getBufferUnderFlow().getCurrent();
      totalUnderflow = status.getBufferUnderFlow().getTotal();
    }
  }
}
