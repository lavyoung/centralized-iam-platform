package com.lavyoung.iam.bootstrap;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
                    "..application..", "..adapter..", "..bootstrap..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..bootstrap..");

    @ArchTest
    static final ArchRule inboundMustNotDependOnOutbound = noClasses()
            .that().resideInAPackage("..adapter.inbound..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..");

    @ArchTest
    static final ArchRule contractsMustNotDependOnImplementation = noClasses()
            .that().resideInAPackage("..contract..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..bootstrap..");

    @ArchTest
    static final ArchRule sdkMustNotDependOnServerImplementation = noClasses()
            .that().resideInAPackage("..sdk..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..bootstrap..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule ambiguousMappingPackagesAreForbidden = noClasses()
            .should().resideInAPackage("..mapping..");

    @ArchTest
    static final ArchRule domainRepositoriesMustUseRepositoryPackage = classes()
            .that().resideInAPackage("..domain..")
            .and().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..domain.*.repository..");

    @ArchTest
    static final ArchRule persistenceObjectsMustUsePoPackage = classes()
            .that().resideInAPackage("..adapter.outbound.persistence..")
            .and().haveSimpleNameEndingWith("PO")
            .should().resideInAPackage("..adapter.outbound.persistence.*.po..");

    @ArchTest
    static final ArchRule persistenceDaosMustUseDaoPackage = classes()
            .that().resideInAPackage("..adapter.outbound.persistence..")
            .and().haveSimpleNameEndingWith("Dao")
            .should().resideInAPackage("..adapter.outbound.persistence.*.dao..");

    @ArchTest
    static final ArchRule persistenceAssemblersMustUseAssemblerPackage = classes()
            .that().resideInAPackage("..adapter.outbound.persistence..")
            .and().haveSimpleNameEndingWith("Assembler")
            .should().resideInAPackage("..adapter.outbound.persistence.*.assembler..");
}
