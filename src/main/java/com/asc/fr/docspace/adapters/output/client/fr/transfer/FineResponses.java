package com.asc.fr.docspace.adapters.output.client.fr.transfer;

import com.asc.fr.docspace.adapters.format.Json;
import com.asc.fr.docspace.adapters.format.Text;
import com.asc.fr.docspace.adapters.output.client.fr.FineMapper;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineFolderNodeResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FinePacksFoldersDataResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineTableAddItemResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineTableSummaryResponse;
import com.asc.fr.docspace.domain.fr.FineFolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FineResponses {
  private static final FineMapper MAPPER = FineMapper.INSTANCE;
  private static final TypeReference<List<FineTableAddItemResponse>> TABLE_ADD_LIST =
      new TypeReference<List<FineTableAddItemResponse>>() {};
  private static final TypeReference<List<FineTableSummaryResponse>> TABLE_SUMMARY_LIST =
      new TypeReference<List<FineTableSummaryResponse>>() {};

  private FineResponses() {}

  private static void collectFolders(
      List<FineFolderNodeResponse> nodes, List<FineFolder> out, int depth) {
    if (nodes == null) return;

    for (FineFolderNodeResponse item : nodes) {
      String id = item.getId() == null ? "" : item.getId();
      String name = item.getName() == null ? "" : item.getName();
      if (!id.isEmpty() && !id.startsWith("__") && !name.isEmpty() && !name.startsWith("__")) {
        out.add(MAPPER.toFolder(item));
        if (depth < 4) {
          collectFolders(item.getChildren(), out, depth + 1);
          collectFolders(item.getFolders(), out, depth + 1);
        }
      }
    }
  }

  private static String firstTableAddName(FineEnvelope envelope) {
    JsonNode data = envelope.dataNode();
    if (data.isArray()) {
      List<FineTableAddItemResponse> items = Json.convert(data, TABLE_ADD_LIST);
      if (items != null) {
        for (FineTableAddItemResponse item : items)
          if (item.getName() != null && !item.getName().isEmpty()) return item.getName();
      }

      return "";
    }

    FineTableAddItemResponse single = envelope.dataAs(FineTableAddItemResponse.class);
    return single == null || single.getName() == null ? "" : single.getName();
  }

  private static JsonNode firstArray(JsonNode... candidates) {
    for (JsonNode candidate : candidates)
      if (candidate != null && candidate.isArray()) return candidate;
    return null;
  }

  private static List<FineTableSummaryResponse> tablesOf(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) return Collections.emptyList();

    JsonNode arr =
        firstArray(
            root.path("data").path("tables").path("availableTables"),
            root.path("data").path("tables"),
            root.path("data").path("availableTables"),
            root.path("tables").path("availableTables"),
            root.path("tables"),
            root.path("availableTables"),
            root);

    if (arr == null) return Collections.emptyList();

    List<FineTableSummaryResponse> tables = Json.convert(arr, TABLE_SUMMARY_LIST);
    return tables == null ? Collections.<FineTableSummaryResponse>emptyList() : tables;
  }

  public static List<FineFolder> foldersFromPacks(FineEnvelope envelope) {
    FinePacksFoldersDataResponse data = envelope.dataAs(FinePacksFoldersDataResponse.class);
    List<FineFolder> result = new ArrayList<>();
    if (data != null) collectFolders(data.getFolders(), result, 0);

    return result;
  }

  /**
   * Extracts the dataset UUID from an excel/add response. Typical shape: {@code
   * {"data":[{"name":"<UUID>","transferName":"...","success":true}]}}.
   */
  public static String datasetUuid(FineEnvelope envelope, String tableName) {
    String uuid = firstTableAddName(envelope);
    if (uuid.isEmpty() && envelope.getName() != null) uuid = envelope.getName();

    if (uuid.isEmpty()) {
      FineTableSummaryResponse table = findTable(envelope.dataNode(), "", tableName);
      if (table != null && table.getName() != null) uuid = table.getName();
    }

    return uuid;
  }

  public static FineTableSummaryResponse findTable(
      JsonNode root, String uuid, String transferName) {
    List<FineTableSummaryResponse> tables = tablesOf(root);
    for (FineTableSummaryResponse item : tables) {
      String name = item.getName() == null ? "" : item.getName();
      String tName = item.getTransferName() == null ? "" : item.getTransferName();
      if ((!uuid.isEmpty() && uuid.equals(name))
          || (!transferName.isEmpty() && transferName.equals(tName))) return item;
    }

    return null;
  }

  public static Set<String> tableIds(JsonNode root) {
    Set<String> ids = new HashSet<>();
    for (FineTableSummaryResponse item : tablesOf(root)) {
      String name = item.getName();
      if (name != null && !name.isEmpty()) ids.add(name);
    }

    return ids;
  }

  public static void requireValidAuthentication(String body, FineEnvelope envelope)
      throws IOException {
    if (envelope.authFailed(body))
      throw new IOException(
          "FineBI authentication failed. Response: " + Text.abbreviate(body, 300));
  }
}
