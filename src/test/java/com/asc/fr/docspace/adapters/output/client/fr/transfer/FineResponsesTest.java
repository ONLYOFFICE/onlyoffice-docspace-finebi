package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineTableSummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class FineResponsesTest {
  private static final String PACK_TABLES =
      "{\"success\":true,\"code\":\"200\",\"data\":{\"id\":\"17bb42\",\"name\":\"部门数据\","
          + "\"folders\":[{\"id\":\"nested\",\"name\":\"运营\",\"tables\":{}}],"
          + "\"tables\":{\"availableTables\":[{\"name\":\"05cb755fa0424f9e86675d430fd5ce08\","
          + "\"transferName\":\"Products\",\"type\":3,\"validStatus\":\"VALID\"}]}}}";

  private static JsonNode root(String body) throws IOException {
    return Json.MAPPER.readTree(body);
  }

  @Test
  void givenNestedPackTablesShape_whenFindingByUuid_thenLocatesTheDataset() throws IOException {
    FineTableSummaryResponse found =
        FineResponses.findTable(root(PACK_TABLES), "05cb755fa0424f9e86675d430fd5ce08", "");

    assertNotNull(found, "dataset present under data.tables.availableTables must be found");
  }

  @Test
  void givenNestedPackTablesShape_whenUuidAbsent_thenReturnsNull() throws IOException {
    assertNull(FineResponses.findTable(root(PACK_TABLES), "does-not-exist", ""));
  }

  @Test
  void givenExcelAddArrayShape_whenFindingByTransferName_thenLocatesTheDataset()
      throws IOException {
    String body =
        "{\"success\":true,\"data\":[{\"name\":\"uuid-1\",\"transferName\":\"Products\"}]}";

    JsonNode dataArray = root(body).path("data");

    assertNotNull(FineResponses.findTable(dataArray, "", "Products"));
  }
}
