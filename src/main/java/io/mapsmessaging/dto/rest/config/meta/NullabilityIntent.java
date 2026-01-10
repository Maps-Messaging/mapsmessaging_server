package io.mapsmessaging.dto.rest.config.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NullabilityIntent {

  io.mapsmessaging.dto.rest.config.meta.Nullability value();
}
