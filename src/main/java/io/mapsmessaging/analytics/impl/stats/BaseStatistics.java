package io.mapsmessaging.analytics.impl.stats;

import com.google.gson.JsonObject;

public class BaseStatistics implements Statistics {
  protected double first;
  protected double last;
  protected double min;
  protected double max;
  protected double average;
  protected int count;

  public BaseStatistics(){
    reset();
  }

  public void reset(){
    first = Double.NaN;
    last = Double.NaN;
    min = Double.MAX_VALUE;
    max = Double.MIN_VALUE;
    average = 0.0;
    count = 0;
  }

  public void update(Object entry) {
    if(entry instanceof Number number){
      update(number.doubleValue());
    }
  }



  protected void update(double cur){
    if(count == 0){
      first = cur;
    }
    if(min > cur){
      min = cur;
    }
    if(max < cur){
      max = cur;
    }
    count++;
    average = ((average * (count - 1)) + cur) / count;
    last = cur;
  }

  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("first", first);
    o.addProperty("last", last);
    o.addProperty("min", min);
    o.addProperty("max", max);
    o.addProperty("average", average);
    o.addProperty("count", count);
    o.addProperty("range", max - min);
    o.addProperty("delta", last - first);
    addSubclassJson(o);
    return o;
  }


  protected void addSubclassJson(JsonObject o) {
    // default: nothing
  }

  @Override
  public Statistics create() {
    return new BaseStatistics();
  }

  @Override
  public String getName() {
    return "Base";
  }

  @Override
  public String getDescription() {
    return "Base statistics";
  }
}
