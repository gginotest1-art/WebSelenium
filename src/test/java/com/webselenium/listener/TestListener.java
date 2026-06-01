
package com.webselenium.listener;

import com.aventstack.extentreports.Status;
import com.webselenium.base.BaseTest;
import com.webselenium.utils.ExtentLogger;
import com.webselenium.utils.ExtentManager;
import com.webselenium.utils.ExtentTestManager;
import com.webselenium.helpers.LogUtils;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    private String getTestName(ITestResult result) {
        return result.getTestName() != null
                ? result.getTestName()
                : result.getMethod().getMethodName();
    }

    private String getTestDescription(ITestResult result) {
        return result.getMethod().getDescription() != null
                ? result.getMethod().getDescription()
                : getTestName(result);
    }

    @Override
    public void onStart(ITestContext context) {
        // If suite parameter "env" provided in testng.xml, set it as system property
        String envFromSuite = context.getSuite().getParameter("env");
        if (envFromSuite != null && !envFromSuite.isBlank()) {
            System.setProperty("env", envFromSuite);
            System.out.println("✓ System property 'env' set from suite parameter: " + envFromSuite);
        }

        ExtentManager.getExtentReports();
        LogUtils.info("===== START SUITE: " + context.getName() + " =====");
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentManager.getExtentReports().flush();
        LogUtils.info("===== END SUITE: " + context.getName() + " =====");
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentLogger.reset();
        ExtentTestManager.saveToReport(
                getTestName(result),
                getTestDescription(result)
        );
        LogUtils.info(">>> START TEST: " + result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentLogger.logAllStepsPass();
//        ExtentLogger.pass("TEST PASSED: " + result.getName());
        LogUtils.info(">>> PASSED: " + result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable error = result.getThrowable();
        String msg = (error != null) ? error.getMessage() : "Unknown error";

        WebDriver driver = BaseTest.getDriver();

        ExtentLogger.logFailure(msg, driver);

        LogUtils.error(">>> FAILED: " + result.getName() + " | " + msg);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String reason = result.getThrowable() != null
                ? result.getThrowable().toString()
                : "Skipped without reason";

        ExtentLogger.skip("TEST SKIPPED: " + result.getName());

        WebDriver driver = BaseTest.getDriver();
        // attach a screenshot (base64) for skipped if driver present
        ExtentTestManager.addScreenshotBase64(Status.SKIP, result.getName() + " | " + reason, driver);

        LogUtils.warn(">>> SKIPPED: " + result.getName() + " | " + reason);
    }
}