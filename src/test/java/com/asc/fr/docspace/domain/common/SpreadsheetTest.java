package com.asc.fr.docspace.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.asc.fr.docspace.domain.common.spreadsheet.Spreadsheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpreadsheetTest {
  private static final String WORKSHEET_WITH_CELLS =
      "<?xml version=\"1.0\"?><worksheet><sheetData><row r=\"1\">"
          + "<c r=\"A1\" t=\"inlineStr\"><is><t>Header</t></is></c></row></sheetData></worksheet>";
  private static final String BLANK_WORKSHEET =
      "<?xml version=\"1.0\"?><worksheet><dimension ref=\"A1\"/><sheetData/></worksheet>";

  /** Builds a minimal .xlsx (zip) from the given entries so the parser is fed controlled input. */
  private static byte[] xlsx(Map<String, String> entries) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return bytes.toByteArray();
  }

  private static String workbook(String sheetsXml) {
    return "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
        + "<sheets>"
        + sheetsXml
        + "</sheets></workbook>";
  }

  private static String rel(String id, String target) {
    return "<Relationship Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
        + " Target=\""
        + target
        + "\" Id=\""
        + id
        + "\"/>";
  }

  private static String rels(String... relationships) {
    return "<Relationships>" + String.join("", relationships) + "</Relationships>";
  }

  @Nested
  class BlankSheetsAreDropped {
    @Test
    void givenLeadingBlankSheet_whenReadingNames_thenOnlyNonBlankSheetsInOrder() {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "xl/workbook.xml",
          workbook(
              "<sheet name=\"Cover\" sheetId=\"1\" r:id=\"rId1\"/>"
                  + "<sheet name=\"Sales\" sheetId=\"2\" r:id=\"rId2\"/>"
                  + "<sheet name=\"Inventory\" sheetId=\"3\" r:id=\"rId3\"/>"));
      entries.put(
          "xl/_rels/workbook.xml.rels",
          rels(
              rel("rId1", "worksheets/sheet1.xml"),
              rel("rId2", "worksheets/sheet2.xml"),
              rel("rId3", "worksheets/sheet3.xml")));
      entries.put("xl/worksheets/sheet1.xml", BLANK_WORKSHEET);
      entries.put("xl/worksheets/sheet2.xml", WORKSHEET_WITH_CELLS);
      entries.put("xl/worksheets/sheet3.xml", WORKSHEET_WITH_CELLS);

      assertThat(new Spreadsheet("book.xlsx", xlsx(entries)).names())
          .containsExactly("Sales", "Inventory");
    }

    @Test
    void givenMidWorkbookBlankSheet_whenReadingNames_thenBlankIsRemovedKeepingOrder() {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "xl/workbook.xml",
          workbook(
              "<sheet name=\"Alpha\" sheetId=\"1\" r:id=\"rId1\"/>"
                  + "<sheet name=\"MidBlank\" sheetId=\"2\" r:id=\"rId2\"/>"
                  + "<sheet name=\"Beta\" sheetId=\"3\" r:id=\"rId3\"/>"));
      entries.put(
          "xl/_rels/workbook.xml.rels",
          rels(
              rel("rId1", "/xl/worksheets/sheet1.xml"), // absolute target
              rel("rId2", "worksheets/sheet2.xml"),
              rel("rId3", "worksheets/sheet3.xml")));
      entries.put("xl/worksheets/sheet1.xml", WORKSHEET_WITH_CELLS);
      entries.put("xl/worksheets/sheet2.xml", BLANK_WORKSHEET);
      entries.put("xl/worksheets/sheet3.xml", WORKSHEET_WITH_CELLS);

      assertThat(new Spreadsheet("book.xlsx", xlsx(entries)).names())
          .containsExactly("Alpha", "Beta");
    }
  }

  @Nested
  class NamesAreRecovered {
    @Test
    void givenSheets_whenReading_thenCarriesStableSheetId() {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "xl/workbook.xml",
          workbook(
              "<sheet name=\"Sales\" sheetId=\"7\" r:id=\"rId1\"/>"
                  + "<sheet name=\"Inventory\" sheetId=\"9\" r:id=\"rId2\"/>"));
      entries.put(
          "xl/_rels/workbook.xml.rels",
          rels(rel("rId1", "worksheets/sheet1.xml"), rel("rId2", "worksheets/sheet2.xml")));
      entries.put("xl/worksheets/sheet1.xml", WORKSHEET_WITH_CELLS);
      entries.put("xl/worksheets/sheet2.xml", WORKSHEET_WITH_CELLS);

      assertThat(new Spreadsheet("book.xlsx", xlsx(entries)).sheets())
          .extracting(Sheet::getName, Sheet::getSheetId)
          .containsExactly(
              org.assertj.core.groups.Tuple.tuple("Sales", 7),
              org.assertj.core.groups.Tuple.tuple("Inventory", 9));
      assertThat(new Spreadsheet("book.xlsx", xlsx(entries)).sheets())
          .extracting(Sheet::getContentHash)
          .allMatch(hash -> hash != null && !hash.isEmpty());
    }

    @Test
    void givenTwoSheets_whenHashing_thenDifferentSheetsHaveDifferentHashes() {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "xl/workbook.xml",
          workbook(
              "<sheet name=\"A\" sheetId=\"1\" r:id=\"rId1\"/>"
                  + "<sheet name=\"B\" sheetId=\"2\" r:id=\"rId2\"/>"));
      entries.put(
          "xl/_rels/workbook.xml.rels",
          rels(rel("rId1", "worksheets/sheet1.xml"), rel("rId2", "worksheets/sheet2.xml")));
      entries.put(
          "xl/worksheets/sheet1.xml",
          "<?xml version=\"1.0\"?><worksheet><sheetData><row r=\"1\">"
              + "<c r=\"A1\" t=\"inlineStr\"><is><t>Alpha</t></is></c></row></sheetData></worksheet>");
      entries.put(
          "xl/worksheets/sheet2.xml",
          "<?xml version=\"1.0\"?><worksheet><sheetData><row r=\"1\">"
              + "<c r=\"A1\" t=\"inlineStr\"><is><t>Beta</t></is></c></row></sheetData></worksheet>");

      List<Sheet> sheets = new Spreadsheet("book.xlsx", xlsx(entries)).sheets();
      assertThat(sheets).hasSize(2);
      assertThat(sheets.get(0).getContentHash()).isNotEqualTo(sheets.get(1).getContentHash());
    }

    @Test
    void givenEntityEscapedSheetName_whenReadingNames_thenNameIsUnescaped() {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "xl/workbook.xml", workbook("<sheet name=\"P&amp;L\" sheetId=\"1\" r:id=\"rId1\"/>"));
      entries.put("xl/_rels/workbook.xml.rels", rels(rel("rId1", "worksheets/sheet1.xml")));
      entries.put("xl/worksheets/sheet1.xml", WORKSHEET_WITH_CELLS);

      assertThat(new Spreadsheet("book.xlsx", xlsx(entries)).names()).containsExactly("P&L");
    }
  }

  @Nested
  class UnreadableSourcesFallBack {
    @Test
    void givenCsvFileName_whenReadingNames_thenEmpty() {
      assertThat(new Spreadsheet("data.csv", new byte[] {1, 2, 3}).names()).isEmpty();
    }

    @Test
    void givenNonZipContent_whenReadingNames_thenEmpty() {
      assertThat(new Spreadsheet("book.xlsx", new byte[] {1, 2, 3}).names()).isEmpty();
    }
  }
}
