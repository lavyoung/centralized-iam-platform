package com.lavyoung.iam.bootstrap;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guards the dependency direction of the modular hexagonal architecture.
 */
@AnalyzeClasses(packages = "com.lavyoung.iam", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustRemainFrameworkIndependent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "org.mybatis..", "com.baomidou..",
                    "..application..", "..adapter..", "..bootstrap..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..bootstrap..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inboundMustNotDependOnOutbound = noClasses()
            .that().resideInAPackage("..adapter.inbound..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule contractsMustNotDependOnImplementation = noClasses()
            .that().resideInAPackage("..contract..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..bootstrap..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule sdkMustNotDependOnServerImplementation = noClasses()
            .that().resideInAPackage("..sdk..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..bootstrap..")
            .allowEmptyShould(true);
}
