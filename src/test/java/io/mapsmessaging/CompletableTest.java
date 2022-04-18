package io.mapsmessaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class CompletableTest {

  @Test
  void check() throws ExecutionException, InterruptedException {
    Executor executor = Executors.newFixedThreadPool(10);
    CompletableFuture<File> start = CompletableFuture.supplyAsync(() -> {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      System.err.println(Thread.currentThread().getName());
      return new File("test.txt");
    }, executor).exceptionally(ex -> {
      System.out.println("Oops1! We have an exception - " + ex.getMessage());
      return null;
    });

    CompletableFuture<FileOutputStream> result = start.thenApply(name -> {
      System.err.println(Thread.currentThread().getName());
      throw new IllegalStateException(new Exception("Testing Exception"));
    });


    CompletableFuture<Void> result2 = result.thenApply(name -> {
      System.err.println(Thread.currentThread().getName());
      try {
        name.write("Hello there".getBytes());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return null;
    });

    TimeUnit.SECONDS.sleep(10);

    System.err.println(Thread.currentThread().getName());
    System.err.println("Exception:"+result2.isCompletedExceptionally());

    result2.handle((res, ex) -> {
      if(ex != null) {
        System.out.println("Oops! We have an exception - " + ex.getMessage());
        return "Unknown!";
      }
      return res;
    });

    System.err.println(result2.get());
    try {
      result.get().close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
