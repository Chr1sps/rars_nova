package rars.venus.run;

import rars.Globals;
import rars.settings.OtherSettings;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.VenusUI;
import rars.venus.actions.GuiAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static rars.util.UtilsKt.unwrap;

/**
 * Action for the Run -> Backstep menu item
 */
public final class RunBackstepAction extends GuiAction {

    public RunBackstepAction(
        final String name, final Icon icon, final String descrip,
        final Integer mnemonic, final KeyStroke accel, final VenusUI gui
    ) {
        super(name, descrip, icon, mnemonic, accel, gui);
    }

    /**
     * {@inheritDoc}
     * <p>
     * perform next simulated instruction step.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final String name = this.getValue(Action.NAME).toString();
        final ExecutePane executePane = this.mainUI.mainPane.executePane;
        if (!FileStatus.isAssembled()) {
            // note: this should never occur since backstepping is only enabled after
            // successful assembly.
            JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
            return;
        }
        this.mainUI.isExecutionStarted = true;
        this.mainUI.messagesPane.selectRunMessageTab();
        executePane.getTextSegment().setCodeHighlighting(true);

        if (OtherSettings.isBacksteppingEnabled()) {
            final var memoryHandle = unwrap(Globals.MEMORY_INSTANCE.subscribe(executePane.getDataSegment()::processMemoryAccessNotice));
            Globals.REGISTER_FILE.addRegistersListener(executePane.getRegisterValues().processRegisterNotice);
            Globals.CS_REGISTER_FILE.addRegistersListener(executePane.getCsrValues().processRegisterNotice);
            Globals.FP_REGISTER_FILE.addRegistersListener(executePane.getFpRegValues().processRegisterNotice);
            Globals.PROGRAM.getBackStepper().backStep();
            Globals.MEMORY_INSTANCE.unsubscribe(memoryHandle);
            Globals.REGISTER_FILE.deleteRegistersListener(executePane.getRegisterValues().processRegisterNotice);
            executePane.getRegisterValues().updateRegisters();
            executePane.getFpRegValues().updateRegisters();
            executePane.getCsrValues().updateRegisters();
            executePane.getDataSegment().updateValues();
            executePane.getTextSegment().highlightStepAtPC(); // Argument aded 25 June 2007
            FileStatus.setSystemState(FileStatus.State.RUNNABLE);
            // if we've backed all the way, disable the button
            // if (Globals.program.getBackStepper().empty()) {
            // ((AbstractAction)((AbstractButton)e.getSource()).getAction()).setEnabled(false);
            // }
            /*
             * if (pe !=null) {
             * RunGoAction.resetMaxSteps();
             * mainUI.getMessagesPane().postMessage(
             * pe.errors().generateErrorReport());
             * mainUI.getMessagesPane().postMessage(
             * "\n"+name+": execution terminated with errors.\n\n");
             * mainUI.getRegistersPane().setSelectedComponent(executePane.
             * getControlAndStatusWindow());
             * FileStatus.set(FileStatus.TERMINATED); // should be redundant.
             * executePane.getTextSegmentWindow().setCodeHighlighting(true);
             * executePane.getTextSegmentWindow().unhighlightAllSteps();
             * executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.
             * getProgramCounter()-4);
             * }
             */
            this.mainUI.isMemoryReset = false;
        }
    }
}
