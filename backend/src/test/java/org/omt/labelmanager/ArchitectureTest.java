package org.omt.labelmanager;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * The two rules {@link ModularityTest} cannot express. Everything about module boundaries lives
 * there; this file is only for constraints that are about layering within a module.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("org.omt.labelmanager");

    /**
     * Keeps domain types unit-testable without a database. A domain record that imports JPA drags
     * the persistence layer into every test that touches it, which is how the unit tier degenerated
     * into record echoes (§4.1). {@code fromEntity()} takes an entity as a parameter — that is a
     * package reference, not a JPA import, and stays allowed until Phase 4 removes it from types
     * that gain behaviour.
     */
    @Test
    void domainDoesNotDependOnJpa() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("jakarta.persistence..")
                        .because(
                                "domain types must be constructible in a plain JUnit test, with no"
                                        + " Spring and no database");

        rule.check(CLASSES);
    }

    /**
     * Pins where HTTP lives. A top-level {@code web} module was tried in Phase 2 and withdrawn
     * (§5.2 rule 4), so a controller belongs to its own module — but still in that module's {@code
     * web} sub-package, never mixed into {@code application} or {@code domain}.
     */
    @Test
    void controllersLiveInAWebPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Controller")
                        .should()
                        .resideInAPackage("..web..")
                        .because("HTTP concerns stay in one place per module");

        rule.check(CLASSES);
    }
}
