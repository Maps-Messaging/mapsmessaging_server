package org.maps.messaging.engine.tasks;


import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.ThreadContext;

public abstract class EngineTask implements Callable<Response> {

  private final Map<String, String> logContext;

  public EngineTask() {
    logContext = ThreadContext.getContext();
  }

  // The exception is derived from the Callable interface and we just extent it here
  @java.lang.SuppressWarnings("squid:S00112")
  public abstract Response taskCall() throws Exception;

  @Override
  public Response call() throws Exception {
    ThreadContext.putAll(logContext);
    try{
      return taskCall();
    }
    catch(Throwable th){
      throw new Exception(th);
    }
    finally{
      ThreadContext.clearAll();
    }
  }
}
