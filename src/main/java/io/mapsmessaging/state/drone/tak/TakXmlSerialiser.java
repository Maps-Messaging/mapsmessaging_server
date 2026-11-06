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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.state.drone.tak.model.*;

public class TakXmlSerialiser {

  public String toXml(TakEvent event) {
    StringBuilder stringBuilder = new StringBuilder(2048);

    stringBuilder.append("<event");
    appendAttribute(stringBuilder, "version", event.getVersion());
    appendAttribute(stringBuilder, "uid", event.getUid());
    appendAttribute(stringBuilder, "type", event.getType());
    appendAttribute(stringBuilder, "how", event.getHow());
    appendAttribute(stringBuilder, "time", event.getTime());
    appendAttribute(stringBuilder, "start", event.getStart());
    appendAttribute(stringBuilder, "stale", event.getStale());
    stringBuilder.append(">");

    appendPoint(stringBuilder, event.getPoint());
    appendDetail(stringBuilder, event.getDetail());

    stringBuilder.append("</event>");
    return stringBuilder.toString();
  }

  private void appendPoint(StringBuilder stringBuilder, TakPoint point) {
    if (point == null) {
      return;
    }

    stringBuilder.append("<point");
    appendAttribute(stringBuilder, "lat", point.getLat());
    appendAttribute(stringBuilder, "lon", point.getLon());
    appendAttribute(stringBuilder, "hae", point.getHae());
    appendAttribute(stringBuilder, "ce", point.getCe());
    appendAttribute(stringBuilder, "le", point.getLe());
    stringBuilder.append("/>");
  }

  private void appendDetail(StringBuilder stringBuilder, TakDetail detail) {
    if (detail == null) {
      return;
    }

    stringBuilder.append("<detail>");
    appendContact(stringBuilder, detail.getContact());
    appendTrack(stringBuilder, detail.getTrack());
    appendStatus(stringBuilder, detail.getStatus());
    appendRemarks(stringBuilder, detail.getRemarks());
    appendPrecisionLocation(stringBuilder, detail.getPrecisionLocation());
    appendTakPlatform(stringBuilder, detail.getTakv());
    appendLinkState(stringBuilder, detail.getMapsLink());
    appendArchive(stringBuilder, detail.getArchive());
    appendColor(stringBuilder, detail.getColorArgb());
    appendUsericon(stringBuilder, detail.getUsericonIconsetPath());

    if (detail.getVideos() != null) {
      for (TakVideo video : detail.getVideos()) {
        appendVideo(stringBuilder, video);
      }
    }

    if (detail.getLinks() != null) {
      for (TakLink link : detail.getLinks()) {
        appendLink(stringBuilder, link);
      }
    }

    stringBuilder.append("</detail>");
  }

  /**
   * The {@code __video} element: the client plays the url, and the connection entry beside it is
   * the same stream taken apart, which is what older clients read. The timeouts and flags are the
   * values ATAK writes for a stream it has been given by hand.
   */
  private void appendVideo(StringBuilder stringBuilder, TakVideo video) {
    if (video == null || video.getUrl() == null || video.getUrl().isBlank()) {
      return;
    }

    stringBuilder.append("<__video");
    appendAttribute(stringBuilder, "uid", video.getUid());
    appendAttribute(stringBuilder, "url", video.getUrl());
    stringBuilder.append(">");

    stringBuilder.append("<ConnectionEntry");
    appendAttribute(stringBuilder, "uid", video.getUid());
    appendAttribute(stringBuilder, "alias", video.getAlias());
    appendAttribute(stringBuilder, "address", video.getAddress());
    appendAttribute(stringBuilder, "port", video.getPort());
    appendAttribute(stringBuilder, "path", video.getPath());
    appendAttribute(stringBuilder, "protocol", video.getProtocol());
    appendAttribute(stringBuilder, "networkTimeout", 12000);
    appendAttribute(stringBuilder, "bufferTime", -1);
    appendAttribute(stringBuilder, "roverPort", -1);
    appendAttribute(stringBuilder, "rtspReliable", "rtsp".equals(video.getProtocol()) ? 1 : 0);
    appendAttribute(stringBuilder, "ignoreEmbeddedKLV", "false");
    stringBuilder.append("/>");

    stringBuilder.append("</__video>");
  }

  private void appendContact(StringBuilder stringBuilder, TakContact contact) {
    if (contact == null) {
      return;
    }

    stringBuilder.append("<contact");
    appendAttribute(stringBuilder, "callsign", contact.getCallsign());
    stringBuilder.append("/>");
  }

  private void appendTrack(StringBuilder stringBuilder, TakTrack track) {
    if (track == null) {
      return;
    }

    stringBuilder.append("<track");
    appendAttribute(stringBuilder, "speed", track.getSpeed());
    appendAttribute(stringBuilder, "course", track.getCourse());
    stringBuilder.append("/>");
  }

  private void appendStatus(StringBuilder stringBuilder, TakStatus status) {
    if (status == null) {
      return;
    }

    stringBuilder.append("<status");
    appendAttribute(stringBuilder, "lifecycle", status.getLifecycle());
    appendAttribute(stringBuilder, "reason", status.getReason());
    appendAttribute(stringBuilder, "readiness", status.getReadiness());
    stringBuilder.append("/>");
  }

  private void appendRemarks(StringBuilder stringBuilder, String remarks) {
    if (remarks == null || remarks.isBlank()) {
      return;
    }

    stringBuilder.append("<remarks>");
    stringBuilder.append(escapeXml(remarks));
    stringBuilder.append("</remarks>");
  }

  private void appendPrecisionLocation(StringBuilder stringBuilder, TakPrecisionLocation precisionLocation) {
    if (precisionLocation == null) {
      return;
    }

    stringBuilder.append("<precisionlocation");
    appendAttribute(stringBuilder, "altsrc", precisionLocation.getAltsrc());
    appendAttribute(stringBuilder, "geopointsrc", precisionLocation.getGeopointsrc());
    stringBuilder.append("/>");
  }

  private void appendArchive(StringBuilder stringBuilder, Boolean archive) {
    if (archive == null || !archive) {
      return;
    }
    stringBuilder.append("<archive/>");
  }

  private void appendColor(StringBuilder stringBuilder, Integer colorArgb) {
    if (colorArgb == null) {
      return;
    }
    stringBuilder.append("<color");
    appendAttribute(stringBuilder, "argb", colorArgb);
    stringBuilder.append("/>");
  }

  private void appendUsericon(StringBuilder stringBuilder, String usericonIconsetPath) {
    if (usericonIconsetPath == null || usericonIconsetPath.isBlank()) {
      return;
    }
    stringBuilder.append("<usericon");
    appendAttribute(stringBuilder, "iconsetpath", usericonIconsetPath);
    stringBuilder.append("/>");
  }

  private void appendTakPlatform(StringBuilder stringBuilder, TakPlatform takPlatform) {
    if (takPlatform == null) {
      return;
    }

    stringBuilder.append("<takv");
    appendAttribute(stringBuilder, "device", takPlatform.getDevice());
    appendAttribute(stringBuilder, "platform", takPlatform.getPlatform());
    appendAttribute(stringBuilder, "os", takPlatform.getOs());
    appendAttribute(stringBuilder, "version", takPlatform.getVersion());
    stringBuilder.append("/>");
  }

  private void appendLinkState(StringBuilder stringBuilder, TakLinkState linkState) {
    if (linkState == null) {
      return;
    }

    stringBuilder.append("<maps-link");
    appendAttribute(stringBuilder, "state", linkState.getState());
    appendAttribute(stringBuilder, "connected", linkState.getConnected());
    appendAttribute(stringBuilder, "rssiDbm", linkState.getRssiDbm());
    appendAttribute(stringBuilder, "snrDb", linkState.getSnrDb());
    appendAttribute(stringBuilder, "latencyMs", linkState.getLatencyMs());
    appendAttribute(stringBuilder, "rxErrorRate", linkState.getRxErrorRate());
    appendAttribute(stringBuilder, "txErrorRate", linkState.getTxErrorRate());
    stringBuilder.append("/>");
  }

  private void appendLink(StringBuilder stringBuilder, TakLink link) {
    if (link == null) {
      return;
    }

    stringBuilder.append("<link");
    appendAttribute(stringBuilder, "uid", link.getUid());
    appendAttribute(stringBuilder, "relation", link.getRelation());
    stringBuilder.append("/>");
  }

  private void appendAttribute(StringBuilder stringBuilder, String name, Object value) {
    if (value == null) {
      return;
    }

    String stringValue = String.valueOf(value);
    if (stringValue.isBlank()) {
      return;
    }

    stringBuilder.append(' ');
    stringBuilder.append(name);
    stringBuilder.append("=\"");
    stringBuilder.append(escapeXml(stringValue));
    stringBuilder.append('"');
  }

  private String escapeXml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
