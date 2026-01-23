package utils;

import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtils {

    public static Object[][] getTestData(String sheetName, String excelPath) {
        FileInputStream file = null;
        XSSFWorkbook book = null;
        Object[][] data = null;

        try {
            file = new FileInputStream(excelPath);
            book = new XSSFWorkbook(file);
            XSSFSheet sheet = book.getSheet(sheetName);

            // Safety Check: Does the sheet exist?
            if (sheet == null) {
                System.err.println("Error: Sheet '" + sheetName + "' not found in " + excelPath);
                return new Object[0][0]; 
            }

            int lastRow = sheet.getLastRowNum();
            int lastCol = 0;
            
            // Find column count safely
            if (sheet.getRow(0) != null) {
                lastCol = sheet.getRow(0).getLastCellNum();
            }

            data = new Object[lastRow][lastCol];

            // Iterate through rows (Skipping header at index 0)
            for (int i = 0; i < lastRow; i++) {
                Row row = sheet.getRow(i + 1); // +1 to skip header

                // --- THE CRASH FIX IS HERE ---
                for (int k = 0; k < lastCol; k++) {
                    if (row == null) {
                        data[i][k] = ""; // If row is missing, treat as empty
                    } else {
                        Cell cell = row.getCell(k);
                        // Use DataFormatter to handle numbers/text without crashing
                        DataFormatter formatter = new DataFormatter();
                        String val = formatter.formatCellValue(cell);
                        data[i][k] = (val == null) ? "" : val;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Object[0][0];
        } finally {
            try {
                if (book != null) book.close();
                if (file != null) file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}