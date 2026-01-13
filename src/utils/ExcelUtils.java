package utils;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelUtils {

    public static Object[][] getTestData(String sheetName, String excelPath) throws IOException {
        FileInputStream fis = new FileInputStream(excelPath);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        XSSFSheet sheet = workbook.getSheet(sheetName);
        
        int rowCount = sheet.getLastRowNum();
        int colCount = sheet.getRow(0).getLastCellNum();
        
        // We skip the header row, so rowCount is used directly for size
        Object[][] data = new Object[rowCount][colCount];
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < rowCount; i++) {
            for (int k = 0; k < colCount; k++) {
                // i+1 to skip header row
                data[i][k] = formatter.formatCellValue(sheet.getRow(i + 1).getCell(k));
            }
        }
        workbook.close();
        return data;
    }
}