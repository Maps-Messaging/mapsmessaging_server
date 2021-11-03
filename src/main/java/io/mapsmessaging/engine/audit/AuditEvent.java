package io.mapsmessaging.engine.audit;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum AuditEvent implements LogMessage {

  // User state changes
  SUCCESSFUL_LOGIN("{} successfully logged in using {}", AUDIT_CATEGORY.AUTHENTICATION),
  SUCCESSFUL_LOGOUT("{} successfully logged off", AUDIT_CATEGORY.AUTHENTICATION),


  // Destination
  DESTINATION_CREATED("Destination {} created by {} ", AUDIT_CATEGORY.CREATION),
  DESTINATION_DELETED("Destination {} deleted by {} ", AUDIT_CATEGORY.DELETION),

  ;


  private final @Getter String message;
  private final @Getter Category category;
  private final @Getter int parameterCount;

  AuditEvent(String message, Category category){
    this.message = message;
    this.category = category;
    int count = 0;
    int location = message.indexOf("{}");
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    parameterCount = count;
  }

  @Override
  public LEVEL getLevel() {
    return LEVEL.AUDIT;
  }


  public enum AUDIT_CATEGORY implements Category {
    AUTHORISATION("Authorisation"),
    AUTHENTICATION("Authentication"),
    CREATION("Creation"),
    DELETION("Deletion"),
    MODIFICATION("Modification");

    private final @Getter String description;

    public String getDivision(){
      return "Messaging";
    }

    AUDIT_CATEGORY(String description) {
      this.description = description;
    }
  }
}
