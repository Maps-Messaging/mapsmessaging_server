package org.maps.messaging.engine.selector.operators.comparison;

import org.maps.messaging.engine.selector.operators.ComparisonOperator;

public class DifferentOperator  extends ComparisonOperator {

  public DifferentOperator(Object lhs, Object rhs) {
    super(lhs, rhs);
  }

  @Override
  protected Boolean compute(double lhs, double rhs) {
    return lhs != rhs;
  }

  @Override
  protected Boolean compute(double lhs, long rhs) {
    return lhs != rhs;
  }

  @Override
  protected Boolean compute(long lhs, double rhs) {
    return lhs != rhs;
  }

  @Override
  protected Boolean compute(long lhs, long rhs) {
    return lhs != rhs;
  }

  @Override
  protected Boolean compute(String lhs, String rhs) {
    return !lhs.equals(rhs);
  }

  @Override
  protected Boolean compute(Boolean lhs, Boolean rhs){
    return !lhs.equals(rhs);
  }

  @Override
  public String toString(){
    return "("+lhs.toString() +") != ("+rhs.toString()+")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof DifferentOperator){
      return (lhs.equals(((DifferentOperator) test).lhs) && rhs.equals(((DifferentOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ rhs.hashCode();
  }
}
