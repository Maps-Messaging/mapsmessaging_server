
# Extending the Selector

The selector syntax has an additional verb that is not found in the JMS Selector syntax called <b>extension</b> that can be used to add further filtering logic that you may need.

The library comes with a built-in JSON extension that can be used to filter JSON objects that are supplied as byte[].

To extend the selector simply

* Create a new filter extension by extending the interface ParserExtension.java
* Update the java services file found in META-INF.servers
* Update your selector string to reference it

The standard syntax for the extension is as follows

```text
Identifier|value = extension ('<extension name>', 'argument 0', 'argument 1', ,,, 'argument n');
```

The extension verb uses the first parameter as the name to look up, this name is the name returned by your extensions getName() function.
Once the selector has located the extension it then creates a new instance and passes the arguments to it. Then on each subsequent <b>ParserExecutor.evaluate</b> call the extensions own evaluate is called with an Identifier to use as part of the lookup


## Example Extension

For example, lets increment a simple counter, all it does is increment a counter whenever it is called

```java

public class CounterExtension implements ParserExtension {

  private AtomicLong counter = new AtomicLong(0);

  @Override
  public ParserExtension createInstance(List<String> arguments)  {
    return new CounterExtension();
  }

  @Override
  public Object evaluate(IdentifierResolver resolver) {
    return counter.getAndIncrement();
  }

  @Override
  public String getName() {
    return "counter";
  }

  @Override
  public String getDescription() {
    return "Simple parse counter, increments every call";
  }
}


```

Update the configuration file META-INF.servers/io.mapsmessaging.selector.extensions.ParserExtension and add the following

```properties
io.mapsmessaging.selector.extensions.CounterExtension
```

Now to use it in the syntax
```java
public class Example {
  void simpleCounter() throws ParseException {
    String selector = "10 = extension ('counter', '')";
    ParserExecutor parserExecutor = SelectorParser.compile(selector);

    // This should fail the counter is less then 10
    for (int x = 0; x < 10; x++) {
      Assertions.assertFalse(parserExecutor.evaluate(key -> "Hi"));
    }
    // This should work since the counter is in fact 10
    Assertions.assertTrue(parserExecutor.evaluate(key -> "Hi"));
  }
}
```



Examples can be found [here](https://github.com/Maps-Messaging/jms_selector_parser/tree/main/src/examples)
