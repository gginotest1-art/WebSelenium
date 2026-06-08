package com.webselenium.pages;

import com.webselenium.base.BasePage;
import com.webselenium.base.DynamicLocator;
import com.webselenium.helpers.TestDataHelper;
import com.webselenium.helpers.UserCredentialsReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

public class HomePage extends BasePage {

    private static final By CLOSE_ADS_BTN = By.xpath("//div[4]/span/*[local-name()='svg']");
    private static final DynamicLocator LOGIN_BTN = DynamicLocator.xpath("//button[@type='button' and text()='%s']");
    private static final By USERNAME_INPUT = By.xpath("//input[@id='username']");
    private static final By PASSWORD_INPUT = By.xpath("//input[@id='password']");
    private static final By PHONE_INPUT = By.xpath("//input[@id='phone']");
    private static final By BALANCE_TEXT = By.xpath("//span[contains(@class,'text-white') and contains(text(),'K')]");
    private static final By SUBMIT_BTN = By.xpath("//button[@type='submit']");
    private static final By ACCOUNT_INFO_BTN = By.xpath("//a[@href='/user/dashboard']");
    private static final By DEPOSIT_BTN = By.xpath("//a[@href='/user/dashboard']/preceding-sibling::a");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    private void openAuthModal(String authButtonText) {
        waitForClickable(CLOSE_ADS_BTN);
        click(CLOSE_ADS_BTN);

        waitForClickable(LOGIN_BTN, authButtonText);
        click(LOGIN_BTN, authButtonText);
    }

    private void submitForm() {
        waitForClickable(SUBMIT_BTN);
        click(SUBMIT_BTN);
    }

    public void login(String btnLogin, String username, String password) {
        openAuthModal(btnLogin);
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        submitForm();
    }

    public void loginWithUserData(String btnLogin, UserCredentialsReader.UserData userData) {
        login(btnLogin, userData.username, userData.password);
    }

    public void register(String btnRegister) {
        openAuthModal(btnRegister);
        type(USERNAME_INPUT, TestDataHelper.randomUsername());
        type(PASSWORD_INPUT, TestDataHelper.randomPassword());
        type(PHONE_INPUT, TestDataHelper.randomPhone10());
        submitForm();
    }

    public boolean isLoginSuccessful() {
        waitForVisible(BALANCE_TEXT);
        Assert.assertTrue(
                isDisplayed(BALANCE_TEXT),
                "Balance text not displayed after login"
        );
        return isDisplayed(BALANCE_TEXT);
    }

    public boolean isRegisterSuccessful() {
        waitForVisible(BALANCE_TEXT);
        return isDisplayed(BALANCE_TEXT);
    }

    public void clickAccountInfo() {
        waitForClickable(ACCOUNT_INFO_BTN);
        click(ACCOUNT_INFO_BTN);
    }
}