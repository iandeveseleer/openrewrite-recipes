package fr.iandeveseleer.openrewrite.recipes;

import fr.iandeveseleer.openrewrite.recipes.selenium.AddFindByWebElement;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddFindByWebElementTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddFindByWebElement("org.openqa.selenium.WebElement","el"))
                .parser(JavaParser.fromJavaVersion()
                        .classpath(
                                "lombok",
                                "selenium-support",
                                "selenium-java",
                                "selenium-api")
                );
    }

    @Test
    void addFindByFieldBasedOnWebElementMethod() {
        rewriteRun(
            java(
                """
                    package com.yourorg;

                    import org.openqa.selenium.WebElement;
                    import org.openqa.selenium.By;

                    class FakePage {
                        public WebElement getBrowserNameCell() {
                            return el(By.cssSelector(".css-selector"), "Simple element");
                        }
                
                        public WebElement el(By by, String description) {
                            return null;
                        }
                    }
                """,
                """
                    package com.yourorg;
                
                    import org.openqa.selenium.WebElement;
                    import org.openqa.selenium.support.FindBy;
                    import lombok.Getter;
                    import org.openqa.selenium.By;

                    class FakePage {
                        @Getter
                        @FindBy(css = ".css-selector")
                        public WebElement browserNameCell;
                
                        public WebElement el(By by, String description) {
                            return null;
                        }
                    }
                """
            )
        );
    }
}