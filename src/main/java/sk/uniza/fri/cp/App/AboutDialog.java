package sk.uniza.fri.cp.App;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AboutDialog extends JDialog {
    public AboutDialog(JFrame parent) {
        super(parent, "O programe", true);

        Box b = Box.createVerticalBox();
        b.add(Box.createGlue());
        b.add(new JLabel("                     CPU emulator v. 1.3.0"));
        b.add(new JLabel("  "));
        b.add(new JLabel("                      Autor: Tomáš Hianik"));
        b.add(new JLabel("Vytvorené ako bakalárska práca v r. 2017"));
        b.add(Box.createGlue());
        getContentPane().add(b, "Center");

        JPanel p2 = new JPanel();
        JButton ok = new JButton("Ok");
        p2.add(ok);
        getContentPane().add(p2, "South");

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });

        setSize(256, 150);
        setLocation(400,300);
    }
}