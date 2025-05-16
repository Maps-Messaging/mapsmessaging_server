/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.rest;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

import static io.mapsmessaging.logging.ServerLogMessages.REST_API_SUCCESSFUL_REQUEST;

@Provider
@Priority(Priorities.USER - 1)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    // needs to be implemented, but do not need to do anything here
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    int status = responseContext.getStatus();
    String method = requestContext.getMethod();
    String path = requestContext.getUriInfo().getPath();
    logger.log(REST_API_SUCCESSFUL_REQUEST, method, path, status);
  }
}
