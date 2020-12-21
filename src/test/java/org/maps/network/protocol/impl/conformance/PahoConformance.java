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

package org.maps.network.protocol.impl.conformance;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.maps.test.BaseTestConfig;

public abstract class PahoConformance extends BaseTestConfig {

  public abstract String getFileName();

  @TestFactory
  List<DynamicTest> createTests() throws IOException {
    //
    String interOpDirectory = System.getProperty("paho.interop.directory", ".");
    List<String> definedTests = scanForTests(new File(interOpDirectory+File.separator+getFileName()));
    List<DynamicTest> tests = new ArrayList<>();
    for(String name:definedTests){
      Executable exec = () -> Assertions.assertTrue(runTests(name));
      DynamicTest test = DynamicTest.dynamicTest(name, exec);
      tests.add(test);
    }
    return tests;
  }

  public boolean runTests(String testName) throws InterruptedException, IOException {
    logTestStart(null);
    String interOpDirectory = System.getProperty("paho.interop.directory", ".");
    File workingDirectory = new File(interOpDirectory);

    String pythonCommand = System.getProperty("python_command", "python");

    String command = pythonCommand+" "+getFileName() + " Test."+testName;
    System.err.println("Working Dir:"+workingDirectory.toString()+" Exists::"+workingDirectory.exists());
    System.err.println("Command :: "+command);
    Process process = Runtime.getRuntime().exec(command, null, workingDirectory);
    StreamReader outputReader = new StreamReader(process.getInputStream());

    StreamReader errReader =new StreamReader(process.getErrorStream());

    process.waitFor();
    System.err.println(outputReader.sb.toString());
    System.err.println(errReader.sb.toString());
    return !outputReader.sb.toString().contains("FAIL") && !errReader.sb.toString().contains("FAIL");
  }

  private List<String> scanForTests(File pythonSource) throws IOException {
    List<String> tests = new ArrayList<>();
    if(!pythonSource.exists()){
      System.err.println("\n\tThe PAHO MQTT conformance tests can not be found at "+pythonSource.toString()+"\n");
      return tests;
    }
    try (FileReader reader = new FileReader(pythonSource)) {
      char[] content = new char[(int) pythonSource.length()];
      if (reader.read(content) == content.length) {
        String parsable = new String(content);
        int idx = 0;
        idx = parsable.indexOf("def test", idx + 1);
        while (idx > -1) {
          String testName = parsable.substring(idx+4, parsable.indexOf('(', idx));
          System.err.println("Test Name:" + testName);
          tests.add(testName);
          idx = parsable.indexOf("def test", idx + 1);
        }
      }
    }
    return tests;
  }

  private static class StreamReader implements Runnable{

    private final InputStream is;
    private final StringBuilder sb;

    public StreamReader(InputStream is){
      this.is = is;
      sb = new StringBuilder();
      Thread outputReaderThread = new Thread(this);
      outputReaderThread.setDaemon(true);
      outputReaderThread.start();
    }

    @Override
    public void run() {
      byte[] buff = new byte[10240];
      while(true){
        try {
          int val = is.read(buff);
          if(val > 0) {
            sb.append(new String(buff, 0, val));
          }
          else{
            return;
          }
        } catch (IOException e) {
          return;
        }
      }
    }
  }

}
