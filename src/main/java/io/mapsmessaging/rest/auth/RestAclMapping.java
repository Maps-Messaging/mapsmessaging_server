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

package io.mapsmessaging.rest.auth;

import io.mapsmessaging.security.access.AccessControlMapping;

public class RestAclMapping implements AccessControlMapping {

  public static final String CREATE = "create";
  public static final String READ = "read";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  public static final long CREATE_VALUE = 1L;
  public static final long READ_VALUE = 2L;
  public static final long UPDATE_VALUE = 4L;
  public static final long DELETE_VALUE = 8L;

  @Override
  public Long getAccessValue(String accessControl) {
    if(accessControl == null){
      return 0L;
    }
    switch (accessControl.toLowerCase()) {
      case READ:
        return READ_VALUE;
      case UPDATE:
        return UPDATE_VALUE;
      case CREATE:
        return CREATE_VALUE;
      case DELETE:
        return DELETE_VALUE;
      default:
        return null;
    }
  }

  public static String getAllAccessControls(long val) {
    StringBuilder sb = new StringBuilder();
    if((val & CREATE_VALUE) != 0){
      sb.append(CREATE).append(",");
    }
    if((val & READ_VALUE) != 0){
      sb.append(READ).append(",");
    }
    if((val & UPDATE_VALUE) != 0){
      sb.append(UPDATE).append(",");
    }
    if((val & DELETE_VALUE) != 0){
      sb.append(DELETE).append(",");
    }
    return sb.toString();
  }

  @Override
  public String getAccessName(long value) {
    if (value == READ_VALUE) {
      return READ;
    } else if (value == UPDATE_VALUE) {
      return UPDATE;
    } else if (value == CREATE_VALUE) {
      return CREATE;
    } else if (value == DELETE_VALUE) {
      return DELETE;
    } else {
      return "";
    }
  }

}