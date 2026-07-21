package com.asc.fr.docspace.application.port.output.docspace;

public interface DocSpacePathService {
  String sanitizeFileName(String fileName);

  String absolutize(String baseUrl, String url);
}
