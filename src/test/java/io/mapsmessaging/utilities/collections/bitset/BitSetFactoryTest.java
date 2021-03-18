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

package io.mapsmessaging.utilities.collections.bitset;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.BaseTest;

public abstract class BitSetFactoryTest extends BaseTest {

  protected BitSetFactory factory;

  abstract BitSetFactory createFactory(int size) throws IOException;

  @AfterEach
  void cleanUp() throws IOException {
    if(factory != null){
      factory.delete();
    }
  }

  @Test
  void checkCreationAndClose() throws IOException {
    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      Assertions.assertTrue(bitSetFactory.getUniqueIds().isEmpty());
      Assertions.assertDoesNotThrow( () ->{
        OffsetBitSet off = bitSetFactory.open(1, 0);
        bitSetFactory.close(off);
      });
    }
  }

  @Test
  void checkGetters() throws IOException {
    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      Assertions.assertTrue(bitSetFactory.getUniqueIds().isEmpty());
      Assertions.assertEquals(1024, bitSetFactory.getSize());
      Assertions.assertEquals(2048, bitSetFactory.getStartIndex(3000));
    }
  }

  // Should be overridden by file backed factories
  @Test
  void checkListReturns() throws IOException {
    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      Assertions.assertTrue(bitSetFactory.getUniqueIds().isEmpty());
      Assertions.assertTrue(bitSetFactory.get(1).isEmpty());
      Assertions.assertTrue(bitSetFactory.getUniqueIds().isEmpty());
    }
  }

}
