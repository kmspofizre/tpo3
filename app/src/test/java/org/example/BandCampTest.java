package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BandCampTest {

    private WebDriver driver;
    private WebDriverWait wait;
    
    private final String albumUrl = "https://alternativenostalgie63.bandcamp.com/album/live-in-texas";

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private void jsClick(WebElement element) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        executor.executeScript("arguments[0].click();", element);
    }

    private void handleCookies() {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(4));
        try {
            WebElement acceptButton = shortWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'accept all')] | //button[@id='bcc-accept-all']")
            ));
            jsClick(acceptButton);
            Thread.sleep(1000); 
        } catch (Exception e) {
            System.out.println("нет куков");
        } finally {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "document.querySelectorAll('#dialog-teleport, #toast-teleport, .bcc-privacy-settings-mask, [id^=\"onetrust\"], .cookie-banner').forEach(el => el.remove());" +
                "document.body.classList.remove('no-scroll');" +
                "document.documentElement.style.overflow = 'auto';"
            );
        }
    }

    @Test
    public void testPlayTrackOnAlbumPage() throws InterruptedException {
        driver.get(albumUrl);
        handleCookies();

        WebElement playButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".inline_player .playbutton")
        ));
        
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", playButton);
        Thread.sleep(500); 

    
        jsClick(playButton);

        boolean isResponding = wait.until(ExpectedConditions.or(
                ExpectedConditions.attributeContains(playButton, "class", "busy"),
                ExpectedConditions.attributeContains(playButton, "class", "playing"),
                ExpectedConditions.attributeContains(By.cssSelector(".inline_player"), "class", "busy"),
                ExpectedConditions.attributeContains(By.cssSelector(".inline_player"), "class", "playing")
        ));

        assertTrue(isResponding, "Плеер не отреагировал");
    }

    @Test
    public void testAlbumTitleAndArtistVisible() {
        driver.get(albumUrl);
        handleCookies();

        WebElement albumTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h2.trackTitle")
        ));
        
        WebElement artistName = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#name-section h3 a")
        ));

        String actualAlbum = albumTitle.getText().toLowerCase().trim();
        String actualArtist = artistName.getText().toLowerCase().trim();

        assertTrue(actualAlbum.contains("live in texas"), 
                "Название альбома не совпадает. Получили: " + actualAlbum);
        
        assertTrue(actualArtist.contains("linkin park") || actualArtist.contains("alternative"), 
                "Имя исполнителя не совпадает. Получили: " + actualArtist);
    }

    @Test
    public void testTrackListIsNotEmpty() {
        driver.get(albumUrl);
        handleCookies();

        List<WebElement> tracks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("table#track_table tr.track_row_view")
        ));

        assertTrue(tracks.size() > 0, "Список треков альбома пуст");
        assertTrue(tracks.size() == 17, "Количество треков не совпадает. Найдено: " + tracks.size());
    }

    @Test
    public void testNavigationToHomeViaLogo() {
        driver.get(albumUrl);
        handleCookies();

        WebElement logo = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("li[data-test='mb-bandcamp-logo'] a")
        ));
        
        jsClick(logo);

        wait.until(ExpectedConditions.and(
                ExpectedConditions.urlContains("bandcamp.com"),
                ExpectedConditions.not(ExpectedConditions.urlContains("album/live-in-texas"))
        ));

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("bandcamp.com"), "Не удалось перейти на main");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}