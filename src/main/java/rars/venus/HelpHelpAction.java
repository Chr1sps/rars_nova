package rars.venus;

import kotlin.Unit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.Directive;
import rars.riscv.Instruction;
import rars.riscv.InstructionsRegistry;
import rars.riscv.Syscall;
import rars.settings.FontSettingsImpl;
import rars.venus.actions.GuiAction;
import rars.venus.util.IconLoading;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import static rars.venus.util.HelpRemarksKt.createHelpRemarks;

/**
 * Action for the Help -> Help menu item
 */
public final class HelpHelpAction extends GuiAction {
    private static final @NotNull Dimension WINDOW_SIZE = new Dimension(800, 600);
    @NotNull
    private final FontSettingsImpl fontSettings;

    public HelpHelpAction(
        final @NotNull VenusUI gui,
        final @NotNull FontSettingsImpl fontSettings
    ) {
        super(
            "Help",
            IconLoading.loadIcon("Help22.png"),
            "Help",
            KeyEvent.VK_H,
            KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), gui
        );
        this.fontSettings = fontSettings;
    }

    private static @NotNull JScrollPane createDirectivesHelpPane(final @NotNull FontSettingsImpl fontSettings) {
        final var descriptions = Directive.getEntries().stream().map(it -> {
            final var blanks = " ".repeat(Math.max(
                0,
                24 - it.getDirectiveName().length()
            ));
            return it.getDirectiveName() + blanks + it.getDescription();
        }).sorted();
        final var examples = new JList<>(descriptions.toArray(String[]::new));
        final JScrollPane scrollPane = new JScrollPane(
            examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        examples.setFont(fontSettings.getCurrentFont());
        fontSettings.onChangeListenerHook.subscribe(ignored -> {
            examples.setFont(fontSettings.getCurrentFont());
            return Unit.INSTANCE;
        });
        return scrollPane;
    }

    private static @NotNull JScrollPane createInstructionHelpPane(
        // final Class<? extends Instruction> instructionClass,
        final @NotNull List<? extends Instruction> instructions,
        final @NotNull FontSettingsImpl fontSettings
    ) {
        final var examples = createExampleList(instructions);
        final var scrollPane = new JScrollPane(
            examples, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        examples.setFont(fontSettings.getCurrentFont());
        fontSettings.onChangeListenerHook.subscribe(ignored -> {
            examples.setFont(fontSettings.getCurrentFont());
            return Unit.INSTANCE;
        });
        examples.setCellRenderer(new MyCellRenderer());
        return scrollPane;
    }

    private static @NotNull JList<String> createExampleList(final @NotNull List<? extends Instruction> instructions) {
        final var descriptions = instructions.stream().map(it -> {
            final var blanks = " ".repeat(Math.max(
                0,
                24 - it.exampleFormat.length()
            ));
            return it.exampleFormat + blanks + it.description;
        }).sorted();
        return new JList<>(descriptions.toArray(String[]::new));
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
            final StringBuilder text = loadFiletoStringBuilder("/License.txt").append("</pre>");
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
            data[i][0] = syscall.getServiceName();
            data[i][1] = Integer.toString(syscall.getServiceNumber());
            data[i][2] = syscall.getDescription();
            data[i][3] = syscall.getInputs();
            data[i][4] = syscall.getOutputs();
            i++;
        }

        final String[] columnNames = {"Name", "Number", "Description", "Inputs", "Outputs"};
        final JEditorPane html = new JEditorPane(
            "text/html",
            loadFiletoStringBuilder(Globals.HELP_PATH + "SyscallHelpPrelude.html")
                + convertToHTMLTable(data, columnNames).toString()
                + loadFiletoStringBuilder(Globals.HELP_PATH + "SyscallHelpConclusion.html")
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
            final StringBuilder text = loadFiletoStringBuilder(Globals.HELP_PATH + filename);
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
        tabbedPane.addTab("Intro", createHTMLHelpPanel("Intro.html"));
        tabbedPane.addTab("IDE", createHTMLHelpPanel("IDE.html"));
        tabbedPane.addTab("Debugging", createHTMLHelpPanel("Debugging.html"));
        tabbedPane.addTab("Tools", createHTMLHelpPanel("Tools.html"));
        tabbedPane.addTab("Command", createHTMLHelpPanel("Command.html"));
        tabbedPane.addTab("Limits", createHTMLHelpPanel("Limits.html"));
        tabbedPane.addTab("History", createHTMLHelpPanel("History.html"));
        helpInfo.add(tabbedPane);
        return helpInfo;
    }

    /**
     * Set up MIPS help tab. Most contents are generated from instruction set info.
     */
    private static @NotNull JPanel createHelpInfoPanel(final @NotNull FontSettingsImpl fontSettings) {
        final var helpInfo = new JPanel(new BorderLayout());
        // Introductory remarks go at the top as a label
        final var backgroundColor = new Color(0xCCFF99);
        final var helpRemarksLabel = new JLabel(createHelpRemarks(backgroundColor), JLabel.CENTER);
        helpRemarksLabel.setOpaque(true);
        helpRemarksLabel.setBackground(backgroundColor);
        helpRemarksLabel.setForeground(Color.BLACK);
        final JScrollPane operandsScrollPane = new JScrollPane(
            helpRemarksLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        helpInfo.add(operandsScrollPane, BorderLayout.NORTH);
        // Below the label is a tabbed pane with categories of MIPS help
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(
            "Basic Instructions",
            createInstructionHelpPane(InstructionsRegistry.BASIC_INSTRUCTIONS.getAllInstructions(), fontSettings)
        );
        tabbedPane.addTab(
            "Extended (pseudo) Instructions",
            createInstructionHelpPane(InstructionsRegistry.EXTENDED_INSTRUCTIONS.getAllInstructions(), fontSettings)
        );
        tabbedPane.addTab("Directives", createDirectivesHelpPane(fontSettings));
        tabbedPane.addTab("Syscalls", createSyscallsHelpPane());
        tabbedPane.addTab("Exceptions", createHTMLHelpPanel("ExceptionsHelp.html"));
        tabbedPane.addTab("Macros", createHTMLHelpPanel("MacrosHelp.html"));
        operandsScrollPane.setPreferredSize(
            new Dimension(
                (int) WINDOW_SIZE.getWidth(),
                (int) (WINDOW_SIZE.getHeight() * 0.2)
            ));
        operandsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        tabbedPane.setPreferredSize(
            new Dimension(
                (int) WINDOW_SIZE.getWidth(),
                (int) (WINDOW_SIZE.getHeight() * 0.6)
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
        tabbedPane.addTab("RISCV", createHelpInfoPanel(fontSettings));
        tabbedPane.addTab("RARS", createRarsHelpInfoPanel());
        tabbedPane.addTab("License", createCopyrightInfoPanel());
        tabbedPane.addTab("Bugs/Comments", createHTMLHelpPanel("BugReportingHelp.html"));
        tabbedPane.addTab("Acknowledgements", createHTMLHelpPanel("Acknowledgements.html"));
        // Create non-modal dialog. Based on java.sun.com "How to Make Dialogs",
        // DialogDemo.java
        final JDialog dialog = new JDialog(this.mainUI, "RARS " + Globals.VERSION + " Help");
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
        dialog.setSize(WINDOW_SIZE);
        dialog.setLocationRelativeTo(this.mainUI);
        dialog.setVisible(true);

    }

    private static class MyCellRenderer extends JLabel implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(
            final JList<? extends String> list,
            final String s,
            final int index,
            final boolean isSelected,
            final boolean cellHasFocus
        ) {
            this.setText(s);
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
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

