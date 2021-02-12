/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.logging;

import org.apache.logging.log4j.ThreadContext;

/**
 * Provides a consistent logging API that hides the logging implementation so it can be changed in the future
 *
 */
public class Logger {

  private static final String CATEGORY = "category";

  private final org.apache.logging.log4j.Logger localLogger;

  Logger(String loggerName) {
    localLogger = org.apache.logging.log4j.LogManager.getLogger(loggerName);
  }

  /**
   * This function logs the predefined message with the attached args
   *
   * @param logMessages The predefined log message
   * @param args Variable list of arguments that will be added to the log message
   */
  public void log(LogMessages logMessages, Object... args) {
    if (logAt(logMessages)) {
      if (logMessages.parameters != args.length) {
        localLogger.warn("Invalid number of arguments for the log messages, expected {} received {}", logMessages.parameters, args.length);
      }

      ThreadContext.put(CATEGORY, logMessages.category.description);
      switch (logMessages.level) {
        case TRACE:
          localLogger.trace(logMessages.message, args);
          break;

        case DEBUG:
          localLogger.debug(logMessages.message, args);
          break;

        case INFO:
          localLogger.info(logMessages.message, args);
          break;

        case WARN:
          localLogger.warn(logMessages.message, args);
          break;

        case ERROR:
          localLogger.error(logMessages.message, args);
          break;

        default:
      }
      ThreadContext.remove(CATEGORY);
    }
  }

/**
 * This function logs the predefined message with the attached args and the exception
 *
 * @param logMessages The predefined log message
 * @param throwable An exception that needs to be logged
 * @param args A list of variable arguments to be logged
 */
  public void log(LogMessages logMessages, Throwable throwable, Object... args) {
    if (logMessages.parameters != args.length) {
      localLogger.warn("Invalid number of arguments for the log messages, expected {} received {}",
          logMessages.parameters,
          args.length);
    }

    ThreadContext.put(CATEGORY, logMessages.category.description);
    switch (logMessages.level) {
      case TRACE:
        if (localLogger.isTraceEnabled()) {
          localLogger
              .atTrace()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessages.message, args);
        }
        break;

      case DEBUG:
        if (localLogger.isDebugEnabled()) {
          localLogger
              .atDebug()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessages.message, args);
        }
        break;

      case INFO:
        if (localLogger.isInfoEnabled()) {
          localLogger
              .atInfo()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessages.message, args);
        }
        break;

      case WARN:
        if (localLogger.isWarnEnabled()) {
          localLogger
              .atDebug()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessages.message, args);
        }
        break;

      case ERROR:
        if (localLogger.isErrorEnabled()) {
          localLogger
              .atError()
              .withThrowable(throwable)
              .withLocation()
              .log(logMessages.message, args);
        }
        break;

      default:
    }
    ThreadContext.remove(CATEGORY);
  }

  private boolean logAt(LogMessages logMessages) {
    switch (logMessages.level) {
      case TRACE:
        return localLogger.isTraceEnabled();

      case DEBUG:
        return localLogger.isDebugEnabled();

      case INFO:
        return localLogger.isInfoEnabled();

      case WARN:
        return localLogger.isWarnEnabled();
      default:
        return false;
    }
  }

  public String getName() {
    return localLogger.getName();
  }

  public boolean isTraceEnabled() {
    return localLogger.isTraceEnabled();
  }

  public boolean isDebugEnabled() {
    return localLogger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return localLogger.isInfoEnabled();
  }

  public boolean isWarnEnabled() {
    return localLogger.isWarnEnabled();
  }

  public boolean isErrorEnabled() {
    return localLogger.isErrorEnabled();
  }
}
