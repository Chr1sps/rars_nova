package rars.venus;

import org.jetbrains.annotations.NotNull;
import rars.tools.*;
import rars.venus.actions.ToolAction;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Function;

/**
 * This class provides functionality to bring external Mars tools into the Mars
 * system by adding them to its Tools menu. This permits anyone with knowledge
 * of the Mars public interfaces, in particular of the Memory and Register
 * classes, to write applications which can interact with a MIPS program
 * executing under Mars. The execution is of course simulated. The
 * private method for loading tool classes is adapted from Bret Barker's
 * GameServer class from the book "Developing Games In Java".
 *
 * @author Pete Sanderson with help from Bret Barker
 * @version August 2005
 */
public final class ToolLoader {
    private static final String TOOLS_MENU_NAME = "Tools";
    /**
     * List of functions that produce tools given the main UI.
     */
    private static final @NotNull List<Function<@NotNull VenusUI, @NotNull AbstractTool>> TOOL_PRODUCERS = List.of(
        BHTSimulator::new,
        CacheSimulator::new,
        DigitalLabSim::new,
        FloatRepresentation::new,
        InstructionCounter::new,
        InstructionMemoryDump::new,
        InstructionStatistics::new,
        KeyboardAndDisplaySimulator::new,
        MemoryReferenceVisualization::new,
        TimerTool::new
    );

    private ToolLoader() {
    }

    /**
     * Called in VenusUI to build its Tools menu. If there are no qualifying tools
     * or any problems accessing those tools, it returns null. A qualifying tool
     * must be a class in the Tools package that implements Tool, must be compiled
     * into a .class file, and its .class file must be in the same Tools folder as
     * Tool.class.
     *
     * @return a Tools JMenu if qualifying tool classes are found, otherwise null
     */
    public static @NotNull JMenu buildToolsMenu(final @NotNull VenusUI mainUI) {
        final var menu = new JMenu(ToolLoader.TOOLS_MENU_NAME);
        menu.setMnemonic(KeyEvent.VK_T);
        // traverse array list and build menu
        for (final var toolProducer : ToolLoader.TOOL_PRODUCERS) {
            menu.add(new ToolAction(toolProducer.apply(mainUI)));
        }
        return menu;
    }
}
