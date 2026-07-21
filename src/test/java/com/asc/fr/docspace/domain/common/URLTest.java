package com.asc.fr.docspace.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class URLTest {
  @Test
  void givenUrlWithTrailingSlash_whenCallingToString_thenStripsSlashForConcatenation() {
    URL url = new URL("https://fr.example.com/");
    assertEquals("https://fr.example.com", url.toString());
    assertEquals("https://fr.example.com/api/2.0", url + "/api/2.0");
  }
}
