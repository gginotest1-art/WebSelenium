// Tạo AssertionUtils.java
package com.webselenium.utils;

import org.testng.Assert;
import org.openqa.selenium.WebDriver;

public class AssertionUtils {

    public static void assertURLContains(WebDriver driver, String urlPart) {
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(
                currentUrl.contains(urlPart),
                "URL does not contain: " + urlPart + ". Current URL: " + currentUrl
        );
    }

    public static void assertPageTitle(WebDriver driver, String expectedTitle) {
        String actualTitle = driver.getTitle();
        Assert.assertEquals(
                actualTitle,
                expectedTitle,
                "Page title mismatch"
        );
    }

    public static void assertElementPresent(String elementName, boolean isPresent) {
        Assert.assertTrue(
                isPresent,
                "Element not found: " + elementName
        );
    }
}