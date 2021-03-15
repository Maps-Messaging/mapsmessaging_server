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

package org.maps.selector.operators.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.ParseException;

public class LikeOperatorTest {

  private static final String SOURCE_STRING = "This is a source string pattern that We use for checking";

  @Test
  public void duplicateWildcardWithEscapeConstructorTests() throws ParseException {
    // These wild card matches are ALL the same, match ANY string, so they should just distill down to a singe '%'
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, "\\%%%%%%%", "\\");
    Assertions.assertEquals("\\%%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "\\_%_%_%_%_%_%", "\\");
    Assertions.assertEquals("\\_%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "\\%_%_%_%_%_%_", "\\");
    Assertions.assertEquals("\\%%", likeOperator.getSearchPattern());

    // These wild card matches are ALL the same, match ANY string, so they should just distill down to a singe '%'
    likeOperator = new LikeOperator(SOURCE_STRING, "%%%%%%\\%", "\\");
    Assertions.assertEquals("%\\%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "\\_%_%_%_%_%_\\%", "\\");
    Assertions.assertEquals("\\_%\\%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "%_%_%_%_%_%\\_", "\\");
    Assertions.assertEquals("%\\_", likeOperator.getSearchPattern());

    // These wild card matches are ALL the same, match ANY string, so they should just distill down to a singe '%'
    likeOperator = new LikeOperator(SOURCE_STRING, "%%%%\\%%%", "\\");
    Assertions.assertEquals("%\\%%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "_%_%_%_\\%_%_%", "\\");
    Assertions.assertEquals("%\\%%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "%_%_%_%\\_%_%_", "\\");
    Assertions.assertEquals("%\\_%", likeOperator.getSearchPattern());
  }

  @Test
  public void duplicateWildcardConstructorTests() throws ParseException {
    // These wild card matches are ALL the same, match ANY string, so they should just distill down to a singe '%'
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, "%%%%%%%");
    Assertions.assertEquals("%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "_%_%_%_%_%_%");
    Assertions.assertEquals("%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "%_%_%_%_%_%_");
    Assertions.assertEquals("%", likeOperator.getSearchPattern());

  }

  @Test
  public void duplicateWildcardInMiddleConstructorTests() throws ParseException {
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, "%%%TEXT%%%%");
    Assertions.assertEquals("%TEXT%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "_%_%_%TEXT_%_%_%");
    Assertions.assertEquals("%TEXT%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "%_%_%_TEXT%_%_%_");
    Assertions.assertEquals("%TEXT%", likeOperator.getSearchPattern());
  }

  @Test
  public void duplicateWildcardAtEndConstructorTests() throws ParseException {

    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, "%%%%%%%TEXT");
    Assertions.assertEquals("%TEXT", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "_%_%_%_%_%_%TEXT");
    Assertions.assertEquals("%TEXT", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "%_%_%_%_%_%_TEXT");
    Assertions.assertEquals("%TEXT", likeOperator.getSearchPattern());
  }

  @Test
  public void duplicateWildcardAtStartConstructorTests() throws ParseException {

    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, "TEXT%%%%%%%");
    Assertions.assertEquals("TEXT%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "TEXT_%_%_%_%_%_%");
    Assertions.assertEquals("TEXT%", likeOperator.getSearchPattern());

    likeOperator = new LikeOperator(SOURCE_STRING, "TEXT%_%_%_%_%_%_");
    Assertions.assertEquals("TEXT%", likeOperator.getSearchPattern());
  }


  @Test
  public void simpleSingleMatchTests() throws ParseException {
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, SOURCE_STRING);
    Assertions.assertEquals(true, likeOperator.evaluate(null));

    likeOperator = new LikeOperator(SOURCE_STRING, "_"+SOURCE_STRING.substring(1));
    Assertions.assertEquals(true, likeOperator.evaluate(null));

    likeOperator = new LikeOperator(SOURCE_STRING, SOURCE_STRING.substring(0, SOURCE_STRING.length()-1)+"_");
    Assertions.assertEquals(true, likeOperator.evaluate(null));

    String test = SOURCE_STRING.substring(0, 4)+"_"+SOURCE_STRING.substring(5);
    test = test.substring(0, 7)+"_"+test.substring(8);
    likeOperator = new LikeOperator(SOURCE_STRING, test);
    Assertions.assertEquals(true, likeOperator.evaluate(null),"Failed on "+test);

    test = "_"+ SOURCE_STRING.substring(1, SOURCE_STRING.length()-1)+"_";
    likeOperator = new LikeOperator(SOURCE_STRING, test);
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+test);

    Assertions.assertNotEquals(likeOperator, this);
  }


  @Test
  public void simpleWildcardMatchAtStartTests() throws ParseException {
    String test = SOURCE_STRING;
    for(int x=0;x<test.length();x++){
      String wildcard = "%"+test.substring(x);
      LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, wildcard);
      Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
    }
  }

  @Test
  public void simpleWildcardMatchAtEndTests() throws ParseException {
    String test = SOURCE_STRING;
    for(int x=1;x<test.length();x++){
      String wildcard = test.substring(0, test.length()-x)+"%";
      LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, wildcard);
      Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
    }
  }

  @Test
  public void simpleWildcardMatchInMiddleTests() throws ParseException {
    String test = SOURCE_STRING;
    int length = test.length()/2;
    String start = test.substring(0, length-1);
    String end = test.substring(length);
    
    for(int x=0;x<length;x++){
      String wildcard = start.substring(0,x)+"%"+end.substring(length-x);
      LikeOperator likeOperator = new LikeOperator(SOURCE_STRING, wildcard);
      Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
    }
  }

  @Test
  public void simpleWildcardMatchEscapeAtStartTests() throws ParseException {
    String wildcard = "\\_"+SOURCE_STRING;
    LikeOperator likeOperator = new LikeOperator("_"+SOURCE_STRING, wildcard, "\\");
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);

    wildcard = "\\__"+SOURCE_STRING;
    likeOperator = new LikeOperator("_T"+SOURCE_STRING, wildcard, "\\"); // Second char should be anything
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);


    wildcard = "\\%%"+SOURCE_STRING;
    likeOperator = new LikeOperator("%THIS SHOULD MATCH "+SOURCE_STRING, wildcard, "\\"); // Second char should be anything
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
  }


  @Test
  public void simpleWildcardMatchEscapeAtEndTests() throws ParseException {
    String wildcard = SOURCE_STRING+"\\_";
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING+"_", wildcard, "\\");
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);

    wildcard = SOURCE_STRING+"\\__";
    likeOperator = new LikeOperator(SOURCE_STRING+"_T", wildcard, "\\"); // Second char should be anything
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);


    wildcard = SOURCE_STRING+"\\%%";
    likeOperator = new LikeOperator(SOURCE_STRING+"%THIS SHOULD MATCH ", wildcard, "\\"); // Second char should be anything
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
  }

  @Test
  public void simpleWildcardMatchEscapeInMiddleTests() throws ParseException {
    String wildcard = SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"\\_"+SOURCE_STRING.substring(SOURCE_STRING.length()/2);
    LikeOperator likeOperator = new LikeOperator(SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"_"+SOURCE_STRING.substring(SOURCE_STRING.length()/2), wildcard, "\\");
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);

    wildcard = SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"\\__"+SOURCE_STRING.substring(SOURCE_STRING.length()/2);
    likeOperator = new LikeOperator(SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"_T"+SOURCE_STRING.substring(SOURCE_STRING.length()/2), wildcard, "\\");
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);


    wildcard = SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"\\%%"+SOURCE_STRING.substring(SOURCE_STRING.length()/2);
    likeOperator = new LikeOperator(SOURCE_STRING.substring(0, SOURCE_STRING.length()/2)+"%THIS SHOULD MATCH "+SOURCE_STRING.substring(SOURCE_STRING.length()/2), wildcard, "\\");
    Assertions.assertEquals(true, likeOperator.evaluate(null), "Failed on "+wildcard);
  }
}
