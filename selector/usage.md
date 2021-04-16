# Usage

To build a ParserExecutor object you simply supply a valid JMS Selector string (As per section 3.8.1 of the JMS 2.0 standard found [here](https://download.oracle.com/otndocs/jcp/jms-2_0_rev_a-mrel-eval-spec/index.html))

The following is a trivial code snippet that demonstrates how to parse a Selector string to a ParserExecutor object and then how to use it to evaluate a Map.
```java
    String selector = "currency IN ('aud', 'usd', 'jpy')"
    ParserExecutor filter = SelectorParser.compile(selector);

    // Build up simple map to be evaluated
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("currency", "aud");
    
    // Now evaluate the map against the supplied Selector syntax
    if(parserExecutor.evaluate(map)){
      System.err.println("Currency is either AUD, USD or JPY");
    }
```
The resultant ParserExecutor is a thread safe filter that can be used with any key/value object that implements [IdentifierResolver.java](src/main/java/io/mapsmessaging/selector/IdentifierResolver.java) or a Map<String, Object> map.

If you have a complex object that requires processing to get the value based on a key name then simply extend [IdentifierResolver.java](src/main/java/io/mapsmessaging/selector/IdentifierResolver.java) and add the code to compute the value to return.

