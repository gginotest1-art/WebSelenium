package com.webselenium.base;

import com.webselenium.helpers.ConfigReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getExplicitWait()));
    }

    // =========================
    // CLICK
    // =========================
    public void click(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception e) {
            jsClick(locator);
        }
    }

    public void click(DynamicLocator locator, Object... args) {
        click(locator.format(args));
    }

    // =========================
    // TYPE
    // =========================
    public void type(By locator, String text) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
            scrollToElement(locator);
            el.clear();
            el.sendKeys(text);
            triggerInputEvents(el);
        } catch (StaleElementReferenceException | NoSuchElementException e) {
            typeViaJavaScript(locator, text);
        }
    }

    public void type(DynamicLocator locator, String text, Object... args) {
        type(locator.format(args), text);
    }

    private void typeViaJavaScript(By locator, String text) {
        try {
            WebElement element = driver.findElement(locator);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].focus();", element);
            js.executeScript("arguments[0].value = arguments[1];", element, text);
            triggerInputEvents(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to type text in element: " + locator, e);
        }
    }

    private void triggerInputEvents(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
        js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", element);
    }

    // =========================
    // GET TEXT
    // =========================
    public String getText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    public String getText(DynamicLocator locator, Object... args) {
        return getText(locator.format(args));
    }

    // =========================
    // WAIT METHODS
    // =========================
    public void waitForVisible(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void waitForVisible(DynamicLocator locator, Object... args) {
        waitForVisible(locator.format(args));
    }

    public void waitForClickable(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void waitForClickable(DynamicLocator locator, Object... args) {
        waitForClickable(locator.format(args));
    }

    public void waitForEnabled(By locator) {
        wait.until(driver -> {
            try {
                WebElement el = driver.findElement(locator);
                return el.isDisplayed() && el.isEnabled() && (el.getAttribute("disabled") == null);
            } catch (Exception e) {
                return false;
            }
        });
    }

    // =========================
    // JS CLICK FALLBACK
    // =========================
    public void jsClick(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        } catch (Exception e) {
            throw new RuntimeException("Failed to click element: " + locator, e);
        }
    }

    // =========================
    // UTILITIES
    // =========================
    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDisplayed(DynamicLocator locator, Object... args) {
        return isDisplayed(locator.format(args));
    }

    public void scrollToElement(By locator) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
    }
}