package rars.simulator;

import org.jetbrains.annotations.NotNull;
import rars.notices.SimulatorNotice;
import rars.util.ListenerDispatcher;
import rars.venus.VenusUI;
import rars.venus.run.RunSpeedPanel;

import javax.swing.*;

public final class GuiSimThread extends SimThreadNew {
    private final @NotNull VenusUI mainUI;

    protected GuiSimThread(
        final int pc,
        final int maxSteps,
        final int[] breakPoints,
        final @NotNull ListenerDispatcher<@NotNull SimulatorNotice> simulatorNoticeDispatcher,
        final @NotNull VenusUI mainUI
    ) {
        super(pc, maxSteps, breakPoints, mainUI.venusIO, simulatorNoticeDispatcher);
        this.mainUI = mainUI;
    }

    @Override
    protected double getRunSpeed() {
        return this.mainUI.runSpeedPanel.getRunSpeed();
    }

    @Override
    protected void onEndLoop() {
        if (this.maxSteps != 1 && this.getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
            SwingUtilities.invokeLater(this::updateUI);
        }
        if (this.maxSteps != 1 && this.getRunSpeed() < RunSpeedPanel.UNLIMITED_SPEED) {
            try {
                this.wait((int) (1000 / this.getRunSpeed()));
            } catch (final InterruptedException ignored) {
            }
        }
    }

    private void updateUI() {
        if (this.mainUI.registersPane.getSelectedComponent() == this.mainUI.mainPane.executePane.registerValues) {
            this.mainUI.mainPane.executePane.registerValues.updateRegisters();
        } else {
            this.mainUI.mainPane.executePane.fpRegValues.updateRegisters();
        }
        this.mainUI.mainPane.executePane.dataSegment.updateValues();
        this.mainUI.mainPane.executePane.textSegment.setCodeHighlighting(true);
        this.mainUI.mainPane.executePane.textSegment.highlightStepAtPC();

    }
}
