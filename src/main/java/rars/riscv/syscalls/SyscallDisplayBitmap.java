package rars.riscv.syscalls;

import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.RegisterFile;
import rars.util.BitmapDisplay;

public class SyscallDisplayBitmap extends AbstractSyscall {
    public SyscallDisplayBitmap() {
        super("DisplayBitmap", "Bitmap displaying memory contents", """
                a0 = address of the bitmap to display
                a1 = width of the bitmap
                a2 = height of the bitmap
                """, "N/A");

    }

    @Override
    public void simulate(final ProgramStatement statement) throws ExitingException {
        final int baseAddress = RegisterFile.getValue("a0");
        final int width = RegisterFile.getValue("a1");
        final int height = RegisterFile.getValue("a2");
        BitmapDisplay.show(baseAddress, width, height);
    }
}
