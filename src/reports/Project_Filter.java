package reports;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions; // Import for Hover
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.Test;

public class Project_Filter extends Project_Reports {

    // 1. Existing Locators
    By headerClient = By.xpath("//div[@role='columnheader' and @data-field='companyName']");
    
    // 2. NEW Locator for the Client Menu Icon (based on your HTML)
    By clientMenuIcon = By.xpath("//button[@aria-label='Client column menu']");

    @Test(priority = 1)
    public void verifySearchFilter() throws InterruptedException {
        test = extent.createTest("Grid Filter Test", "Verifies clicking the Client Menu Icon");

        // Navigate to the page
        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");
        Thread.sleep(8000);
        driver.get("https://atresiq-demo-augqbvgzbjgjash3.centralindia-01.azurewebsites.net/admin/projects");

        // Wait for the grid to load (Using Explicit Wait is better than Thread.sleep)
        wait.until(ExpectedConditions.visibilityOfElementLocated(headerClient));

        // --- LOGIC TO CLICK THE MENU ICON ---
        
        try {
            // Step A: Locate the Client Header
            WebElement clientHeader = driver.findElement(headerClient);

            // Step B: Hover over the header
            // (Crucial for MUI grids: the icon often only appears on hover)
            Actions action = new Actions(driver);
            action.moveToElement(clientHeader).perform();
            
            test.info("Hovered over Client column header");

            // Step C: Wait for the menu icon to be visible and clickable
            WebElement menuBtn = wait.until(ExpectedConditions.elementToBeClickable(clientMenuIcon));

            // Step D: Click the icon
            menuBtn.click();
            WebElement filter = driver.findElement(By.xpath("(//li[@role='menuitem'])[3]"));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("(//li[@role='menuitem'])[3]")));
            filter.click();
            test.pass("Successfully clicked the Client column menu icon");
            
            WebElement filterHeader = driver.findElement(By.cssSelector(".MuiDataGrid-filterForm"));
            wait.until(ExpectedConditions.visibilityOf(filterHeader));
            if(filterHeader.isDisplayed()) {
            	test.pass("Filter header is visible");
            }else {
            	test.fail("Filter header is invisible");

            }
            
          WebElement filterValue = driver.findElement(By.xpath("//input[@placeholder='Filter value']"));
          filterValue.sendKeys("3P");
          
            
            
            
        } catch (Exception e) {
            test.fail("Failed to click menu icon: " + e.getMessage());
            // Optional: capture screenshot here using your existing utility
            throw e; 
        }
    }
}