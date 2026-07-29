package com.asc.fr.docspace.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.asc.fr.docspace.domain.docspace.DocSpaceSpreadsheet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DocSpaceSpreadsheetTest {
  @Nested
  class GetFileName {
    @ParameterizedTest
    @CsvSource(
        nullValues = "null",
        value = {
          "a/b\\report, a_b_report.xlsx",
          "'  data.csv  ', data.csv",
          "legacy.xls, legacy.xls",
          "null, file.xlsx",
          "'  ', file.xlsx"
        })
    void givenRawName_whenCreatingSpreadsheet_thenNormalizesFilename(
        String input, String expected) {
      assertThat(new DocSpaceSpreadsheet(input).getFileName()).isEqualTo(expected);
    }
  }

  @Nested
  class GetTableName {
    @ParameterizedTest
    @CsvSource(
        nullValues = "null",
        value = {
          "report.xlsx, report",
          "archive.2024.csv, archive.2024",
          "noext, noext",
          ".hidden, .hidden",
          "null, file"
        })
    void givenFilename_whenGettingTableName_thenStripsLastExtension(String input, String expected) {
      assertThat(new DocSpaceSpreadsheet(input).getTableName()).isEqualTo(expected);
    }
  }

  @Nested
  class DatasetNameForSheet {
    @ParameterizedTest
    @CsvSource(
        nullValues = "null",
        value = {
          "Sales, Report_Sales",
          "'  Q1 Data  ', Report_Q1 Data",
          "a/b\\c, Report_a_b_c",
          "'Wide   gap', Report_Wide gap",
          "null, Report",
          "'', Report"
        })
    void givenSheetName_whenNaming_thenSanitizesAndAppendsPostfix(String sheet, String expected) {
      assertThat(new DocSpaceSpreadsheet("Report.xlsx").datasetNameForSheet(sheet))
          .isEqualTo(expected);
    }
  }
}
