package com.asc.fr.docspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.asc.fr.docspace",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  private static final String BASE = "com.asc.fr.docspace";

  @ArchTest
  static final ArchRule only_allowed_top_level_packages =
      classes()
          .should()
          .resideInAnyPackage(
              BASE, BASE + ".domain..", BASE + ".application..", BASE + ".adapters..");

  @ArchTest
  static final ArchRule domain_depends_only_on_jdk_and_lombok =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(BASE + ".application..", BASE + ".adapters..", BASE);

  @ArchTest
  static final ArchRule application_never_touches_adapters =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(BASE + ".adapters..", BASE);

  @ArchTest
  static final ArchRule services_are_package_hidden_behind_ports =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".adapters..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".application.service..")
          .because(
              "only the composition root may construct service implementations; "
                  + "everything else must use the port interfaces");

  @ArchTest
  static final ArchRule outbound_adapters_stay_out_of_the_web_layer =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".adapters.output..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".adapters.input..");
}
