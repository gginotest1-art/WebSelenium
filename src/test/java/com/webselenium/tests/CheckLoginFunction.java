package com.webselenium.tests;

import com.webselenium.base.BaseTest;
import com.webselenium.dataconstants.DataConstants;
import com.webselenium.helpers.UserCredentialsReader;
import com.webselenium.helpers.UserCredentialsReader.UserData;
import com.webselenium.utils.ExtentLogger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class CheckLoginFunction extends BaseTest {

    /**
     * Auto-discover từ users.yaml — mọi user có active != false sẽ được test.
     * Add/remove user = chỉnh users.yaml, không cần đụng class này.
     */
    @DataProvider(name = "validUsers")
    public Object[][] validUsers() {
        List<UserData> users = UserCredentialsReader.allActive();
        Object[][] data = new Object[users.size()][1];
        for (int i = 0; i < users.size(); i++) data[i][0] = users.get(i);
        return data;
    }

    @Test(
            description = "Login with all active users defined in users.yaml",
            groups = "regression",
            dataProvider = "validUsers"
    )
    public void testLoginWithValidUser(UserData user) {
        ExtentLogger.step("Login as " + user.type + " (role=" + user.role + ")");
        homePage.loginWithUserData(DataConstants.LOGIN_BUTTON_TEXT, user);

        ExtentLogger.step("Verify login successfully");
        homePage.isLoginSuccessful();
    }
}