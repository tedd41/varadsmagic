package reports;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    // --- GLOBAL VARIABLES ---
    private static JButton btnUpload;
    private static JButton btnRun;
    private static JLabel lblStatus;
    private static String[] selectedPath = {null}; 
    private static JDialog progressDialog; // The Loader Window

    // Dynamic Base Path (Current Folder)
    private static final String APP_DIR = System.getProperty("user.dir");

    public static void main(String[] args) {
        
        // 1. Setup Main Window (Your UI Style)
        JFrame frame = new JFrame("ResIQ Automation Tool");
        frame.setSize(500, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());
        
        // Layout Constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 25, 10, 25);
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 2. UI Elements
        JLabel lblTitle = new JLabel("ResIQ Automation Launcher", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        
        JLabel lblSelect = new JLabel("Select Automation Script:");
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        String[] testOptions = { 
            "1. Create Projects (Data Driven)", 
            "2. Count Projects Per Client", 
            "3. Grid UI Validation" 
        };
        
        JComboBox<String> cmbTests = new JComboBox<>(testOptions);
        cmbTests.setPreferredSize(new Dimension(300, 35));
        
        // --- FIX: NUCLEAR DROPDOWN RENDERER (Prevents Blank Text) ---
        cmbTests.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(new Color(220, 220, 220)); // Light Grey Highlight
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        cmbTests.setForeground(Color.BLACK);
        cmbTests.setBackground(Color.WHITE);
        // -----------------------------------------------------------

        btnUpload = new JButton("Select Excel File");
        btnUpload.setPreferredSize(new Dimension(300, 40));
        btnUpload.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btnRun = new JButton("Run Automation");
        btnRun.setPreferredSize(new Dimension(300, 40));
        btnRun.setBackground(new Color(40, 167, 69)); // Green
        btnRun.setForeground(Color.WHITE);
        btnRun.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRun.setEnabled(false); 

        lblStatus = new JLabel("Status: Waiting for input...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // --- LOGIC: Handle Dropdown Selection ---
        cmbTests.addActionListener(e -> {
            String selected = (String) cmbTests.getSelectedItem();
            if (selected.contains("Grid UI") || selected.contains("Count Projects")) {
                btnUpload.setEnabled(false);
                btnUpload.setText("Excel Not Required");
                btnRun.setEnabled(true);
                lblStatus.setText("Status: Ready to run " + selected);
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

        // --- ACTION: Upload Button (Portable Path) ---
        btnUpload.addActionListener(e -> {
            // Open File Chooser in the APP DIRECTORY (Portable)
            JFileChooser fileChooser = new JFileChooser(APP_DIR);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx", "xls"));
            
            int option = fileChooser.showOpenDialog(frame);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                selectedPath[0] = file.getAbsolutePath();
                lblStatus.setText("File: " + file.getName());
                btnRun.setEnabled(true);
            }
        });

        // --- ACTION: Run Button ---
        btnRun.addActionListener(e -> {
            lblStatus.setText("Status: Initializing...");
            btnRun.setEnabled(false);
            btnUpload.setEnabled(false);
            cmbTests.setEnabled(false);
            
            // SHOW THE LOADER
            showProgressDialog(frame);

            new Thread(() -> {
                try {
                    // Set Excel Property for Tests
                    if (selectedPath[0] != null) {
                        System.setProperty("custom.excel.path", selectedPath[0]);
                    }

                    // --- BUILD XML SUITE ---
                    XmlSuite suite = new XmlSuite();
                    suite.setName("ResIQ_Portable_Suite");

                    XmlTest test = new XmlTest(suite);
                    test.setName("Selected_Test_Run");
                    
                    List<XmlClass> classes = new ArrayList<>();
                    String selected = (String) cmbTests.getSelectedItem();
                    
                    // --- MAP DROPDOWN TO CLASS FILES ---
                    // Make sure these Class Names match your files exactly!
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

                    // --- RUN TESTNG ---
                    TestNG testng = new TestNG();
                    testng.setXmlSuites(suites);
                    testng.run();
                    
                    // --- CLEANUP ---
                    progressDialog.dispose(); // Close loader

                    lblStatus.setText("Status: Completed!");
                    JOptionPane.showMessageDialog(frame, "Success! Automation finished.");
                    openLatestReport();
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if(progressDialog != null) progressDialog.dispose();
                    lblStatus.setText("Error: Execution Failed");
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                } finally {
                    // Re-enable UI
                    cmbTests.setEnabled(true);
                    String currentSelection = (String) cmbTests.getSelectedItem();
                    if (currentSelection.contains("Create Projects")) {
                        btnUpload.setEnabled(true);
                        btnRun.setEnabled(selectedPath[0] != null);
                    } else {
                        btnRun.setEnabled(true);
                    }
                }
            }).start();
        });

        // 3. Add Components to Frame
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

    // --- LOADER METHOD ---
    private static void showProgressDialog(JFrame parent) {
        progressDialog = new JDialog(parent, "Executing...", false); 
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setUndecorated(true);
        
        ((JComponent)progressDialog.getContentPane()).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JLabel msgLabel = new JLabel("Executing Script... Please Wait", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        msgLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); 
        progressBar.setForeground(new Color(40, 167, 69));

        progressDialog.add(msgLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);

        progressDialog.setVisible(true);
    }

    // --- REPORT OPENER ---
    private static void openLatestReport() {
        try {
            // Look for 'Reports' folder in the App Directory
            File reportFolder = new File(APP_DIR + File.separator + "Reports");
            if (reportFolder.exists() && reportFolder.isDirectory()) {
                File[] files = reportFolder.listFiles();
                if (files != null && files.length > 0) {
                    // Sort by newest first
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                    File newest = files[0];
                    if (Desktop.isDesktopSupported()) {
                         Desktop.getDesktop().open(newest);
                    }
                }
            }
        } catch (Exception e) { 
            System.out.println("Could not open report: " + e.getMessage());
        }
    }
}