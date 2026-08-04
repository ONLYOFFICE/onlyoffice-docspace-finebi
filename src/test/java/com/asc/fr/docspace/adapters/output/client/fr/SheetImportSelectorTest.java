package com.asc.fr.docspace.adapters.output.client.fr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.asc.fr.docspace.adapters.output.client.fr.SheetImportSelector.PreviewExecutor;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.FineSheetPreview;
import com.asc.fr.docspace.domain.common.Sheet;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;

class SheetImportSelectorTest {
  private static String ok(String field) {
    return "{\"success\":true,\"code\":\"200\",\"data\":{\"baseAttach\":{\"attach_id\":\"a\","
        + "\"sheetIndex\":[0]},\"fields\":[{\"id\":\"x\",\"name\":\""
        + field
        + "\",\"type\":16}],\"data\":[[\"v\"]],\"tableName\":\"probe\"}}";
  }

  private static final String END =
      "{\"success\":true,\"code\":\"200\",\"data\":{\"baseAttach\":{\"attach_id\":\"a\","
          + "\"sheetIndex\":[9]},\"fields\":[],\"data\":[],\"tableName\":\"probe\"}}";

  private static final String HEADER_FAIL =
      "{\"success\":false,\"code\":\"400\",\"data\":{\"fields\":[],\"data\":[]},"
          + "\"errorCode\":\"61310043\",\"errorMsg\":\"FineExcelHeaderException: excel header "
          + "exception, wrong excelReadItem\"}";

  private static final String TOKEN_FAIL =
      "{\"success\":false,\"errorMsg\":\"com.fr.decision.webservice.exception.login."
          + "TokenNotExistException\"}";

  private static PreviewExecutor responding(Map<Integer, String> idxs) {
    return (index, tableName) -> idxs.getOrDefault(index, END);
  }

  private static Map<Integer, String> at(Object... payload) {
    Map<Integer, String> index = new LinkedHashMap<>();
    for (int i = 0; i < payload.length; i += 2)
      index.put((Integer) payload[i], (String) payload[i + 1]);
    return index;
  }

  private static List<Sheet> sheets(Object... nameThenId) {
    List<Sheet> sheets = new ArrayList<>();
    for (int i = 0; i < nameThenId.length; i += 2)
      sheets.add(new Sheet((String) nameThenId[i], (Integer) nameThenId[i + 1], ""));
    return sheets;
  }

  @Test
  void givenBadSheetBetweenGoodOnes_whenSelecting_thenSkipsItAndKeepsTheRest() throws IOException {
    Map<Integer, String> responses = at(0, ok("Region"), 1, HEADER_FAIL, 2, ok("City"));

    List<FineSheetPreview> selected =
        SheetImportSelector.select(
            "Book", sheets("Sales", 5, "Bad", 6, "Inventory", 7), responding(responses));

    assertThat(selected).extracting(FineSheetPreview::getSheetIndex).containsExactly(0, 2);
    assertThat(selected)
        .extracting(FineSheetPreview::getTableName)
        .containsExactly("Book_Sales", "Book_Inventory");
    assertThat(selected)
        .extracting(FineSheetPreview::getSheetName)
        .containsExactly("Sales", "Inventory");
    assertThat(selected).extracting(FineSheetPreview::getSheetId).containsExactly(5, 7);
  }

  @Test
  void givenEverySheetFailsHeader_whenSelecting_thenSelectsNothing() throws IOException {
    Map<Integer, String> responses = at(0, HEADER_FAIL, 1, HEADER_FAIL, 2, END);

    List<FineSheetPreview> selected =
        SheetImportSelector.select("Book", sheets("A", 1, "B", 2), responding(responses));

    assertThat(selected).isEmpty();
  }

  @Test
  void givenTokenExpired_whenSelecting_thenThrowsAuthFailure() {
    Map<Integer, String> responses = at(0, TOKEN_FAIL);

    assertThatThrownBy(
            () -> SheetImportSelector.select("Book", sheets("Sales", 1), responding(responses)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("authentication");
  }

  @Test
  void givenNoParsedSheetNames_whenSelecting_thenNamesFirstSheetBareAndRestPositionally()
      throws IOException {
    Map<Integer, String> responses = at(0, ok("A"), 1, ok("B"), 2, END);

    List<FineSheetPreview> selected =
        SheetImportSelector.select("Book", Collections.emptyList(), responding(responses));

    assertThat(selected)
        .extracting(FineSheetPreview::getTableName)
        .containsExactly("Book", "Book_Sheet2");
  }

  @Test
  void givenRealIndices_whenSelecting_thenPreviewsAtThatPositionNotItsPositionInTheSubset()
      throws IOException {
    Map<Integer, String> responses = at(0, ok("WrongSheet"), 1, ok("Extra"));

    List<FineSheetPreview> selected =
        SheetImportSelector.select(
            "Book", sheets("Extra", 2), Collections.singletonList(1), responding(responses));

    assertThat(selected).extracting(FineSheetPreview::getSheetIndex).containsExactly(1);
    assertThat(selected).extracting(FineSheetPreview::getSheetName).containsExactly("Extra");
  }

  @Test
  void givenOneTargetedSheetNotImportable_whenSelecting_thenSkipsItWithoutStoppingTheOthers()
      throws IOException {
    Map<Integer, String> responses = at(0, ok("A"), 2, END);

    List<FineSheetPreview> selected =
        SheetImportSelector.select(
            "Book", sheets("A", 1, "C", 3), Arrays.asList(0, 2), responding(responses));

    assertThat(selected).extracting(FineSheetPreview::getSheetIndex).containsExactly(0);
  }
}
