package com.asc.fr.docspace.application.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDocSpaceTenantAdminServiceTest {
  private static final DocSpaceTenantConfiguration CURRENT =
      new DocSpaceTenantConfiguration(
          "https://docspace.example.com",
          new DocSpaceAccountCredentials("admin@example.com", "1", "hash"));
  private static final DocSpaceAccountCredentials NEW_ADMIN =
      new DocSpaceAccountCredentials("new-admin@example.com", "2", "hash2");

  @Mock private DocSpaceTenantService tenant;
  @Mock private DocSpaceSavedTenantService savedTenants;
  @Mock private SynchronizationLinkRegistry registry;
  @Mock private SynchronizationSettings synchronizationSettings;
  @InjectMocks private DefaultDocSpaceTenantAdminService service;

  @BeforeEach
  void setUp() {
    lenient().when(tenant.load()).thenReturn(DocSpaceTenantConfiguration.empty());
    lenient().when(savedTenants.hasCapacityFor(any())).thenReturn(true);
  }

  @Test
  void givenTrackedLinks_whenResetting_thenClearsLinksSavedTenantsThenTenant() throws Exception {
    service.reset();

    InOrder inOrder = inOrder(registry, savedTenants, tenant);
    inOrder.verify(registry).removeAll();
    inOrder.verify(savedTenants).clearAll();
    inOrder.verify(tenant).clear();

    verifyNoInteractions(synchronizationSettings);
  }

  @Test
  void givenSavedTenant_whenRemovingIt_thenDropsLinksCredentialsAndLeavesOthers() throws Exception {
    when(tenant.load())
        .thenReturn(
            new DocSpaceTenantConfiguration(
                "https://other.example.com",
                new DocSpaceAccountCredentials("other@example.com", "9", "hash9")));

    service.removeTenant(CURRENT.getUrl());

    InOrder inOrder = inOrder(registry, savedTenants);
    inOrder.verify(registry).removeByTenant(CURRENT.getUrl().getValue());
    inOrder.verify(savedTenants).remove(CURRENT.getUrl().getValue());
    verify(tenant, never()).clear();
    verify(synchronizationSettings, never()).clearSecret();
  }

  @Test
  void givenActiveTenant_whenRemovingIt_thenClearsActiveRowAndSecret() throws Exception {
    when(tenant.load()).thenReturn(CURRENT);

    service.removeTenant(CURRENT.getUrl());

    verify(registry).removeByTenant(CURRENT.getUrl().getValue());
    verify(savedTenants).remove(CURRENT.getUrl().getValue());
    verify(tenant).clear();
    verify(synchronizationSettings).clearSecret();
  }

  @Test
  void givenActiveTenantWithAnotherSaved_whenRemovingIt_thenActivatesTheRemainingOne()
      throws Exception {
    when(tenant.load()).thenReturn(CURRENT);
    com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection remaining =
        new com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection(
            new DocSpaceTenantConfiguration("https://other.example.com", NEW_ADMIN),
            "OtherSecret123");
    when(savedTenants.listConnections()).thenReturn(java.util.Collections.singletonList(remaining));

    service.removeTenant(CURRENT.getUrl());

    verify(tenant).save(remaining.getConfiguration());
    verify(synchronizationSettings).storeSecret("OtherSecret123");
    verify(tenant, never()).clear();
    verify(synchronizationSettings, never()).clearSecret();
  }

  @Test
  void givenSavedTenant_whenSelectingIt_thenActivatesAndRestoresSecret() throws Exception {
    when(tenant.load()).thenReturn(DocSpaceTenantConfiguration.empty());
    when(savedTenants.listConnections())
        .thenReturn(
            java.util.Collections.singletonList(
                new com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection(
                    CURRENT, "SavedSecret123")));

    service.selectTenant(CURRENT.getUrl());

    verify(tenant).save(CURRENT);
    verify(synchronizationSettings).storeSecret("SavedSecret123");
    verify(savedTenants, never()).upsert(any(), any());
  }

  @Test
  void givenActiveTenant_whenChangingTenant_thenPreservesItsCredentialsAndKeepsLinks()
      throws Exception {
    when(tenant.load()).thenReturn(CURRENT);
    when(synchronizationSettings.loadSecret()).thenReturn("TestSecret123");

    service.changeTenant();

    InOrder inOrder = inOrder(savedTenants, tenant);
    inOrder.verify(savedTenants).upsert(CURRENT, "TestSecret123");
    inOrder.verify(tenant).clear();

    verify(registry, never()).removeAll();
    verify(savedTenants, never()).clearAll();
  }

  @Test
  void givenSavedTenantsAtCapacity_whenChangingTenant_thenRefusesWithoutMutating()
      throws Exception {
    when(tenant.load()).thenReturn(CURRENT);
    when(savedTenants.hasCapacityFor(CURRENT.getUrl().getValue())).thenReturn(false);

    assertThatThrownBy(() -> service.changeTenant())
        .isInstanceOf(TenantLimitExceededException.class);

    verify(savedTenants, never()).upsert(any(), any());
    verify(tenant, never()).clear();
  }

  @Test
  void givenNoTenantConfiguredYet_whenSaving_thenPersistsWithoutTouchingSavedTenants()
      throws Exception {
    service.save(new URL("https://docspace.example.com"), CURRENT.getAdmin());

    verify(tenant).save(any(DocSpaceTenantConfiguration.class));
    verify(savedTenants, never()).upsert(any(), any());
  }

  @Test
  void givenDifferentTenantAlreadyConfigured_whenSaving_thenPreservesTheOutgoingOne()
      throws Exception {
    when(tenant.load()).thenReturn(CURRENT);
    when(synchronizationSettings.loadSecret()).thenReturn("TestSecret123");

    service.save(new URL("https://other.example.com"), NEW_ADMIN);

    InOrder inOrder = inOrder(savedTenants, tenant);
    inOrder.verify(savedTenants).upsert(CURRENT, "TestSecret123");
    inOrder.verify(tenant).save(any(DocSpaceTenantConfiguration.class));
  }

  @Test
  void givenSameTenantResaved_whenSaving_thenDoesNotConsumeASavedSlot() throws Exception {
    when(tenant.load()).thenReturn(CURRENT);

    service.save(new URL("https://docspace.example.com"), NEW_ADMIN);

    verify(savedTenants, never()).upsert(any(), any());
    verify(tenant).save(any(DocSpaceTenantConfiguration.class));
  }

  @Test
  void givenSavedTenantsAtCapacity_whenSavingANewOne_thenRefusesWithoutTouchingTenant()
      throws Exception {
    when(savedTenants.hasCapacityFor("https://third.example.com")).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.save(new URL("https://third.example.com"), NEW_ADMIN))
        .isInstanceOf(TenantLimitExceededException.class);

    verify(tenant, never()).save(any());
    verify(savedTenants, never()).upsert(any(), any());
  }

  @Test
  void givenSavedTenantsAtCapacity_whenReconnectingToTheActiveTenant_thenStillAllowed()
      throws Exception {
    when(tenant.load()).thenReturn(CURRENT);

    lenient().when(savedTenants.hasCapacityFor(CURRENT.getUrl().getValue())).thenReturn(false);

    service.save(CURRENT.getUrl(), NEW_ADMIN);

    verify(tenant).save(any(DocSpaceTenantConfiguration.class));
  }
}
