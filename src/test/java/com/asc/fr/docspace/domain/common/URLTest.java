package com.asc.fr.docspace.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class URLTest {
  @Test
  void givenUrlWithTrailingSlash_whenCallingToString_thenStripsSlashForConcatenation() {
    URL url = new URL("https://fr.example.com/");
    assertThat(url.toString()).isEqualTo("https://fr.example.com");
    assertThat(url + "/api/2.0").isEqualTo("https://fr.example.com/api/2.0");
  }
}
