package com.browserstack.assignment;

import com.browserstack.assignment.DTO.Article;
import com.browserstack.assignment.PageObjects.ElPaisPage;
import com.browserstack.assignment.Utils.TranslationService;
import com.browserstack.assignment.Utils.WordAnalyzer;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElPaisTest {
    private WebDriver driver;

    @BeforeMethod
    @Parameters({ "browser", "os", "os_version", "device", "browser_version", "real_mobile" })
    public void setup(@Optional String browser, @Optional String os, @Optional String osVersion,
            @Optional String device, @Optional String browserVersion,
            @Optional String realMobile) throws MalformedURLException {
        if (browser != null || device != null) {
            // BrowserStack Execution
            String username = System.getenv("BROWSERSTACK_USERNAME");
            String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");

            if (username == null || accessKey == null) {
                // Fallback to properties or throw exception if env vars are missing, we might
                // want to warn or fail
                username = System.getProperty("browserstack.username", username);
                accessKey = System.getProperty("browserstack.accessKey", accessKey);
            }

            if (username == null || accessKey == null) {
                throw new RuntimeException(
                        "BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY must be set in environment variables or system properties.");
            }

            MutableCapabilities capabilities = new MutableCapabilities();
            HashMap<String, Object> bstackOptions = new HashMap<>();
            bstackOptions.put("userName", username);
            bstackOptions.put("accessKey", accessKey);
            bstackOptions.put("projectName", "El Pais Assignment");
            bstackOptions.put("buildName", "Build 1.0");

            if (device != null) {
                bstackOptions.put("deviceName", device);
                if (realMobile != null) {
                    bstackOptions.put("realMobile", realMobile);
                }
            }

            if (os != null)
                bstackOptions.put("os", os);
            if (osVersion != null)
                bstackOptions.put("osVersion", osVersion);

            if (browser != null)
                capabilities.setCapability("browserName", browser);
            if (browserVersion != null)
                capabilities.setCapability("browserVersion", browserVersion);

            capabilities.setCapability("bstack:options", bstackOptions);

            driver = new RemoteWebDriver(new URL("https://hub-cloud.browserstack.com/wd/hub"), capabilities);
        } else {
            // Local Execution
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);
        }
        try {
            driver.manage().window().maximize();
        } catch (Exception e) {
            System.out.println("Could not maximize window (likely mobile device): " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown(org.testng.ITestResult result) {
        if (driver != null) {
            String status = "passed";
            String reason = "Test Passed";

            if (result.getStatus() == org.testng.ITestResult.FAILURE) {
                status = "failed";
                reason = result.getThrowable().getMessage();
            }

            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \""
                                + status + "\", \"reason\": \"" + reason + "\"}}");
            } catch (Exception e) {
                // Ignore if not running on BrowserStack or driver is already closed
            }
            driver.quit();
        }
    }

    @Test
    public void testScrapeAndAnalyze() {
        ElPaisPage page = new ElPaisPage(driver);

        // 1. Visit El Pais
        page.navigateToHomePage();
        // page.ensureSpanishLanguage(); // Optional

        // 2. Navigate to Opinion
        page.navigateToOpinionSection();

        // 3. Scrape Articles
        List<Article> articles = page.getArticles(5);

        // QUIT DRIVER HERE to prevent BrowserStack Idle Timeout
        // The subsequent steps (Image Download, Translation) are local and do not
        // require the browser.
        if (driver != null) {
            driver.quit();
            driver = null; // Prevent tearDown from trying to quit again
        }

        if (articles.isEmpty()) {
            System.err.println("WARNING: No articles found. Check selectors for this device/view.");
        }

        System.out.println("--- Scraped Articles ---");
        for (Article art : articles) {
            System.out.println("Title: " + art.getTitle());
            System.out.println("Content: " + art.getContent());
        }

        // 4. Download Images
        // Only download if we are running locally? Or always?
        // If remote, saving to local disk works if we use bytes, but we are using URLs.
        // The code downloads to local machine where code is running.
        page.downloadImages(articles);

        // 5. Translate Headers
        TranslationService translator = new TranslationService("");
        List<String> translatedHeaders = new ArrayList<>();

        System.out.println("--- Translated Headers ---");
        for (Article art : articles) {
            String translated = translator.translate(art.getTitle(), "en");
            art.setTranslatedTitle(translated);
            translatedHeaders.add(translated);
            System.out.println("Original: " + art.getTitle() + " -> Translated: " + translated);
        }

        // 6. Analyze Headers
        Map<String, Integer> repeatedWords = WordAnalyzer.analyzeRepeatedWords(translatedHeaders);
        System.out.println("--- Repeated Words (>2 occurrences) ---");
        repeatedWords.forEach((word, count) -> System.out.println(word + ": " + count));

        Assert.assertFalse(articles.isEmpty(), "Zero articles scrapped!");
    }
}
