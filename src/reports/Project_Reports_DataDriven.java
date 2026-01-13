package reports;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import utils.ExcelUtils; 

public class Project_Reports_DataDriven extends Project_Reports {

    // ================= ROBUST DATA PROVIDER =================
    @DataProvider(name = "ProjectData")
    public Object[][] getData() {
        String finalPath = "";
        try {
            // 1. Get Path sent from the GUI Launcher
            String guiPath = System.getProperty("custom.excel.path");
            
            if (guiPath != null && !guiPath.isEmpty()) {
                finalPath = guiPath;
            } else {
                // Fallback: Hardcoded path for testing inside Eclipse (Debugging only)
                finalPath = "C:\\Users\\VaradDesai\\Downloads\\CreateProject.xlsx";
            }
            
            System.out.println("Data Provider Loading File: " + finalPath); 
            
            // 2. Verification: Check if file actually exists
            File file = new File(finalPath);
            if (!file.exists()) {
                System.err.println("CRITICAL ERROR: Excel file not found at " + finalPath);
                throw new IOException("Excel file missing: " + finalPath);
            }

            // 3. Call Utility to read data
            return ExcelUtils.getTestData("CreateProject", finalPath);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Throw runtime exception to stop TestNG from trying to run tests with null data
            throw new RuntimeException("Data Provider Failed: " + e.getMessage());
        }
    }

    // ================= TEST CASE =================
    @Test(dataProvider = "ProjectData")
    public void createProjectDD(
            String testCaseID, String runMode, String client, String projName, 
            String status, String partner, String category, String manager, 
            String practice, String type, String technology, String hours, 
            String startDate, String endDate, String available, String active, 
            String expectedResult, String expectedMessage) {

        // Check RunMode logic
        if (runMode != null && runMode.equalsIgnoreCase("No")) {
            throw new org.testng.SkipException("Skipping Test Case: " + testCaseID);
        }

        test = extent.createTest(testCaseID + ": " + projName, "Scenario: " + expectedResult);

        try {
            // 1. INITIAL NAVIGATION & LOGIN HANDLING
            // Trigger the URL to start the MSAL Login process
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
			/*
			 * driver.findElement(By.cssSelector(
			 * "#tilesHolder > div:nth-child(1) > div > div > div > div.table-cell.text-left.content > div:nth-child(2)"
			 * )).click();
			 * 
			 * driver.findElement(By.cssSelector("button[data-slot='button']")).click();
			 * Thread.sleep(2000);
			 * 
			 * // WAITING FOR LOGIN: Sleep 8 seconds as requested to allow MSAL to auto-sign
			 * in Thread.sleep(8000);
			 */
            Thread.sleep(8000);
            // RE-NAVIGATE: Ensure we are definitely on the Admin Projects page after login
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
            
            // 2. Start Automation Flow
            clickSidebarItem(driver, "Projects");
            addProj(driver);
            popupDisplay(driver);

            // 3. Form Filling
            if (client != null && !client.isEmpty()) { clckClient(driver); searClient(driver, client); }
            if (projName != null && !projName.isEmpty()) { fillProjName(driver, projName); }
            if (status != null && !status.isEmpty()) { selectCustomDropdown(driver, status); }
            if (partner != null && !partner.isEmpty()) { clckPart(driver); searchManager(driver, partner); }
            if (category != null && !category.isEmpty()) { clckProjCat(driver); selectCustomDropdown(driver, category); }
            if (manager != null && !manager.isEmpty()) { clckProjMang(driver); searchManager(driver, manager); }
            if (practice != null && !practice.isEmpty()) { clickPractice(driver); clearPractice(driver); searPractice(driver, practice); }
            if (type != null && !type.isEmpty()) { clckProjType(driver); selectCustomDropdown(driver, type); }
            if (technology != null && !technology.isEmpty()) { clickTech(driver); clearPractice(driver); searPractice(driver, technology); }
            if (hours != null && !hours.isEmpty()) { clickHours(driver, hours); }
            if (startDate != null && !startDate.isEmpty()) { sendateDD(driver, startDate); }
            if (endDate != null && !endDate.isEmpty()) { endateDD(driver, endDate); }
            if (available != null && !available.isEmpty()) selectAvailableOption(driver, available);
            if (active != null && !active.isEmpty()) setActiveCheckbox(driver, active);

            takeScreenshot(testCaseID + "_FilledForm");

            clickcreate(driver);

            // 4. Validation Logic
            if (expectedResult.equalsIgnoreCase("Success")) {
                captureMuiAlertAndValidateDD(test, expectedMessage, true, testCaseID);
            } else {
                // Check for inline validation red text first
                boolean fieldValidationFound = checkFieldValidationsDD(driver, test, testCaseID);
                
                if (!fieldValidationFound) {
                    // If no inline errors, check for popup alert
                    captureMuiAlertAndValidateDD(test, expectedMessage, false, testCaseID);
                } else {
                    test.pass("Field validation errors found as expected.");
                }
            }

        } catch (Exception e) {
            String path = takeScreenshot(testCaseID + "_Exception");
            test.fail("Exception: " + e.getMessage(), 
                MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            Assert.fail(e.getMessage());
        }
    }

    // ================= HELPER METHODS =================

    public void captureMuiAlertAndValidateDD(ExtentTest test, String expectedText, boolean isSuccessExpected, String testCaseID) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement alertMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".MuiAlert-message")));
            
            String actualText = alertMessage.getText().trim();
            String screenshotName = testCaseID + (isSuccessExpected ? "_SuccessAlert" : "_ErrorAlert");
            String path = takeScreenshot(screenshotName);

            if (actualText.contains(expectedText)) {
                test.pass("Alert Text Match: " + actualText, 
                        MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            } else {
                test.fail("Alert Mismatch. Expected: " + expectedText + " | Actual: " + actualText, 
                        MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            }
        } catch (Exception e) {
             if(isSuccessExpected) {
                 String path = takeScreenshot(testCaseID + "_MissingAlert");
                 test.fail("Expected success alert '" + expectedText + "' but none appeared.",
                         MediaEntityBuilder.createScreenCaptureFromPath(path).build());
                 Assert.fail("Success alert missing");
             } else {
                 test.info("No MUI Alert appeared.");
             }
        }
    }

    public boolean checkFieldValidationsDD(WebDriver driver, ExtentTest test, String testCaseID) {
        // Calls the helper method from the Parent Class (Project_Reports)
        List<WebElement> list = driver.findElements(By.cssSelector(".text-xs.text-red-500.mt-1"));
        if (list.isEmpty()) return false;

        boolean hasText = false;
        for (WebElement ele : list) {
            String text = ele.getText().trim();
            if (!text.isEmpty()) {
                test.info("🔴 Field Error: " + text);
                hasText = true;
            }
        }
        
        if (hasText) {
             String path = takeScreenshot(testCaseID + "_ValidationErrors");
             test.info("Validation errors visible", MediaEntityBuilder.createScreenCaptureFromPath(path).build());
        }
        
        return hasText;
    }

    public void sendateDD(WebDriver driver, String dateVal) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement startDate = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//input[@data-slot='input'])[4]")));
        startDate.click();
        startDate.sendKeys(Keys.CONTROL + "a", Keys.DELETE); 
        startDate.sendKeys(dateVal);
    }

    public void endateDD(WebDriver driver, String dateVal) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement endDate = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//input[@data-slot='input'])[5]")));
        endDate.click();
        endDate.sendKeys(Keys.CONTROL + "a", Keys.DELETE);
        endDate.sendKeys(dateVal);
    }
}