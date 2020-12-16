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

package org.maps.utilities.collections.bitset;

import java.util.Iterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.BaseTest;

public abstract class OffsetBitSetTest extends BaseTest {

  public abstract  BitSet createBitSet();

  public OffsetBitSet createOffsetBitset(long offset){
    return new OffsetBitSet(createBitSet(), offset);
  }

  @Test
  public void testBasicFunctions(){
    long offset = 1L << 32;
    OffsetBitSet offsetBitSet = createOffsetBitset(offset); // Ensure long values are set and returned

    for(int x=0;x<100;x++){
      offsetBitSet.set(offset+x);
      Assertions.assertTrue(offsetBitSet.isSet(offset+x));
    }

    Iterator<Long> itr = offsetBitSet.iterator();
    long start = offset;
    while(itr.hasNext()){
      Assertions.assertEquals(start, itr.next());
      start++;
    }
  }

  @Test
  public void testGeneralFunctions(){
    long offset = 64;
    OffsetBitSet offsetBitSet = createOffsetBitset(offset); // Ensure long values are set and returned

    // Should fail
    try {
      offsetBitSet.set(0);
      Assertions.fail("This should have failed");
    } catch (IndexOutOfBoundsException e) {
      // Correct behaviour
    }


    // Should fail
    try {
      offsetBitSet.set(offset+offsetBitSet.length()+1);
      Assertions.fail("This should have failed");
    } catch (IndexOutOfBoundsException e) {
      // Correct behaviour
    }

    offsetBitSet.set(offset);
    Assertions.assertTrue(offsetBitSet.isSet(offset));

    offsetBitSet.set(offset+offsetBitSet.length()-1);
    Assertions.assertTrue(offsetBitSet.isSet(offset+offsetBitSet.length()-1));



  }

}
