package com.asc.fr.docspace.adapters.output.client.fr;

enum Paths {
  ATTACH_UPLOAD("/v10/attach/upload"),
  PACKS_FOLDERS("/v5/conf/packs/folders"),
  PACK_TABLES("/v5/conf/packs/%s"),
  TABLE_ADD("/v5/conf/tables/excel/add"),
  SHEET_PREVIEW("/v5/conf/excel/sheet/preview"),
  TABLE_UPDATE("/v5/conf/tables/update");

  /** Parent id for top-level business packages. */
  static final String ROOT_GROUP_ID = "__root_group__";

  private final String path;

  Paths(String path) {
    this.path = path;
  }

  String path() {
    return path;
  }

  String path(String arg) {
    return String.format(path, arg);
  }
}
