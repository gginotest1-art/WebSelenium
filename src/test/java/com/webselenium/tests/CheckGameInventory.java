package com.webselenium.tests;

import com.aventstack.extentreports.Status;
import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.GameReportExporter;
import com.webselenium.helpers.LogUtils;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.models.DuplicateGame;
import com.webselenium.models.GameInventoryItem;
import com.webselenium.utils.ExtentLogger;
import com.webselenium.utils.ExtentTestManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class CheckGameInventory extends BaseTest {

    @Test(description = "Quét toàn bộ game qua lobby CỔNG GAME (chứa tất cả game) và xuất Excel",
            groups = "smoke",
            timeOut = 15 * 60 * 1000)
    public void testInventoryAllGames() {
//        ExtentLogger.step("Login as market4");
//        UserCredentialsReader.UserData market4 = DataConstants.getMarket4User();
//        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, market4);
//        Assert.assertTrue(homePage.isLoginSuccessful(), "Login failed");

        ExtentLogger.step("Inventory game portal (CỔNG GAME + Xem thêm)");
        List<GameInventoryItem> items = new ArrayList<>();
        Throwable scanError = null;
        try {
            items = gameLobbyPage.inventoryGamePortal();
        } catch (Throwable t) {
            scanError = t;
            LogUtils.error("Inventory scan crashed: " + t.getMessage(), t);
        }

        int originalCount = items.size();
        List<DuplicateGame> duplicates = GameReportExporter.findDuplicates(items);
        items = GameReportExporter.dedupeByName(items);
        int dupes = originalCount - items.size();
        ExtentLogger.step("Total: " + originalCount + " → " + items.size()
                + " (dedupe -" + dupes + " | " + duplicates.size() + " tên bị trùng)");

        // Log từng game vào ExtentReport: nếu >200 game thì chỉ log 100 đầu + 10 cuối + count, tránh bloat
        int n = items.size();
        if (n <= 200) {
            for (GameInventoryItem g : items) ExtentLogger.pass(g.toString());
        } else {
            for (int i = 0; i < 100; i++) ExtentLogger.pass(items.get(i).toString());
            ExtentLogger.pass("... (" + (n - 110) + " games khác, xem Excel để đầy đủ) ...");
            for (int i = n - 10; i < n; i++) ExtentLogger.pass(items.get(i).toString());
        }

        // Bảng tổng hợp HTML trong ExtentReport
        if (ExtentTestManager.getExtentTest() != null && !items.isEmpty()) {
            String providerHtml = GameReportExporter.buildProviderSummaryHtml(items);
            ExtentTestManager.getExtentTest().log(Status.INFO, "Game by Provider:" + providerHtml);
            if (!duplicates.isEmpty()) {
                String dupHtml = GameReportExporter.buildDuplicatesHtml(duplicates);
                ExtentTestManager.getExtentTest().log(Status.WARNING,
                        "Duplicate Games (" + duplicates.size() + " tên trùng):" + dupHtml);
            }
            String html = GameReportExporter.buildInventoryHtmlTable(items);
            ExtentTestManager.getExtentTest().log(Status.INFO, "Full Game Inventory:" + html);
        }

        // Xuất Excel (kèm sheet Duplicates)
        String excelPath = GameReportExporter.exportInventoryToExcel(
                items,
                duplicates,
                System.getProperty("user.dir") + "/test-output/excel"
        );
        if (excelPath != null && ExtentTestManager.getExtentTest() != null) {
            ExtentTestManager.getExtentTest().log(Status.INFO, "Excel inventory: " + excelPath);
        }

        if (scanError != null) {
            Assert.fail("Inventory bị crash giữa chừng: " + scanError.getMessage()
                    + " | Đã capture " + items.size() + " game | Excel: " + excelPath);
        }
        Assert.assertFalse(items.isEmpty(), "Không tìm thấy game nào!");
    }
}
