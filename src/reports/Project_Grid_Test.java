package reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.aventstack.extentreports.MediaEntityBuilder;

public class Project_Grid_Test extends Project_Reports {

    // ================= LOCATORS =================
    By searchBox = By.xpath("//input[@id='search-button']"); 
    By gridRows = By.xpath("//div[@role='row' and contains(@class, 'MuiDataGrid-row')]");
    
    // Distinct locators for each column header
    By headerClient  = By.xpath("//div[@role='columnheader' and @data-field='companyName']");
    By headerProject = By.xpath("//div[@role='columnheader' and @data-field='projectName']");
    By headerSource  = By.xpath("//div[@role='columnheader' and @data-field='source']");
    
    By nextPageBtn   = By.xpath("//button[@title='Go to next page']");
    By prevPageBtn   = By.xpath("//button[@title='Go to previous page']");
    By pageInfoLabel = By.xpath("//p[contains(@class, 'MuiTablePagination-displayedRows')]"); 

    // ================= TESTS =================

    @Test(priority = 1)
    public void verifySearchFilter() throws InterruptedException {
        test = extent.createTest("Grid Filter Test", "Verifies searching for Client");
        
        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        Thread.sleep(8000); 
        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        
        popupDisplay(driver); 

        String searchText = "American Board of Ophthalmology"; 
        
        String pathBefore = takeScreenshot("Search_1_Before");
        test.info("Before Search", MediaEntityBuilder.createScreenCaptureFromPath(pathBefore).build());

        WebElement search = wait.until(ExpectedConditions.visibilityOfElementLocated(searchBox));
        search.click();
        search.clear();
        search.sendKeys(searchText, Keys.ENTER);
        
        Thread.sleep(5000); 

        String pathAfter = takeScreenshot("Search_2_Results");
        test.info("Search results for: " + searchText, MediaEntityBuilder.createScreenCaptureFromPath(pathAfter).build());

        List<String> clientValues = getCellDataByField("companyName");

        if(clientValues.isEmpty()) {
            test.fail("No rows found after searching.");
        } else {
            boolean allMatch = true;
            for (String val : clientValues) {
                if (!val.toLowerCase().contains(searchText.toLowerCase())) {
                    allMatch = false;
                    test.fail("Row mismatch: " + val);
                }
            }
            if (allMatch) {
                test.pass("Verified: All rows contain '" + searchText + "'");
            } else {
                Assert.fail("Search filter verification failed.");
            }
        }
    }

    @Test(priority = 2)
    public void verifySorting() throws InterruptedException {
        test = extent.createTest("Client Sorting Test", "Verifies A-Z and Z-A sorting on Client Column");

        // Ensure we are on the correct page
        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        wait.until(ExpectedConditions.visibilityOfElementLocated(gridRows));
        Thread.sleep(4500); 

        // 1. Capture Original State
        String path1 = takeScreenshot("Client_Sort_1_Before");
        test.info("Grid Before Sorting", MediaEntityBuilder.createScreenCaptureFromPath(path1).build());

        List<String> originalList = getCellDataByField("companyName");

        WebElement header = driver.findElement(headerClient);
        
        // 2. Click Once -> A-Z
        header.click();
        Thread.sleep(3500); 

        String path2 = takeScreenshot("Client_Sort_2_AZ");
        test.info("Grid After Sorting (A-Z)", MediaEntityBuilder.createScreenCaptureFromPath(path2).build());

        List<String> azListUI = getCellDataByField("companyName");
        List<String> expectedAZ = new ArrayList<>(originalList);
        expectedAZ.removeIf(String::isEmpty); 
        Collections.sort(expectedAZ, String.CASE_INSENSITIVE_ORDER); 

        if(!azListUI.isEmpty() && !expectedAZ.isEmpty()) {
            if(azListUI.get(0).equals(expectedAZ.get(0))) {
                test.pass("A-Z Sorting verified. Top item is '" + azListUI.get(0) + "'");
            } else {
                test.warning("A-Z Sorting mismatch. Expected: " + expectedAZ.get(0) + " but found: " + azListUI.get(0));
            }
        }

        // 3. Click Again -> Z-A
        header.click();
        Thread.sleep(3500);

        String path3 = takeScreenshot("Client_Sort_3_ZA");
        test.info("Grid After Sorting (Z-A)", MediaEntityBuilder.createScreenCaptureFromPath(path3).build());

        List<String> zaListUI = getCellDataByField("companyName");
        List<String> expectedZA = new ArrayList<>(originalList);
        expectedZA.removeIf(String::isEmpty);
        // Sort Reverse Order
        Collections.sort(expectedZA, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));

        if(!zaListUI.isEmpty() && !expectedZA.isEmpty()) {
            if(zaListUI.get(0).equals(expectedZA.get(0))) {
                test.pass("Z-A Sorting verified. Top item is '" + zaListUI.get(0) + "'");
            } else {
                test.warning("Z-A Sorting mismatch. Expected: " + expectedZA.get(0) + " but found: " + zaListUI.get(0));
            }
        }
    }

    @Test(priority = 3)
    public void verifyHideAllColumns() throws InterruptedException {
        test = extent.createTest("Hide All Columns Test", "Hides each of the 4 columns one by one");

        String[][] columns = {
            { "Client",       "companyName",  "//button[@aria-label='Client column menu']" },
            { "Project Name", "projectName",  "//button[@aria-label='Project Name column menu']" },
            { "Source",       "source",       "//button[@aria-label='Source column menu']" },
            { "Active",       "active",       "//button[@aria-label='Active column menu']" }
        };

        for (String[] col : columns) {
            String colName = col[0];
            String dataField = col[1];
            String menuXpath = col[2];

            test.info("<b>Testing Column: " + colName + "</b>");
            
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(gridRows));
            } catch (Exception e) {
                test.fail("Grid load timeout for " + colName);
                continue;
            }

            String pathVisible = takeScreenshot("Hide_" + colName + "_1_Visible");
            test.info("Column Visible: " + colName, MediaEntityBuilder.createScreenCaptureFromPath(pathVisible).build());

            if(driver.findElements(By.xpath("//div[@data-field='" + dataField + "']")).size() == 0) {
                test.fail("Column already missing.");
                continue; 
            }

            try {
                WebElement menuBtn = driver.findElement(By.xpath(menuXpath));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuBtn);
            } catch (Exception e) {
                test.fail("Menu button not found");
                continue;
            }

            try {
                WebElement hideOption = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//li[@role='menuitem' and .//span[text()='Hide column']]")
                ));
                hideOption.click();
                Thread.sleep(3000); 
            } catch (Exception e) {
                test.fail("Hide option not clickable");
                continue;
            }

            String pathHidden = takeScreenshot("Hide_" + colName + "_2_Hidden");
            
            int cellsAfter = driver.findElements(By.xpath("//div[@data-field='" + dataField + "']")).size();
            if (cellsAfter == 0) {
                test.pass("Column '" + colName + "' hidden successfully.", 
                        MediaEntityBuilder.createScreenCaptureFromPath(pathHidden).build());
            } else {
                test.fail("Column '" + colName + "' is still visible!", 
                        MediaEntityBuilder.createScreenCaptureFromPath(pathHidden).build());
            }

            driver.navigate().refresh();
            Thread.sleep(3000); 
        }
    }
    
    @Test(priority = 4)
    public void verifyPaginationFullTraversal() throws InterruptedException {
        test = extent.createTest("Pagination Full Traversal", "Navigates to the Last Page and back to First Page");
        
        driver.navigate().refresh();
        Thread.sleep(4000); 

        String pathStart = takeScreenshot("Pagination_1_Start");
        test.info("Starting Pagination (Page 1)", MediaEntityBuilder.createScreenCaptureFromPath(pathStart).build());

        test.info("<b>Starting Forward Navigation...</b>");
        int pageCounter = 1;
        
        while(true) {
            WebElement nextBtn = driver.findElement(nextPageBtn);
            
            if (!nextBtn.isEnabled() || nextBtn.getAttribute("disabled") != null) {
                String label = driver.findElement(pageInfoLabel).getText();
                String pathEnd = takeScreenshot("Pagination_2_LastPage");
                test.pass("Reached Last Page (" + pageCounter + "). Footer: " + label, 
                        MediaEntityBuilder.createScreenCaptureFromPath(pathEnd).build());
                break; 
            }

            nextBtn.click();
            pageCounter++;
            Thread.sleep(2500); 
        }

        test.info("<b>Starting Backward Navigation...</b>");
        
        while(true) {
            WebElement prevBtn = driver.findElement(prevPageBtn);
            
            if (!prevBtn.isEnabled() || prevBtn.getAttribute("disabled") != null) {
                String label = driver.findElement(pageInfoLabel).getText();
                String pathReturn = takeScreenshot("Pagination_3_ReturnedFirst");
                test.pass("Returned to First Page. Footer: " + label,
                        MediaEntityBuilder.createScreenCaptureFromPath(pathReturn).build());
                break; 
            }

            prevBtn.click();
            pageCounter--;
            Thread.sleep(2500); 
        }
        
        if (pageCounter == 1) {
            test.pass("Successfully traversed all pages and returned to Page 1.");
        } else {
            test.warning("Traversal logic finished but page counter is " + pageCounter);
        }
    }

    @Test(priority = 5)
    public void verifySortingproj() throws InterruptedException {
        test = extent.createTest("Project Sorting Test", "Verifies A-Z and Z-A sorting on Project Column");

        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        wait.until(ExpectedConditions.visibilityOfElementLocated(gridRows));
        Thread.sleep(4500); 

        // 1. Capture Original
        String path1 = takeScreenshot("Project_Sort_1_Before");
        test.info("Grid Before Sorting", MediaEntityBuilder.createScreenCaptureFromPath(path1).build());

        List<String> originalList = getCellDataByField("projectName");

        // USE CORRECT LOCATOR: headerProject
        WebElement header = driver.findElement(headerProject);
        
        // 2. Click Once -> A-Z
        header.click();
        Thread.sleep(3500); 

        String projectNameAZ = takeScreenshot("Project_Sort_2_AZ");
        test.info("Grid After Sorting (A-Z)", MediaEntityBuilder.createScreenCaptureFromPath(projectNameAZ).build());

        List<String> azListUI = getCellDataByField("projectName");
        List<String> expectedAZ = new ArrayList<>(originalList);
        expectedAZ.removeIf(String::isEmpty); 
        Collections.sort(expectedAZ, String.CASE_INSENSITIVE_ORDER); 

        if(!azListUI.isEmpty() && !expectedAZ.isEmpty()) {
            if(azListUI.get(0).equals(expectedAZ.get(0))) {
                test.pass("A-Z Sorting verified. Top item is '" + azListUI.get(0) + "'");
            } else {
                test.warning("A-Z Sorting mismatch.");
            }
        }

        // 3. Click Again -> Z-A
        header.click();
        Thread.sleep(3500);

        String projectNameZA = takeScreenshot("Project_Sort_3_ZA");
        test.info("Grid After Sorting (Z-A)", MediaEntityBuilder.createScreenCaptureFromPath(projectNameZA).build());

        List<String> zaListUI = getCellDataByField("projectName");
        List<String> expectedZA = new ArrayList<>(originalList);
        expectedZA.removeIf(String::isEmpty);
        // Sort Reverse
        Collections.sort(expectedZA, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));

        if(!zaListUI.isEmpty() && !expectedZA.isEmpty()) {
            if(zaListUI.get(0).equals(expectedZA.get(0))) {
                test.pass("Z-A Sorting verified. Top item is '" + zaListUI.get(0) + "'");
            } else {
                test.warning("Z-A Sorting mismatch.");
            }
        }
    }

    @Test(priority = 6)
    public void verifySortingsource() throws InterruptedException {
        test = extent.createTest("Source Sorting Test", "Verifies A-Z and Z-A sorting on Source Column");

        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        wait.until(ExpectedConditions.visibilityOfElementLocated(gridRows));
        Thread.sleep(4500); 

        // 1. Capture Original
        String path1 = takeScreenshot("Source_Sort_1_Before");
        test.info("Grid Before Sorting", MediaEntityBuilder.createScreenCaptureFromPath(path1).build());

        List<String> originalList = getCellDataByField("source");

        // USE CORRECT LOCATOR: headerSource
        WebElement header = driver.findElement(headerSource);
        
        // 2. Click Once -> A-Z
        header.click();
        Thread.sleep(3500); 

        String sourceAZ = takeScreenshot("Source_Sort_2_AZ");
        test.info("Grid After Sorting (A-Z)", MediaEntityBuilder.createScreenCaptureFromPath(sourceAZ).build());

        List<String> azListUI = getCellDataByField("source");
        List<String> expectedAZ = new ArrayList<>(originalList);
        expectedAZ.removeIf(String::isEmpty); 
        Collections.sort(expectedAZ, String.CASE_INSENSITIVE_ORDER); 

        if(!azListUI.isEmpty() && !expectedAZ.isEmpty()) {
            if(azListUI.get(0).equals(expectedAZ.get(0))) {
                test.pass("A-Z Sorting verified. Top item is '" + azListUI.get(0) + "'");
            } else {
                test.warning("A-Z Sorting mismatch.");
            }
        }

        // 3. Click Again -> Z-A
        header.click();
        Thread.sleep(3500);

        String sourceZA = takeScreenshot("Source_Sort_3_ZA");
        test.info("Grid After Sorting (Z-A)", MediaEntityBuilder.createScreenCaptureFromPath(sourceZA).build());

        List<String> zaListUI = getCellDataByField("source");
        List<String> expectedZA = new ArrayList<>(originalList);
        expectedZA.removeIf(String::isEmpty);
        // Sort Reverse
        Collections.sort(expectedZA, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));

        if(!zaListUI.isEmpty() && !expectedZA.isEmpty()) {
            if(zaListUI.get(0).equals(expectedZA.get(0))) {
                test.pass("Z-A Sorting verified. Top item is '" + zaListUI.get(0) + "'");
            } else {
                test.warning("Z-A Sorting mismatch.");
            }
        }
    }
    
    // ================= HELPER METHODS =================

    public List<String> getCellDataByField(String dataField) {
        List<String> values = new ArrayList<>();
        List<WebElement> cells = driver.findElements(By.xpath("//div[@role='gridcell' and @data-field='" + dataField + "']"));
        for (WebElement cell : cells) {
            values.add(cell.getText().trim());
        }
        return values;
    }
}