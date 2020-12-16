package org.maps.messaging.engine.selector.operators;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;

public class ParserExecutor {

  private final Operation parser;

  public ParserExecutor(Operation parser)  {
    this.parser = parser;
  }

  public boolean evaluate(Message message) throws ParseException {
    Object result = parser.evaluate(message);
    if(result instanceof Boolean){
      return (Boolean)result;
    }
    return false;
  }

  @Override
  public String toString(){
    return parser.toString();
  }

  @Override
  public boolean equals(Object rhs){
    if(rhs instanceof ParserExecutor){
      return parser.equals(((ParserExecutor) rhs).parser);
    }
    return false;
  }

  @Override
  public int hashCode(){
    return parser.hashCode();
  }


}