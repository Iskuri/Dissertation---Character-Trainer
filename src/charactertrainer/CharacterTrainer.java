/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package charactertrainer;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Wade
 */
public class CharacterTrainer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        // TODO code application logic here
        
        //Setup frame and panel to view characters being processed and open custom panel.
        
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.add(new DrawSpace());
                
        frame.add(panel);
        frame.setSize(100, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.pack(); 
               
    }
}
