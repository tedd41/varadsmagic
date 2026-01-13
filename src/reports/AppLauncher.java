package reports;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

// TestNG Imports
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class AppLauncher {

    // --- GLOBAL VARIABLES ---
    private static JFrame frame;
    private static JTextArea logArea;
    private static JButton btnUpload;
    private static JButton btnRun;
    private static JLabel lblStatus;
    private static JLabel lblFileSelected;
    
    // Data holders
    private static String selectedFilePath = null;
    
    // Get the folder where the .exe is running
    private static final String APP_DIR = System.getProperty("user.dir");

    public static void main(String[] args) {
        
        // 1. SETUP RELATIVE PATHS (The "Portable" Fix)
        setupPortableEnvironment();

        // 2. UI THEME
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { }

        // 3. MAIN WINDOW
        frame = new JFrame("ResIQ Automation Launcher (Portable)");
        frame.setSize(650, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // --- CONTROLS PANEL ---
        JPanel panelControls = new JPanel(new GridBagLayout());
        panelControls.setBorder(new EmptyBorder(20, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0; gbc.gridy = 0;

        // Title
        JLabel lblTitle = new JLabel("🚀 ResIQ Automation (Portable)", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        panelControls.add(lblTitle, gbc);

        // Test Selector
        gbc.gridy++;
        panelControls.add(new JLabel("Select Test Scenario:"), gbc);

        gbc.gridy++;
        String[] testOptions = { 
            "1. Filter Logic (Excel Driven)", 
            "2. Count Projects", 
            "3. Grid UI Validation" 
        };
        JComboBox<String> cmbTests = new JComboBox<>(testOptions);
        panelControls.add(cmbTests, gbc);

        // Upload Button
        gbc.gridy++;
        btnUpload = new JButton("📂 Select Excel File");
        lblFileSelected = new JLabel("No file selected");
        JPanel filePanel = new JPanel(new BorderLayout(10, 0));
        filePanel.add(btnUpload, BorderLayout.WEST);
        filePanel.add(lblFileSelected, BorderLayout.CENTER);
        panelControls.add(filePanel, gbc);

        // Run Button
        gbc.gridy++;
        btnRun = new JButton("▶ Run Automation");
        btnRun.setBackground(new Color(40, 167, 69));
        btnRun.setForeground(Color.WHITE);
        btnRun.setEnabled(false);
        panelControls.add(btnRun, gbc);

        // Status
        gbc.gridy++;
        lblStatus = new JLabel("Status: Ready", SwingConstants.CENTER);
        panelControls.add(lblStatus, gbc);

        // --- LOG PANEL ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(40, 44, 52));
        logArea.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Execution Logs"));
        
        frame.add(panelControls, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // --- LISTENERS ---

        // Test Selection Logic
        cmbTests.addActionListener(e -> {
            String selected = (String) cmbTests.getSelectedItem();
            if (selected.contains("Grid UI")) {
                btnUpload.setEnabled(false);
                btnUpload.setText("Excel Not Required");
                btnRun.setEnabled(true);
            } else {
                btnUpload.setEnabled(true);
                btnUpload.setText("📂 Select Excel File");
                btnRun.setEnabled(selectedFilePath != null);
            }
        });

        // Upload Logic (Starts in App Folder now)
        btnUpload.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(APP_DIR); // Open in current folder
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
            
            int option = fileChooser.showOpenDialog(frame);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                selectedFilePath = file.getAbsolutePath();
                lblFileSelected.setText(file.getName());
                btnRun.setEnabled(true);
                log("Selected: " + selectedFilePath);
            }
        });

        // Run Logic
        btnRun.addActionListener(e -> {
            btnRun.setEnabled(false);
            lblStatus.setText("Status: Running...");
            log("--- STARTING TEST ---");

            new Thread(() -> {
                try {
                    // Pass the Excel path to the Test Class
                    if (selectedFilePath != null) {
                        System.setProperty("custom.excel.path", selectedFilePath);
                    }

                    // Setup TestNG
                    TestNG testng = new TestNG();
                    XmlSuite suite = new XmlSuite();
                    suite.setName("Portable_Suite");
                    XmlTest test = new XmlTest(suite);
                    test.setName("Run_1");
                    
                    List<XmlClass> classes = new ArrayList<>();
                    // NOTE: Add your real class names here
                    classes.add(new XmlClass(Project_Grid_Test.class)); 
                    
                    test.setXmlClasses(classes);
                    List<XmlSuite> suites = new ArrayList<>();
                    suites.add(suite);
                    testng.setXmlSuites(suites);
                    
                    testng.run();

                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Status: Completed");
                        JOptionPane.showMessageDialog(frame, "Execution Finished!");
                        btnRun.setEnabled(true);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    log("Error: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> btnRun.setEnabled(true));
                }
            }).start();
        });

        frame.setVisible(true);
        log("App started in: " + APP_DIR);
        log("Driver path set to: " + System.getProperty("webdriver.edge.driver"));
    }

    // --- HELPER METHODS ---

    private static void setupPortableEnvironment() {
        // Look for 'drivers' folder next to the .exe
        String driverPath = APP_DIR + File.separator + "drivers" + File.separator + "msedgedriver.exe";
        File driverFile = new File(driverPath);

        if (driverFile.exists()) {
            System.setProperty("webdriver.edge.driver", driverPath);
        } else {
            // Fallback: Check if it's in the root folder
            String rootPath = APP_DIR + File.separator + "msedgedriver.exe";
            if (new File(rootPath).exists()) {
                System.setProperty("webdriver.edge.driver", rootPath);
            } else {
                JOptionPane.showMessageDialog(null, 
                    "Driver Error:\nCould not find msedgedriver.exe in:\n" + driverPath + "\n\nPlease ensure the 'drivers' folder is next to the app.", 
                    "Missing Component", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(">> " + msg + "\n"));
    }
}