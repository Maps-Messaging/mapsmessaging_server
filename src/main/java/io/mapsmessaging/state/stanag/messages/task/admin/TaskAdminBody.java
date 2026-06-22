package io.mapsmessaging.state.stanag.messages.task.admin;

import io.mapsmessaging.state.config.capability.Authorities;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TaskAdminBody {

  private final UUID identifier;

  private final UUID node;

  private final TaskAdminActionEnum action;

  private final Authorities authority;
}