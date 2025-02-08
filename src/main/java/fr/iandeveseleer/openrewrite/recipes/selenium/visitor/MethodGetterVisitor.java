package fr.iandeveseleer.openrewrite.recipes.selenium.visitor;

import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

public class MethodGetterVisitor extends JavaIsoVisitor<List<J.MethodDeclaration>> {

    private final String fullyQualifiedClassName;

    public MethodGetterVisitor(String pFullyQualifiedClassName) {
        fullyQualifiedClassName = pFullyQualifiedClassName;
    }

    @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration pCurrentMethod, List<J.MethodDeclaration> pMethodsList) {
            if (pCurrentMethod.getReturnTypeExpression() != null &&
                    pCurrentMethod.getReturnTypeExpression().getType() instanceof JavaType.Class returnType &&
                    fullyQualifiedClassName.equals(returnType.getFullyQualifiedName())) {
                pMethodsList.add(pCurrentMethod);
            }
            return super.visitMethodDeclaration(pCurrentMethod, pMethodsList);
        }

        public static List<J.MethodDeclaration> getMethods(String fullyQualifiedClassName, Tree tree) {
            return new MethodGetterVisitor(fullyQualifiedClassName).reduce(tree, new ArrayList<>());
        }
    }