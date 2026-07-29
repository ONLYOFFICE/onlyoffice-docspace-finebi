package com.asc.fr.docspace.domain.common.spreadsheet;

import lombok.Getter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashSet;
import java.util.Set;

class Worksheet {
    private final XMLInputFactory factory;
    private final byte[] xml;

    @Getter
     static class WorksheetScan {
        private final Boolean hasCell;
        private final Set<Integer> sharedRefs;

        WorksheetScan(boolean hasCell, Set<Integer> sharedRefs) {
            this.hasCell = hasCell;
            this.sharedRefs = sharedRefs;
        }
    }

    Worksheet(XMLInputFactory factory, byte[] xml) {
        this.factory = factory;
        this.xml = xml;
    }

    WorksheetScan toWorksheetScan() throws XMLStreamException {
        boolean hasCell = false;
        Set<Integer> sharedRefs = new LinkedHashSet<>();
        XMLStreamReader reader = this.factory.createXMLStreamReader(new ByteArrayInputStream(this.xml), "UTF-8");
        try {
            boolean inSharedCell = false; // current <c> has t="s"
            boolean inValue = false; // inside that cell's <v>
            StringBuilder value = new StringBuilder();

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("c".equals(local)) {
                        hasCell = true;
                        inSharedCell = "s".equals(SpreadsheetManipulator.attribute(reader, "t"));
                    } else if (inSharedCell && "v".equals(local)) {
                        inValue = true;
                        value.setLength(0);
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && inValue) {
                    value.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("v".equals(local) && inValue) {
                        inValue = false;
                        try {
                            sharedRefs.add(Integer.parseInt(value.toString().trim()));
                        } catch (NumberFormatException ignored) {
                            // Malformed <v> — ignore this index, keep scanning.
                        }
                    } else if ("c".equals(local)) {
                        inSharedCell = false;
                    }
                }
            }
        } finally {
            reader.close();
        }

        return new WorksheetScan(hasCell, sharedRefs);
    }
}
