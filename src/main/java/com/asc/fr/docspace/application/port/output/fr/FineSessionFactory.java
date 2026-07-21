package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.application.exception.SessionGenerationException;
import com.asc.fr.docspace.domain.fr.FineSession;

public interface FineSessionFactory {
  FineSession generateSession(String baseUrl) throws SessionGenerationException;
}
