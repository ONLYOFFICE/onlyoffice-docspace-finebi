package com.asc.fr.docspace.application.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultDocSpaceTenantAdminServiceTest {
  private TestPorts.InMemoryDocSpaceTenantService tenant;
  private TestPorts.InMemorySynchronizationService registry;
  private DefaultDocSpaceTenantAdminService service;

  @BeforeEach
  void setUp() {
    tenant = new TestPorts.InMemoryDocSpaceTenantService();
    tenant.config =
        new DocSpaceTenantConfiguration(
            "https://docspace.example.com",
            new DocSpaceAccountCredentials("admin@example.com", "1", "hash"));
    registry = new TestPorts.InMemorySynchronizationService();
    service = new DefaultDocSpaceTenantAdminService(tenant, registry);
  }

  @Test
  void givenTrackedLinks_whenResetting_thenClearsLinksAndTenant() throws IOException {
    registry.put(new FileSynchronizationRecord("22", "Report", "folder-1", "uuid-1"));
    registry.put(new FileSynchronizationRecord("23", "Sales", "folder-1", "uuid-2"));

    service.reset();

    assertTrue(registry.entries.isEmpty());
    assertNull(registry.find("22"));
    assertTrue(tenant.load().getUrl().getValue().isEmpty());
  }
}
