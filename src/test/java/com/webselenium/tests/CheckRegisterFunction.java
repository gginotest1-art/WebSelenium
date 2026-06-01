package com.webselenium.tests;

import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.utils.ExtentLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CheckRegisterFunction extends BaseTest {

    @Test(description = "Register a new user with random credentials", groups = "regression")
    public void testRegisterNewUser() {
        ExtentLogger.step("Close ads");
        ExtentLogger.step("Click register button");
        homePage.register(DataConstants.REGISTER_BUTTON_TEXT);

        ExtentLogger.step("Verify register successfully");
        Assert.assertTrue(
                homePage.isRegisterSuccessful(),
                "Balance text not displayed after registration"
        );
    }
}