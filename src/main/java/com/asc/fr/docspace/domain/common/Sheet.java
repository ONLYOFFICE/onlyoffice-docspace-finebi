package com.asc.fr.docspace.domain.common;

import lombok.Value;

@Value
public class Sheet {
    String name;
    int sheetId;
    String contentHash;
}
