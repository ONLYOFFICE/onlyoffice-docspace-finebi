const IMPORTABLE_EXTS = [".xlsx", ".xls", ".csv"];

export const FileUtils = Object.freeze({
  isImportable(item: { isFolder?: boolean; fileExst?: string }): boolean {
    if (item.isFolder) return false;
    return IMPORTABLE_EXTS.includes((item.fileExst ?? "").toLowerCase());
  },
});
