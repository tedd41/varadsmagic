package reports;

import java.io.File;
import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.aventstack.extentreports.MediaEntityBuilder;
import utils.ExcelUtils; 

public class Project_Count_Test extends Project_Reports {

    // ================= LOCATORS =================
    // Using the placeholder locator as it is more stable for MUI DataGrids
    By searchBox = By.xpath("//input[@id='search-button']"); 
    
    // Footer label "1-50 of 669"
    By footerLabel = By.xpath("//p[contains(@class, 'MuiTablePagination-displayedRows')]");
    
    // Grid rows (used for waiting)
    By gridRows = By.xpath("//div[@role='row' and contains(@class, 'MuiDataGrid-row')]");

    // ================= DATA PROVIDER (Dynamic Path) =================
    @DataProvider(name = "ClientData")
    public Object[][] getClientList() {
        String finalPath = "";
        try {
            // 1. Get Path sent from the GUI Launcher
            String guiPath = System.getProperty("custom.excel.path");
            
            if (guiPath != null && !guiPath.isEmpty()) {
                finalPath = guiPath;
            } else {
                // Fallback for Eclipse debugging
                finalPath = "C:\\Users\\VaradDesai\\Downloads\\CreateProject.xlsx";
            }
            
            // 2. Check File Existence
            File file = new File(finalPath);
            if (!file.exists()) {
                throw new IOException("Excel file not found at: " + finalPath);
            }

            // 3. Read data from "ClientList" sheet
            return ExcelUtils.getTestData("ClientList", finalPath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Data Provider Error: " + e.getMessage());
        }
    }

    // ================= TEST =================
    @Test(priority = 1, dataProvider = "ClientData")
    public void countProjectsPerCompany(String clientName) throws InterruptedException {
        test = extent.createTest("Count Projects: " + clientName);

        // 1. Initial Navigation & Login Handling
        if (!driver.getCurrentUrl().contains("admin/projects")) {
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
            
            // --- ADDED: Wait for MSAL Login ---
            Thread.sleep(8000); 
            
            // --- ADDED: Re-navigate to ensure correct page ---
            driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        }

        try {
            // 2. Clear Search & Enter Client Name
            WebElement search = wait.until(ExpectedConditions.visibilityOfElementLocated(searchBox));
            
            search.click();
            search.sendKeys(Keys.CONTROL + "a");
            search.sendKeys(Keys.DELETE);
            
            search.sendKeys(clientName);
            Thread.sleep(500); 
            search.sendKeys(Keys.ENTER);

            // 3. Wait for Grid to Reload
            Thread.sleep(2000); 

            // 4. Extract Count
            int visibleRows = driver.findElements(gridRows).size();
            
            // --- CREATE UNIQUE FILENAME FOR SCREENSHOT ---
            String cleanName = clientName.replaceAll("[^a-zA-Z0-9]", "_");
            String screenshotPath = takeScreenshot("Search_" + cleanName);

            if (visibleRows == 0) {
                test.warning("Client: <b>" + clientName + "</b> - No projects found.",
                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                return; 
            }

            try {
                WebElement footer = driver.findElement(footerLabel);
                String footerText = footer.getText(); 
                
                String totalCount = "0";
                
                if (footerText.contains("of")) {
                    totalCount = footerText.split("of")[1].trim();
                } else {
                    totalCount = String.valueOf(visibleRows);
                }

                // 5. Log Success WITH Unique Screenshot
                test.pass("<b>Client:</b> " + clientName + " <br><b>Total Projects:</b> " + totalCount,
                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                
                System.out.println("Client: " + clientName + " | Count: " + totalCount);

            } catch (Exception e) {
                test.pass("<b>Client:</b> " + clientName + " <br><b>Visible Rows:</b> " + visibleRows,
                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            }

        } catch (Exception e) {
            // Error Screenshot also needs unique name
            String cleanName = clientName.replaceAll("[^a-zA-Z0-9]", "_");
            String path = takeScreenshot("Error_" + cleanName);
            
            test.fail("Error processing " + clientName + ": " + e.getMessage(), 
                    MediaEntityBuilder.createScreenCaptureFromPath(path).build());
        }
    }
    
    /**
     * Overrides the parent method to prevent double screenshots.
     * We only want the screenshot we took manually inside the test, 
     * not the generic "Test Passed" screenshot from the parent class.
     */
    @Override
    public void captureAfterEach(org.testng.ITestResult result) {
        // Only take a screenshot if the test FAILED.
        // If it Passed, do nothing (because we already attached a better screenshot manually).
        if (result.getStatus() == org.testng.ITestResult.FAILURE) {
            try {
                // Keep the failure screenshot logic
                String path = takeScreenshot("FAIL_" + result.getName());
                test.fail(result.getThrowable(), 
                        MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}