package com.asc.fr.docspace.adapters.input.web.imports.transfer;

import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.domain.fr.FineFolder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public final class FoldersResponse extends OkResponse {
  @Getter
  static final class Item {
    private final String id;
    private final String name;

    Item(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  private final List<Item> folders;

  private FoldersResponse(List<Item> folders) {
    super(true);
    this.folders = folders;
  }

  public static FoldersResponse of(List<FineFolder> folders) {
    List<Item> items = new ArrayList<>();
    for (FineFolder folder : folders) {
      items.add(new Item(folder.getId(), folder.getName()));
    }
    return new FoldersResponse(items);
  }
}
