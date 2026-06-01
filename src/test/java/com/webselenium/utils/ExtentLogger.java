package com.webselenium.utils;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.webselenium.helpers.ScreenshotUtils;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class ExtentLogger {

    private static final ThreadLocal<List<String>> steps =
            ThreadLocal.withInitial(ArrayList::new);

    private static final ThreadLocal<Integer> counter =
            ThreadLocal.withInitial(() -> 0);

    public static void reset() {
        steps.get().clear();
        counter.set(0);
    }

    public static void step(String message) {
        int current = counter.get() + 1;
        counter.set(current);
        steps.get().add("Step " + current + ": " + message);
    }

    public static void logAllStepsPass() {
        if (ExtentTestManager.getExtentTest() == null) return;

        for (String step : steps.get()) {
            ExtentTestManager.getExtentTest().log(Status.PASS, step);
        }
    }

    public static void logFailure(String error, WebDriver driver) {

        if (ExtentTestManager.getExtentTest() == null) return;

        List<String> list = steps.get();

        // Log all steps
        for (String step : list) {
            ExtentTestManager.getExtentTest().log(Status.PASS, step);
        }

        String message = (!list.isEmpty())
                ? list.get(list.size() - 1) + " | ERROR: " + error
                : "TEST FAILED | ERROR: " + error;

        // ✅ USE BASE64 SCREENSHOT (THIS WORKS!)
        ExtentTestManager.addScreenshotBase64(Status.FAIL, message, driver);
    }

    public static void pass(String message) {
        ExtentTestManager.logMessage(Status.PASS, message);
    }

    public static void fail(String message) {
        ExtentTestManager.logMessage(Status.FAIL, message);
    }

    public static void failWithScreenshotPath(String message, String screenshotPath) {
        com.aventstack.extentreports.ExtentTest test = ExtentTestManager.getExtentTest();
        if (test == null) return;
        if (screenshotPath != null && !screenshotPath.isBlank()) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(screenshotPath));
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                test.log(Status.FAIL, message,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(
                                "data:image/png;base64," + base64).build());
                return;
            } catch (Exception ignored) {}
        }
        test.log(Status.FAIL, message);
    }

    public static void skip(String message) {
        ExtentTestManager.logMessage(Status.SKIP, message);
    }
}