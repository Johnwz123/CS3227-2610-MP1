package budgetbot.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/** Verifies the package boundaries documented for BudgetBot's layered design. */
class ArchitectureTest {
  private static final String APPLICATION_PACKAGE = "budgetbot";
  private static final String MODEL_PACKAGE = "budgetbot.model..";
  private static final String PERSISTENCE_PACKAGE = "budgetbot.persistence..";
  private static final String SERVICE_PACKAGE = "budgetbot.service..";
  private static final String TOOLS_PACKAGE = "budgetbot.tools..";
  private static final String UI_PACKAGE = "budgetbot.ui..";

  @Test
  void topLevelPackagesHaveNoDependencyCycles() {
    slices()
        .matching("budgetbot.(*)..")
        .should()
        .beFreeOfCycles()
        .check(importedApplicationClasses());
  }

  @Test
  void layersOnlyUseTheirAllowedDependencies() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Model")
        .definedBy(MODEL_PACKAGE)
        .layer("Persistence")
        .definedBy(PERSISTENCE_PACKAGE)
        .layer("Service")
        .definedBy(SERVICE_PACKAGE)
        .layer("UI")
        .definedBy(UI_PACKAGE)
        .layer("Tools")
        .definedBy(TOOLS_PACKAGE)
        .whereLayer("Model")
        .mayOnlyBeAccessedByLayers("Persistence", "Service", "UI", "Tools")
        .whereLayer("Persistence")
        .mayOnlyAccessLayers("Model")
        .whereLayer("Service")
        .mayOnlyAccessLayers("Model", "Persistence")
        .whereLayer("UI")
        .mayOnlyAccessLayers("Model", "Persistence", "Service")
        .whereLayer("Tools")
        .mayOnlyAccessLayers("Model", "Persistence", "Service")
        .check(importedApplicationClasses());
  }

  @Test
  void modelDoesNotDependOnApplicationLayers() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(MODEL_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(PERSISTENCE_PACKAGE, SERVICE_PACKAGE, TOOLS_PACKAGE, UI_PACKAGE);

    rule.check(importedApplicationClasses());
  }

  private static com.tngtech.archunit.core.domain.JavaClasses importedApplicationClasses() {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(APPLICATION_PACKAGE);
  }
}
