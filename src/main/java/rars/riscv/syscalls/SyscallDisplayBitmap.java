package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.ProgramStatement;
import rars.notices.SimulatorNotice;
import rars.riscv.AbstractSyscall;
import rars.util.BitmapDisplay;

import static rars.Globals.REGISTER_FILE;

public final class SyscallDisplayBitmap extends AbstractSyscall {
    private @Nullable BitmapDisplay display;

    public SyscallDisplayBitmap() {
        super(
            "DisplayBitmap", "Bitmap displaying memory contents", """
                a0 = address of the bitmap to display
                a1 = width of the bitmap
                a2 = height of the bitmap
                """, "N/A"
        );
        this.display = null;
        Globals.SIMULATOR.simulatorNoticeHook.subscribe(notice -> {
            if (notice.action() == SimulatorNotice.Action.START) {
                if (this.display != null) {
                    this.display.dispose();
                }
                this.display = null;
            }
        });
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        final int baseAddress = REGISTER_FILE.getIntValue("a0");
        final int width = REGISTER_FILE.getIntValue("a1");
        final int height = REGISTER_FILE.getIntValue("a2");
        this.show(baseAddress, width, height);
    }

    private void show(final int baseAddress, final int width, final int height) {
        if (this.display == null) {
            this.display = new BitmapDisplay(baseAddress, width, height);
        } else if (this.display.displayWidth != width || this.display.displayHeight != height) {
            this.display.unsubscribeFromMemory();
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
}
