package rars.venus;

import rars.Globals;
import rars.assembler.Directive;
import rars.riscv.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Help -> Help menu item
 */
public class HelpHelpAction extends GuiAction {
    /**
     * Separates Instruction name descriptor from detailed (operation) description
     * in help string.
     */
    public static final String descriptionDetailSeparator = ":";
    // Light gray background color for alternating lines of the instruction lists
    static final Color altBackgroundColor = new Color(0xEE, 0xEE, 0xEE);
    private final VenusUI mainUI;

    /**
     * <p>Constructor for HelpHelpAction.</p>
     *
     * @param name     a {@link java.lang.String} object
     * @param icon     a {@link javax.swing.Icon} object
     * @param descrip  a {@link java.lang.String} object
     * @param mnemonic a {@link java.lang.Integer} object
     * @param accel    a {@link javax.swing.KeyStroke} object
     * @param gui      a {@link VenusUI} object
     */
    public HelpHelpAction(final String name, final Icon icon, final String descrip,
                          final Integer mnemonic, final KeyStroke accel, final VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel);
        this.mainUI = gui;
    }

    // ideally read or computed from config file...
    private static Dimension getSize() {
        return new Dimension(800, 600);
    }

    /////////////////////////////////////////////////////////////////////////////
    private static JScrollPane createDirectivesHelpPane() {
        final Vector<String> exampleList = new Vector<>();
        final String blanks = "            "; // 12 blanks
        for (final Directive direct : Directive.getDirectiveList()) {
            exampleList.add(direct.toString()
                    + blanks.substring(0, Math.max(0, blanks.length() - direct.toString().length()))
                    + direct.getDescription());
        }
        Collections.sort(exampleList);
        final JList<String> examples = new JList<>(exampleList);
        final JScrollPane scrollPane = new JScrollPane(examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        examples.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return scrollPane;
    }

    ////////////////////////////////////////////////////////////////////////////
    private static JScrollPane createInstructionHelpPane(final Class<? extends Instruction> instructionClass) {
        final ArrayList<Instruction> instructionList = Globals.instructionSet.getInstructionList();
        final Vector<String> exampleList = new Vector<>(instructionList.size());
        final String blanks = "                        "; // 24 blanks
        for (final Instruction instr : instructionList) {
            if (instructionClass.isInstance(instr)) {
                exampleList.add(instr.getExampleFormat()
                        + blanks.substring(0, Math.max(0, blanks.length() - instr.getExampleFormat().length()))
                        + instr.getDescription());
            }
        }
        Collections.sort(exampleList);
        final JList<String> examples = new JList<>(exampleList);
        final JScrollPane scrollPane = new JScrollPane(examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        examples.setFont(new Font("Monospaced", Font.PLAIN, 12));
        examples.setCellRenderer(new MyCellRenderer());
        return scrollPane;
    }

    private static StringBuilder convertToHTMLTable(final String[][] data, final String[] headers) {
        final StringBuilder sb = new StringBuilder("<table border=1>");
        sb.append("<tr>");
        for (final String elem : headers) {
            sb.append("<td>").append(elem).append("</td>");
        }
        sb.append("</tr>");
        for (final String[] row : data) {
            sb.append("<tr>");
            for (final String elem : row) {
                sb.append("<td>").append(elem).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Displays tabs with categories of information
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("RISCV", this.createHelpInfoPanel());
        tabbedPane.addTab("RARS", this.createRarsHelpInfoPanel());
        tabbedPane.addTab("License", this.createCopyrightInfoPanel());
        tabbedPane.addTab("Bugs/Comments", this.createHTMLHelpPanel("BugReportingHelp.html"));
        tabbedPane.addTab("Acknowledgements", this.createHTMLHelpPanel("Acknowledgements.html"));
        // Create non-modal dialog. Based on java.sun.com "How to Make Dialogs",
        // DialogDemo.java
        final JDialog dialog = new JDialog(this.mainUI, "RARS " + Globals.version + " Help");
        // assure the dialog goes away if user clicks the X
        dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                });
        // Add a "close" button to the non-modal help dialog.
        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(
                e1 -> {
                    dialog.setVisible(false);
                    dialog.dispose();
                });
        final JPanel closePanel = new JPanel();
        closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.LINE_AXIS));
        closePanel.add(Box.createHorizontalGlue());
        closePanel.add(closeButton);
        closePanel.add(Box.createHorizontalGlue());
        closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(tabbedPane);
        contentPane.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPane.add(closePanel);
        contentPane.setOpaque(true);
        dialog.setContentPane(contentPane);
        // Show it.
        dialog.setSize(HelpHelpAction.getSize());
        dialog.setLocationRelativeTo(this.mainUI);
        dialog.setVisible(true);

        //////////////////////////////////////////////////////////////////
    }

    // Create panel containing Help Info read from html document.
    private JPanel createHTMLHelpPanel(final String filename) {
        final JPanel helpPanel = new JPanel(new BorderLayout());
        JScrollPane helpScrollPane;
        final JEditorPane helpDisplay;
        try {
            final StringBuilder text = this.loadFiletoStringBuilder(Globals.helpPath + filename);
            helpDisplay = new JEditorPane("text/html", text.toString());
            helpDisplay.setEditable(false);
            helpDisplay.setCaretPosition(0); // assure top of document displayed
            helpScrollPane = new JScrollPane(helpDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            helpDisplay.addHyperlinkListener(new HelpHyperlinkListener());
        } catch (final Exception ie) {
            helpScrollPane = new JScrollPane(
                    new JLabel("Error (" + ie + "): " + filename + " contents could not be loaded."));
        }
        helpPanel.add(helpScrollPane);
        return helpPanel;
    }

    /////////////// Methods to construct MIPS help tabs from internal MARS objects
    /////////////// //////////////

    // Set up the copyright notice for display.
    private JPanel createCopyrightInfoPanel() {
        final JPanel copyrightInfo = new JPanel(new BorderLayout());
        JScrollPane copyrightScrollPane;
        final JEditorPane copyrightDisplay;
        try {
            final StringBuilder text = this.loadFiletoStringBuilder("/License.txt").append("</pre>");
            copyrightDisplay = new JEditorPane("text/html", "<pre>" + text);
            copyrightDisplay.setEditable(false);
            copyrightDisplay.setCaretPosition(0); // assure top of document displayed
            copyrightScrollPane = new JScrollPane(copyrightDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        } catch (final Exception ioe) {
            copyrightScrollPane = new JScrollPane(
                    new JLabel("Error: license contents could not be loaded."));
        }
        copyrightInfo.add(copyrightScrollPane);
        return copyrightInfo;
    }

    // Set up MARS help tab. Subtabs get their contents from HTML files.
    private JPanel createRarsHelpInfoPanel() {
        final JPanel helpInfo = new JPanel(new BorderLayout());
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Intro", this.createHTMLHelpPanel("Intro.html"));
        tabbedPane.addTab("IDE", this.createHTMLHelpPanel("IDE.html"));
        tabbedPane.addTab("Debugging", this.createHTMLHelpPanel("Debugging.html"));
        tabbedPane.addTab("Tools", this.createHTMLHelpPanel("Tools.html"));
        tabbedPane.addTab("Command", this.createHTMLHelpPanel("Command.html"));
        tabbedPane.addTab("Limits", this.createHTMLHelpPanel("Limits.html"));
        tabbedPane.addTab("History", this.createHTMLHelpPanel("History.html"));
        helpInfo.add(tabbedPane);
        return helpInfo;
    }

    // Set up MIPS help tab. Most contents are generated from instruction set info.
    private JPanel createHelpInfoPanel() {
        final JPanel helpInfo = new JPanel(new BorderLayout());
        final String helpRemarksColor = "CCFF99";
        // Introductory remarks go at the top as a label
        // TODO: update this to consider 12 and 20 bit numbers rather than 16
        final String helpRemarks = "<html><center><table bgcolor=\"#" + helpRemarksColor + "\" border=0 cellpadding=0>" + // width="+this.getSize().getWidth()+">"+
                "<tr>" +
                "<th colspan=2><b><i><font size=+1>&nbsp;&nbsp;Operand Key for Example Instructions&nbsp;&nbsp;</font></i></b></th>"
                +
                "</tr>" +
                "<tr>" +
                "<td><tt>label, target</tt></td><td>any textual label</td>" +
                "</tr><tr>" +
                "<td><tt>t1, t2, t3</tt></td><td>any integer register</td>" +
                "</tr><tr>" +
                "<td><tt>f2, f4, f6</tt></td><td><i>even-numbered</i> floating point register</td>" +
                "</tr><tr>" +
                "<td><tt>f0, f1, f3</tt></td><td><i>any</i> floating point register</td>" +
                "</tr><tr>" +
                "<td><tt>10</tt></td><td>unsigned 5-bit integer (0 to 31)</td>" +
                "</tr><tr>" +
                "<td><tt>-100</tt></td><td>signed 16-bit integer (-32768 to 32767)</td>" +
                "</tr><tr>" +
                "<td><tt>100</tt></td><td>unsigned 16-bit integer (0 to 65535)</td>" +
                "</tr><tr>" +
                "<td><tt>100000</tt></td><td>signed 32-bit integer (-2147483648 to 2147483647)</td>" +
                "</tr><tr>" +
                "</tr><tr>" +
                "<td colspan=2><b><i><font size=+1>Load & Store addressing mode, basic instructions</font></i></b></td>"
                +
                "</tr><tr>" +
                "<td><tt>-100(t2)</tt></td><td>sign-extended 16-bit integer added to contents of t2</td>" +
                "</tr><tr>" +
                "</tr><tr>" +
                "<td colspan=2><b><i><font size=+1>Load & Store addressing modes, pseudo instructions</font></i></b></td>"
                +
                "</tr><tr>" +
                "<td><tt>(t2)</tt></td><td>contents of t2</td>" +
                "</tr><tr>" +
                "<td><tt>-100</tt></td><td>signed 16-bit integer</td>" +
                "</tr><tr>" +
                "<td><tt>100</tt></td><td>unsigned 16-bit integer</td>" +
                "</tr><tr>" +
                "<td><tt>100000</tt></td><td>signed 32-bit integer</td>" +
                "</tr><tr>" +
                "<td><tt>100(t2)</tt></td><td>zero-extended unsigned 16-bit integer added to contents of t2</td>" +
                "</tr><tr>" +
                "<td><tt>100000(t2)</tt></td><td>signed 32-bit integer added to contents of t2</td>" +
                "</tr><tr>" +
                "<td><tt>label</tt></td><td>32-bit address of label</td>" +
                "</tr><tr>" +
                "<td><tt>label(t2)</tt></td><td>32-bit address of label added to contents of t2</td>" +
                "</tr><tr>" +
                "<td><tt>label+100000</tt></td><td>32-bit integer added to label's address</td>" +
                "</tr><tr>" +
                "<td><tt>label+100000(t2)&nbsp;&nbsp;&nbsp;</tt></td><td>sum of 32-bit integer, label's address, and contents of t2</td>"
                +
                "</tr>" +
                "</table></center></html>";
        // Original code: mipsHelpInfo.add(new JLabel(helpRemarks, JLabel.CENTER),
        // BorderLayout.NORTH);
        final JLabel helpRemarksLabel = new JLabel(helpRemarks, JLabel.CENTER);
        helpRemarksLabel.setOpaque(true);
        helpRemarksLabel.setBackground(Color.decode("0x" + helpRemarksColor));
        final JScrollPane operandsScrollPane = new JScrollPane(helpRemarksLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        helpInfo.add(operandsScrollPane, BorderLayout.NORTH);
        // Below the label is a tabbed pane with categories of MIPS help
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Basic Instructions", HelpHelpAction.createInstructionHelpPane(BasicInstruction.class));
        tabbedPane.addTab("Extended (pseudo) Instructions", HelpHelpAction.createInstructionHelpPane(ExtendedInstruction.class));
        tabbedPane.addTab("Directives", HelpHelpAction.createDirectivesHelpPane());
        tabbedPane.addTab("Syscalls", this.createSyscallsHelpPane());
        tabbedPane.addTab("Exceptions", this.createHTMLHelpPanel("ExceptionsHelp.html"));
        tabbedPane.addTab("Macros", this.createHTMLHelpPanel("MacrosHelp.html"));
        operandsScrollPane.setPreferredSize(
                new Dimension((int) HelpHelpAction.getSize().getWidth(), (int) (HelpHelpAction.getSize().getHeight() * .2)));
        operandsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        tabbedPane.setPreferredSize(
                new Dimension((int) HelpHelpAction.getSize().getWidth(), (int) (HelpHelpAction.getSize().getHeight() * .6)));
        final JSplitPane splitsville = new JSplitPane(JSplitPane.VERTICAL_SPLIT, operandsScrollPane, tabbedPane);
        splitsville.setOneTouchExpandable(true);
        splitsville.resetToPreferredSizes();
        helpInfo.add(splitsville);
        // mipsHelpInfo.add(tabbedPane);
        return helpInfo;
    }

    /*
     * Ideally, this would not use HTML to make the table, but the other methods
     * tried were far uglier / not useful.
     */
    private JScrollPane createSyscallsHelpPane() {
        final ArrayList<AbstractSyscall> list = SyscallLoader.getSyscallList();
        final String[] columnNames = {"Name", "Number", "Description", "Inputs", "Ouputs"};
        final String[][] data = new String[list.size()][5];
        Collections.sort(list);

        int i = 0;
        for (final AbstractSyscall syscall : list) {
            data[i][0] = syscall.getName();
            data[i][1] = Integer.toString(syscall.getNumber());
            data[i][2] = syscall.getDescription();
            data[i][3] = syscall.getInputs();
            data[i][4] = syscall.getOutputs();
            i++;
        }

        final JEditorPane html = new JEditorPane("text/html",
                this.loadFiletoStringBuilder(Globals.helpPath + "SyscallHelpPrelude.html") +
                        HelpHelpAction.convertToHTMLTable(data, columnNames).toString()
                        + this.loadFiletoStringBuilder(Globals.helpPath + "SyscallHelpConclusion.html"));

        html.setCaretPosition(0); // this affects scroll position
        html.setEditable(false);
        return new JScrollPane(html, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private StringBuilder loadFiletoStringBuilder(final String path) {
        final InputStream is = this.getClass().getResourceAsStream(path);
        final BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        final StringBuilder out = new StringBuilder();
        try {
            while ((line = in.readLine()) != null) {
                out.append(line).append("\n");
            }
            in.close();
        } catch (final IOException io) {
            return new StringBuilder(path + " could not be loaded.");
        }
        return out;

    }

    private static class MyCellRenderer extends JLabel implements ListCellRenderer<String> {
        // This is the only method defined by ListCellRenderer.
        // We just reconfigure the JLabel each time we're called.
        @Override
        public Component getListCellRendererComponent(
                final JList<? extends String> list, // the list
                final String s, // value to display
                final int index, // cell index
                final boolean isSelected, // is the cell selected
                final boolean cellHasFocus) // does the cell have focus
        {
            this.setText(s);
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
//                this.setBackground((index % 2 == 0) ? HelpHelpAction.altBackgroundColor : list.getBackground());
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            this.setEnabled(list.isEnabled());
            this.setFont(list.getFont());
            this.setOpaque(true);
            return this;
        }
    }

    /*
     * Determines MARS response when user click on hyperlink in displayed help page.
     * The response will be to pop up a simple dialog with the page contents. It
     * will not display URL, no navigation, nothing. Just display the page and
     * provide a Close button.
     */
    private class HelpHyperlinkListener implements HyperlinkListener {
        private static final String cannotDisplayMessage = "<html><title></title><body><strong>Unable to display requested document.</strong></body></html>";
        JDialog webpageDisplay;
        JTextField webpageURL;

        @Override
        public void hyperlinkUpdate(final HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                final JEditorPane pane = (JEditorPane) e.getSource();
                if (e instanceof final HTMLFrameHyperlinkEvent evt) {
                    final HTMLDocument doc = (HTMLDocument) pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                } else {
                    this.webpageDisplay = new JDialog(HelpHelpAction.this.mainUI, "Primitive HTML Viewer");
                    this.webpageDisplay.setLayout(new BorderLayout());
                    this.webpageDisplay.setLocation(HelpHelpAction.this.mainUI.getSize().width / 6, HelpHelpAction.this.mainUI.getSize().height / 6);
                    JEditorPane webpagePane;
                    try {
                        webpagePane = new JEditorPane(e.getURL());
                    } catch (final Throwable t) {
                        webpagePane = new JEditorPane("text/html", HelpHyperlinkListener.cannotDisplayMessage);
                    }
                    webpagePane.addHyperlinkListener(
                            e12 -> {
                                if (e12.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                    final JEditorPane pane1 = (JEditorPane) e12.getSource();
                                    if (e12 instanceof final HTMLFrameHyperlinkEvent evt) {
                                        final HTMLDocument doc = (HTMLDocument) pane1.getDocument();
                                        doc.processHTMLFrameHyperlinkEvent(evt);
                                    } else {
                                        try {
                                            pane1.setPage(e12.getURL());
                                        } catch (final Throwable t) {
                                            pane1.setText(HelpHyperlinkListener.cannotDisplayMessage);
                                        }
                                        HelpHyperlinkListener.this.webpageURL.setText(e12.getURL().toString());
                                    }
                                }
                            });
                    webpagePane.setPreferredSize(
                            new Dimension(HelpHelpAction.this.mainUI.getSize().width * 2 / 3, HelpHelpAction.this.mainUI.getSize().height * 2 / 3));
                    webpagePane.setEditable(false);
                    webpagePane.setCaretPosition(0);
                    final JScrollPane webpageScrollPane = new JScrollPane(webpagePane,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    this.webpageURL = new JTextField(e.getURL().toString(), 50);
                    this.webpageURL.setEditable(false);
                    this.webpageURL.setBackground(Color.WHITE);
                    final JPanel URLPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
                    URLPanel.add(new JLabel("URL: "));
                    URLPanel.add(this.webpageURL);
                    this.webpageDisplay.add(URLPanel, BorderLayout.NORTH);
                    this.webpageDisplay.add(webpageScrollPane);
                    final JButton closeButton = new JButton("Close");
                    closeButton.addActionListener(
                            e1 -> {
                                HelpHyperlinkListener.this.webpageDisplay.setVisible(false);
                                HelpHyperlinkListener.this.webpageDisplay.dispose();
                            });
                    final JPanel closePanel = new JPanel();
                    closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.LINE_AXIS));
                    closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
                    closePanel.add(Box.createHorizontalGlue());
                    closePanel.add(closeButton);
                    closePanel.add(Box.createHorizontalGlue());
                    this.webpageDisplay.add(closePanel, BorderLayout.SOUTH);
                    this.webpageDisplay.pack();
                    this.webpageDisplay.setVisible(true);
                }
            }
        }
    }
}
