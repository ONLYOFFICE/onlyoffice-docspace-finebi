package com.asc.fr.docspace.domain;

import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.io.IOException;

public interface DocSpaceTenantService {
  DocSpaceTenantConfiguration load();

  void save(DocSpaceTenantConfiguration config) throws IOException;

  void clear() throws IOException;
}
