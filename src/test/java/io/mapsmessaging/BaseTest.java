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

package io.mapsmessaging;

import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class BaseTest {

  private static final Logger logger = LoggerFactory.getLogger("TestLogger");
  private final int HEADER_SIZE = 80;

  private long start;

  public void delay(long time_ms){
    try {
      TimeUnit.MILLISECONDS.sleep(time_ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  @BeforeEach
  public void logTestStart(TestInfo testInfo){
    start = System.currentTimeMillis();
    Date dt = new Date();
    System.err.println(getHeaderPartition());
    if(testInfo != null) {
      System.err.println(pad("Starting:: " + testInfo.getDisplayName(), " "));
    }
    System.err.println(pad("Date    :: "+dt.toString(), " "));
    System.err.println(getHeaderPartition());
    if(testInfo != null && testInfo.getDisplayName() != null) {
      logger.log(LogMessages.DEBUG, "Starting " + testInfo.getDisplayName());
    }
  }

  @AfterEach
  public void logTestFinish(TestInfo testInfo){
    System.err.println(pad("Completed:: "+testInfo.getDisplayName()+", "+(System.currentTimeMillis()-start)+"ms", " "));
    System.err.println(getHeaderPartition());
    logger.log(LogMessages.DEBUG, "Completed "+ testInfo.getDisplayName()+" " +(System.currentTimeMillis()-start)+"ms");
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
    return pad(header, "-");
  }

  private String pad(String value, String pad){
    StringBuilder sb = new StringBuilder("|");
    int len = HEADER_SIZE - value.length();
    if(len >= 0){
      sb.append(value);
      for(int x=0;x<len;x++){
        sb.append(pad);
      }
    }
    else{
      String h1 = value.substring(0, HEADER_SIZE);
      String h2 = value.substring(HEADER_SIZE);

      String t = getHeader(h1)+"\n";
      t += getHeader(h2);
      return t;
    }

    sb.append("|");
    return sb.toString();
  }

}
