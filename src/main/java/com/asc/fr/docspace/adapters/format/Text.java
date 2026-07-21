package com.asc.fr.docspace.adapters.format;

public final class Text {
  private Text() {}

  public static String abbreviate(String value, int max) {
    if (value == null) return "";
    return value.length() > max ? value.substring(0, max) + "…" : value;
  }
}
