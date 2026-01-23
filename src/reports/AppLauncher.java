package reports;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static JList<TestListItem> listTests; 
    private static DefaultListModel<TestListItem> listModel; 
    private static JButton btnUpload;
    private static JButton btnRun;
    private static JButton btnViewReports; 
    private static JLabel lblStatus;
    private static String[] selectedPath = {null}; 
    private static JDialog progressDialog; 

    // Dynamic Base Path
    private static final String APP_DIR = System.getProperty("user.dir");

    public static void main(String[] args) {
        
        JFrame frame = new JFrame("ResIQ Automation Tool");
        frame.setSize(500, 500); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 25, 8, 25); 
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("ResIQ Automation Launcher", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        
        JLabel lblSelect = new JLabel("Select Automation Script(s):");
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        listModel = new DefaultListModel<>();
        listModel.addElement(new TestListItem("1. Create Projects (Excel Data Driven)"));
        listModel.addElement(new TestListItem("2. Count Projects Per Client"));
        listModel.addElement(new TestListItem("3. Grid UI Validation"));

        listTests = new JList<>(listModel);
        listTests.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        listTests.setVisibleRowCount(4);
        listTests.setCellRenderer(new CheckboxListRenderer());

        listTests.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = listTests.locationToIndex(e.getPoint());
                if (index != -1) {
                    TestListItem item = listModel.getElementAt(index);
                    item.setSelected(!item.isSelected()); 
                    listTests.repaint(); 
                    updateButtonsState(); 
                }
            }
        });

        JScrollPane scrollTests = new JScrollPane(listTests);
        scrollTests.setPreferredSize(new Dimension(300, 100));

        btnUpload = new JButton("Select Excel File");
        btnUpload.setPreferredSize(new Dimension(300, 35));
        btnUpload.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnUpload.setEnabled(false); 

        btnRun = new JButton("Run Automation");
        btnRun.setPreferredSize(new Dimension(300, 40));
        btnRun.setBackground(new Color(40, 167, 69)); // Green
        btnRun.setForeground(Color.WHITE);
        btnRun.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRun.setEnabled(false); 

        btnViewReports = new JButton("📂 View Reports Folder");
        btnViewReports.setPreferredSize(new Dimension(300, 35));
        btnViewReports.setBackground(new Color(70, 130, 180)); 
        btnViewReports.setForeground(Color.WHITE);
        btnViewReports.setFont(new Font("Segoe UI", Font.BOLD, 12));

        lblStatus = new JLabel("Status: Select a test...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // --- BUTTON ACTIONS ---

        btnUpload.addActionListener(e -> {
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

        btnViewReports.addActionListener(e -> {
            try {
                File reportFolder = new File(APP_DIR + File.separator + "Reports");
                if (!reportFolder.exists()) {
                    JOptionPane.showMessageDialog(frame, "No reports found yet.\nRun a test to generate one.");
                } else {
                    Desktop.getDesktop().open(reportFolder);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error opening folder: " + ex.getMessage());
            }
        });

        btnRun.addActionListener(e -> {
            lblStatus.setText("Status: Initializing...");
            toggleControls(false); 
            showProgressDialog(frame);

            new Thread(() -> {
                try {
                    if (selectedPath[0] != null) {
                        System.setProperty("custom.excel.path", selectedPath[0]);
                    }

                    XmlSuite suite = new XmlSuite();
                    suite.setName("ResIQ_Multi_Suite");
                    XmlTest test = new XmlTest(suite);
                    test.setName("Selected_Tests_Run");
                    
                    List<XmlClass> classes = new ArrayList<>();
                    
                    for (int i = 0; i < listModel.size(); i++) {
                        TestListItem item = listModel.get(i);
                        if (item.isSelected()) {
                            String name = item.toString();
                            if (name.contains("Create Projects")) {
                                classes.add(new XmlClass(Project_Reports_DataDriven.class));
                            } 
                            else if (name.contains("Count Projects")) {
                                classes.add(new XmlClass(Project_Count_Test.class));
                            }
                            else if (name.contains("Grid UI")) {
                                classes.add(new XmlClass(Project_Grid_Test.class));
                            }
                        }
                    }

                    test.setXmlClasses(classes);
                    List<XmlSuite> suites = new ArrayList<>();
                    suites.add(suite);

                    TestNG testng = new TestNG();
                    testng.setXmlSuites(suites);
                    testng.run();
                    
                    progressDialog.dispose(); 
                    lblStatus.setText("Status: Completed!");
                    
                    int choice = JOptionPane.showConfirmDialog(frame, 
                        "Execution Finished! Do you want to open the report?", 
                        "Success", JOptionPane.YES_NO_OPTION);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        openLatestReport(); // THIS METHOD IS FIXED BELOW
                    }
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if(progressDialog != null) progressDialog.dispose();
                    lblStatus.setText("Error: Execution Failed");
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                } finally {
                    toggleControls(true); 
                }
            }).start();
        });

        frame.add(lblTitle, gbc);
        gbc.gridy++;
        frame.add(lblSelect, gbc);
        gbc.gridy++;
        frame.add(scrollTests, gbc); 
        gbc.gridy++;
        frame.add(btnUpload, gbc);
        gbc.gridy++;
        frame.add(btnRun, gbc);
        gbc.gridy++;
        frame.add(btnViewReports, gbc); 
        gbc.gridy++;
        frame.add(lblStatus, gbc);

        frame.setVisible(true);
    }

    private static void updateButtonsState() {
        boolean excelRequired = false;
        boolean anySelected = false;

        for (int i = 0; i < listModel.size(); i++) {
            TestListItem item = listModel.get(i);
            if (item.isSelected()) {
                anySelected = true;
                String name = item.toString();
                if (name.contains("Create Projects") || name.contains("Count Projects")) {
                    excelRequired = true;
                }
            }
        }

        if (excelRequired) {
            btnUpload.setEnabled(true);
            btnUpload.setText("Select Excel File");
            if (selectedPath[0] != null) {
                btnRun.setEnabled(true);
                lblStatus.setText("Status: File selected");
            } else {
                btnRun.setEnabled(false);
                lblStatus.setText("Status: Excel file required");
            }
        } else {
            btnUpload.setEnabled(false);
            btnUpload.setText("Excel Not Required");
            btnRun.setEnabled(anySelected);
            if(anySelected) lblStatus.setText("Status: Ready");
            else lblStatus.setText("Status: Select a test...");
        }
    }

    private static void toggleControls(boolean enable) {
        btnRun.setEnabled(enable);
        btnUpload.setEnabled(enable && btnUpload.getText().contains("Select Excel"));
        listTests.setEnabled(enable);
        btnViewReports.setEnabled(enable);
    }

    private static void showProgressDialog(JFrame parent) {
        progressDialog = new JDialog(parent, "Executing...", false); 
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(parent);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setUndecorated(true);
        ((JComponent)progressDialog.getContentPane()).setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JLabel msgLabel = new JLabel("Executing Script(s)... Please Wait", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        msgLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); 
        progressBar.setForeground(new Color(40, 167, 69));
        progressDialog.add(msgLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setVisible(true);
    }

    // --- FIX: OPEN ACTUAL HTML FILE INSTEAD OF FOLDER ---
    private static void openLatestReport() {
        try {
            File reportRoot = new File(APP_DIR + File.separator + "Reports");
            if (reportRoot.exists() && reportRoot.isDirectory()) {
                File[] subFolders = reportRoot.listFiles(File::isDirectory);
                if (subFolders != null && subFolders.length > 0) {
                    // 1. Sort folders by newest
                    Arrays.sort(subFolders, Comparator.comparingLong(File::lastModified).reversed());
                    File newestFolder = subFolders[0];
                    
                    // 2. Find the .html file inside that newest folder
                    File[] htmlFiles = newestFolder.listFiles((dir, name) -> name.endsWith(".html"));
                    
                    if (htmlFiles != null && htmlFiles.length > 0) {
                        // 3. Open the file directly
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(htmlFiles[0]);
                        }
                    } else {
                        // Fallback: If no HTML file found, open the folder
                        Desktop.getDesktop().open(newestFolder);
                    }
                }
            }
        } catch (Exception e) { 
            System.out.println("Could not open report: " + e.getMessage());
        }
    }

    // --- INNER CLASSES ---
    static class TestListItem {
        private String label;
        private boolean isSelected = false;
        public TestListItem(String label) { this.label = label; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean isSelected) { this.isSelected = isSelected; }
        @Override public String toString() { return label; }
    }

    static class CheckboxListRenderer extends JCheckBox implements ListCellRenderer<TestListItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends TestListItem> list, TestListItem value, int index, boolean isSelected, boolean cellHasFocus) {
            setEnabled(list.isEnabled());
            setSelected(value.isSelected());
            setFont(list.getFont());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setText(value.toString());
            return this;
        }
    }
}