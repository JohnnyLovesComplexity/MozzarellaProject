package org.jlc.arar.mozzarella.waterfox;

import java.awt.EventQueue;
import java.nio.charset.StandardCharsets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class View extends JFrame {
    private static final long serialVersionUID = -1374851023131011832L;

    private JTextField txtAdresse;
    private JTextField txtFile;

    private JButton btnStart;
    private JButton btnPut;
    JLabel labelImage;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    View frame = new View();
                    if(Waterfox.quitter)
                        frame.dispose();
                    else
                        frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public View() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(578, 542);
        setLocationRelativeTo(null);

        JLabel statusLabel = new JLabel("Statut");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPanel.setLayout(null);
        setContentPane(contentPanel);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(5, 44, 573, 451);
        contentPanel.add(tabbedPane);

        JPanel panelText = new JPanel();
        tabbedPane.addTab("Text", null, panelText, null);
        panelText.setLayout(null);

        JTextArea headerArea = new JTextArea();
        headerArea.setEditable(false);
        JScrollPane scrollHeaderPane = new JScrollPane(headerArea);
        scrollHeaderPane.setBounds(6, 6, 540, 140);

        JTextArea txtPageArea = new JTextArea();
        txtPageArea.setEditable(false);
        JScrollPane scrollPagePane = new JScrollPane(txtPageArea);
        scrollPagePane.setBounds(6, 150, 540, 252);
        panelText.add(scrollPagePane);

        JPanel panelPage = new JPanel();
        tabbedPane.addTab("Image", null, panelPage, null);
        panelPage.setLayout(null);

        labelImage = new JLabel("");
        labelImage.setBounds(6, 6, 540, 393);
        panelPage.add(labelImage);

        Waterfox client = new Waterfox();
        if (Waterfox.data != null) {
            String data = new String(Waterfox.data, StandardCharsets.UTF_8);
            while(data.equals("")) {}
            txtPageArea.append(data);
            statusLabel.setText("Reçu");

            if( Waterfox.path.contains("jpg")) {
                labelImage.setIcon(new ImageIcon(Waterfox.data));
            }
        }
    }
}
