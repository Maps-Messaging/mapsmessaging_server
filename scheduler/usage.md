# Usage

Usage of the scheduler is actually very straight forward, the complexity is more about how the tasks are defined and how your application ensures correct access your structures.

You will need to ensure that all access to your structure is performed by a single Scheduler Queue. Then simply create the tasks that modify this structure and set up the appropriate functions to

* Collect the context
* Create the task
* Queue the task
* Wait for completion (Optional, depending on your application)


## pom.xml setup

Add the repository configuration into the pom.xml
``` xml
    <!-- MapsMessaging jfrog server -->
    <repository>
      <id>mapsmessaging.io</id>
      <name>artifactory-releases</name>
      <url>https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-mvn-prod</url>
    </repository>
```    

Then include the dependency
``` xml
     <!-- Non Blocking Task Queue module -->
    <dependency>
      <groupId>io.mapsmessaging</groupId>
      <artifactId>Non_Block_Task_Scheduler</artifactId>
      <version>1.0.0</version>
    </dependency>
```    

##Example
All source can be found [here at github](https://github.com/Maps-Messaging/non_block_task_scheduler/tree/main/src/examples/java/io/mapsmessaging/utilities/threads/tasks/examples)


Here is a very simple example of adding a number to a java native long using 20 threads. Typically, you would either use a lock, an AtomicLong or, even better, a LongAdder, however, for demonstration purposes it works. Saves building a complex structure and confuse the issue at hand


First up define your simple "structure" class

```java
package io.mapsmessaging.utilities.threads.tasks.examples;

public class LongCounter {
  
  private long counter;
  
  
  public LongCounter(){
    counter = 0;
  }
  
  public long add(long addition){
    counter += addition;
    return counter;
  }

  public long sub(long subtraction){
    counter -= subtraction;
    return counter;
  }

  public long getCounter(){
    return counter;
  }
}
```

Straight forward, simple class with a long, no locking involved. 

Now that we have the resource lets create the tasks that can manipulate the resource, in this case, lets create 2

* AdditionTask - Adds a number to the counter
* SubtractionTask - Subtracts a number from the counter

```java
public class AdderTask extends Task {

  public AdderTask(long actionValue, LongCounter counter) {
    super(actionValue, counter);
  }

  @Override
  public Long call() throws Exception {
    return counter.add(actionValue);
  }
}
```

```java
public class SubTask extends Task {

  public SubTask(long actionValue, LongCounter counter) {
    super(actionValue, counter);
  }

  @Override
  public Long call() throws Exception {
    return counter.sub(actionValue);
  }
}
```

Then we build the resource manager that create the tasks for the specific functions required, notice there are 2 functions Add and Subtract, these functions cause a task to be created and queued.

```java
public class ResourceManager {

  private final ConcurrentTaskScheduler<Long> concurrentTaskScheduler;
  private final LongCounter counter;


  public ResourceManager(){
    concurrentTaskScheduler = new SingleConcurrentTaskScheduler<>("UniqueDomainName");
    counter = new LongCounter();
  }

  public FutureTask<Long> add(long value){
    AdderTask adderTask = new AdderTask(value, counter);
    FutureTask<Long> future = new FutureTask<>(adderTask);
    concurrentTaskScheduler.addTask(future);
    return future;
  }

  public FutureTask<Long> sub(long value){
    SubTask subTask = new SubTask(value, counter);
    FutureTask<Long> future = new FutureTask<>(subTask);
    concurrentTaskScheduler.addTask(future);
    return future;
  }

  public long getResourceLong(){
    return counter.getCounter();
  }
}
```

Last up, is the actual "application", in this case it is a simple caller class that randomly adds or subtracts values N times, and a demo class that creates M number of caller classes and manages the life cycle

```java
public class Caller extends Thread {

  private static final int MAX_LOOPS = 100000;

  private final ResourceManager resourceManager;
  private final CountDownLatch countDownLatch;

  public Caller(ResourceManager resourceManager, CountDownLatch countDownLatch){
    this.resourceManager = resourceManager;
    this.countDownLatch = countDownLatch;
  }

  @Override
  public void run() {
    Random random = new Random(System.nanoTime());
    for(int x=0;x<MAX_LOOPS;x++){
      long val = Math.abs(random.nextInt());
      if(random.nextBoolean()){
        waitOnFuture(resourceManager.add(val));
      }
      else{
        waitOnFuture(resourceManager.sub(val));
      }
    }
    countDownLatch.countDown();
  }

  private void waitOnFuture(FutureTask<Long> task){
    // We just wait here, it is up to your application to decide what it should do while it waits
    // for the task to complete
    while(!task.isDone() && !task.isCancelled()){
      LockSupport.parkNanos(1);
    }
  }
}
```

```java
public class DemoRunner {

  private static final int MAX_CALLERS = 100;

  public static void main(String[] args) throws InterruptedException {
    // Create the resource
    ResourceManager resource = new ResourceManager();
    CountDownLatch countDownLatch = new CountDownLatch(MAX_CALLERS);

    // Create the callers
    List<Caller> workerList = new ArrayList<>();
    for(int x=0;x<MAX_CALLERS;x++){
      workerList.add(new Caller(resource, countDownLatch));
    }

    // Now start the callers and wait for completion
    for(Caller caller:workerList){
      caller.start();
    }

    if(!countDownLatch.await(300, TimeUnit.SECONDS)){
      System.err.println("Tasks still running, you may need to tune the numbers to match your machine");
    }
    else{
      System.err.println(resource.getResourceLong());
    }
    System.exit(1);
  }
}

```

What is happening here is that the DemoRunner creates and starts MAX_CALLER threads and execute the Caller.run() function. This function simply loops around MAX_LOOPS adding or subtracting random integer values and wait for the future to complete. Once all Callers are complete the DemoRunner simply displays the resultant value.

Again, do not use this to manage longs, the AtomicLong and LongAdder are far superior for that, however, if you have a complex structure that you want non-blocking access to then this will work. 

