package com.webselenium.base;

import com.webselenium.pages.AccountPage;
import com.webselenium.pages.GameLobbyPage;
import com.webselenium.pages.HomePage;
import com.webselenium.helpers.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

public class BaseTest {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    protected HomePage homePage;
    protected AccountPage accountPage;
    protected GameLobbyPage gameLobbyPage;

    @BeforeMethod
    public void setUp() {
        // Load config dựa trên environment
        String browser = ConfigReader.getBrowser();
        WebDriver webDriver = initDriver(browser);

        webDriver.manage().window().maximize();
        webDriver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(ConfigReader.getImplicitWait()))
                .pageLoadTimeout(Duration.ofSeconds(ConfigReader.getPageLoadTimeout()));

        driver.set(webDriver);

        // Navigate to base URL
        getDriver().get(ConfigReader.getBaseUrl());

        homePage = new HomePage(getDriver());
        accountPage = new AccountPage(getDriver());
        gameLobbyPage = new GameLobbyPage(getDriver());

        System.out.println("✓ Browser initialized: " + browser);
        System.out.println("✓ URL: " + ConfigReader.getBaseUrl());
    }

    private WebDriver initDriver(String browserName) {
        return switch (browserName.toLowerCase()) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (ConfigReader.isHeadless()) {
                    options.addArguments("--headless");
                }
                yield new FirefoxDriver(options);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver();
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(getChromeOptions());
            }
        };
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        if (ConfigReader.isHeadless()) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
        }

        // Window size
        options.addArguments(
                "--window-size=" + ConfigReader.getBrowserWidth() +
                "," + ConfigReader.getBrowserHeight()
        );

        // Disable notifications
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");

        return options;
    }

    public static WebDriver getDriver() {
        WebDriver webDriver = driver.get();
        if (webDriver == null) {
            throw new IllegalStateException(
                    "WebDriver chưa được khởi tạo trên thread '" + Thread.currentThread().getName()
                            + "'. Hãy chắc rằng setUp() (@BeforeMethod) đã chạy."
            );
        }
        return webDriver;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver webDriver = driver.get();
        if (webDriver != null) {
            webDriver.quit();
            driver.remove();
            System.out.println("✓ Browser closed");
        }
    }
}