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

    if (detail.getLinks() != null) {
      for (TakLink link : detail.getLinks()) {
        appendLink(stringBuilder, link);
      }
    }

    stringBuilder.append("</detail>");
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