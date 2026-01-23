package reports;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class AppLauncher {

    // Global variables
    private static JButton btnUpload;
    private static JButton btnRun;
    private static JLabel lblStatus;
    private static String[] selectedPath = {null}; 
    private static JDialog progressDialog; // The Loader Window

    public static void main(String[] args) {
        // 1. Setup Main Window
        JFrame frame = new JFrame("ResIQ Automation Tool");
        frame.setSize(500, 420);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 2. UI Elements
        JLabel lblTitle = new JLabel("ResIQ Automation Launcher", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        JLabel lblSelect = new JLabel("Select Automation Script:");
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        String[] testOptions = { 
     		"1. Count Projects Per Client", 
            "2. Grid UI Validation", 
     		"3. Create Projects (Excel Data Driven)" 
        };
        JComboBox<String> cmbTests = new JComboBox<>(testOptions);
        cmbTests.setPreferredSize(new Dimension(250, 35));

        btnUpload = new JButton("Select Excel File");
        btnUpload.setPreferredSize(new Dimension(250, 40));

        btnRun = new JButton("Run Automation");
        btnRun.setPreferredSize(new Dimension(250, 40));
        btnRun.setBackground(new Color(40, 167, 69)); // Green
        btnRun.setForeground(Color.WHITE);
        btnRun.setEnabled(false); 

        lblStatus = new JLabel("Status: Waiting for input...", SwingConstants.CENTER);

        // --- Logic: Handle Grid Test (No Excel) ---
        cmbTests.addActionListener(e -> {
            String selected = (String) cmbTests.getSelectedItem();
            if (selected.contains("Grid UI")) {
                btnUpload.setEnabled(false);
                btnUpload.setText("Excel Not Required");
                btnRun.setEnabled(true);
                lblStatus.setText("Status: Ready to run Grid Test");
            } else {
                btnUpload.setEnabled(true);
                btnUpload.setText("Select Excel File");
                if (selectedPath[0] != null) {
                    btnRun.setEnabled(true);
                    lblStatus.setText("Status: File selected");
                } else {
                    btnRun.setEnabled(false);
                    lblStatus.setText("Status: Please select Excel file");
                }
            }
        });

        // 3. Action: Upload
        btnUpload.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
            int option = fileChooser.showOpenDialog(frame);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                selectedPath[0] = file.getAbsolutePath();
                lblStatus.setText("File: " + file.getName());
                btnRun.setEnabled(true);
            }
        });

        // 4. Action: Run
        btnRun.addActionListener(e -> {
            lblStatus.setText("Status: Initializing...");
            btnRun.setEnabled(false);
            btnUpload.setEnabled(false);
            cmbTests.setEnabled(false);
            
            // SHOW THE LOADER
            showProgressDialog(frame);

            new Thread(() -> {
                try {
                    if (selectedPath[0] != null) {
                        System.setProperty("custom.excel.path", selectedPath[0]);
                    }

                    // --- BUILD XML SUITE ---
                    XmlSuite suite = new XmlSuite();
                    suite.setName("ResIQ_Offline_Suite");

                    XmlTest test = new XmlTest(suite);
                    test.setName("Selected_Test_Run");
                    
                    List<XmlClass> classes = new ArrayList<>();
                    String selected = (String) cmbTests.getSelectedItem();
                    
                    if (selected.contains("Create Projects")) {
                        classes.add(new XmlClass(Project_Reports_DataDriven.class));
                    } 
                    else if (selected.contains("Count Projects")) {
                        classes.add(new XmlClass(Project_Count_Test.class));
                    }
                    else if (selected.contains("Grid UI")) {
                        classes.add(new XmlClass(Project_Grid_Test.class));
                    }

                    test.setXmlClasses(classes);
                    List<XmlSuite> suites = new ArrayList<>();
                    suites.add(suite);

                    TestNG testng = new TestNG();
                    testng.setXmlSuites(suites);
                    testng.run();
                    // -----------------------

                    // HIDE LOADER
                    progressDialog.dispose();

                    lblStatus.setText("Status: Completed!");
                    openLatestReport();
                    JOptionPane.showMessageDialog(frame, "Success! Automation finished.");
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if(progressDialog != null) progressDialog.dispose(); // Close loader on error
                    lblStatus.setText("Error: See Console");
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                } finally {
                    cmbTests.setEnabled(true);
                    String currentSelection = (String) cmbTests.getSelectedItem();
                    if (!currentSelection.contains("Grid UI")) {
                        btnUpload.setEnabled(true);
                        btnRun.setEnabled(selectedPath[0] != null);
                    } else {
                        btnRun.setEnabled(true);
                    }
                    lblStatus.setText("Status: Ready");
                }
            }).start();
        });

        // 5. Add to Frame
        frame.add(lblTitle, gbc);
        gbc.gridy++;
        frame.add(lblSelect, gbc);
        gbc.gridy++;
        frame.add(cmbTests, gbc);
        gbc.gridy++;
        frame.add(btnUpload, gbc);
        gbc.gridy++;
        frame.add(btnRun, gbc);
        gbc.gridy++;
        frame.add(lblStatus, gbc);

        frame.setVisible(true);
    }

    // --- NEW: PROGRESS LOADER METHOD ---
    private static void showProgressDialog(JFrame parent) {
        progressDialog = new JDialog(parent, "Executing...", false); // false = not modal (allows background thread)
        progressDialog.setSize(300, 120);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setUndecorated(true); // Removes title bar for "modern" look
        
        // Add a nice border
        ((JComponent)progressDialog.getContentPane()).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JLabel msgLabel = new JLabel("Automation in Progress... Please Wait", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Makes the bar bounce back and forth
        progressBar.setForeground(new Color(40, 167, 69)); // Green loader

        progressDialog.add(msgLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);

        progressDialog.setVisible(true);
    }

    // Helper to open report
    private static void openLatestReport() {
        try {
            File reportFolder = new File(System.getProperty("user.dir") + "/Reports");
            if (reportFolder.exists() && reportFolder.isDirectory()) {
                File[] files = reportFolder.listFiles();
                if (files != null && files.length > 0) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                    File newestFolder = files[0];
                    File reportFile = new File(newestFolder, "ProjectCreationReport.html");
                    if (Desktop.isDesktopSupported()) {
                        if (reportFile.exists()) Desktop.getDesktop().open(reportFile);
                        else Desktop.getDesktop().open(newestFolder);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}