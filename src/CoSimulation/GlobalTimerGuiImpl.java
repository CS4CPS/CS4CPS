package CoSimulation;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class GlobalTimerGuiImpl extends JFrame implements GlobalTimerGui {
    
    private GlobalTimer myAgent;
	
    public void setAgent(GlobalTimer a) {
        myAgent = a;
        setTitle(myAgent.getLocalName());
    }
	
    private JTextField tfNet, tfTime;
    private JButton btNet, btReset, btRun;
    private JTextArea logTA;
    
    public GlobalTimerGuiImpl() {
        super();
        
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        } );

        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new GridBagLayout());
        rootPanel.setMinimumSize(new Dimension(430, 125));
        rootPanel.setPreferredSize(new Dimension(430, 125));
        GridBagConstraints gridBagConstraints;
        
        // Line 0 --------------------------------------------------------------
        JLabel l = new JLabel("IP;Port:");
        l.setHorizontalAlignment(SwingConstants.LEFT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 3, 0, 3);
        rootPanel.add(l, gridBagConstraints);

        tfNet = new JTextField(64);
        tfNet.setMinimumSize(new Dimension(146, 20));
        tfNet.setPreferredSize(new Dimension(146, 20));
        //tfNet.setText("49.123.120.232;6666");
        tfNet.setText("10.0.2.15;6666");
        //tfNet.setText("192.168.43.87;6666");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 3, 0, 3);
        rootPanel.add(tfNet, gridBagConstraints);

        btNet = new JButton("Start");
        btNet.setMinimumSize(new Dimension(70, 20));
        btNet.setPreferredSize(new Dimension(70, 20));
        btNet.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String[] tmpIPPort =tfNet.getText().split(";"); 
                myAgent.MAS_init(tmpIPPort[0], Integer.parseInt(tmpIPPort[1]));                
            }
        } );
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 3, 0, 3);
        rootPanel.add(btNet, gridBagConstraints);    

        // Line 1 --------------------------------------------------------------
        l = new JLabel("Time");
        l.setHorizontalAlignment(SwingConstants.LEFT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 3, 0, 3);
        rootPanel.add(l, gridBagConstraints);

        tfTime = new JTextField(64);
        tfTime.setMinimumSize(new Dimension(73, 20));
        tfTime.setPreferredSize(new Dimension(73, 20));
        tfTime.setText("50");
        //tfTime.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 3, 0, 3);
        rootPanel.add(tfTime, gridBagConstraints);
        
        btReset = new JButton("Reset");
        btReset.setMinimumSize(new Dimension(70, 20));
        btReset.setPreferredSize(new Dimension(70, 20));
        btReset.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                myAgent.MAS_reset();
            }
        } );
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 3, 0, 3);
        rootPanel.add(btReset, gridBagConstraints);    

        btRun = new JButton("Run");
        btRun.setMinimumSize(new Dimension(70, 20));
        btRun.setPreferredSize(new Dimension(70, 20));
        btRun.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int tmpSimTime = Integer.parseInt(tfTime.getText()); 
                if(tmpSimTime > 0)
                    myAgent.MAS_run(tmpSimTime);
            }
        } );
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 3, 0, 3);
        rootPanel.add(btRun, gridBagConstraints);    
        
        rootPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        getContentPane().add(rootPanel, BorderLayout.NORTH);

        // Line 3 --------------------------------------------------------------        
        logTA = new JTextArea();
        //logTA.setEnabled(false);
        JScrollPane jsp = new JScrollPane(logTA);
        jsp.setMinimumSize(new Dimension(400, 380));
        jsp.setPreferredSize(new Dimension(400, 380));
        JPanel p = new JPanel();
        p.setBorder(new BevelBorder(BevelBorder.LOWERED));
        p.add(jsp);
        getContentPane().add(p, BorderLayout.SOUTH);

        pack();

        setResizable(false);
    }
	
    public void notifyUser(String message) {
        logTA.append(message);
    }
}
