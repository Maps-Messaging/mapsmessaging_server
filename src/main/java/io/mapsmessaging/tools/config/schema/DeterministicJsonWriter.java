package io.mapsmessaging.tools.config.schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DeterministicJsonWriter {

  private static final List<String> TOP_KEY_ORDER = List.of(
      "$schema", "$id", "title", "description", "$ref", "type",
      "oneOf", "discriminator", "properties", "required",
      "items", "additionalProperties", "enum", "examples",
      "default", "minimum", "maximum", "multipleOf", "format", "pattern", "$defs"
  );

  private DeterministicJsonWriter() {
  }

  static String write(Object jsonObject) {
    StringBuilder sb = new StringBuilder(16_384);
    writeValue(sb, jsonObject, 0, null);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private static void writeValue(StringBuilder sb, Object v, int indent, String contextKey) {
    if (v == null) {
      sb.append("null");
      return;
    }

    if (v instanceof String) {
      String s = (String) v;

      if (shouldCoerceStringNumber(contextKey)) {
        BigDecimal coerced = tryParseNumber(s);
        if (coerced != null) {
          sb.append(coerced.toPlainString());
          return;
        }
      }

      writeString(sb, s);
      return;
    }

    if (v instanceof Boolean) {
      sb.append(((Boolean) v) ? "true" : "false");
      return;
    }

    if (v instanceof Integer || v instanceof Long) {
      sb.append(v);
      return;
    }

    if (v instanceof BigDecimal) {
      sb.append(((BigDecimal) v).toPlainString());
      return;
    }

    if (v instanceof Double || v instanceof Float) {
      sb.append(v);
      return;
    }

    if (v instanceof Map) {
      writeObject(sb, (Map<String, Object>) v, indent);
      return;
    }

    if (v instanceof List) {
      writeArray(sb, (List<?>) v, indent, contextKey);
      return;
    }

    throw new IllegalStateException("Unsupported JSON value type: " + v.getClass().getName());
  }

  private static void writeObject(StringBuilder sb, Map<String, Object> map, int indent) {
    sb.append("{");

    List<String> keys = new ArrayList<>(map.keySet());
    keys.sort(DeterministicJsonWriter::compareKeys);

    boolean first = true;
    boolean wroteAny = false;

    for (String key : keys) {
      Object value = map.get(key);
      if (value == null) {
        continue;
      }

      if (!first) {
        sb.append(",");
      }
      first = false;
      wroteAny = true;

      newline(sb, indent + 2);
      writeString(sb, key);
      sb.append(": ");
      writeValue(sb, value, indent + 2, key);
    }

    if (wroteAny) {
      newline(sb, indent);
    }
    sb.append("}");
  }

  private static void writeArray(StringBuilder sb, List<?> list, int indent, String contextKey) {
    sb.append("[");
    boolean first = true;

    for (Object item : list) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      newline(sb, indent + 2);

      if (item instanceof String && "enum".equals(contextKey)) {
        BigDecimal coerced = tryParseNumber((String) item);
        if (coerced != null) {
          sb.append(coerced.toPlainString());
          continue;
        }
      }

      writeValue(sb, item, indent + 2, contextKey);
    }

    if (!list.isEmpty()) {
      newline(sb, indent);
    }
    sb.append("]");
  }

  private static int compareKeys(String a, String b) {
    int ai = TOP_KEY_ORDER.indexOf(a);
    int bi = TOP_KEY_ORDER.indexOf(b);
    if (ai != -1 || bi != -1) {
      if (ai == -1) {
        return 1;
      }
      if (bi == -1) {
        return -1;
      }
      return Integer.compare(ai, bi);
    }
    return a.compareTo(b);
  }

  private static void writeString(StringBuilder sb, String s) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          }
          else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
  }

  private static void newline(StringBuilder sb, int indent) {
    sb.append("\n");
    for (int i = 0; i < indent; i++) {
      sb.append(' ');
    }
  }

  private static boolean shouldCoerceStringNumber(String contextKey) {
    if (contextKey == null) {
      return false;
    }
    return "default".equals(contextKey)
        || "minimum".equals(contextKey)
        || "maximum".equals(contextKey)
        || "multipleOf".equals(contextKey)
        || "enum".equals(contextKey);
  }

  private static BigDecimal tryParseNumber(String s) {
    if (s == null) {
      return null;
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      return null;
    }

    // JSON numbers do not allow leading '+'
    if (trimmed.charAt(0) == '+') {
      return null;
    }

    // Quick reject: must contain only plausible numeric characters
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (!(c == '-' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9'))) {
        return null;
      }
    }

    try {
      BigDecimal bd = new BigDecimal(trimmed);

      // Preserve integers as integers: avoid "9600.0" if input was "9600"
      if (isIntegerLexeme(trimmed)) {
        return bd.stripTrailingZeros();
      }

      return bd.stripTrailingZeros();
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean isIntegerLexeme(String s) {
    // Integer if it has no '.' and no exponent
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '.' || c == 'e' || c == 'E') {
        return false;
      }
    }
    // Must contain at least one digit
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') {
        return true;
      }
    }
    return false;
  }
}
