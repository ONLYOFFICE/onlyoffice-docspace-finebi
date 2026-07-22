package com.asc.fr.docspace.adapters.input.web.debug;

import lombok.Value;

@Value
public class DebugLinkView {
  String tableId;
  String tableName;
  String fileId;
  String folderId;
  String lastReconciledAt;
  String fileUrl;
  String folderUrl;
}
