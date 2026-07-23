package com.asc.fr.docspace.application.service;

import static org.mockito.Mockito.inOrder;

import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDocSpaceTenantAdminServiceTest {
  @Mock private DocSpaceTenantService tenant;
  @Mock private SynchronizationLinkRegistry registry;
  @InjectMocks private DefaultDocSpaceTenantAdminService service;

  @Test
  void givenTrackedLinks_whenResetting_thenClearsLinksThenTenant() throws Exception {
    service.reset();

    InOrder inOrder = inOrder(registry, tenant);
    inOrder.verify(registry).removeAll();
    inOrder.verify(tenant).clear();
  }
}
