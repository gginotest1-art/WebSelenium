package com.webselenium.pages;

import com.webselenium.base.BasePage;
import com.webselenium.base.DynamicLocator;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;


public class AccountPage extends BasePage {

    private static final DynamicLocator SIDEBAR_BTN = DynamicLocator.xpath("//span[contains(text(),'%s')]");

    private static final String[] EXPECTED_SIDEBAR_BUTTONS = {
            "Nạp tiền",
            "Rút tiền",
            "Tài khoản ngân hàng",
            "Lịch Sử Cược/Giao Dịch"
    };

    public AccountPage(WebDriver driver) {
        super(driver);
    }

    public List<String> getMissingSidebarButtons() {
        List<String> missing = new ArrayList<>();
        for (String buttonName : EXPECTED_SIDEBAR_BUTTONS) {
            try {
                waitForVisible(SIDEBAR_BTN, buttonName);
                if (!isDisplayed(SIDEBAR_BTN, buttonName)) {
                    missing.add(buttonName);
                }
            } catch (Exception e) {
                missing.add(buttonName);
            }
        }
        return missing;
    }
}