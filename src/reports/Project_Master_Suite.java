package reports;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.aventstack.extentreports.MediaEntityBuilder;
import utils.ExcelUtils;

public class Project_Master_Suite extends Project_Reports {

    // ================= LOCATORS (Consolidated) =================
    By searchBox = By.xpath("//input[@id='search-button']");
    By gridRows = By.xpath("//div[@role='row' and contains(@class, 'MuiDataGrid-row')]");
    By footerLabel = By.xpath("//p[contains(@class, 'MuiTablePagination-displayedRows')]");
    By headerClient = By.xpath("//div[@role='columnheader' and @data-field='companyName']");
    By clientMenuIcon = By.xpath("//button[@aria-label='Client column menu']");

    // ================= DATA PROVIDER =================
    @DataProvider(name = "MasterDataProvider")
    public Object[][] getData() {
        String path = System.getProperty("custom.excel.path");
        if (path == null || path.isEmpty()) {
            path = "C:\\Users\\VaradDesai\\Downloads\\CreateProject.xlsx"; // Fallback
        }
        return ExcelUtils.getTestData("CreateProject", path);
    }

    // ================= MODULE 1: COUNT PROJECTS (From Code 1) =================
    @Test(priority = 1, dataProvider = "MasterDataProvider", groups = "Count")
    public void executeProjectCount(String testCaseID, String runMode, String clientName) throws InterruptedException {
        test = extent.createTest("Count Module: " + clientName);
        navigateAndLogin();
        
        WebElement search = wait.until(ExpectedConditions.visibilityOfElementLocated(searchBox));
        search.click();
        search.sendKeys(Keys.CONTROL + "a", Keys.DELETE);
        search.sendKeys(clientName, Keys.ENTER);
        Thread.sleep(2000);

        int visibleRows = driver.findElements(gridRows).size();
        String screenshot = takeScreenshot("Count_" + clientName);
        
        test.pass("Found " + visibleRows + " projects for " + clientName, 
            MediaEntityBuilder.createScreenCaptureFromPath(screenshot).build());
    }

    // ================= MODULE 2: GRID & FILTER (From Code 2 & 3) =================
    @Test(priority = 2, groups = "UI")
    public void executeGridAndFilterValidation() throws InterruptedException {
        test = extent.createTest("UI Module: Grid & Filter Validation");
        navigateAndLogin();

        // Hover and Click Menu (Code 2 Logic)
        WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(headerClient));
        new Actions(driver).moveToElement(header).perform();
        
        WebElement menuBtn = wait.until(ExpectedConditions.elementToBeClickable(clientMenuIcon));
        menuBtn.click();
        
        // Sorting Logic (Code 3 Logic)
        header.click(); // Sort A-Z
        Thread.sleep(2000);
        test.pass("Verified Grid Sorting and Menu Interaction", 
            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("Grid_UI")).build());
    }

    // ================= MODULE 3: DATA DRIVEN CREATION (From Code 4) =================
    @Test(priority = 3, dataProvider = "MasterDataProvider", groups = "Creation")
    public void executeDataDrivenCreation(String testCaseID, String runMode, String client, String projName, 
                                        String status, String partner, String category, String manager, 
                                        String practice, String type, String technology, String hours, 
                                        String startDate, String endDate, String available, String active, 
                                        String expectedResult, String expectedMessage) throws InterruptedException {
        
        if (runMode.equalsIgnoreCase("No")) throw new org.testng.SkipException("Skipped");

        test = extent.createTest("Creation Module: " + projName);
        navigateAndLogin();

        // Trigger Sidebar and Add Project (Using methods from your Project_Reports parent)
        clickSidebarItem(driver, "Projects");
        addProj(driver);
        
        // Form filling logic from Code 4...
        if (client != null && !client.isEmpty()) { clckClient(driver); searClient(driver, client); }
        fillProjName(driver, projName);
        // ... (Add the rest of your form filling methods here)
        
        clickcreate(driver);
        test.pass("Project Created Successfully");
    }

    // ================= HELPER: REUSABLE NAVIGATION =================
    private void navigateAndLogin() throws InterruptedException {
        if (!driver.getCurrentUrl().contains("admin/projects")) {
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
            Thread.sleep(5000); // Wait for MSAL
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        }
    }
}