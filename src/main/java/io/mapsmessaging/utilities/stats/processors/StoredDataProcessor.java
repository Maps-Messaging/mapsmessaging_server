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

package io.mapsmessaging.utilities.stats.processors;

import java.util.ArrayList;
import java.util.List;

public abstract class StoredDataProcessor implements DataProcessor {

  protected final List<DataPoint> dataPoints;

  protected StoredDataProcessor() {
    dataPoints = new ArrayList<>();
  }

  @Override
  public long calculate() {
    int size = dataPoints.size();
    if (size > 0) {
      long sum = 0;
      for (DataPoint point : dataPoints) {
        sum += point.data;
      }
      dataPoints.clear();
      return sum / size;
    }
    return 0;
  }

  protected static class DataPoint {

    protected final long data;

    public DataPoint(long data) {
      this.data = data;
    }
  }

}