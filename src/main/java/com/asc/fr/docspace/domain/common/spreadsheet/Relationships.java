package com.asc.fr.docspace.domain.common.spreadsheet;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class Relationships {
  private final XMLInputFactory factory;
  private final byte[] xml;

  Relationships(XMLInputFactory factory, byte[] xml) {
    this.factory = factory;
    this.xml = xml;
  }

  Map<String, String> toParts() throws XMLStreamException {
    Map<String, String> result = new LinkedHashMap<>();
    XMLStreamReader reader =
        this.factory.createXMLStreamReader(new ByteArrayInputStream(this.xml), "UTF-8");
    try {
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT
            && "Relationship".equals(reader.getLocalName())) {
          if (!SpreadsheetManipulator.attribute(reader, "Type")
              .toLowerCase(Locale.ROOT)
              .endsWith("worksheet")) continue;

          String id = SpreadsheetManipulator.attribute(reader, "Id");
          String target =
              SpreadsheetManipulator.normalizeTarget(
                  SpreadsheetManipulator.attribute(reader, "Target"));

          if (!id.isEmpty() && !target.isEmpty()) result.put(id, target);
        }
      }
    } finally {
      reader.close();
    }

    return result;
  }
}
