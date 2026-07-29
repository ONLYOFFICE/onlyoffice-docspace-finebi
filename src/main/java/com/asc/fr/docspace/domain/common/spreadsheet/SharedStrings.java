package com.asc.fr.docspace.domain.common.spreadsheet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SharedStrings {
    private final XMLInputFactory factory;
    private final byte[] xml;

    SharedStrings(XMLInputFactory factory, byte[] xml) {
        this.factory = factory;
        this.xml = xml;
    }

    List<String> toItems() throws XMLStreamException {
        if (this.xml == null || this.xml.length == 0)
            return Collections.emptyList();
        List<String> items = new ArrayList<>();
        XMLStreamReader reader = this.factory.createXMLStreamReader(new ByteArrayInputStream(this.xml), "UTF-8");
        try {
            StringBuilder text = new StringBuilder();
            boolean inItem = false;
            boolean inText = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("si".equals(local)) {
                        inItem = true;
                        text.setLength(0);
                    } else if (inItem && "t".equals(local)) {
                        inText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && inText) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("t".equals(local)) {
                        inText = false;
                    } else if ("si".equals(local)) {
                        items.add(text.toString());
                        inItem = false;
                    }
                }
            }
        } finally {
            reader.close();
        }

        return items;
    }
}
