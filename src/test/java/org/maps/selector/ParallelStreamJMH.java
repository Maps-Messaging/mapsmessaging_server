/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package org.maps.selector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.maps.selector.operators.IdentifierResolver;
import org.maps.selector.operators.ParserExecutor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ParallelStreamJMH {

  private List<IdentifierResolver> data;
  ParserExecutor executor;

  @Setup
  public void parallelStreams() throws ParseException {
    data = new ArrayList<>();
    for(int x=0;x<10000000;x++){
      HashMap<String, Object> entry = new LinkedHashMap<>();
      entry.put("even", x%2 == 0);
      data.add(key -> entry.get(key));
    }

    executor = SelectorParser.compile("even = true");
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void compilation(){
    for (String selector : SelectorConformanceTest.SELECTOR_TEXT) {
      try {
        Object parser = SelectorParser.compile(selector);
        parser.toString();
      } catch (ParseException e) {
        Assertions.fail("Selector text:" + selector + " failed with exception " + e.getMessage());
      }
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void calculateParallelFilteredCount(){
    long count = data.parallelStream()
        .filter(executor::evaluate)
        .count();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void calculateFilteredCount(){
    long count = data.stream()
        .filter(executor::evaluate)
        .count();
  }
  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void calculateFilteredAny(){
    data.stream()
        .anyMatch(executor::evaluate);
  }

}
