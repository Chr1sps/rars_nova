package rars.venus.run;

import org.jetbrains.annotations.NotNull;
import rars.venus.VenusUI;
import rars.venus.actions.GuiAction;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;

/**
 * Action class for the Run menu item to clear execution breakpoints that have
 * been set.
 * It is a listener and is notified whenever a breakpoint is added or removed,
 * thus will
 * set its enabled status true or false depending on whether breakpoints remain
 * after that action.
 */
public final class RunClearBreakpointsAction extends GuiAction implements TableModelListener {

    /**
     * Create the object and register with text segment window as a listener on its
     * table model.
     * The table model has not been created yet, so text segment window will hang
     * onto this
     * registration info and transfer it to the table model upon creation (which
     * happens with
     * each successful assembly).
     */
    public RunClearBreakpointsAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final @NotNull VenusUI gui
    ) {
        super(name, icon, descrip, mnemonic, accel, gui);
        this.mainUI.mainPane.executePane.textSegment.registerTableModelListener(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When this option is selected, tell text segment window to clear breakpoints
     * in its table model.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        this.mainUI.mainPane.executePane.textSegment.clearAllBreakpoints();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Required TableModelListener method. This is response upon editing of text
     * segment table
     * model. The only editable column is breakpoints so this method is called only
     * when user
     * adds or removes a breakpoint. Gets new breakpoint count and sets enabled
     * status
     * accordingly.
     */
    @Override
    public void tableChanged(final TableModelEvent e) {
        setEnabled(this.mainUI.mainPane.executePane.textSegment.getBreakpointCount() > 0);
    }

}
