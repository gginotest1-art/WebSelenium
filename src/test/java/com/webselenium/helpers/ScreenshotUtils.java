package com.webselenium.helpers;

import org.openqa.selenium.*;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = "test-output/screenshots/";

    public static String capture(WebDriver driver, String name) {
        try {
            if (driver == null) {
                System.out.println("Driver is NULL → cannot take screenshot");
                return null;
            }

            TakesScreenshot ts = (TakesScreenshot) driver;
            File src = ts.getScreenshotAs(OutputType.FILE);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fileName = name + "_" + timestamp + ".png";

            // Create directory if not exists
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // ABSOLUTE PATH - THIS IS KEY!
            File dest = new File(SCREENSHOT_DIR + fileName);
            String absolutePath = dest.getAbsolutePath();

            Files.copy(src.toPath(), dest.toPath());

            System.out.println("Screenshot saved at: " + absolutePath);

            return absolutePath;  // ✅ Return ABSOLUTE path

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}