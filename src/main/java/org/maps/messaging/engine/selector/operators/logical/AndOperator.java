package org.maps.messaging.engine.selector.operators.logical;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.LogicalOperator;
import org.maps.messaging.engine.selector.operators.Operation;

public class AndOperator extends LogicalOperator {

  public AndOperator(Object lhs, Object rhs) {
    super( lhs, rhs);
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    return(test(lhs, message) && test(rhs, message));
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(rhs instanceof Operation){
      rhs = ((Operation)rhs).compile();
    }
    if(lhs instanceof Boolean && rhs instanceof Boolean){
      return ((Boolean)lhs) && ((Boolean)rhs);
    }
    return this;
  }

  @Override
  public String toString(){
    return "("+lhs.toString() +") AND ("+rhs.toString()+")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof AndOperator){
      return (lhs.equals(((AndOperator) test).lhs) && rhs.equals(((AndOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ rhs.hashCode();
  }
}
