package org.maps.messaging.engine.selector.operators.logical;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.LogicalOperator;
import org.maps.messaging.engine.selector.operators.Operation;

public class OrOperator extends LogicalOperator {

  public OrOperator(Object lhs, Object rhs) {
    super( lhs, rhs);
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    return(test(lhs, message) || test(rhs, message));
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(rhs instanceof Operation){
      rhs = ((Operation)rhs).compile();
    }
    if(lhs instanceof Boolean && rhs instanceof Boolean){
      return ((Boolean)lhs) || ((Boolean)rhs);
    }
    return this;
  }
  @Override
  public String toString(){
    return "("+lhs.toString() +") OR ("+rhs.toString()+")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof OrOperator){
      return (lhs.equals(((OrOperator) test).lhs) && rhs.equals(((OrOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ rhs.hashCode();
  }

}
