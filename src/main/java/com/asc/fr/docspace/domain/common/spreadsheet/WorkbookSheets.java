package com.asc.fr.docspace.domain.common.spreadsheet;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

class WorkbookSheets {
  private final XMLInputFactory factory;
  private final byte[] xml;

  @Getter
  static class SheetRef {
    private final String name;
    private final int sheetId;
    private final String rid;

    SheetRef(String name, int sheetId, String rid) {
      this.name = name;
      this.sheetId = sheetId;
      this.rid = rid;
    }
  }

  WorkbookSheets(XMLInputFactory factory, byte[] xml) {
    this.factory = factory;
    this.xml = xml;
  }

  List<SheetRef> toSheets() throws XMLStreamException {
    List<SheetRef> refs = new ArrayList<>();
    XMLStreamReader reader =
        this.factory.createXMLStreamReader(new ByteArrayInputStream(this.xml), "UTF-8");
    try {
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT
            && "sheet".equals(reader.getLocalName())) {
          String name = SpreadsheetManipulator.attribute(reader, "name");
          // r:id is in the relationships namespace; StAX exposes the local name as "id".
          String rid = SpreadsheetManipulator.attribute(reader, "id");
          if (!name.isEmpty() && !rid.isEmpty())
            refs.add(
                new SheetRef(
                    name,
                    SpreadsheetManipulator.parseInt(
                        SpreadsheetManipulator.attribute(reader, "sheetId")),
                    rid));
        }
      }
    } finally {
      reader.close();
    }

    return refs;
  }
}
