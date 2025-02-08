package fr.iandeveseleer.openrewrite.recipes;

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
                        .classpath("selenium-support", "selenium-java", "selenium-api")
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

                    class FooBar {
                        public WebElement getBrowserNameCell() {
                            return el(By.cssSelector("div.row:nth-child(6) > div:nth-child(2)"), "Field containing browser name");
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

                    class FooBar {
                        @FindBy(css = ".css-selector")
                        public WebElement myElement;
                
                        public WebElement getBrowserNameCell() {
                            return el(By.cssSelector("div.row:nth-child(6) > div:nth-child(2)"), "Field containing browser name");
                        }
                
                        public WebElement el(By by, String description) {
                            return null;
                        }
                    }
                """
            )
        );
    }
}