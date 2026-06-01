package com.webselenium.tests;

import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.utils.ExtentLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class CheckAccountInfo extends BaseTest {

    @Test(description = "Verify account information and sidebar buttons", groups = "regression")
    public void testAccountInfoAndSidebar() {
        ExtentLogger.step("Close ads");

        ExtentLogger.step("Click login button");
        UserCredentialsReader.UserData market4 = DataConstants.getMarket4User();
        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, market4);

        ExtentLogger.step("Navigate to account information page");
        homePage.clickAccountInfo();

        ExtentLogger.step("Verify account information displayed correctly");
        List<String> missing = accountPage.getMissingSidebarButtons();
        Assert.assertTrue(
                missing.isEmpty(),
                "Sidebar buttons not displayed: " + missing
        );
    }
}
