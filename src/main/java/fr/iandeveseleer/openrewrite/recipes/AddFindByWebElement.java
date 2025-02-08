package fr.iandeveseleer.openrewrite.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.iandeveseleer.openrewrite.recipes.selenium.visitor.MethodGetterVisitor;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class AddFindByWebElement extends Recipe {
    public static final String SELENIUM_WEB_ELEMENT = "org.openqa.selenium.WebElement";
    private static final String SELENIUM_FIND_BY = "org.openqa.selenium.support.FindBy";

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
        System.out.println("fullyQualifiedClassName: " + fullyQualifiedClassName);
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
                JavaTemplate.builder("@FindBy(css = \"#{}\")public WebElement #{};")
                        .imports(SELENIUM_WEB_ELEMENT, SELENIUM_FIND_BY)
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath("selenium-support", "selenium-java", "selenium-api"))
                        .build();
        private final String fullyQualifiedClassName;
        private final String returnCalledMethodInvocation;

        public AddFindByElementVisitor(String pFullyQualifiedClassName, String pReturnCalledMethodInvocation) {
            fullyQualifiedClassName = pFullyQualifiedClassName;
            returnCalledMethodInvocation = pReturnCalledMethodInvocation;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration pClassDeclaration, ExecutionContext pExecutionContext) {
            List<J.MethodDeclaration> matchingMethods = MethodGetterVisitor.getMethods(fullyQualifiedClassName, pClassDeclaration);

            if (shouldAddFindByElement(getFields(pClassDeclaration))) {
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
                                        J.MethodInvocation bySelector = (J.MethodInvocation) arguments.get(0);
                                        J.Literal cssSelector = ((J.Literal) bySelector.getArguments().get(0));
                                        J.Literal description = (J.Literal) arguments.get(1);

                                        // Add a new @FindBy(css = cssSelector) field
                                        J.Block addElement = javaTemplate.apply(new Cursor(getCursor(), pClassDeclaration.getBody()),
                                                pClassDeclaration.getBody().getCoordinates().firstStatement(),
                                                cssSelector.getValue(), "myElement");
                                        pClassDeclaration = pClassDeclaration.withBody(addElement);

                                        // Add missing imports if needed
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

        private List<J.VariableDeclarations> getFields(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.VariableDeclarations)
                    .map(J.VariableDeclarations.class::cast)
                    .toList();
        }

        private boolean shouldAddFindByElement(List<J.VariableDeclarations> variableDeclarations) {
            return variableDeclarations.stream().noneMatch(variableDeclaration -> variableDeclaration.getVariables().stream()
                    .anyMatch(variableDeclarator -> variableDeclarator.getSimpleName().equals("myElement")));
        }
    }
}