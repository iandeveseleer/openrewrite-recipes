package fr.iandeveseleer.openrewrite.recipes.utils;

public class ORStringUtils {

    private ORStringUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractElementName(String input) {
        if (input.startsWith("get") && input.length() > 3) {
            String result = input.substring(3);
            return Character.toLowerCase(result.charAt(0)) + result.substring(1);
        }
        return input;
    }
}
