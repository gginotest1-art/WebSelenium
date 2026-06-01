package com.webselenium.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {

    private static ExtentReports extent;

    public static synchronized ExtentReports getExtentReports() {

        if (extent == null) {

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());

            String reportDir = System.getProperty("user.dir")
                    + "/test-output/ExtentReports/";

            String reportPath = reportDir + "ExtentReport_" + timestamp + ".html";

            File dir = new File(reportDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

            spark.config().setReportName("Selenium Automation Report");
            spark.config().setDocumentTitle("Automation Test Results");
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setEncoding("utf-8");

            // ✅ KEY CONFIG - Enable inline images instead of path references
            spark.config().setOfflineMode(false);

            extent = new ExtentReports();
            extent.attachReporter(spark);

            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
            extent.setSystemInfo("Framework", "Selenium + TestNG");
        }

        return extent;
    }
}