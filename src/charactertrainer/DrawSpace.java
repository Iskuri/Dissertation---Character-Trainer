/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package charactertrainer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Wade
 */
class DrawSpace extends JPanel
{
    private int xRes = 8;
    private int yRes = 8;

    //private NeuralNetwork network = new NeuralNetwork(new int[] {xRes*yRes+2,62,62,62});
    private NeuralNetwork network = new NeuralNetwork(new int[] {xRes*yRes+2,92,62});

    public static String[] characterLoop;
    private Font[] fontList;

    private int fontInc = 0;
    private int characterInc = 0;

    int errorInc = 0;

    double errorCount = 0;
    double errorSum = 0;
    double allError = 0;

    boolean stopTraining;

    public DrawSpace()
    {
        setPreferredSize(new Dimension(100,100));


        generateCharList();

        getUseFonts();


        //Gather saved weights from XML file. Try/catch auto-generated.
        try {
            network.xmlTrain();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DrawSpace.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(DrawSpace.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DrawSpace.class.getName()).log(Level.SEVERE, null, ex);
        }


        //Runtime.getRuntime().exit(0);
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;

        //Setup Image to draw and select character and font

        BufferedImage img = (BufferedImage) createImage(100,100);
        Graphics imgGraphics = img.getGraphics();

        imgGraphics.setFont(fontList[fontInc]);
        imgGraphics.drawString(characterLoop[characterInc], 50, 50);

            int xLow = 100;
            int xHigh = 0;
            int yLow = 100;
            int yHigh = 0;

            //Convert Image to grayscale and check for input values

            for(int i = 0 ; i < 100 ; i++)
            {
                for(int j = 0 ; j < 100 ; j++)
                {
                    int grayScale = grayScale(img.getRGB(i, j));

                    if(grayScale<128)
                    {
                        if(xLow>i)
                        {
                            xLow = i;
                        }
                        if(xHigh<i)
                        {
                            xHigh = i;
                        }
                        if(yLow>j)
                        {
                            yLow = j;
                        }
                        if(yHigh<j)
                        {
                            yHigh = j;
                        }
                    }

                }
            }


            //Get bounds of character and put into new subimage

            int xDiv = (xHigh-xLow);
            int yDiv = (yHigh-yLow);

            BufferedImage sub = img.getSubimage(xLow, yLow, xDiv+1, yDiv+1);

            double div = sub.getWidth()+sub.getHeight();
            double wid = sub.getWidth();
            double hei = sub.getHeight();

            sub = reScale(sub);
            int neuronCount = 0 ;


            //Loop through rescaled image, adding inputs to the network as grayscale values
            for(int i = 0 ; i < sub.getWidth() ; i++)
            {
                for(int j = 0 ; j < sub.getHeight() ; j++)
                {
                    //System.out.println(i+" : "+j);
                    double calc = grayScale(sub.getRGB(i, j));
                    //System.out.println(calc);
                    //System.out.println(i+" : "+j+" : "+calc);
                    network.setInput(neuronCount, calc/255);
                    neuronCount++;
                }
            }

            //Add normalised width and height to the final two inputs
            network.setInput(neuronCount,wid/div);
            neuronCount++;
            network.setInput(neuronCount,hei/div);


        //Train the Neural Network and gather the errors
        networkTrain(characterInc);

        calcError(characterInc);

        characterInc++;

        if(characterInc >= characterLoop.length)
        {
            //Change errors and character, and represent some values

            characterInc = 0;

            //errorSum = errorSum/errorCount;

            System.out.println("ERROR IS: "+errorInc+" : "+errorSum);

            allError += errorSum;

            if(errorSum>0.2)
            {
                stopTraining=true;
            }

            errorSum = 0;
            errorCount = 0;
            errorInc = 0;

            fontInc++;

            if(fontInc >= fontList.length)
            {
                //Save network training to XML file.

                try
                {
                    network.xmlWrite();
                }
                catch (Exception ex)
                {
                    Logger.getLogger(DrawSpace.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println("XML SAVED! "+allError);
                allError = 0;
                fontInc = 0;

                if(stopTraining==false)
                {
                    Runtime.getRuntime().exit(0);
                }

                stopTraining = false;
            }

            //System.out.println(fontList[fontInc].getName());
        }

        //System.out.println(characterLoop[characterInc]);

        //g2d.drawImage(img,0, 0,null);
        g2d.drawImage(sub, 50 , 50, null);

        repaint();
    }

    private BufferedImage reScale(BufferedImage tempMap)
    {
        //Rescale image presented to fit the 8*8 grid I chose for network inputs.

        BufferedImage scaledImage = new BufferedImage(xRes, yRes, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(tempMap, 0, 0, xRes, yRes, null);
        g2d.dispose();

        return scaledImage;
    }

    private void calcError(int chosenChar)
    {
        //Calculates the errors of the network

        double[] outArr = network.getAllOutputs();

        int highestChar = 0;
        double highestOut = -1;

        for(int i = 0 ; i < outArr.length ; i++)
        {
            //Check for highest output neuron
            if(outArr[i] > highestOut)
            {
                highestOut = outArr[i];
                highestChar = i;
            }

            //Check if it is the correct neuron and add to error
            if(i==chosenChar)
            {
                double tempErr = 1-outArr[i];
                //System.out.println(tempErr);
                errorSum += Math.pow(tempErr, 2);
            }
            else
            {
                double tempErr = 0-outArr[i];
                //System.out.println(tempErr);
                errorSum += Math.pow(tempErr, 2);
            }



            errorCount++;
       }


        //Add to cumulative error if incorrect
        if(highestChar!=chosenChar)
        {
            //stopTraining = true;
            errorInc++;
        }


    }


    private void networkTrain(int currChar)
    {
        //Train each neuron to be 1 or 0 depending on character

        for(int i = 0 ; i < characterLoop.length ; i++)
        {
            if(i == currChar)
            {
                network.trainNetwork(i,1);
            }
            else
            {
                network.trainNetwork(i, 0);
            }
        }
    }


    private int grayScale(int rgb)
    {
        //Convert rgb to grayscale, standard function

        double r = (rgb & 0xFF0000) >> 16;
        double g = (rgb & 0xFF00) >> 8;
        double b = (rgb & 0xFF);
        return (int)(r*0.3+g*0.59+b*0.11);
    }


    private void getUseFonts()
    {
        //Create an array of font classes for use as training data along with alphanumeric characters

        String[] fontStrings = {"Courier New", "Courier New Bold", "Courier New Italic",
            "Arial", "Arial Bold", "Arial Italic",
            "Times New Roman","Times New Roman Bold","Times New Roman Italic",
            "Comic Sans MS","Comic Sans MS Bold","Comic Sans MS Italic","Segoe Script",
            "Segoe Script Bold","Segoe Script Italic","Gabriola",
            "MV Boli","Tunga","Vani","Vijaya","Impact","GungsuhChe","Segoe Print","Xirod",
            "Roman","Dotum","Gisha","Lucida Console","Nyala","Miriam Fixed","Franklin Gothic Medium",
            "MoolBoran","KaiTi"};


//        String[] fontStrings = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        fontList = new Font[fontStrings.length];

        for(int i = 0 ; i < fontList.length ; i++)
        {
            fontList[i] = new Font(fontStrings[i], Font.PLAIN, 18);
        }
    }


    private void generateCharList()
    {
        //Generate list of alphanumeric characters that will be trained

        characterLoop = new String[62];
        //characterLoop = new String[10];
        int charCount = 0;

        for(int i = 48 ; i < 58 ; i++)
        {
            characterLoop[charCount] = Character.toString((char)i);
            charCount++;
        }

        for(int i = 65; i < 91 ; i++)
        {
            characterLoop[charCount] = Character.toString((char)i);
            charCount++;
        }

        for(int i = 97; i < 123 ; i++)
        {
            characterLoop[charCount] = Character.toString((char)i);
            charCount++;
        }

        /*for(int i = 0 ; i  < characterLoop.length ; i++)
        {
            System.out.println(characterLoop[i]);
        }*/

    }

}
