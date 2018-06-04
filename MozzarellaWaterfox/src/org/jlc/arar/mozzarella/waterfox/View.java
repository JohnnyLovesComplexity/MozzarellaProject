package org.jlc.arar.mozzarella.waterfox;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTabbedPane;


//////////////////////////////////////
////// INTERFACE POUR LE CLIENT //////
//////////////////////////////////////

//A LANCER AU LIEU DU MAIN DANS LE CLIENT
public class View extends JFrame {
    private static final long serialVersionUID = -1374851023131011832L;

    private JPanel contentPane;

    private JTextArea txtHeaderArea;
    private JTextArea txtPageArea;

    private JScrollPane scrollHeaderPane;
    private JScrollPane scrollPagePane;

    private JProgressBar progressBar;

    private JLabel statusLabel;

    private JTextField txtAdresse;
    private JTextField txtFile;

    private JButton btnStart;
    private JButton btnPut;
    private JPanel panelPage;
    JLabel lblImage;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    View frame = new View();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public View() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(578, 542);
        setLocationRelativeTo(null);

        statusLabel = new JLabel("Statut");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        JPanel statusPanel = new JPanel();
        statusPanel.setBounds(2, 496, 573, 20);
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusPanel.setLayout(new GridLayout(1, 2));
        statusPanel.add(statusLabel);
        statusPanel.add(progressBar);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        contentPane.add(statusPanel);
        setContentPane(contentPane);

//        JLabel lblAdresse = new JLabel("Adresse");
//        lblAdresse.setBounds(6, 16, 61, 16);
//        contentPane.add(lblAdresse);

//        txtAdresse = new JTextField();
//        txtAdresse.setBounds(79, 10, 183, 28);
//        contentPane.add(txtAdresse);
//        txtAdresse.setColumns(10);
//
//        txtFile = new JTextField();
//        txtFile.setColumns(10);
//        txtFile.setBounds(279, 10, 183, 28);
//        contentPane.add(txtFile);

//        JLabel label = new JLabel(" /");
//        label.setBounds(264, 17, 15, 16);
//        contentPane.add(label);

//          btnStart = new JButton("START");
//          btnStart.setBounds(200, 10, 98, 30);
//          btnStart.addActionListener(this);
//          contentPane.add(btnStart);
//
//        btnPut = new JButton(("PUT"));
//        btnPut.setBounds(474, 40, 98, 30);
//        btnPut.addActionListener(this);
//        contentPane.add(btnPut);


        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(5, 44, 573, 451);
        contentPane.add(tabbedPane);

        JPanel panelCode = new JPanel();
        tabbedPane.addTab("Code", null, panelCode, null);
        panelCode.setLayout(null);

        txtHeaderArea = new JTextArea();
        txtHeaderArea.setEditable(false);
        scrollHeaderPane = new JScrollPane(txtHeaderArea);
        scrollHeaderPane.setBounds(6, 6, 540, 140);
        panelCode.add(scrollHeaderPane);

        txtPageArea = new JTextArea();
        txtPageArea.setEditable(false);
        scrollPagePane = new JScrollPane(txtPageArea);
        scrollPagePane.setBounds(6, 150, 540, 252);
        panelCode.add(scrollPagePane);

        panelPage = new JPanel();
        tabbedPane.addTab("Image", null, panelPage, null);
        panelPage.setLayout(null);

        lblImage = new JLabel("");
        lblImage.setBounds(6, 6, 540, 393);
        panelPage.add(lblImage);

        Waterfox client = new Waterfox();
        while(client.message == "") {}
        txtPageArea.append(client.message);
        statusLabel.setText("Reçu");

        if( client.path.contains("jpg")) {
            lblImage.setIcon(new ImageIcon(client.message.getBytes()));
        }

    }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//        if(e.getSource() == btnStart) {
//            txtPageArea.setText("");
//            txtHeaderArea.setText("");
//            statusLabel.setText("Reception");
//            lblImage.setIcon(null);

//            String commande ="GET /";
//            commande+=txtAdresse.getText();
//            commande+="/";
//            commande+=txtFile.getText();
//            commande+=" HTTP/1.1";



}
