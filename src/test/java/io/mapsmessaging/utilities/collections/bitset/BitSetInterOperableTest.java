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

package io.mapsmessaging.utilities.collections.bitset;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.BaseTest;

public class BitSetInterOperableTest extends BaseTest {

  private static final int byteCount = 1024;

  @Test
  public void testAndOperations(){
    BitSetImpl inMemory = new BitSetImpl(byteCount);
    ByteBufferBackedBitMap buffered = new ByteBufferBackedBitMap(ByteBuffer.allocate(byteCount/8), 0);

    for(int x=0;x<byteCount;x++){
      if( (x%2)==0){
        inMemory.set(x);
      }
      else{
        buffered.set(x);
      }
    }
    for(int x=0;x<byteCount;x++){
      Assertions.assertTrue(inMemory.isSet(x) != buffered.isSet(x));
    }

    //
    // Test into the java bit set
    //
    testAnd(inMemory, buffered);
    testAnd(buffered, inMemory);
  }

  @Test
  public void testOrOperations(){
    BitSetImpl inMemory = new BitSetImpl(byteCount);
    ByteBufferBackedBitMap buffered = new ByteBufferBackedBitMap(ByteBuffer.allocate(byteCount/8), 0);
    testOr(inMemory, buffered);
    testOr(buffered, inMemory);
  }

  @Test
  public void testXorOperations(){
    BitSetImpl inMemory = new BitSetImpl(byteCount);
    ByteBufferBackedBitMap buffered = new ByteBufferBackedBitMap(ByteBuffer.allocate(byteCount/8), 0);
    testXor(inMemory, buffered);
    testXor(buffered, inMemory);
  }

  @Test
  public void testAndNotOperations(){
    BitSetImpl inMemory = new BitSetImpl(byteCount);
    ByteBufferBackedBitMap buffered = new ByteBufferBackedBitMap(ByteBuffer.allocate(byteCount/8), 0);
    testAndNot(inMemory, buffered);
    testAndNot(buffered, inMemory);
  }

  private void testAndNot(BitSet lhs, BitSet rhs){
    lhs.clear();
    rhs.clear();

    long bitCount = lhs.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        lhs.set(x);
      }
      else{
        rhs.set(x);
      }
    }

    //
    // AND_NOTing the bitmaps should result in no bit changes in bitmap1 since there is no overlap
    //
    lhs.andNot(rhs);
    for(int x=0;x<bitCount;x++) {
      if(x%2 == 0) {
        Assertions.assertTrue(lhs.isSet(x));
      }
      else{
        Assertions.assertFalse(lhs.isSet(x));
      }
    }
  }

  private void testXor(BitSet lhs, BitSet rhs){
    lhs.clear();
    rhs.clear();
    long bitCount = lhs.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        lhs.set(x);
      }
      else{
        rhs.set(x);
      }
    }

    //
    // XORing the bitmaps should result in ALL bits set in bitmap1
    //
    lhs.xor(rhs);
    for(int x=0;x<bitCount;x++) {
      Assertions.assertTrue(lhs.isSet(x));
    }

    lhs.clear();
    rhs.clear();
    for(int x=0;x<bitCount;x++){
      int test = (x%4);
      if(test == 0){
        lhs.set(x);
      }
      else if(test == 1){
        rhs.set(x);
      }
      else if(test == 2){
        lhs.set(x);
        rhs.set(x);
      }
    }

    //
    // XORing should result in every 3rd bit remaining set
    //
    lhs.xor(rhs);
    for(int x=0;x<bitCount;x++) {
      if(x%4 == 0 || x%4 == 1) { // XOR so only true if either one or the other is true not both
        Assertions.assertTrue(lhs.isSet(x));
      }
      else{
        Assertions.assertFalse(lhs.isSet(x));
      }
    }
  }

  private void testOr(BitSet lhs, BitSet rhs){
    lhs.clear();
    rhs.clear();
    for(int x=0;x<byteCount;x++){
      if( (x%2)==0){
        lhs.set(x);
      }
      else{
        rhs.set(x);
      }
    }
    for(int x=0;x<byteCount;x++){
      Assertions.assertTrue(lhs.isSet(x) != rhs.isSet(x));
    }

    //
    // Test into the java bit set
    //
    lhs.or(rhs); // Should set ALL bits
    for(int x=0;x<byteCount;x++){
      Assertions.assertTrue(lhs.isSet(x));
    }
  }


  private void testAnd(BitSet lhs, BitSet rhs){
    rhs.clear();
    lhs.clear();

    //
    // Test into the java bit set
    //
    for(int x=0;x<byteCount;x++){
      if( (x%2)==0){
        rhs.set(x);
      }
      if( (x%4)==0){
        lhs.set(x);
      }
      else if( (x%4)==3){
        lhs.set(x);
      }
    }
    lhs.and(rhs); // Should set every 3rd bits
    for(int x=0;x<byteCount;x++){
      if(x%4 == 0) {
        Assertions.assertTrue(lhs.isSet(x));
      }
      else{
        Assertions.assertFalse(lhs.isSet(x));
      }
    }
  }
}
