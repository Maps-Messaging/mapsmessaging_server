/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.hardware.trigger;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_SCHEDULE_TASK_EXCCEDED_TIME;
import static io.mapsmessaging.logging.ServerLogMessages.DEVICE_SCHEDULE_TASK_FAILED;

public class PeriodicRunner implements Runnable {


  private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Getter
  private final Runnable task;
  private final Runnable wrapper;

  @Getter
  @Setter
  private volatile Future<?> submittedFuture;

  public PeriodicRunner(Runnable task) {
    this.task = task;
    wrapper = new Wrapper();
  }

  public void close(){
    Future<?> future = submittedFuture;
    if(future != null && !future.isDone()){
      future.cancel(true);
    }
  }

  public void queueTask(){
    if(running.compareAndSet(false, true)){
      try {
        submittedFuture = executorService.submit(wrapper);
      } catch (Exception e) {
        submittedFuture = null;
        running.set(false);
      }
    }
  }

  public void run(){
    queueTask();
  }

  private final class Wrapper implements Runnable {

    public void run() {
      long startTime = System.nanoTime();
      try {
        task.run();
      }
      catch(Throwable throwable) {
        logger.log(DEVICE_SCHEDULE_TASK_FAILED, throwable);
      } finally {
        running.set(false);
        startTime = (System.nanoTime() - startTime)/ 1_000_000;
        if(startTime > 1000){
          logger.log(DEVICE_SCHEDULE_TASK_EXCCEDED_TIME, startTime);
        }
      }
    }
  }
}
