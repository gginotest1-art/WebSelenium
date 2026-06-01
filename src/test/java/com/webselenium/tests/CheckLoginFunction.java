package com.webselenium.tests;

import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.utils.ExtentLogger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CheckLoginFunction extends BaseTest {

    @DataProvider(name = "validUsers")
    public Object[][] validUsers() {
        return new Object[][]{
                {"market4", DataConstants.getMarket4User()},
                {"admin",   DataConstants.getAdminUser()},
                {"vip",     DataConstants.getVipUser()}
        };
    }

    @Test(
            description = "Login with valid user types",
            groups = "regression",
            dataProvider = "validUsers"
    )
    public void testLoginWithValidUser(String userType, UserCredentialsReader.UserData user) {
        ExtentLogger.step("Close ads");
        ExtentLogger.step("Click login button as " + userType);
        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, user);

        ExtentLogger.step("Verify login successfully");
        Assert.assertTrue(
                homePage.isLoginSuccessful(),
                "Balance text not displayed after login as " + userType
        );
    }
}