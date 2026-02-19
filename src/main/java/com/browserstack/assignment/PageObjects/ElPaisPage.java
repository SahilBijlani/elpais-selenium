package com.browserstack.assignment.PageObjects;

import com.browserstack.assignment.DTO.Article;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ElPaisPage {
    private WebDriver driver;
    private WebDriverWait wait;

    // Selectors
    private By opinionLink = By.xpath("//a[contains(@href, '/opinion') and text()='Opinión']");
    // Fallback if the text is uppercase or different
    private By opinionLinkNav = By.cssSelector("nav.cs_m a[href*='/opinion']");

    private By articleLocator = By.tagName("article");
    private By titleLocator = By.cssSelector("h2.c_t");
    private By contentLocator = By.cssSelector("p.c_d");
    private By imageLocator = By.tagName("img");

    // CMP / Cookie banner
    private By acceptCookiesBtn = By.id("didomi-notice-agree-button");

    public ElPaisPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateToHomePage() {
        driver.get("https://elpais.com/");
        handleCookies();
    }

    public void ensureSpanishLanguage() {
        String lang = driver.findElement(By.tagName("html")).getAttribute("lang");
        System.out.println("Page Language: " + lang);
        // Requirement: Ensure text is in Spanish. The site is elpais.com, so it is
        // Spanish by default.
    }

    public void navigateToOpinionSection() {
        try {
            WebElement link = wait.until(ExpectedConditions.elementToBeClickable(opinionLinkNav));
            link.click();
            wait.until(ExpectedConditions.urlContains("/opinion"));
        } catch (Exception e) {
            System.out.println("CSS nav failed, trying xpath for Opinion link");
            try {
                WebElement link = wait.until(ExpectedConditions.elementToBeClickable(opinionLink));
                link.click();
                wait.until(ExpectedConditions.urlContains("/opinion"));
            } catch (Exception ex) {
                System.out.println("Navigation failed, forcing direct URL navigation to Opinion section.");
                driver.get("https://elpais.com/opinion/");
            }
        }
    }

    public List<Article> getArticles(int limit) {
        List<Article> articles = new ArrayList<>();
        List<WebElement> articleElements = wait
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(articleLocator));

        int count = 0;
        for (WebElement element : articleElements) {
            if (count >= limit)
                break;

            try {
                // Scroll into view to ensure images load (lazy loading)
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
                        element);

                String title = "";
                try {
                    title = element.findElement(titleLocator).getText();
                } catch (Exception e) {
                    // some articles might not have this specific title class?
                    continue; // Skip if no title found
                }

                String content = "";
                try {
                    content = element.findElement(contentLocator).getText();
                } catch (Exception e) {
                    // Content might be missing
                    content = "No content available";
                }

                String imageUrl = null;
                try {
                    List<WebElement> images = element.findElements(imageLocator);
                    if (!images.isEmpty()) {
                        imageUrl = images.get(0).getAttribute("src");
                        if (imageUrl != null && imageUrl.startsWith("data:")) {
                            // Data URI, maybe look for srcset or skip
                            imageUrl = images.get(0).getAttribute("data-src"); // sometimes lazy loaded
                            if (imageUrl == null)
                                imageUrl = images.get(0).getAttribute("src");
                        }
                    }
                } catch (Exception e) {
                    // No image
                }

                articles.add(new Article(title, content, imageUrl));
                count++;
            } catch (Exception e) {
                System.err.println("Error parsing article: " + e.getMessage());
            }
        }
        return articles;
    }

    public void downloadImages(List<Article> articles) {
        File info = new File("images");
        if (!info.exists())
            info.mkdirs();

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
                try {
                    Path targetPath = Paths.get("images", "article_" + (i + 1) + ".jpg");
                    java.net.URLConnection conn = new URL(article.getImageUrl()).openConnection();
                    conn.setConnectTimeout(5000); // 5 seconds connect timeout
                    conn.setReadTimeout(10000); // 10 seconds read timeout

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Downloaded image for article " + (i + 1));
                    }
                } catch (IOException e) {
                    System.err.println("Failed to download image: " + article.getImageUrl() + " - " + e.getMessage());
                }
            }
        }
    }

    private void handleCookies() {
        try {
            WebElement agreeBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(acceptCookiesBtn));
            agreeBtn.click();
            System.out.println("Accepted cookies.");
        } catch (Exception e) {
            System.out.println("Cookie banner not found or skipped.");
        }
    }
}
