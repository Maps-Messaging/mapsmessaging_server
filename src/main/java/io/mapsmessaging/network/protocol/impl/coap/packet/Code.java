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

package io.mapsmessaging.network.protocol.impl.coap.packet;

import lombok.Getter;

import static io.mapsmessaging.network.protocol.impl.coap.packet.Clazz.*;

/*
            +------+------------------------------+-----------+
            | Code | Description                  | Reference |
            +------+------------------------------+-----------+
            | 2.01 | Created                      | [RFC7252] |
            | 2.02 | Deleted                      | [RFC7252] |
            | 2.03 | Valid                        | [RFC7252] |
            | 2.04 | Changed                      | [RFC7252] |
            | 2.05 | Content                      | [RFC7252] |
            | 4.00 | Bad Request                  | [RFC7252] |
            | 4.01 | Unauthorized                 | [RFC7252] |
            | 4.02 | Bad Option                   | [RFC7252] |
            | 4.03 | Forbidden                    | [RFC7252] |
            | 4.04 | Not Found                    | [RFC7252] |
            | 4.05 | Method Not Allowed           | [RFC7252] |
            | 4.06 | Not Acceptable               | [RFC7252] |
            | 4.12 | Precondition Failed          | [RFC7252] |
            | 4.13 | Request Entity Too Large     | [RFC7252] |
            | 4.15 | Unsupported Content-Format   | [RFC7252] |
            | 5.00 | Internal Server Error        | [RFC7252] |
            | 5.01 | Not Implemented              | [RFC7252] |
            | 5.02 | Bad Gateway                  | [RFC7252] |
            | 5.03 | Service Unavailable          | [RFC7252] |
            | 5.04 | Gateway Timeout              | [RFC7252] |
            | 5.05 | Proxying Not Supported       | [RFC7252] |
            +------+------------------------------+-----------+
 */
public enum Code {

  EMPTY(REQUEST, 0, "Empty"),

  CREATED(SUCCESS, 1, "Created"),
  DELETED(SUCCESS, 2, "Deleted"),
  VALID(SUCCESS, 3, "Valid"),
  CHANGED(SUCCESS, 4, "Changed"),
  CONTENT(SUCCESS, 5, "Content"),
  CONTINUE(SUCCESS, 31, "Continue"),

  BAD_REQUEST(ERROR, 0, "Bad Request"),
  UNAUTHORISED(ERROR, 1, "Unauthorized"),
  BAD_OPTION(ERROR, 2, "Bad Option"),
  FORBIDDEN(ERROR, 3, "Forbidden"),
  NOT_FOUND(ERROR, 4, "Not Found"),
  METHOD_NOT_ALLOWED(ERROR, 5, "Method Not Allowed"),
  NOT_ACCEPTABLE(ERROR, 6, "Not Acceptable"),
  PRECONDITION_FAILED(ERROR, 12, "Precondition Failed"),
  REQUEST_ENTITY_TOO_LARGE(ERROR, 13, "Request Entity Too Large"),
  UNSUPPORTED_CONTENT_FORMAT(ERROR, 15, "Unsupported Content-Format"),

  INTERNAL_SERVER_ERROR(SERVER_ERROR, 0, "Internal Server Error"),
  NOT_IMPLEMENTED(SERVER_ERROR, 1, "Not Implemented"),
  BAD_GATEWAY(SERVER_ERROR, 2, "Bad Gateway"),
  SERVICE_UNAVAILABLE(SERVER_ERROR, 3, "Service Unavailable"),
  GATEWAY_TIMEOUT(SERVER_ERROR, 4, "Gateway Timeout"),
  PROXYING_NOT_SUPPORTED(SERVER_ERROR, 5, "Proxying Not Supported"),

  ;

  @Getter
  private final Clazz clazz;

  @Getter
  private final int minor;

  @Getter
  private final String description;

  Code(Clazz clazz, int minor, String description){
    this.clazz = clazz;
    this.minor = minor;
    this.description = description;
  }

  public byte getValue() {
    int val = clazz.getValue() << 5;
    val = val | minor & 0b11111;
    return (byte)(val & 0xff);
  }

  public static Code valueOf(byte val){
    Clazz clazz1 = Clazz.valueOf(val >> 5);
    if(clazz1 != null) {
      switch (clazz1) {
        case SUCCESS:
          return getSuccess(val & 0b11111);
        case ERROR:
          return getError(val & 0b11111);
        case SERVER_ERROR:
          return getServerError(val & 0b11111);

        default:
          return INTERNAL_SERVER_ERROR;

      }
    }
    return INTERNAL_SERVER_ERROR;
  }

  private static Code getSuccess(int val){
    switch (val){
      case 1:
        return CREATED;
      case 2:
        return DELETED;
      case 3:
        return VALID;
      case 4:
        return CHANGED;
      case 5:
        return CONTENT;

      default:
        return CREATED;
    }
  }


  private static Code getError(int val) {
    switch (val) {
      case 0:
        return BAD_REQUEST;
      case 1:
        return UNAUTHORISED;
      case 2:
        return BAD_OPTION;
      case 3:
        return FORBIDDEN;
      case 4:
        return NOT_FOUND;
      case 5:
        return METHOD_NOT_ALLOWED;
      case 6:
        return NOT_ACCEPTABLE;

      case 12:
        return PRECONDITION_FAILED;
      case 13:
        return REQUEST_ENTITY_TOO_LARGE;
      case 15:
        return UNSUPPORTED_CONTENT_FORMAT;

      default:
        return BAD_REQUEST;

    }
  }

  private static Code getServerError(int val) {
    switch (val) {
      case 0:
        return INTERNAL_SERVER_ERROR;
      case 1:
        return NOT_IMPLEMENTED;
      case 2:
        return BAD_GATEWAY;
      case 3:
        return SERVICE_UNAVAILABLE;
      case 4:
        return GATEWAY_TIMEOUT;
      case 5:
        return PROXYING_NOT_SUPPORTED;

      default:
        return INTERNAL_SERVER_ERROR;
    }
  }

}
