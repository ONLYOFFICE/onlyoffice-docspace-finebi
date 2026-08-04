package com.asc.fr.docspace.adapters.input.web.debug;

import lombok.Value;

@Value
public class DebugLinkView {
  String tableId;
  String sheetId;
  String contentHash;
  String fileId;
  String tenantUrl;
  String lastReconciledAt;
}
