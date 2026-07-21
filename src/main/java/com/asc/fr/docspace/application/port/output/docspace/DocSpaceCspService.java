package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.domain.common.URL;
import java.io.IOException;
import java.util.List;

public interface DocSpaceCspService {
  List<String> allowedDomains(URL docSpaceUrl) throws IOException;
}
