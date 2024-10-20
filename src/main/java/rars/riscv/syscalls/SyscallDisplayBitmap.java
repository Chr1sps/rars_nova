package rars.riscv.syscalls;

import org.jetbrains.annotations.Nullable;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.notices.SimulatorNotice;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.RegisterFile;
import rars.simulator.Simulator;
import rars.util.BitmapDisplay;
import rars.util.SimpleSubscriber;

import java.util.concurrent.Flow;

public class SyscallDisplayBitmap extends AbstractSyscall implements SimpleSubscriber<SimulatorNotice> {
    private @Nullable BitmapDisplay display;
    private Flow.Subscription subscription;

    public SyscallDisplayBitmap() {
        super("DisplayBitmap", "Bitmap displaying memory contents", """
                a0 = address of the bitmap to display
                a1 = width of the bitmap
                a2 = height of the bitmap
                """, "N/A");
        this.display = null;
        Simulator.getInstance().subscribe(this);
    }

    @Override
    public void simulate(final ProgramStatement statement) throws ExitingException {
        final int baseAddress = RegisterFile.getValue("a0");
        final int width = RegisterFile.getValue("a1");
        final int height = RegisterFile.getValue("a2");
        this.show(baseAddress, width, height);
    }

    private void show(final int baseAddress, final int width, final int height) {
        if (this.display == null) {
            this.display = new BitmapDisplay(baseAddress, width, height);
        } else if (this.display.displayWidth != width || this.display.displayHeight != height) {
            Memory.getInstance().deleteSubscriber(this.display);
            this.display.dispose();
            this.display = new BitmapDisplay(baseAddress, width, height);
        } else if (this.display.baseAddress != baseAddress) {
            this.display.changeBaseAddress(baseAddress);
            this.display.repaint();
        }
        if (!this.display.isVisible()) {
            this.display.setVisible(true);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(SimulatorNotice item) {
        if (item.action() == SimulatorNotice.Action.STOP) {
            this.display.dispose();
            this.display = null;
        }
        this.subscription.request(1);
    }
}
