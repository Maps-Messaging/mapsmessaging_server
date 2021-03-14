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

import org.maps.selector.ParseException;
import org.maps.selector.operators.FunctionOperator;
import org.maps.selector.operators.IdentifierResolver;

public class LikeOperator  extends FunctionOperator {

  private static final char MULTI_CHARACTER = '%';
  private static final char SINGLE_CHARACTER = '_';

  private static final String MULTI_STRING  = String.valueOf(MULTI_CHARACTER);

  private final Object lhs;
  private final String searchPattern;
  private final char escape;
  private final boolean hasEscape;

  public LikeOperator(Object lhs, Object rhs) throws ParseException {
    this(lhs, rhs, null);
  }

  public LikeOperator(Object lhs, Object rhs, Object escape) throws ParseException {
    this.lhs = lhs;
    String workingPattern = rhs.toString();
    if(workingPattern.length() > 100*1024){
      throw new ParseException("Wildcard pattern exceeds size limit");
    }
    if(escape != null) {
      this.escape = escape.toString().charAt(0);
      hasEscape = true;
      workingPattern = removeDuplicates(workingPattern,MULTI_STRING+SINGLE_CHARACTER);// Since a wild card eats single character wild cards
      workingPattern = removeDuplicates(workingPattern,SINGLE_CHARACTER+MULTI_STRING);// Since a wild card eats single character wild cards
      workingPattern = removeDuplicates(workingPattern,MULTI_STRING+MULTI_STRING);// This syntax is the same as having 1 global wildcard, so lets simplify
    }
    else{
      workingPattern = removeDuplicates(workingPattern,MULTI_STRING+SINGLE_CHARACTER);// Since a wild card eats single character wild cards
      workingPattern = removeDuplicates(workingPattern,SINGLE_CHARACTER+MULTI_STRING);// Since a wild card eats single character wild cards
      workingPattern = removeDuplicates(workingPattern,MULTI_STRING+MULTI_STRING);// This syntax is the same as having 1 global wildcard, so lets simplify
      this.escape = '\\';
      hasEscape = false;
    }
    searchPattern = workingPattern;
  }

  public Object compile(){
    if(lhs instanceof String){
      return compare((String)lhs, searchPattern);
    }
    return this;
  }

  private String removeDuplicates(String source, String pattern){
    if(hasEscape){
      return removeDuplicatesWithEscape(source);
    }
    else {
      while (source.contains(pattern)) {
        source = source.replaceAll(pattern, MULTI_STRING);
      }
    }
    return source;
  }

  private String removeDuplicatesWithEscape(String source){
    int position = 0;
    while(position < source.length()-1){
      if(source.charAt(position) == escape){
        position++; // skip the next char, regardless of its type
      }
      else{
        //if the current char is a multichar wild card check the next and see if we need to remove it
        if(source.charAt(position) == MULTI_CHARACTER &&
            ( source.charAt(position+1) == MULTI_CHARACTER || source.charAt(position+1) == SINGLE_CHARACTER)){
          String start = source.substring(0, position+1);
          String end = source.substring(position+2);
          source = start+end;
          position--;
        }
        else if(source.charAt(position) == SINGLE_CHARACTER && source.charAt(position+1) == MULTI_CHARACTER){
          // Remove the single char
          String start = source.substring(0, position);
          String end = source.substring(position+1);
          source = start+end;
          position--;
        }
      }
      position++;
    }

    return source;
  }


  @Override
  public Object evaluate(IdentifierResolver resolver) throws ParseException {
    Object lookup = evaluate(lhs, resolver);
    if(lookup != null){
      String sourceString = lookup.toString();
      return compare(sourceString, searchPattern);
    }
    return false;
  }

  private boolean compare( String sourceString, String wildcard)
  {

    // We have detected the escape character, so we need to test the next char as a literal and not a wild card element
    if(hasEscape && (wildcard.length()>0 &&wildcard.charAt(0) == escape)) {
      // skip the escape char and now we do a direct test
      wildcard = wildcard.substring(1);
      if (wildcard.charAt(0) != sourceString.charAt(0)) { // Doesn't match
        return false;
      }
      return compare(sourceString.substring(1), wildcard.substring(1));
    }

    // This is the end of the strings, we have matched to here
    if (wildcard.length() == 0 && sourceString.length() == 0) {
      return true;
    }

    // Check for multiple character wild cards and see if we have run off the end of the string
    if (wildcard.length() > 1 && wildcard.charAt(0) == MULTI_CHARACTER && sourceString.length() == 0)
      return false;

    // Check both the first and last entry in the wildcard and see if we can handle a single character
    if ((wildcard.length() > 0 && wildcard.charAt(0) == SINGLE_CHARACTER) ||
        (wildcard.length() > 0 && sourceString.length() > 0 && wildcard.charAt(0) == sourceString.charAt(0))) {
      return compare(sourceString.substring(1), wildcard.substring(1));
    }

    // We are either at the end of the wild card and its a multiple wildcard or there is more to go
    if (wildcard.length() > 0 && wildcard.charAt(0) == MULTI_CHARACTER) {
      return compare(sourceString, wildcard.substring(1)) || compare(sourceString.substring(1), wildcard);
    }
    return false;
  }

  public String toString(){
    if(hasEscape) {
      return "(" + lhs.toString() + ") LIKE (" + searchPattern + ", ESCAPE " + escape + ")";
    }
    return "(" + lhs.toString() + ") LIKE (" + searchPattern + ")";
  }

  protected String getSearchPattern(){
    return searchPattern;
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof LikeOperator){
      return (lhs.equals(((LikeOperator) test).lhs) &&
          searchPattern.equals(((LikeOperator) test).searchPattern) &&
          escape == (((LikeOperator) test).escape));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ searchPattern.hashCode() + escape;
  }

}
