package fr.iandeveseleer.openrewrite.recipes.selenium;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.iandeveseleer.openrewrite.recipes.selenium.visitor.MethodGetterVisitor;
import fr.iandeveseleer.openrewrite.recipes.utils.ORStringUtils;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static fr.iandeveseleer.openrewrite.recipes.selenium.utils.MethodUtils.isMethodToDelete;

public class AddFindByWebElement extends Recipe {
    public static final String SELENIUM_WEB_ELEMENT = "org.openqa.selenium.WebElement";
    private static final String SELENIUM_FIND_BY = "org.openqa.selenium.support.FindBy";
    private static final String SELENIUM_BY = "org.openqa.selenium.By";
    private static final String LOMBOK_GETTER = "lombok.Getter";

    @Option(displayName = "Fully qualified class name",
            description = "A fully qualified class name, will be used to identify which method are to transform to @FindBy fields.",
            example = "org.openqa.selenium.WebElement")
    String fullyQualifiedClassName;
    @Option(displayName = "Name of the invocated method on return",
            description = "The name of the method which is called as the return statement of the methods.",
            example = "el")
    String returnCalledMethodInvocation;

    @JsonCreator
    public AddFindByWebElement(@JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName,
                               @JsonProperty("returnCalledMethodInvocation") String returnCalledMethodInvocation) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.returnCalledMethodInvocation = returnCalledMethodInvocation;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add a field to a class with a @FindBy annotation";
    }

    @Override
    public @NotNull String getDescription() {
        return "Add a field 'myElement' to a class with a @FindBy annotation.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddFindByElementVisitor(fullyQualifiedClassName, returnCalledMethodInvocation);
    }

    public static class AddFindByElementVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate javaTemplate =
                JavaTemplate.builder("@Getter@FindBy(css = \"#{}\")public WebElement #{};")
                        .imports(SELENIUM_WEB_ELEMENT, SELENIUM_FIND_BY, LOMBOK_GETTER)
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath(
                                        "lombok",
                                        "selenium-support",
                                        "selenium-java",
                                        "selenium-api"))
                        .build();
        private final String fullyQualifiedClassName;
        private final String returnCalledMethodInvocation;

        public AddFindByElementVisitor(String pFullyQualifiedClassName, String pReturnCalledMethodInvocation) {
            fullyQualifiedClassName = pFullyQualifiedClassName;
            returnCalledMethodInvocation = pReturnCalledMethodInvocation;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration pCurrentMethod, ExecutionContext pExecutionContext) {
            if (isMethodToDelete(pCurrentMethod, fullyQualifiedClassName)) {
                maybeRemoveImport(SELENIUM_BY);
                return null;
            }
            return super.visitMethodDeclaration(pCurrentMethod, pExecutionContext);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration pClassDeclaration, ExecutionContext pExecutionContext) {
            if(pClassDeclaration.getModifiers().stream().noneMatch(modifier -> modifier.getType() == J.Modifier.Type.Abstract)
                    && pClassDeclaration.getName().getSimpleName().endsWith("Page")) {
                List<J.MethodDeclaration> matchingMethods = MethodGetterVisitor.getMethods(fullyQualifiedClassName, pClassDeclaration);
                doAfterVisit(new FindMissingTypes().getVisitor());
                // Iterate one founded methods
                for (J.MethodDeclaration method : matchingMethods) {
                    // Get the method body
                    J.Block methodBody = method.getBody();
                    // List the method body statements
                    List<Statement> statements = methodBody.getStatements();
                    // For each statement
                    for (Statement statement : statements) {
                        // If the statement is a "return"
                        if (statement instanceof J.Return) {
                            J.Return returnStatement = (J.Return) statement;
                            // And a method is invoked in this 'return' statement
                            if (returnStatement.getExpression() instanceof J.MethodInvocation) {
                                // Get the invocated method
                                J.MethodInvocation methodInvocation = (J.MethodInvocation) ((J.Return) statement).getExpression();
                                // If matching wanted one
                                if (methodInvocation.getSimpleName().equals(returnCalledMethodInvocation)) {
                                    // Retrieve arguments of the called method
                                    List<Expression> arguments = methodInvocation.getArguments();
                                    // If as matching count of arguments
                                    if (arguments.size() == 2) {
                                        // Store cssSelector and element description
                                        System.out.println(arguments.get(0));
                                        System.out.println(arguments.get(1));
                                        J.MethodInvocation bySelector = (J.MethodInvocation) arguments.get(0);
                                        String methodName = method.getSimpleName();
                                        J.Literal cssSelector = ((J.Literal) bySelector.getArguments().get(0));
                                        J.Literal description = (J.Literal) arguments.get(1);

                                        // Add a new @FindBy(css = cssSelector) field
                                        J.Block addElement = javaTemplate.apply(new Cursor(getCursor(), pClassDeclaration.getBody()),
                                                pClassDeclaration.getBody().getCoordinates().firstStatement(),
                                                cssSelector.getValue(), ORStringUtils.extractElementName(methodName));
                                        pClassDeclaration = pClassDeclaration.withBody(addElement);

                                        // Add missing imports if needed
                                        maybeAddImport(LOMBOK_GETTER, null, false);
                                        maybeAddImport(SELENIUM_FIND_BY, null, false);
                                        maybeAddImport(SELENIUM_WEB_ELEMENT, null, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return super.visitClassDeclaration(pClassDeclaration, pExecutionContext);
        }
    }
}