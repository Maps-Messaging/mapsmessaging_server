# Usage
# Extensible JMS Selector Parser
JMS Selector parser, this is a 2 pass parser that compiles the selector to the simplest form for faster execution.

Anything that has a key:value configuration can be filtered, for example a java bean object such as the following trivial example.

### Filtering Beans
Here we have an object, BeanTest, that has an integer counter. We create a filter that will return true only if the counter == 10.

```java
class SelectorValidationTest {

  @Test
  void checkBeans() {
    BeanTest bean = new BeanTest();
    bean.setCounter(0);
    ParserExecutor parser = SelectorParser.compile("counter = 10");
    for (int x = 0; x < 9; x++) {
      bean.increment();
      Assertions.assertFalse(parser.evaluate(bean));
    }
    bean.increment();
    Assertions.assertTrue(parser.evaluate(bean));
  }

  // Test bean class, can be any Java class that offers get functions
  public class BeanTest {
    private int counter;

    public void increment() {
      counter++;
    }

    public int getCounter() {
      return counter;
    }

    public void setCounter(int counter) {
      this.counter = counter;
    }
  }
}
```
Please note that for Java beans the name of the key is case-sensitive, so notice that the syntax uses "counter" and not "Counter" as the key.

### Filtering Maps
It can also be used to filter a Map<String, Object> such as
```java
class Examples {

  public void example() {
    String selector = "currency IN ('aud', 'usd', 'jpy')";
    ParserExecutor filter = SelectorParser.compile(selector);

    // Build up simple map to be evaluated
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("currency", "aud");

    // Now evaluate the map against the supplied Selector syntax
    if (parserExecutor.evaluate(map)) {
      System.err.println("Currency is either AUD, USD or JPY");
    }
  }
}
```
### Complex Objects

If your application has complex objects or data contained within an object that requires a more complex method of access, then simply write a class that extends [IdentifierResolver.java](https://github.com/Maps-Messaging/jms_selector_parser/blob/main/src/main/java/io/mapsmessaging/selector/IdentifierResolver.java). Here your code will implement the get(key) function, and you can return the calculated value. 

Suppose we have a message object such as below. While it is a very simplistic message object it will illustrate the point.

```java
public class Message implements IdentifierResolver {

  private Map<String, Object> map;
  private byte[] opaqueData;

  public Message(){

  }

  @Override
  public Object get(String key) {
    if (map != null) {
      return map.get(key);
    }
    return null;
  }

  @Override
  public byte[] getOpaqueData() {
    return opaqueData;
  }

  public void setMap(Map<String, Object> map) {
    this.map = map;
  }

  public void setOpaqueData(byte[] opaqueData) {
    this.opaqueData = opaqueData;
  }

  public Map<String, Object> getMap() {
    return map;
  }
}
```

Since it implements IdentifierResolver, the parser will call the get(String key) function to resolve the value. This is where your application can either calculate the value, 
do some complex lookups or whatever is required to resolve the value.