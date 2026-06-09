package com.devhouse.financial_plan;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.devhouse.financial_plan", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application = noClasses()
            .that().resideInAPackage("com.devhouse.financial_plan.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.devhouse.financial_plan.application..")
            .because("Domain must not depend on application layer.");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("com.devhouse.financial_plan.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.devhouse.financial_plan.infrastructure..")
            .because("Domain must not depend on infrastructure layer.");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("com.devhouse.financial_plan.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.devhouse.financial_plan.infrastructure..")
            .because("Application layer must not depend on infrastructure layer.");

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories = noClasses()
            .that().resideInAPackage("com.devhouse.financial_plan.infrastructure.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.devhouse.financial_plan.infrastructure.repository..")
            .because("Controllers must not access repositories directly, use application services.");

}
