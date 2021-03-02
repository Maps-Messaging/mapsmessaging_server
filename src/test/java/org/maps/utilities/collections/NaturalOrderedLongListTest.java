/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.utilities.collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.BaseTest;
import org.maps.utilities.collections.bitset.ByteBufferBitSetFactoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

class NaturalOrderedLongListTest extends BaseTest {


  @Test
  void constructors(){
    Assertions.assertThrows(NullPointerException.class, () ->{
      new NaturalOrderedLongList(0, null);
    });
    ByteBufferBitSetFactoryImpl factory = new ByteBufferBitSetFactoryImpl(4096);
    Assertions.assertDoesNotThrow(()->{
      new NaturalOrderedLongList(0, factory);
    });
  }

  @Test
  void listIterator() {
    List<Long> list = new ArrayList<>();
    for(long x=0;x< 1000;x++){
      list.add(x);
    }
    ByteBufferBitSetFactoryImpl factory = new ByteBufferBitSetFactoryImpl(4096);
    NaturalOrderedLongList noll = new NaturalOrderedLongList(0, factory);
    noll.addAll(list);
    Assertions.assertEquals(1000, noll.size());

    ListIterator<Long> listIterator = noll.listIterator();
    long expected = 0;
    // Forward walk
    while(listIterator.hasNext()){
      Assertions.assertEquals(expected, listIterator.next());
      expected++;
    }
    expected--;
    // Backward walk
    while(listIterator.hasPrevious()){
      if(expected == 0){
        System.err.println("time to check");
      }
      Assertions.assertEquals(expected, listIterator.previous());
      expected--;
    }
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ listIterator.nextIndex(); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ listIterator.previousIndex(); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ listIterator.set(12L); });
  }

  @Test
  void unsupportedFunctions(){
    ByteBufferBitSetFactoryImpl factory = new ByteBufferBitSetFactoryImpl(4096);
    NaturalOrderedLongList noll = new NaturalOrderedLongList(0, factory);
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.get(1); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.set(1, 4L); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.add(1, 4L); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.remove(1); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.indexOf(5L); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.lastIndexOf(5L); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.listIterator(5); });
    Assertions.assertThrows(UnsupportedOperationException.class, () ->{ noll.subList(5, 10); });
  }

  @Test
  void allAPICollections(){
    List<Long> list = new ArrayList<>();
    for(long x=0;x< 1000;x++){
      list.add(x);
    }
    ByteBufferBitSetFactoryImpl factory = new ByteBufferBitSetFactoryImpl(4096);
    NaturalOrderedLongList noll = new NaturalOrderedLongList(0, factory);
    noll.addAll(list);
    Assertions.assertEquals(1000, noll.size());

    Assertions.assertTrue(noll.containsAll(list));

    Assertions.assertFalse(noll.retainAll(list));
    Assertions.assertEquals(1000, noll.size());

    noll.removeAll(list);
    Assertions.assertEquals(0, noll.size());

    noll.addAll(list);
    Assertions.assertEquals(1000, noll.size());

    list.clear();
    for(long x=0;x<1000;x++){
      if(x%2 == 0){
        list.add(x);
      }
    }
    noll.retainAll(list);
    Assertions.assertEquals(list.size(), noll.size());
    Assertions.assertTrue(noll.containsAll(list));
  }

}
