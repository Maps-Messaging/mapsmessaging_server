package io.mapsmessaging.api;

import io.mapsmessaging.api.message.Message;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class MessageEvent {

  private final @Getter String destinationName;
  private final @Getter SubscribedEventManager subscription;
  private final @Getter Message message;
  private final @Getter Runnable completionTask;

  public MessageEvent(
  @NonNull @NotNull String destinationName,
  @NonNull @NotNull SubscribedEventManager subscription,
  @NonNull @NotNull Message message,
  @NonNull @NotNull Runnable completionTask){
    this.destinationName = destinationName;
    this.subscription = subscription;
    this.message = message;
    this.completionTask = completionTask;
  }
}
