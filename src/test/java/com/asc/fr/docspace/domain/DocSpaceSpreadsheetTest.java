package com.asc.fr.docspace.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DocSpaceSpreadsheetTest {
  static Stream<Arguments> getFileNameCases() {
    return Stream.of(
        Arguments.of("a/b\\report", "a_b_report.xlsx"),
        Arguments.of("  data.csv  ", "data.csv"),
        Arguments.of("legacy.xls", "legacy.xls"),
        Arguments.of(null, "file.xlsx"),
        Arguments.of("  ", "file.xlsx"));
  }

  static Stream<Arguments> getTableNameCases() {
    return Stream.of(
        Arguments.of("report.xlsx", "report"),
        Arguments.of("archive.2024.csv", "archive.2024"),
        Arguments.of("noext", "noext"),
        Arguments.of(".hidden", ".hidden"),
        Arguments.of(null, "file"));
  }

  @Nested
  class GetFileName {
    @ParameterizedTest
    @MethodSource("com.asc.fr.docspace.domain.DocSpaceSpreadsheetTest#getFileNameCases")
    void givenRawName_whenCreatingSpreadsheet_thenNormalizesFilename(
        String input, String expected) {
      assertEquals(expected, new DocSpaceSpreadsheet(input).getFileName());
    }
  }

  @Nested
  class GetTableName {
    @ParameterizedTest
    @MethodSource("com.asc.fr.docspace.domain.DocSpaceSpreadsheetTest#getTableNameCases")
    void givenFilename_whenGettingTableName_thenStripsLastExtension(String input, String expected) {
      assertEquals(expected, new DocSpaceSpreadsheet(input).getTableName());
    }
  }
}
