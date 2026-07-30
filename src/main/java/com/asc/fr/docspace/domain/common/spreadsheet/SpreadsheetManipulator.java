package com.asc.fr.docspace.domain.common.spreadsheet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamReader;

class SpreadsheetManipulator {
  private static String toHex(byte[] digest) {
    StringBuilder hex = new StringBuilder(digest.length * 2);
    for (byte b : digest) hex.append(String.format("%02x", b));

    return hex.toString();
  }

  static String fingerprint(byte[] worksheet, Set<Integer> sharedRefs, List<String> sharedStrings) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(worksheet);
      for (Integer index : sharedRefs) {
        digest.update((byte) 0);
        if (index != null && index >= 0 && index < sharedStrings.size())
          digest.update(sharedStrings.get(index).getBytes(StandardCharsets.UTF_8));
      }

      return toHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  static String normalize(String name) {
    String path = name == null ? "" : name.replace('\\', '/');
    return path.startsWith("/") ? path.substring(1) : path;
  }

  static String normalizeTarget(String target) {
    String path = normalize(target);
    if (path.isEmpty()) return path;
    return path.startsWith("xl/") ? path : "xl/" + path;
  }

  static String attribute(XMLStreamReader reader, String localName) {
    for (int i = 0; i < reader.getAttributeCount(); i++)
      if (localName.equals(reader.getAttributeLocalName(i))) return reader.getAttributeValue(i);
    return "";
  }

  static int parseInt(String value) {
    try {
      return value.isEmpty() ? 0 : Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
