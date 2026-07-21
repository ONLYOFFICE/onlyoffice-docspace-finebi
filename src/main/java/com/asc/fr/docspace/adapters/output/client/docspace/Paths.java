package com.asc.fr.docspace.adapters.output.client.docspace;

enum Paths {
  AUTHENTICATION("/api/2.0/authentication"),
  CSP("/api/2.0/security/csp"),
  WEBHOOK("/api/2.0/settings/webhook");

  private final String path;

  Paths(String path) {
    this.path = path;
  }

  String path() {
    return path;
  }

  static String file(String fileId) {
    return "/api/2.0/files/file/" + fileId;
  }

  static String filePresignedUri(String fileId) {
    return file(fileId) + "/presigneduri";
  }

  static String folderUpload(String folderId) {
    return "/api/2.0/files/" + folderId + "/upload";
  }
}
