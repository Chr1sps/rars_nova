package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.Directive;
import rars.riscv.*;
import rars.venus.actions.GuiAction;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Objects;
import java.util.Vector;

import static rars.Globals.FONT_SETTINGS;

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
public final class HelpHelpAction extends GuiAction {
    // ideally read or computed from config file...
    private static final @NotNull Dimension WINDOW_SIZE = new Dimension(800, 600);

    public HelpHelpAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final @NotNull VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    private static @NotNull JScrollPane createDirectivesHelpPane() {
        final Vector<String> exampleList = new Vector<>();
        final String blanks = "            "; // 12 blanks
        for (final var directive : Directive.values()) {
            exampleList.add("%s%s%s".formatted(
                directive.toString(),
                blanks.substring(
                    0,
                    Math.max(0, blanks.length() - directive.toString().length())
                ),
                directive.getDescription()
            ));
        }
        Collections.sort(exampleList);
        final JList<String> examples = new JList<>(exampleList);
        final JScrollPane scrollPane = new JScrollPane(
            examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        examples.setFont(FONT_SETTINGS.getCurrentFont());
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignored -> {
            examples.setFont(FONT_SETTINGS.getCurrentFont());
            return Unit.INSTANCE;
        });
        return scrollPane;
    }

    private static @NotNull JScrollPane createInstructionHelpPane(final Class<? extends Instruction> instructionClass) {
        final var exampleList = createExampleList(instructionClass);
        Collections.sort(exampleList);
        final JList<String> examples = new JList<>(exampleList);
        final JScrollPane scrollPane = new JScrollPane(
            examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        examples.setFont(FONT_SETTINGS.getCurrentFont());
        FONT_SETTINGS.onChangeListenerHook.subscribe(ignored -> {
            examples.setFont(FONT_SETTINGS.getCurrentFont());
            return Unit.INSTANCE;
        });
        examples.setCellRenderer(new MyCellRenderer());
        return scrollPane;
    }

    private static @NotNull Vector<String> createExampleList(final Class<? extends Instruction> instructionClass) {
        final var instructionList = InstructionsRegistry.ALL_INSTRUCTIONS.allInstructions;
        final Vector<String> exampleList = new Vector<>(instructionList.size());
        final String blanks = "                        "; // 24 blanks
        for (final Instruction instr : instructionList) {
            if (instructionClass.isInstance(instr)) {
                exampleList.add(instr.exampleFormat
                    + blanks.substring(
                    0,
                    Math.max(0, blanks.length() - instr.exampleFormat.length())
                )
                    + instr.description);
            }
        }
        return exampleList;
    }

    private static @NotNull StringBuilder convertToHTMLTable(final String[][] data, final String @NotNull [] headers) {
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

    /// Set up the copyright notice for display.
    private static @NotNull JPanel createCopyrightInfoPanel() {
        final JPanel copyrightInfo = new JPanel(new BorderLayout());
        JScrollPane copyrightScrollPane;
        try {
            final StringBuilder text = HelpHelpAction.loadFiletoStringBuilder("/License.txt").append("</pre>");
            final JEditorPane copyrightDisplay = new JEditorPane("text/html", "<pre>" + text);
            copyrightDisplay.setEditable(false);
            copyrightDisplay.setCaretPosition(0); // assure top of document displayed
            copyrightScrollPane = new JScrollPane(
                copyrightDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            );
        } catch (final Exception ioe) {
            copyrightScrollPane = new JScrollPane(
                new JLabel("Error: license contents could not be loaded."));
        }
        copyrightInfo.add(copyrightScrollPane);
        return copyrightInfo;
    }

    /**
     * Ideally, this would not use HTML to make the table, but the other methods
     * tried were far uglier / not useful.
     */
    @Contract(" -> new")
    private static @NotNull JScrollPane createSyscallsHelpPane() {
        final var sortedList = Syscall.getEntries().stream().sorted().toList();
        final String[][] data = new String[sortedList.size()][5];

        int i = 0;
        for (final var syscall : sortedList) {
            data[i][0] = syscall.serviceName;
            data[i][1] = Integer.toString(syscall.serviceNumber);
            data[i][2] = syscall.description;
            data[i][3] = syscall.inputs;
            data[i][4] = syscall.outputs;
            i++;
        }

        final String[] columnNames = {"Name", "Number", "Description", "Inputs", "Ouputs"};
        final JEditorPane html = new JEditorPane(
            "text/html",
            HelpHelpAction.loadFiletoStringBuilder(Globals.helpPath + "SyscallHelpPrelude.html") +
                HelpHelpAction.convertToHTMLTable(data, columnNames).toString()
                + HelpHelpAction.loadFiletoStringBuilder(Globals.helpPath + "SyscallHelpConclusion.html")
        );

        html.setCaretPosition(0); // this affects scroll position
        html.setEditable(false);
        return new JScrollPane(
            html, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
    }

    // Methods to construct MIPS help tabs from internal MARS objects

    private static @NotNull StringBuilder loadFiletoStringBuilder(final String path) {
        try (final var stream = HelpHelpAction.class.getResourceAsStream(path)) {
            final var result = new StringBuilder();
            try (final var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))) {
                for (var line = reader.readLine(); line != null; line = reader.readLine()) {
                    result.append(line).append('\n');
                }
            }
            return result;
        } catch (final IOException io) {
            return new StringBuilder(path + " could not be loaded.");
        }
    }

    /// Create panel containing Help Info read from html document.
    private static @NotNull JPanel createHTMLHelpPanel(final String filename) {
        final JPanel helpPanel = new JPanel(new BorderLayout());
        JScrollPane helpScrollPane;
        try {
            final StringBuilder text = HelpHelpAction.loadFiletoStringBuilder(Globals.helpPath + filename);
            final JEditorPane helpDisplay = new JEditorPane("text/html", text.toString());
            helpDisplay.setEditable(false);
            helpDisplay.setCaretPosition(0); // assure top of document displayed
            helpScrollPane = new JScrollPane(
                helpDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            );
            helpDisplay.addHyperlinkListener(new HelpHyperlinkListener(helpPanel));
        } catch (final Exception ie) {
            helpScrollPane = new JScrollPane(
                new JLabel("Error (" + ie + "): " + filename + " contents could not be loaded."));
        }
        helpPanel.add(helpScrollPane);
        return helpPanel;
    }

    /// Set up MARS help tab. Subtabs get their contents from HTML files.
    private static @NotNull JPanel createRarsHelpInfoPanel() {
        final JPanel helpInfo = new JPanel(new BorderLayout());
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Intro", HelpHelpAction.createHTMLHelpPanel("Intro.html"));
        tabbedPane.addTab("IDE", HelpHelpAction.createHTMLHelpPanel("IDE.html"));
        tabbedPane.addTab("Debugging", HelpHelpAction.createHTMLHelpPanel("Debugging.html"));
        tabbedPane.addTab("Tools", HelpHelpAction.createHTMLHelpPanel("Tools.html"));
        tabbedPane.addTab("Command", HelpHelpAction.createHTMLHelpPanel("Command.html"));
        tabbedPane.addTab("Limits", HelpHelpAction.createHTMLHelpPanel("Limits.html"));
        tabbedPane.addTab("History", HelpHelpAction.createHTMLHelpPanel("History.html"));
        helpInfo.add(tabbedPane);
        return helpInfo;
    }

    // Set up MIPS help tab. Most contents are generated from instruction set info.
    private static @NotNull JPanel createHelpInfoPanel() {
        final JPanel helpInfo = new JPanel(new BorderLayout());
        final String helpRemarksColor = "CCFF99";
        // Introductory remarks go at the top as a label
        // TODO: update this to consider 12 and 20 bit numbers rather than 16
        final String helpRemarks = "<html><center><table bgcolor=\"#" + helpRemarksColor + "\" border=0 " +
            "cellpadding=0>" + // width="+this.getSize().getWidth()+">"+
            "<tr>" +
            "<th colspan=2><b><i><font size=+1>&nbsp;&nbsp;Operand Key for Example Instructions&nbsp;&nbsp;" +
            "</font></i></b></th>"
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
            "<td colspan=2><b><i><font size=+1>Load & Store addressing modes, usePseudoInstructions " +
            "instructions</font></i></b></td>"
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
            "<td><tt>label+100000(t2)&nbsp;&nbsp;&nbsp;</tt></td><td>sum of 32-bit integer, label's address, and " +
            "contents of t2</td>"
            +
            "</tr>" +
            "</table></center></html>";
        // Original code: mipsHelpInfo.add(new JLabel(helpRemarks, JLabel.CENTER),
        // BorderLayout.NORTH);
        final JLabel helpRemarksLabel = new JLabel(helpRemarks, JLabel.CENTER);
        helpRemarksLabel.setOpaque(true);
        helpRemarksLabel.setBackground(Color.decode("0x" + helpRemarksColor));
        helpRemarksLabel.setForeground(Color.BLACK);
        final JScrollPane operandsScrollPane = new JScrollPane(
            helpRemarksLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        helpInfo.add(operandsScrollPane, BorderLayout.NORTH);
        // Below the label is a tabbed pane with categories of MIPS help
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Basic Instructions", HelpHelpAction.createInstructionHelpPane(BasicInstruction.class));
        tabbedPane.addTab(
            "Extended (pseudo) Instructions",
            HelpHelpAction.createInstructionHelpPane(ExtendedInstruction.class)
        );
        tabbedPane.addTab("Directives", HelpHelpAction.createDirectivesHelpPane());
        tabbedPane.addTab("Syscalls", HelpHelpAction.createSyscallsHelpPane());
        tabbedPane.addTab("Exceptions", HelpHelpAction.createHTMLHelpPanel("ExceptionsHelp.html"));
        tabbedPane.addTab("Macros", HelpHelpAction.createHTMLHelpPanel("MacrosHelp.html"));
        operandsScrollPane.setPreferredSize(
            new Dimension(
                (int) HelpHelpAction.WINDOW_SIZE.getWidth(),
                (int) (HelpHelpAction.WINDOW_SIZE.getHeight() * 0.2)
            ));
        operandsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        tabbedPane.setPreferredSize(
            new Dimension(
                (int) HelpHelpAction.WINDOW_SIZE.getWidth(),
                (int) (HelpHelpAction.WINDOW_SIZE.getHeight() * 0.6)
            ));
        final JSplitPane splitsville = new JSplitPane(JSplitPane.VERTICAL_SPLIT, operandsScrollPane, tabbedPane);
        splitsville.setOneTouchExpandable(true);
        splitsville.resetToPreferredSizes();
        helpInfo.add(splitsville);
        // mipsHelpInfo.add(tabbedPane);
        return helpInfo;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Displays tabs with categories of information
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("RISCV", HelpHelpAction.createHelpInfoPanel());
        tabbedPane.addTab("RARS", HelpHelpAction.createRarsHelpInfoPanel());
        tabbedPane.addTab("License", HelpHelpAction.createCopyrightInfoPanel());
        tabbedPane.addTab("Bugs/Comments", HelpHelpAction.createHTMLHelpPanel("BugReportingHelp.html"));
        tabbedPane.addTab("Acknowledgements", HelpHelpAction.createHTMLHelpPanel("Acknowledgements.html"));
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
        dialog.setSize(HelpHelpAction.WINDOW_SIZE);
        dialog.setLocationRelativeTo(this.mainUI);
        dialog.setVisible(true);

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
            final boolean cellHasFocus
        ) // does the cell have focus
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

    /**
     * Determines MARS response when user click on hyperlink in displayed help page.
     * The response will be to pop up a simple dialog with the page contents. It
     * will not display URL, no navigation, nothing. Just display the page and
     * provide a Close button.
     */
    private record HelpHyperlinkListener(@NotNull JPanel parent) implements HyperlinkListener {
        private static final @NotNull String cannotDisplayMessage = "<html><title></title><body><strong>Unable to " +
            "display " +
            "requested document.</strong></body></html>";

        @Override
        public void hyperlinkUpdate(final @NotNull HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    final var desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(event.getURL().toURI());
                    } catch (final Exception e) {
                        JOptionPane.showMessageDialog(
                            this.parent,
                            "Unable to open browser to display requested document."
                        );
                    }
                }
            }
        }
    }
}

