package com.asc.fr.docspace.domain.common.spreadsheet;

import com.asc.fr.docspace.domain.common.Sheet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

public final class Spreadsheet {
  private static final XMLInputFactory XML = secureInputFactory();
  private static final String SHEETS_MANIFEST = "xl/workbook.xml";
  private static final String SPREADSHEET_RELATIONSHIPS_MANIFEST = "xl/_rels/workbook.xml.rels";
  private static final String SPREADSHEET_STRINGS = "xl/sharedStrings.xml";
  private static final String SHEET_MANIFEST = "xl/worksheets/";
  private static final String EXTENSION = ".xml";

  private final List<Sheet> sheets;

  private static void setProperty(XMLInputFactory factory, String name, boolean value) {
    try {
      factory.setProperty(name, value);
    } catch (IllegalArgumentException ignored) {
    }
  }

  /** StAX factory hardened against XXE: no DTDs, no external entities. */
  private static XMLInputFactory secureInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    setProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
    setProperty(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    setProperty(factory, XMLInputFactory.IS_COALESCING, true);
    return factory;
  }

  private static byte[] readBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;

    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);

    return out.toByteArray();
  }

  private static List<Sheet> read(byte[] content) throws IOException, XMLStreamException {
    byte[] manifestXML = null;
    byte[] relationshipsXML = null;
    byte[] sharedStringsXML = null;

    Map<String, byte[]> worksheetBytes = new LinkedHashMap<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
      for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        String path = SpreadsheetManipulator.normalize(entry.getName());
        if (SHEETS_MANIFEST.equals(path)) {
          manifestXML = readBytes(zip);
        } else if (SPREADSHEET_RELATIONSHIPS_MANIFEST.equals(path)) {
          relationshipsXML = readBytes(zip);
        } else if (SPREADSHEET_STRINGS.equals(path)) {
          sharedStringsXML = readBytes(zip);
        } else if (path.startsWith(SHEET_MANIFEST) && path.endsWith(EXTENSION)) {
          worksheetBytes.put(path, readBytes(zip));
        }
      }
    }

    if (manifestXML == null || relationshipsXML == null) return Collections.emptyList();

    SharedStrings sharedStrings = new SharedStrings(XML, sharedStringsXML);
    List<String> sharedStringItems = sharedStrings.toItems();

    Relationships relationships = new Relationships(XML, relationshipsXML);
    Map<String, String> parts = relationships.toParts();

    WorkbookSheets workbookSheets = new WorkbookSheets(XML, manifestXML);
    List<WorkbookSheets.SheetRef> sheetRefs = workbookSheets.toSheets();

    List<Sheet> result = new ArrayList<>();
    for (WorkbookSheets.SheetRef ref : sheetRefs) {
      String part = parts.get(ref.getRid());
      if (part == null) continue; // broken relationship — skip this sheet entry

      byte[] worksheet = worksheetBytes.get(part);
      if (worksheet == null) continue; // catalog points at a missing zip part

      Worksheet wrksheet = new Worksheet(XML, worksheet);
      Worksheet.WorksheetScan scan = wrksheet.toWorksheetScan();
      if (!scan.getHasCell()) continue; // blank sheet

      result.add(
          new Sheet(
              ref.getName(),
              ref.getSheetId(),
              SpreadsheetManipulator.fingerprint(
                  worksheet, scan.getSharedRefs(), sharedStringItems)));
    }

    return result;
  }

  private static List<Sheet> parse(String fileName, byte[] content) {
    if (content == null || content.length == 0) return Collections.emptyList();

    String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    if (!lower.endsWith(".xlsx")) return Collections.emptyList();

    try {
      return read(content);
    } catch (Exception unreadable) {
      // Malformed zip or XML: treat as no sheets.
      return Collections.emptyList();
    }
  }

  public Spreadsheet(String fileName, byte[] content) {
    this.sheets = Collections.unmodifiableList(parse(fileName, content));
  }

  public List<Sheet> sheets() {
    return sheets;
  }

  public List<String> names() {
    List<String> names = new ArrayList<>(sheets.size());
    for (Sheet sheet : sheets) names.add(sheet.getName());

    return names;
  }

  public boolean isEmpty() {
    return sheets.isEmpty();
  }
}
