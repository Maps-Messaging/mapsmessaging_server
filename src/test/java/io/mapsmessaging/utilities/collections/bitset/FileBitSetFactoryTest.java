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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileBitSetFactoryTest extends BitSetFactoryTest{



  @Override
  BitSetFactory createFactory(int size) throws IOException {
    factory = new FileBitSetFactoryImpl("./bitsetTest.bit", size);
    return factory;
  }

  @Override
  @Test
  void checkListReturns() throws IOException {
    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      long id = System.currentTimeMillis();
      OffsetBitSet bitSet = bitSetFactory.open(id, 1);
      bitSetFactory.close(bitSet);
      Assertions.assertFalse(bitSetFactory.getUniqueIds().isEmpty());
      Assertions.assertFalse(bitSetFactory.get(-1).isEmpty());
    }
  }

  @Test
  void checkReload() throws IOException {
    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      long id = System.currentTimeMillis();
      OffsetBitSet bitSet = bitSetFactory.open(id, 1);
      for (int x = 0; x < 1024; x++) {
        bitSet.set(x);
      }
      factory.close();
    }

    try( BitSetFactory bitSetFactory = createFactory(1024)) {
      Assertions.assertFalse(bitSetFactory.getUniqueIds().isEmpty());
      for(long id:bitSetFactory.getUniqueIds()){
        List<OffsetBitSet> bitSets = bitSetFactory.get(id);
        Assertions.assertFalse(bitSets.isEmpty());
        for(OffsetBitSet bitSet:bitSets){
          for(int x=0;x<1024;x++){
            Assertions.assertTrue(bitSet.isSet(x));
          }
        }
      }
    }
  }

}
