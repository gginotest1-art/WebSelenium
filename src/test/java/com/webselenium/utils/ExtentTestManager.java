package com.webselenium.utils;

import com.webselenium.base.BaseTest;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.ConcurrentHashMap;

public class ExtentTestManager {

    private static final ConcurrentHashMap<Long, ExtentTest> extentTestMap =
            new ConcurrentHashMap<>();

    private static final ExtentReports extent =
            ExtentManager.getExtentReports();

    public static ExtentTest getExtentTest() {
        return extentTestMap.get(Thread.currentThread().getId());
    }

    public static synchronized ExtentTest saveToReport(String testName, String desc) {
        ExtentTest test = extent.createTest(testName, desc);
        extentTestMap.put(Thread.currentThread().getId(), test);
        return test;
    }

    public static void logMessage(String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }

    public static void logMessage(Status status, String message) {
        ExtentTest test = getExtentTest();
        if (test != null) {
            test.log(status, message);
        }
    }

    // ✅ BEST METHOD - Use BASE64 (Embedded Image - GUARANTEED TO WORK)
    public static void addScreenshotBase64(Status status, String message, WebDriver driver) {
        ExtentTest test = getExtentTest();
        if (test == null) return;

        if (driver != null) {
            try {
                String base64 = ((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.BASE64);

                test.log(
                        status,
                        message,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(
                                "data:image/png;base64," + base64
                        ).build()
                );
            } catch (Exception e) {
                test.log(status, message + " (Screenshot error: " + e.getMessage() + ")");
            }
        } else {
            test.log(status, message + " (Driver is NULL)");
        }
    }

    // Convert file to Base64
    private static String convertImageToBase64(String imagePath) {
        try {
            byte[] fileContent = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(imagePath));
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert image to Base64: " + imagePath, e);
        }
    }
}