package kr.chapchap.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static final List<String> DOMAIN_MODULE_PACKAGES = List.of(
            "kr.chapchap.account",
            "kr.chapchap.consumption",
            "kr.chapchap.report",
            "kr.chapchap.place",
            "kr.chapchap.recommendation",
            "kr.chapchap.notification"
    );

    private static final String[] DOMAIN_MODULE_PACKAGE_PATTERNS = DOMAIN_MODULE_PACKAGES.stream()
            .map(packageName -> packageName + "..")
            .toArray(String[]::new);

    private static final String[] DOMAIN_EXCEPTION_PACKAGE_PATTERNS = DOMAIN_MODULE_PACKAGES.stream()
            .map(packageName -> packageName + ".exception..")
            .toArray(String[]::new);

    private static final String[] DOMAIN_LAYER_PACKAGE_PATTERNS = Stream.concat(
            Stream.of("..api..", "..application..", "..domain..", "..infra.."),
            Arrays.stream(DOMAIN_EXCEPTION_PACKAGE_PATTERNS)
    ).toArray(String[]::new);

    private static final JavaClasses DOMAIN_MODULE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(DOMAIN_MODULE_PACKAGES.toArray(String[]::new));

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("kr.chapchap");

    @Test
    void 도메인_모듈의_클래스는_허용된_계층에_위치해야_한다() {
        // given
        ArchRule rule = classes()
                .should().resideInAnyPackage(DOMAIN_LAYER_PACKAGE_PATTERNS)
                .as("도메인 모듈의 클래스는 API, Application, Domain, Infra, Exception 계층 중 하나에 위치해야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void 도메인_모듈의_계층은_허용된_방향으로만_의존해야_한다() {
        // given
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .optionalLayer("API").definedBy("..api..")
                .optionalLayer("Application").definedBy("..application..")
                .optionalLayer("Domain").definedBy("..domain..")
                .optionalLayer("Infra").definedBy("..infra..")
                .optionalLayer("Exception").definedBy(DOMAIN_EXCEPTION_PACKAGE_PATTERNS)
                .whereLayer("API").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infra")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infra")
                .whereLayer("Infra").mayNotBeAccessedByAnyLayer()
                .whereLayer("Exception").mayOnlyBeAccessedByLayers("API", "Application", "Domain", "Infra")
                .as("API는 Application, Application은 Domain, Infra는 Application과 Domain을 참조하고 Exception은 모든 계층에서 공유할 수 있다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void 도메인_모듈은_다른_도메인_모듈을_참조할_때_Application_계층만_참조해야_한다() {
        // given
        ArchRule rule = classes()
                .should(accessOtherDomainModulesThroughApplication())
                .as("도메인 모듈은 다른 도메인 모듈을 참조할 때 Application 계층만 참조해야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void 도메인_모듈은_서로_순환_의존하지_않아야_한다() {
        // given
        ArchRule rule = slices()
                .matching("kr.chapchap.(*)..")
                .should().beFreeOfCycles()
                .as("도메인 모듈은 서로 순환 의존하지 않아야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void module_core는_도메인_모듈에_의존하지_않아야_한다() {
        // given
        ArchRule rule = noClasses()
                .that().resideInAPackage("kr.chapchap.core..")
                .should().dependOnClassesThat().resideInAnyPackage(DOMAIN_MODULE_PACKAGE_PATTERNS)
                .as("module-core는 도메인 모듈에 의존하지 않아야 한다");

        // when & then
        rule.check(MAIN_CLASSES);
    }

    @Test
    void Application_Port는_인터페이스여야_한다() {
        // given
        ArchRule rule = classes()
                .that().resideInAPackage("..application.port..")
                .should().beInterfaces()
                .allowEmptyShould(true)
                .as("Application Port는 인터페이스여야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void Domain_Repository는_인터페이스여야_한다() {
        // given
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.repository..")
                .should().beInterfaces()
                .allowEmptyShould(true)
                .as("Domain Repository는 인터페이스여야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    @Test
    void CommandService와_QueryService는_Application_Service_패키지에_위치해야_한다() {
        // given
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("CommandService")
                .or().haveSimpleNameEndingWith("QueryService")
                .should().resideInAPackage("..application.service..")
                .allowEmptyShould(true)
                .as("CommandService와 QueryService는 Application Service 패키지에 위치해야 한다");

        // when & then
        rule.check(DOMAIN_MODULE_CLASSES);
    }

    private static ArchCondition<JavaClass> accessOtherDomainModulesThroughApplication() {
        return new ArchCondition<>("다른 도메인 모듈의 Application 계층만 참조해야 한다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<String> sourceModule = findDomainModule(javaClass);
                if (sourceModule.isEmpty()) {
                    return;
                }

                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    Optional<String> targetModule = findDomainModule(targetClass);
                    if (targetModule.isEmpty() || sourceModule.equals(targetModule)) {
                        continue;
                    }

                    String applicationPackage = targetModule.get() + ".application";
                    boolean isApplicationDependency = targetClass.getPackageName().equals(applicationPackage)
                            || targetClass.getPackageName().startsWith(applicationPackage + ".");
                    if (!isApplicationDependency) {
                        String message = "%s가 다른 모듈의 Application 계층 밖에 의존한다: %s"
                                .formatted(javaClass.getName(), dependency.getDescription());
                        events.add(SimpleConditionEvent.violated(dependency, message));
                    }
                }
            }
        };
    }

    private static Optional<String> findDomainModule(JavaClass javaClass) {
        return DOMAIN_MODULE_PACKAGES.stream()
                .filter(packageName -> javaClass.getPackageName().equals(packageName)
                        || javaClass.getPackageName().startsWith(packageName + "."))
                .findFirst();
    }
}
