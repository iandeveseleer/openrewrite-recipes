package fr.iandeveseleer.openrewrite.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddFindByWebElement extends Recipe {

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
        return new SayHelloVisitor();
    }

    public static class SayHelloVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate javaTemplate =
                JavaTemplate.builder( "@FindBy(css = \"#{}\")public WebElement #{};")
                        .imports("org.openqa.selenium.WebElement", "org.openqa.selenium.support.FindBy")
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath("selenium-support", "selenium-java", "selenium-api")
                        )
                        .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration pClassDeclaration, ExecutionContext pExecutionContext) {
            if (shouldAddFindByElement(getFields(pClassDeclaration))) {
                J.Block addElement = javaTemplate.apply(new Cursor(getCursor(), pClassDeclaration.getBody()),
                        pClassDeclaration.getBody().getCoordinates().firstStatement(),
                        ".css-selector", "myElement" );
                pClassDeclaration = pClassDeclaration.withBody(addElement);

                maybeAddImport("org.openqa.selenium.support.FindBy", null, false);
                maybeAddImport("org.openqa.selenium.WebElement", null, false);
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