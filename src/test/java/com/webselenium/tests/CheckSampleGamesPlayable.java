package com.webselenium.tests;

import com.aventstack.extentreports.Status;
import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.GameReportExporter;
import com.webselenium.helpers.LogUtils;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.models.GameCheckResult;
import com.webselenium.utils.ExtentLogger;
import com.webselenium.utils.ExtentTestManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class CheckSampleGamesPlayable extends BaseTest {

    private static final int SAMPLES_PER_PROVIDER = 7;

    @Test(description = "Sample N game/provider, click vào và verify playable / phát hiện error modal",
            groups = "smoke",
            timeOut = 60 * 60 * 1000)
    public void testSampleGamesPlayable() {
//        ExtentLogger.step("Login as market4");
//        UserCredentialsReader.UserData market4 = DataConstants.getMarket4User();
//        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, market4);
//        Assert.assertTrue(homePage.isLoginSuccessful(), "Login failed");

        ExtentLogger.step("Sample " + SAMPLES_PER_PROVIDER + " game/provider trong CỔNG GAME và verify");
        List<GameCheckResult> results = new ArrayList<>();
        Throwable scanError = null;
        try {
            results = gameLobbyPage.sampleVerifyGames(SAMPLES_PER_PROVIDER);
        } catch (Throwable t) {
            scanError = t;
            LogUtils.error("Sample verify crashed: " + t.getMessage(), t);
        }

        long passCount = results.stream().filter(r -> r.playable).count();
        long failCount = results.size() - passCount;
        ExtentLogger.step("Sample verify: " + results.size() + " game | PASS: " + passCount + " | FAIL: " + failCount);

        // Log từng kết quả — fail kèm screenshot nếu có
        for (GameCheckResult r : results) {
            if (r.playable) ExtentLogger.pass(r.toString());
            else ExtentLogger.failWithScreenshotPath(r.toString(), r.screenshotPath);
        }

        // Bảng HTML tổng hợp
        if (ExtentTestManager.getExtentTest() != null && !results.isEmpty()) {
            String html = GameReportExporter.buildExtentHtmlTable(results);
            ExtentTestManager.getExtentTest().log(
                    failCount > 0 ? Status.WARNING : Status.PASS,
                    "Sample Game Verify Results:" + html
            );
        }

        // Xuất Excel
        String excelPath = GameReportExporter.exportToExcel(
                results,
                System.getProperty("user.dir") + "/test-output/excel"
        );
        if (excelPath != null && ExtentTestManager.getExtentTest() != null) {
            ExtentTestManager.getExtentTest().log(Status.INFO, "Excel: " + excelPath);
        }

        if (scanError != null) {
            Assert.fail("Sample verify crashed: " + scanError.getMessage()
                    + " | Đã capture " + results.size() + " result | Excel: " + excelPath);
        }
        Assert.assertFalse(results.isEmpty(), "Không verify được game nào!");
    }
}