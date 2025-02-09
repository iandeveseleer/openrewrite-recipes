package fr.iandeveseleer.openrewrite.recipes.selenium.utils;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

public class MethodUtils {

    private MethodUtils(){
        throw new IllegalStateException("Utility class");
    }

    public static boolean isMethodToDelete(J.MethodDeclaration pCurrentMethod, String pReturnTypeFullyQualified) {
        boolean isMatchingReturnType = pCurrentMethod.getReturnTypeExpression() != null &&
                pCurrentMethod.getReturnTypeExpression().getType() instanceof JavaType.Class returnType &&
                pReturnTypeFullyQualified.equals(returnType.getFullyQualifiedName());

        boolean isReturnStatementValid = false;

        J.Block methodBody = pCurrentMethod.getBody();
        if(methodBody != null) {
            Statement statement = methodBody.getStatements().get(0);

            if (statement instanceof J.Return) {
                J.Return returnStatement = (J.Return) statement;
                // And a method is invoked in this 'return' statement
                if (returnStatement.getExpression() instanceof J.MethodInvocation) {
                    // Get the invocated method
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) ((J.Return) statement).getExpression();
                    isReturnStatementValid = methodInvocation != null;
                }
            }
        }
        return isMatchingReturnType && isReturnStatementValid;
    }
}
