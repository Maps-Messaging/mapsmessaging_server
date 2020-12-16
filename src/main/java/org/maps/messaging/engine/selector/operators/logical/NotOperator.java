package org.maps.messaging.engine.selector.operators.logical;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.LogicalOperator;
import org.maps.messaging.engine.selector.operators.Operation;

public class NotOperator extends LogicalOperator {

  public NotOperator(Object lhs) {
    super( lhs, null);
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    return !test(lhs, message);
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(lhs instanceof Boolean){
      return !((Boolean)lhs);
    }
    return this;
  }

  @Override
  public String toString(){
    return "NOT ("+lhs.toString() +")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof NotOperator){
      return (lhs.equals(((NotOperator) test).lhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return ~lhs.hashCode();
  }

}
