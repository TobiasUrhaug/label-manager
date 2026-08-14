package org.omt.labelmanager;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Enforces the module boundaries ARCHITECTURE.md states in prose: no cycles between top-level
 * modules, and no access to any module's internals from another module.
 *
 * <p>The modules are the direct sub-packages of {@code org.omt.labelmanager}. Each module's public
 * surface is declared by the {@code @NamedInterface("api")} annotations on its {@code api} packages
 * — everything else is internal. Nothing here is hand-maintained: the dependency matrix is derived
 * from the package structure, so a new module needs no edit to this test.
 */
class ModularityTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(LabelManagerApplication.class);

    @Test
    void moduleStructureIsValid() {
        MODULES.verify();
    }

    /**
     * Writes the module canvases and dependency diagrams to {@code build/spring-modulith-docs}.
     * Derived from the package structure, so unlike the hand-drawn diagram in ARCHITECTURE.md it
     * cannot drift from the code.
     *
     * <p>The writers are named individually rather than calling {@code writeDocumentation()}, which
     * also runs {@code writeModuleMetadata()}. That one ignores the output folder and writes {@code
     * application-modules.json} into {@code build/resources/main}, which {@code bootJar} then
     * packages — making the shipped artifact depend on whether the tests had run.
     */
    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases()
                .writeAggregatingDocument();
    }
}
