package com.webselenium.tests;

import com.aventstack.extentreports.Status;
import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.GameReportExporter;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.models.GameCheckResult;
import com.webselenium.utils.ExtentLogger;
import com.webselenium.utils.ExtentTestManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class CheckAllGamesPlayable extends BaseTest {

    @Test(description = "Scan tất cả game trong các lobby/provider, verify chơi được",
            groups = "smoke",
            timeOut = 60 * 60 * 1000)
    public void testAllGamesPlayable() {
//        ExtentLogger.step("Login as market4");
//        UserCredentialsReader.UserData market4 = DataConstants.getMarket4User();
//        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, market4);
//        Assert.assertTrue(homePage.isLoginSuccessful(), "Login failed");

        ExtentLogger.step("Scan tất cả game");
        List<GameCheckResult> results = gameLobbyPage.scanAllGames();

        long failedCount = results.stream().filter(r -> !r.playable).count();
        long passedCount = results.size() - failedCount;
        ExtentLogger.step("Total: " + results.size() + " | PASS: " + passedCount + " | FAIL: " + failedCount);

        // Log từng game riêng vào ExtentReport
        for (GameCheckResult r : results) {
            if (r.playable) {
                ExtentLogger.pass(r.toString());
            } else {
                ExtentLogger.fail(r.toString());
            }
        }

        // Nhúng bảng tổng hợp HTML vào ExtentReport
        if (ExtentTestManager.getExtentTest() != null) {
            String html = GameReportExporter.buildExtentHtmlTable(results);
            ExtentTestManager.getExtentTest().log(
                    failedCount > 0 ? Status.FAIL : Status.PASS,
                    "Game Scan Summary Table:" + html
            );
        }

        // Xuất Excel
        String excelPath = GameReportExporter.exportToExcel(
                results,
                System.getProperty("user.dir") + "/test-output/excel"
        );
        if (excelPath != null && ExtentTestManager.getExtentTest() != null) {
            ExtentTestManager.getExtentTest().log(Status.INFO, "Excel report: " + excelPath);
        }

        List<GameCheckResult> failed = results.stream().filter(r -> !r.playable).toList();
        Assert.assertTrue(
                failed.isEmpty(),
                "Có " + failed.size() + " game không chơi được. Xem chi tiết trong ExtentReport hoặc " + excelPath
        );
    }
}