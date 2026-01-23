package reports;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane; 

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
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
        extent.setSystemInfo("Tester", System.getProperty("user.name"));
        extent.setSystemInfo("Browser", "System Edge");
    }

    @BeforeClass
    public void setupBrowser() {
        // ============================================================
        // 1. OFFLINE FIX: Check for manual driver FIRST
        // ============================================================
        File localDriver = new File("msedgedriver.exe");
        if (localDriver.exists()) {
            System.setProperty("webdriver.edge.driver", localDriver.getAbsolutePath());
            // System.out.println("✅ Found local driver: " + localDriver.getAbsolutePath());
        } else {
            // If missing, warn the user because offline download will fail
            JOptionPane.showMessageDialog(null, 
                "⚠️ Warning: 'msedgedriver.exe' not found in this folder.\n\n" +
                "If you are OFFLINE, the app will crash because it cannot download the driver.\n" +
                "Please place the driver in the same folder as this App.", 
                "Driver Missing", JOptionPane.WARNING_MESSAGE);
        }

        // 2. Configure Edge Options
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");

        // 3. Dynamic User Profile (Finds the current user's Edge)
        String userHome = System.getProperty("user.home");
        String edgeDataPath = userHome + "\\AppData\\Local\\Microsoft\\Edge\\User Data";
        
        File profileDir = new File(edgeDataPath);
        if (profileDir.exists()) {
            options.addArguments("user-data-dir=" + edgeDataPath);
            options.addArguments("profile-directory=Default"); 
        }

        try {
            // 4. Initialize Driver
            driver = new EdgeDriver(options);
            
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            js = (JavascriptExecutor) driver;
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
        } catch (SessionNotCreatedException e) {
            String msg = "Failed to launch Edge.\nPlease CLOSE ALL OPEN EDGE WINDOWS and try again.";
            JOptionPane.showMessageDialog(null, msg, "Browser Locked", JOptionPane.ERROR_MESSAGE);
            throw e;
        } catch (Exception e) {
            // This is where your DNS error is caught
            JOptionPane.showMessageDialog(null, 
                "Launch Error:\n" + e.getMessage() + "\n\nCHECK: Is 'msedgedriver.exe' in the folder?", 
                "Launch Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    // ... (Keep the rest of your cleanup, screenshot, and helper methods exactly as they are) ...
    // ================= CLEANUP =================
    @AfterMethod
    public void captureAfterEach(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                String path = takeScreenshot(result.getName() + "_FAIL");
                if (test != null && path != null) 
                    test.fail(result.getThrowable(), MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                if (test != null) test.pass("Test passed");
            }
        } catch (Exception e) {
            System.err.println("Screenshot issue: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
        if (extent != null) extent.flush();
    }

    public String takeScreenshot(String name) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String path = reportDir + "/screenshots/" + name + ".png";
            File src = ts.getScreenshotAs(OutputType.FILE);
            File dest = new File(path);
            dest.getParentFile().mkdirs();
            org.openqa.selenium.io.FileHandler.copy(src, dest);
            return path;
        } catch (IOException e) { return null; }
    }
    
    // (Paste your Helper Methods like clickSidebarItem here...)
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
    public static void addProj(WebDriver driver) { new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(By.cssSelector("#add-project-btn"))).click(); }
    public static void clckClient(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Client --']]")).click(); }
    public static void searClient(WebDriver driver, String clientName) { driver.findElement(By.xpath("//input[@placeholder='Search...']")).sendKeys(clientName, Keys.TAB, Keys.ENTER); }
    public static void fillProjName(WebDriver driver, String projName) { driver.findElement(By.xpath("(//input[@id='search-button'])[2]")).sendKeys(projName, Keys.TAB, Keys.ENTER); }
    public static void selectCustomDropdown(WebDriver driver, String optionText) { new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class,'max-h-[300px]')]//button[span[text()='" + optionText + "']]"))).click(); }
    public static void clckPart(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Partner --']]")).click(); }
    public static void clckStat(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Status --']]")).click(); }

    public static void clckProjCat(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Category --']]")).click(); }
    public static void clckProjMang(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Manager --']]")).click(); }
    public static void searchManager(WebDriver driver, String managerName) { driver.findElement(By.xpath("//input[@placeholder='Search...']")).sendKeys(managerName, Keys.TAB, Keys.ENTER); }
    public static void clickPractice(WebDriver driver) { driver.findElement(By.xpath("(//div[@role='combobox'])[2]")).click(); }
    public static void searPractice(WebDriver driver, String practiceName) { driver.findElement(By.xpath("//input[@placeholder='Search...']")).sendKeys(practiceName, Keys.TAB, Keys.TAB, Keys.ENTER); }
    public static void clearPractice(WebDriver driver) { 
        WebElement searchBox = driver.findElement(By.xpath("//input[@placeholder='Search...']"));
        Actions act = new Actions(driver);
        act.moveToElement(searchBox).click().perform();
        searchBox.clear();
    }
    public static void clckProjType(WebDriver driver) { driver.findElement(By.xpath("//button[.//span[normalize-space()='-- Select Type --']]")).click(); }
    public static void clickTech(WebDriver driver) { driver.findElement(By.xpath("(//div[@role='combobox'])[3]")).click(); }
    public static void clickHours(WebDriver driver, String hours) {
        WebElement hour = driver.findElement(By.xpath("(//input[@data-slot='input'])[3]"));
        hour.click();
        hour.sendKeys(Keys.CONTROL + "a", Keys.DELETE); 
        hour.sendKeys(hours);
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
    public static void clickcreate(WebDriver driver) { driver.findElement(By.id("save-project-btn")).click(); }
    public static void popupDisplay(WebDriver driver) { }
    public boolean catchvalidation(WebDriver driver, ExtentTest test) {
        List<WebElement> list = driver.findElements(By.cssSelector(".text-xs.text-red-500.mt-1"));
        if (list.isEmpty()) return false;
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
        if (path != null) test.info("Validation screenshot attached", MediaEntityBuilder.createScreenCaptureFromPath(path).build());
        return true;
    }
    public void captureMuiAlertAndValidate(ExtentTest test) { }
}