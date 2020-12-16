/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps;

import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class BaseTest {

  private final int HEADER_SIZE = 80;

  private long start;

  public void delay(long time_ms){
    java.util.concurrent.locks.LockSupport.parkNanos(time_ms * 1000000);
  }


  @BeforeEach
  public void logTestStart(TestInfo testInfo){
    start = System.currentTimeMillis();
    Date dt = new Date();
    System.err.println(getHeaderPartition());
    if(testInfo != null) {
      System.err.println(getHeader("Starting:: " + testInfo.getDisplayName()));
    }
    System.err.println(getHeader("Date    :: "+dt.toString()));
    System.err.println(getHeaderPartition());
  }

  @AfterEach
  public void logTestFinish(TestInfo testInfo){
    System.err.println(getHeader("Completed:: "+testInfo.getDisplayName()+", "+(System.currentTimeMillis()-start)+"ms"));
    System.err.println(getHeaderPartition());
  }

  private String getHeaderPartition(){
    StringBuilder sb = new StringBuilder("+");
    for(int x=0;x<HEADER_SIZE;x++){
      sb.append("-");
    }
    sb.append("+");
    return sb.toString();
  }

  private String getHeader(String header){
    StringBuilder sb = new StringBuilder("|");
    int len = HEADER_SIZE - header.length();
    if(len >= 0){
      sb.append(header);
      for(int x=0;x<len;x++){
        sb.append("-");
      }
    }
    else{
      String h1 = header.substring(0, HEADER_SIZE);
      String h2 = header.substring(HEADER_SIZE);

      String t = getHeader(h1)+"\n";
      t += getHeader(h2);
      return t;
    }

    sb.append("|");
    return sb.toString();
  }
}
