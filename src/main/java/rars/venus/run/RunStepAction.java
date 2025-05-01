package rars.venus.run;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.SimulatorNotice;
import rars.settings.BoolSetting;
import rars.simulator.StoppingEvent;
import rars.venus.ExecutePane;
import rars.venus.FileStatus;
import rars.venus.VenusUI;
import rars.venus.actions.GuiAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static rars.Globals.BOOL_SETTINGS;
import static rars.simulator.ProgramArgumentListKt.storeProgramArguments;
import static rars.simulator.SimulationKt.isDone;

/**
 * Action for the Run -> Step menu item
 */
public final class RunStepAction extends GuiAction {

    private String name;
    private ExecutePane executePane;

    public RunStepAction(
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
        this.name = this.getValue(Action.NAME).toString();
        this.executePane = this.mainUI.mainPane.executePane;
        if (FileStatus.isAssembled()) {
            if (!this.mainUI.isExecutionStarted) {
                this.processProgramArgumentsIfAny();
            }
            this.mainUI.isExecutionStarted = true;
            this.mainUI.messagesPane.selectRunMessageTab();
            this.executePane.getTextSegment().setCodeHighlighting(true);

            final var stopListener = new Function1<SimulatorNotice, Unit>() {
                @Override
                public Unit invoke(final SimulatorNotice item) {
                    if (item.action != SimulatorNotice.Action.STOP) {
                        return Unit.INSTANCE;
                    }
                    EventQueue.invokeLater(() -> RunStepAction.this.stepped(
                        item.event
                    ));

                    Globals.SIMULATOR.simulatorNoticeHook.unsubscribe(this);
                    return Unit.INSTANCE;
                }
            };
            Globals.SIMULATOR.simulatorNoticeHook.subscribe(stopListener);

            Globals.SIMULATOR.startSimulation(
                Globals.REGISTER_FILE.getProgramCounter(),
                1,
                new int[0],
                this.mainUI
            );
        } else {
            // note: this should never occur since "Step" is only enabled after successful
            // assembly.
            JOptionPane.showMessageDialog(
                this.mainUI,
                "The program must be assembled before it can be run."
            );
        }
    }

    /**
     * When step is completed, control returns here (from execution thread,
     * indirectly)
     * to update the GUI.
     */
    public void stepped(
        final @NotNull StoppingEvent event
    ) {
        this.executePane.getRegisterValues().updateRegisters();
        this.executePane.getFpRegValues().updateRegisters();
        this.executePane.getCsrValues().updateRegisters();
        this.executePane.getDataSegment().updateValues();
        if (isDone(event)) {
            RunGoAction.resetMaxSteps();
            this.executePane.getTextSegment().unhighlightAllSteps();
            FileStatus.setSystemState(FileStatus.State.TERMINATED);
            if (!(event instanceof StoppingEvent.ErrorHit)) {
                this.mainUI.messagesPane.postMessage('\n' + this.name + ": execution " + (
                    (event instanceof StoppingEvent.CliffTermination)
                        ? "terminated due to null instruction."
                        : "completed successfully."
                ) + "\n\n");
                this.mainUI.messagesPane.postRunMessage(
                    "\n-- program is finished running" + (
                        (event instanceof StoppingEvent.CliffTermination)
                            ? "(dropped off bottom)"
                            : " (" + Globals.exitCode + ')'
                    ) + " --\n\n");
                this.mainUI.messagesPane.selectRunMessageTab();
            }
        } else {
            this.executePane.getTextSegment().highlightStepAtPC();
            FileStatus.setSystemState(FileStatus.State.RUNNABLE);
        }
        if (event instanceof final StoppingEvent.ErrorHit errorHit) {
            RunGoAction.resetMaxSteps();
            this.mainUI.messagesPane.postMessage(
                errorHit.getError().getMessage().generateReport());
            this.mainUI.messagesPane.postMessage(
                '\n' + this.name + ": execution terminated with errors.\n\n");
            this.mainUI.registersPane.setSelectedComponent(this.executePane.getCsrValues());
            FileStatus.setSystemState(FileStatus.State.TERMINATED); // should be redundant.
            this.executePane.getTextSegment().setCodeHighlighting(true);
            this.executePane.getTextSegment().unhighlightAllSteps();
            this.executePane.getTextSegment()
                .highlightStepAtAddress(Globals.REGISTER_FILE.getProgramCounter() - 4);
        }
        this.mainUI.isMemoryReset = false;
    }

    /**
     * Method to store any program arguments into MIPS memory and registers before
     * execution begins. Arguments go into the gap between $sp and kernel memory.
     * Argument pointers and count go into runtime stack and $sp is adjusted accordingly.
     * $a0 gets argument count (argc), $a1 gets stack address of first arg pointer (argv).
     */
    private void processProgramArgumentsIfAny() {
        final String programArguments = this.executePane.getTextSegment()
            .getProgramArguments();
        if (programArguments == null || programArguments.isEmpty() ||
            !BOOL_SETTINGS.getSetting(BoolSetting.PROGRAM_ARGUMENTS)) {
            return;
        }
        storeProgramArguments(programArguments);
    }
}
