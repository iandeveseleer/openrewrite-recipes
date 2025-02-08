package fr.iandeveseleer.openrewrite.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.Java17Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddFindByWebElementTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddFindByWebElement())
                .parser(JavaParser.fromJavaVersion().classpath("selenium-support", "selenium-java"));
    }

    @Test
    void addsHelloToFooBar() {
        rewriteRun(
            java(
                """
                    package com.yourorg;

                    class FooBar {
                    }
                """,
                """
                    package com.yourorg;
                
                    import org.openqa.selenium.WebElement;
                    import org.openqa.selenium.support.FindBy;

                    class FooBar {
                        @FindBy(css = ".css-selector")
                        public WebElement myElement;
                    }
                """
            )
        );
    }
}