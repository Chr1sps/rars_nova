package rars.venus.run;

import rars.Globals;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Class for the Run speed slider control. One is created and can be obtained
 * using
 * getInstance().
 *
 * @author Pete Sanderson
 * @version August 2005
 */
public class RunSpeedPanel extends JPanel {
    /**
     * Constant that represents unlimited run speed. Compare with return value of
     * getRunSpeed() to determine if set to unlimited. At the unlimited setting, the
     * GUI
     * will not attempt to update register and memory contents as each instruction
     * is executed. This is the only possible value for command-line use of Mars.
     */
    public final static double UNLIMITED_SPEED = 40;

    private final static int SPEED_INDEX_MIN = 0;
    private final static int SPEED_INDEX_MAX = 40;
    private final static int SPEED_INDEX_INIT = 40;
    private static final int SPEED_INDEX_INTERACTION_LIMIT = 35;
    private final double[] speedTable = {
            .05, .1, .2, .3, .4, .5, 1, 2, 3, 4, 5, // 0-10
            6, 7, 8, 9, 10, 11, 12, 13, 14, 15, // 11-20
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // 21-30
            26, 27, 28, 29, 30, RunSpeedPanel.UNLIMITED_SPEED, RunSpeedPanel.UNLIMITED_SPEED, // 31-37
            RunSpeedPanel.UNLIMITED_SPEED, RunSpeedPanel.UNLIMITED_SPEED, RunSpeedPanel.UNLIMITED_SPEED // 38-40
    };
    private JLabel sliderLabel = null;
    private static RunSpeedPanel runSpeedPanel = null;
    private volatile int runSpeedIndex = RunSpeedPanel.SPEED_INDEX_MAX;

    /**
     * Retrieve the run speed panel object
     *
     * @return the run speed panel
     */
    public static RunSpeedPanel getInstance() {
        if (RunSpeedPanel.runSpeedPanel == null) {
            RunSpeedPanel.runSpeedPanel = new RunSpeedPanel();
            Globals.runSpeedPanelExists = true; // DPS 24 July 2008 (needed for standalone tools)
        }
        return RunSpeedPanel.runSpeedPanel;
    }

    /*
     * private constructor (this is a singleton class)
     */
    private RunSpeedPanel() {
        super(new BorderLayout());
        final JSlider runSpeedSlider = new JSlider(JSlider.HORIZONTAL, RunSpeedPanel.SPEED_INDEX_MIN, RunSpeedPanel.SPEED_INDEX_MAX, RunSpeedPanel.SPEED_INDEX_INIT);
        runSpeedSlider.setSize(new Dimension(100, (int) runSpeedSlider.getSize().getHeight()));
        runSpeedSlider.setMaximumSize(runSpeedSlider.getSize());
        runSpeedSlider.setMajorTickSpacing(5);
        runSpeedSlider.setPaintTicks(true); // Create the label table
        runSpeedSlider.addChangeListener(new RunSpeedListener());
        this.sliderLabel = new JLabel(this.setLabel(this.runSpeedIndex));
        this.sliderLabel.setHorizontalAlignment(JLabel.CENTER);
        this.sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(this.sliderLabel, BorderLayout.NORTH);
        this.add(runSpeedSlider, BorderLayout.CENTER);
        this.setToolTipText("Simulation speed for \"Go\".  At " +
                ((int) this.speedTable[RunSpeedPanel.SPEED_INDEX_INTERACTION_LIMIT]) + " inst/sec or less, tables updated " +
                "after each instruction.");
    }

    /**
     * returns current run speed setting, in instructions/second. Unlimited speed
     * setting is equal to RunSpeedPanel.UNLIMITED_SPEED
     *
     * @return run speed setting in instructions/second.
     */
    public double getRunSpeed() {
        return this.speedTable[this.runSpeedIndex];
    }

    /*
     * set label wording depending on current speed setting
     */
    private String setLabel(final int index) {
        String result = "Run speed ";
        if (index <= RunSpeedPanel.SPEED_INDEX_INTERACTION_LIMIT) {
            if (this.speedTable[index] < 1) {
                result += this.speedTable[index];
            } else {
                result += ((int) this.speedTable[index]);
            }
            result += " inst/sec";
        } else {
            result += ("at max (no interaction)");
        }
        return result;
    }

    /*
     * Both revises label as user slides and updates current index when sliding
     * stops.
     */

    private class RunSpeedListener implements ChangeListener {
        @Override
        public void stateChanged(final ChangeEvent e) {
            final JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                RunSpeedPanel.this.runSpeedIndex = source.getValue();
            } else {
                RunSpeedPanel.this.sliderLabel.setText(RunSpeedPanel.this.setLabel(source.getValue()));
            }
        }
    }
}