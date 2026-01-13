package reports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.*;
// 1. CHANGED: Using Edge instead of Chrome
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import org.testng.*;
import org.testng.annotations.*;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.MediaEntityBuilder;

public class Project_Reports {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    protected ExtentReports extent;
    protected ExtentTest test;
    protected String reportDir;

    // ================= SETUP =================

    @BeforeSuite
    public void setupReport() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        reportDir = System.getProperty("user.dir") + "/Reports/ProjectCreation_" + timestamp;
        new File(reportDir + "/screenshots").mkdirs();

        ExtentSparkReporter reporter = new ExtentSparkReporter(reportDir + "/ProjectCreationReport.html");
        reporter.config().setDocumentTitle("ResIQ Automation Report");
        reporter.config().setReportName("Project Creation Automation");

        extent = new ExtentReports();
        extent.attachReporter(reporter);
        extent.setSystemInfo("Tester", "Varad");
        extent.setSystemInfo("Environment", "QA");
        extent.setSystemInfo("Browser", "Microsoft Edge");
    }

    @BeforeClass
    public void setupBrowser() {
        // 2. CHANGED: Setup Offline Edge Driver
        setupOfflineDriver();

        // 3. CHANGED: Configure Edge Options
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");

        // 4. CHANGED: Initialize Edge Driver
        driver = new EdgeDriver(options);
        
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    // ================= OFFLINE DRIVER LOGIC (EDGE) =================
    
    public void setupOfflineDriver() {
        try {
            // Look for msedgedriver.exe in the 'drivers' folder inside resources
            InputStream is = getClass().getResourceAsStream("/drivers/msedgedriver.exe");
            
            // Fallback for non-standard folder structures
            if (is == null) is = getClass().getResourceAsStream("/reports/msedgedriver.exe");
            if (is == null) is = getClass().getResourceAsStream("/msedgedriver.exe");

            if (is == null) {
                // If checking local file system (Eclipse debugging)
                File localFile = new File("src/main/resources/drivers/msedgedriver.exe");
                if (localFile.exists()) {
                    System.setProperty("webdriver.edge.driver", localFile.getAbsolutePath());
                    return;
                }
                System.err.println("CRITICAL: msedgedriver.exe not found in resources!");
                return;
            }

            // Create a temporary file on the Windows PC to run the driver
            File tempDriver = File.createTempFile("msedgedriver_bundled", ".exe");
            tempDriver.deleteOnExit(); 

            // Copy the driver from inside the EXE/JAR to the temp file
            Files.copy(is, tempDriver.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Tell Selenium to use this temp path
            System.setProperty("webdriver.edge.driver", tempDriver.getAbsolutePath());
            tempDriver.setExecutable(true);

        } catch (Exception e) {
            System.err.println("Failed to extract Edge driver: " + e.getMessage());
        }
    }

    // ================= CLEANUP =================

    @AfterMethod
    public void captureAfterEach(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                String path = takeScreenshot(result.getName() + "_FAIL");
                if (test != null) test.fail(result.getThrowable(), MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                 if (test != null) test.pass("Test passed");
            }
        } catch (Exception e) {
            System.err.println("Screenshot issue: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        if (extent != null) {
            extent.flush();
        }
    }

    // ================= SCREENSHOT =================

    public String takeScreenshot(String name) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String path = reportDir + "/screenshots/" + name + ".png";
            File src = ts.getScreenshotAs(OutputType.FILE);
            File dest = new File(path);
            dest.getParentFile().mkdirs();
            org.openqa.selenium.io.FileHandler.copy(src, dest);
            return path;
        } catch (IOException e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    // ================= ACTION METHODS =================

    public static void clickSidebarItem(WebDriver driver, String menuName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> menuItems = driver.findElements(By.xpath("//ul[@data-sidebar='menu-sub']//span | //ul[@data-sidebar='menu']//span"));
        
        for (WebElement item : menuItems) {
            if (item.getText().trim().equalsIgnoreCase(menuName)) {
                wait.until(ExpectedConditions.elementToBeClickable(item));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", item.findElement(By.xpath("./ancestor::a | ./ancestor::button")));
                break;
            }
        }
    }
    
    public static void addProj(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(By.cssSelector("#add-project-btn"))).click();
    }
    
    public static void clckClient(WebDriver driver) {
        driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Client --']]")).click();
    }
    
    public static void searClient(WebDriver driver, String clientName) {
        WebElement el = driver.findElement(By.xpath("//input[@placeholder='Search...']"));
        el.sendKeys(clientName, Keys.TAB, Keys.ENTER);
    }
    
    public static void fillProjName(WebDriver driver, String projName) {
        driver.findElement(By.xpath("(//input[@id='search-button'])[2]")).sendKeys(projName, Keys.TAB, Keys.ENTER);
    }

    public static void selectCustomDropdown(WebDriver driver, String optionText) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class,'max-h-[300px]')]//button[span[text()='" + optionText + "']]"))).click();
    }

    public static void clckPart(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Partner --']]")).click(); }
    public static void clckProjCat(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Category --']]")).click(); }
    public static void clckProjMang(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Manager --']]")).click(); }
    public static void searchManager(WebDriver driver, String managerName) { driver.findElement(By.xpath("//input[@placeholder='Search...']")).sendKeys(managerName, Keys.TAB, Keys.ENTER); }
    public static void clickPractice(WebDriver driver) { driver.findElement(By.xpath("(//div[@role='combobox'])[2]")).click(); }
    public static void searPractice(WebDriver driver, String practiceName) { driver.findElement(By.xpath("//input[@placeholder='Search...']")).sendKeys(practiceName, Keys.TAB, Keys.TAB, Keys.ENTER); }
    public static void clearPractice(WebDriver driver) { 
        WebElement searchBox = driver.findElement(By.xpath("//input[@placeholder='Search...']"));
        new Actions(driver).moveToElement(searchBox).click().perform();
        searchBox.clear();
    }
    public static void clckProjType(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Type --']]")).click(); }
    public static void clickTech(WebDriver driver) { driver.findElement(By.xpath("(//div[@role='combobox'])[3]")).click(); }
    
    public static void clickHours(WebDriver driver, String hours) {
        WebElement hour = driver.findElement(By.xpath("(//input[@data-slot='input'])[3]"));
        hour.click();
        hour.sendKeys(Keys.ARROW_LEFT, hours);
    }
    
    public static void sendate(WebDriver driver) { driver.findElement(By.xpath("(//input[@data-slot='input'])[4]")).sendKeys("01/12/2025"); }
    public static void endate(WebDriver driver) { driver.findElement(By.xpath("(//input[@data-slot='input'])[5]")).sendKeys("01/12/2026"); }

    public static void selectAvailableOption(WebDriver driver, String choice) {
        choice = choice.trim().toLowerCase();
        if (choice.equals("yes") || choice.equals("no")) {
            driver.findElement(By.xpath("//label[contains(.,'" + choice.substring(0, 1).toUpperCase() + choice.substring(1) + "')]//input")).click();
        }
    }

    public static void setActiveCheckbox(WebDriver driver, String choice) {
        WebElement checkbox = driver.findElement(By.id("active-checkbox"));
        if ((choice.equalsIgnoreCase("yes") && !checkbox.isSelected()) || (choice.equalsIgnoreCase("no") && checkbox.isSelected())) {
            checkbox.click();
        }
    }

    public static void clickcreate(WebDriver driver) {
        driver.findElement(By.id("save-project-btn")).click();
    }

    public static void popupDisplay(WebDriver driver) { }

    // ================= VALIDATION LOGIC =================

    public boolean catchvalidation(WebDriver driver, ExtentTest test) {
        List<WebElement> list = driver.findElements(By.cssSelector(".text-xs.text-red-500.mt-1"));

        if (list.isEmpty()) {
            return false;
        }

        test.info("Validation messages displayed:");
        boolean hasText = false;
        for (WebElement ele : list) {
            String text = ele.getText().trim();
            if (!text.isEmpty()) {
                test.info("🔴 " + text);
                hasText = true;
            }
        }
        
        if (!hasText) return false;

        String path = takeScreenshot("Validation_Error");
        if (path != null) {
            test.info("Validation screenshot attached", MediaEntityBuilder.createScreenCaptureFromPath(path).build());
        }
        return true;
    }
    
    public void captureMuiAlertAndValidate(ExtentTest test) { }
}