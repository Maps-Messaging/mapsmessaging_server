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

import java.util.Iterator;
import java.util.ListIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.BaseTest;

public abstract class BitSetTest extends BaseTest {

  private static final int offsetDefault = 8192;

  public BitSet getBitSet(){
    return getBitSet(offsetDefault);
  }
  public abstract BitSet getBitSet(int size);

  @Test
  public void simpleGetSet(){
    BitSet bitmap = getBitSet();
    long id = System.currentTimeMillis();
    bitmap.setUniqueId(id);
    Assertions.assertEquals(id, bitmap.getUniqueId());
  }

  @Test
  public void testSimpleBitOperations(){
    BitSet bitmap = getBitSet();
    bitmap.set(0);
    Assertions.assertTrue(bitmap.isSet(0));
    bitmap.set(63);
    Assertions.assertTrue(bitmap.isSet(63));
    bitmap.set(64);
    Assertions.assertTrue(bitmap.isSet(64));
    bitmap.set(127);
    Assertions.assertTrue(bitmap.isSet(127));
    bitmap.set(128);
    Assertions.assertTrue(bitmap.isSet(128));

    bitmap.clear(0);
    Assertions.assertFalse(bitmap.isSet(0));
    bitmap.clear(63);
    Assertions.assertFalse(bitmap.isSet(63));
    Assertions.assertTrue(bitmap.isSet(64));
    Assertions.assertTrue(bitmap.isSet(127));
    Assertions.assertTrue(bitmap.isSet(128));

    bitmap.clear();

    bitmap.set(1);
    Assertions.assertTrue(bitmap.isSet(1));
    Assertions.assertFalse(bitmap.isSet(0));
    Assertions.assertFalse(bitmap.isSet(2));
    bitmap.flip(1);
    Assertions.assertFalse(bitmap.isSet(0));
    Assertions.assertFalse(bitmap.isSet(1));
    Assertions.assertFalse(bitmap.isSet(2));


    bitmap.flip(1);
    Assertions.assertTrue(bitmap.isSet(1));
    Assertions.assertFalse(bitmap.isSet(0));
    Assertions.assertFalse(bitmap.isSet(2));
    bitmap.flip(1);
    Assertions.assertFalse(bitmap.isSet(0));
    Assertions.assertFalse(bitmap.isSet(1));
    Assertions.assertFalse(bitmap.isSet(2));
  }

  @Test
  public void testClearing(){
    BitSet bitmap = getBitSet();
    long bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.set(x);
      Assertions.assertTrue(bitmap.isSet(x));
    }
    Assertions.assertFalse(bitmap.isEmpty());
    bitmap.clear();
    for(int x=0;x<bitCount;x++){
      Assertions.assertFalse(bitmap.isSet(x));
    }
    Assertions.assertTrue(bitmap.isEmpty());
  }


  @Test
  public void testIsSetAndClear(){
    BitSet bitmap = getBitSet();
    long bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.set(x);
      Assertions.assertTrue(bitmap.isSet(x));
    }
    for(int x=0;x<bitCount;x++){
      Assertions.assertTrue(bitmap.isSetAndClear(x));
    }
    for(int x=0;x<bitCount;x++){
      Assertions.assertFalse(bitmap.isSet(x));
    }
  }

  @Test
  public void testNextSetAndClear(){
    BitSet bitmap = getBitSet();
    long bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.set(x);
      Assertions.assertTrue(bitmap.isSet(x));
    }
    for(int x=0;x<bitCount;x++){
      Assertions.assertNotEquals(-1, bitmap.nextSetBitAndClear(0));
    }
    for(int x=0;x<bitCount;x++){
      Assertions.assertFalse(bitmap.isSet(x));
    }
  }


  @Test
  public void testFlipping(){
    BitSet bitmap = getBitSet();
    long bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.flip(x);
      Assertions.assertTrue(bitmap.isSet(x));
    }
    for(int x=0;x<bitCount;x++){
      bitmap.flip(x);
      Assertions.assertFalse(bitmap.isSet(x));
    }
  }

  @Test
  public void testRangedFlipping(){
    BitSet bitmap = getBitSet();

    long bitCount = bitmap.length();
    bitmap.flip(1, 24);
    Assertions.assertFalse(bitmap.isSet(0));

    for(int x=1; x<24;x++){
      Assertions.assertTrue(bitmap.isSet(x));
    }

    for(int x=25; x<bitCount;x++){
      Assertions.assertFalse(bitmap.isSet(x));
    }
  }

  @Test
  public void testFindSetBit(){
    BitSet bitmap = getBitSet();
    bitmap.set(0);
    Assertions.assertEquals(1, bitmap.cardinality());
    bitmap.set(63);
    Assertions.assertEquals(2, bitmap.cardinality());
    bitmap.set(64);
    Assertions.assertEquals(3, bitmap.cardinality());
    bitmap.set(127);
    Assertions.assertEquals(4, bitmap.cardinality());
    bitmap.set(128);
    Assertions.assertEquals(5, bitmap.cardinality());
    Assertions.assertEquals(0, bitmap.nextSetBit(0));
    bitmap.clear(0);
    Assertions.assertEquals(63, bitmap.nextSetBit(0));
    bitmap.clear(63);
    Assertions.assertEquals(64, bitmap.nextSetBit(0));
    bitmap.clear(64);
    Assertions.assertEquals(127, bitmap.nextSetBit(0));
    bitmap.clear(127);
    Assertions.assertEquals(128, bitmap.nextSetBit(0));
    bitmap.clear(128);
    Assertions.assertEquals(-1, bitmap.nextSetBit(0));
  }

  @Test
  public void testFindClearBit(){
    BitSet bitmap = getBitSet();
    long bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.set(x);
    }
    bitmap.clear(128);
    Assertions.assertEquals(128, bitmap.nextClearBit(0));
  }

  @Test
  public void testFindPreviousSetBit(){
    BitSet bitmap = getBitSet();
    bitmap.set(128);
    Assertions.assertEquals(1, bitmap.cardinality());
    int bitCount = bitmap.length()-1;
    Assertions.assertEquals(128, bitmap.previousSetBit(bitCount));
  }

  @Test
  public void testFindPreviousClearBit(){
    BitSet bitmap = getBitSet();
    int bitCount = bitmap.length();
    for(int x=0;x<bitCount;x++){
      bitmap.set(x);
    }

    bitmap.clear(128);
    Assertions.assertEquals(bitmap.length()-1, bitmap.cardinality());
    bitCount = bitmap.length()-1;
    Assertions.assertEquals(128, bitmap.previousClearBit(bitCount));
  }

  @Test
  public void testBitWiseANDOperations(){
    BitSet bitmap1 = getBitSet();
    BitSet bitmap2 = getBitSet();

    int bitCount = bitmap1.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        bitmap1.set(x);
      }
      else{
        bitmap2.set(x);
      }
    }

    //
    // ANDing the bitmaps should result in ALL bits not set in bitmap1
    //
    bitmap1.and(bitmap2);
    for(int x=0;x<bitCount;x++) {
      Assertions.assertFalse(bitmap1.isSet(x));
    }

    bitmap1.clear();
    bitmap2.clear();
    for(int x=0;x<bitCount;x++){
      int test = (x%3);
      if(test == 0){
        bitmap1.set(x);
      }
      else if(test == 1){
        bitmap2.set(x);
      }
      else{
        bitmap1.set(x);
        bitmap2.set(x);
      }
    }

    //
    // ANDing should result in every 3rd bit remaining set
    //
    bitmap1.and(bitmap2);
    for(int x=0;x<bitCount;x++) {
      if(x%3 == 2) {
        Assertions.assertTrue(bitmap1.isSet(x));
      }
      else{
        Assertions.assertFalse(bitmap1.isSet(x));
      }
    }
  }

  @Test
  public void testBitWiseOROperations(){
    BitSet bitmap1 = getBitSet();
    BitSet bitmap2 = getBitSet();

    long bitCount = bitmap1.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        bitmap1.set(x);
      }
      else{
        bitmap2.set(x);
      }
    }

    //
    // ORing the bitmaps should result in ALL bits set in bitmap1
    //
    bitmap1.or(bitmap2);
    for(int x=0;x<bitCount;x++) {
      Assertions.assertTrue(bitmap1.isSet(x));
    }

    bitmap1.clear();
    bitmap2.clear();
    for(int x=0;x<bitCount;x++){
      int test = (x%3);
      if(test == 0){
        bitmap1.set(x);
      }
      else if(test == 1){
        bitmap2.set(x);
      }
    }

    //
    // ANDing should result in every 3rd bit remaining set
    //
    bitmap1.or(bitmap2);
    for(int x=0;x<bitCount;x++) {
      if(x%3 == 2) {
        Assertions.assertFalse(bitmap1.isSet(x));
      }
      else{
        Assertions.assertTrue(bitmap1.isSet(x));
      }
    }
  }

  @Test
  public void testListIterator(){
    BitSet bitSet = getBitSet();


    int[] test = {0, 2, 5, 10, 100, 200};

    for(int set:test){
      bitSet.set(set);
    }

    ListIterator<Integer> itr = bitSet.listIterator();
    for (int i : test) {
      Assertions.assertTrue(itr.hasNext());
      Assertions.assertEquals(itr.next(), i);
    }

    Assertions.assertFalse(itr.hasNext());
    for(int i=test.length-1;i>0;i--){
      Assertions.assertTrue(itr.hasPrevious());
      Assertions.assertEquals(itr.previous(), test[i]);
    }

  }

  @Test
  public void testBitWiseXOROperations(){
    BitSet bitmap1 = getBitSet();
    BitSet bitmap2 = getBitSet();

    long bitCount = bitmap1.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        bitmap1.set(x);
      }
      else{
        bitmap2.set(x);
      }
    }

    //
    // XORing the bitmaps should result in ALL bits set in bitmap1
    //
    bitmap1.xor(bitmap2);
    for(int x=0;x<bitCount;x++) {
      Assertions.assertTrue(bitmap1.isSet(x));
    }

    bitmap1.clear();
    bitmap2.clear();
    for(int x=0;x<bitCount;x++){
      int test = (x%4);
      if(test == 0){
        bitmap1.set(x);
      }
      else if(test == 1){
        bitmap2.set(x);
      }
      else if(test == 2){
        bitmap1.set(x);
        bitmap2.set(x);
      }
    }

    //
    // XORing should result in every 3rd bit remaining set
    //
    bitmap1.xor(bitmap2);
    for(int x=0;x<bitCount;x++) {
      if(x%4 == 0 || x%4 == 1) { // XOR so only true if either one or the other is true not both
        Assertions.assertTrue(bitmap1.isSet(x));
      }
      else{
        Assertions.assertFalse(bitmap1.isSet(x));
      }
    }

  }

  @Test
  public void testBitWiseANDNOTOperations(){
    BitSet bitmap1 = getBitSet();
    BitSet bitmap2 = getBitSet();

    long bitCount = bitmap1.length();
    for(int x=0;x<bitCount;x++){
      if(x%2 == 0){
        bitmap1.set(x);
      }
      else{
        bitmap2.set(x);
      }
    }

    //
    // AND_NOTing the bitmaps should result in no bit changes in bitmap1 since there is no overlap
    //
    bitmap1.andNot(bitmap2);
    for(int x=0;x<bitCount;x++) {
      if(x%2 == 0) {
        Assertions.assertTrue(bitmap1.isSet(x));
      }
      else{
        Assertions.assertFalse(bitmap1.isSet(x));
      }
    }

    bitmap1.clear();
    bitmap2.clear();
    for(int x=0;x<bitCount;x++){
      int test = (x%4);
      if(test == 0){
        bitmap1.set(x);
      }
      else if(test == 1){
        bitmap2.set(x);
      }
      else if(test == 2){
        bitmap1.set(x);
        bitmap2.set(x);
      }
      else{
        bitmap1.clear(x);
        bitmap2.clear(x);
      }
    }

    //
    // ANDing should result in every 3rd bit remaining set
    //
    bitmap1.andNot(bitmap2);
    for(int x=0;x<bitCount;x++) {
      if(x%4 == 0) {
        Assertions.assertTrue(bitmap1.isSet(x));
      }
      else{
        Assertions.assertFalse(bitmap1.isSet(x));
      }
    }
  }


  @Test
  public void testExceptions(){
    BitSet bitmap = getBitSet();

    try{
      bitmap.set(-1);
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }

    try{
      bitmap.set((1+bitmap.length()));
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }

    try{
      bitmap.clear(-1);
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }

    try{
      bitmap.clear((1+bitmap.length()));
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }

    try{
      bitmap.isSet(-1);
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }

    try{
      bitmap.isSet((1+bitmap.length()));
      Assertions.fail("This should have thrown an exception");
    }
    catch(IndexOutOfBoundsException correct){
      // Correct behaviour
    }
  }

  @Test
  public void testIterator(){
    BitSet bitmap = getBitSet();

    int[] test = {0, 2, 5, 10, 100, 200};

    for(int set:test){
      bitmap.set(set);
    }

    Iterator<Integer> iterator = bitmap.iterator();
    int idx = 0;
    while(iterator.hasNext()){
      Assertions.assertEquals(test[idx], iterator.next());
      idx++;
    }

    bitmap.clear();
    for(int x=0;x<bitmap.length();x++){
      bitmap.set(x);
    }

    iterator = bitmap.iterator();
    idx = 0;
    while(iterator.hasNext()){
      long next = iterator.next();
      Assertions.assertEquals(idx, next);
      idx++;
    }

    bitmap = getBitSet();

    for(int x=0;x<bitmap.length();x++){
      bitmap.set(x);
    }

    iterator = bitmap.iterator();
    idx = 0;
    while(iterator.hasNext()){
      int next = iterator.next();
      Assertions.assertEquals(idx, next);
      idx++;
    }

    idx =0;
    iterator = bitmap.iterator();
    while(iterator.hasNext()){
      int next = iterator.next();
      Assertions.assertEquals(idx, next);
      iterator.remove();
      idx++;
    }
    Assertions.assertTrue(bitmap.isEmpty());
  }


}
