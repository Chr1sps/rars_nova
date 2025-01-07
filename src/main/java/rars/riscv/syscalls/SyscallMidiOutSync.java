package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.riscv.AbstractSyscall;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Modified 2009-10-19 by Max Hailperin <max@gustavus.edu>
to use a specific method for synchronously generating
a tone, rather than using an asyncronous method followed
by a Thread.sleep, because sleeping isn't a reliable
synchronization method -- depending on thread scheduling,
the actual tone generation could have been delayed, in
which case the tone might still be playing when the
sleep ended.

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

public final class SyscallMidiOutSync extends AbstractSyscall {

    // Endpoints of ranges for the three "byte" parameters. The duration
    // parameter is limited at the high end only by the int range.
    private static final int RANGE_LOW_END = 0;
    private static final int RANGE_HIGH_END = 127;

    public SyscallMidiOutSync() {
        super(
            "MidiOutSync", "Outputs simulated MIDI tone to sound card, then waits until the sound finishes playing.",
            "See MIDI note below", "N/A"
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Arguments:
     * a0 - pitch (note). Integer value from 0 to 127, with 60 being middle-C on a
     * piano.<br>
     * a1 - duration. Integer value in milliseconds.<br>
     * a2 - instrument. Integer value from 0 to 127, with 0 being acoustic grand
     * piano.<br>
     * a3 - volume. Integer value from 0 to 127.<br>
     * <p>
     * Default values, in case any parameters are outside the above ranges, are
     * a0=60, a1=1000,
     * a2=0, a3=100.<br>
     * <p>
     * See MARS/RARS documentation elsewhere or www.midi.org for more information.
     * Note that the pitch,
     * instrument and volume value ranges 0-127 are from javax.sound.midi; actual
     * MIDI instruments
     * use the range 1-128.
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        int pitch = Globals.REGISTER_FILE.getIntValue("a0");
        int duration = Globals.REGISTER_FILE.getIntValue("a1");
        int instrument = Globals.REGISTER_FILE.getIntValue("a2");
        int volume = Globals.REGISTER_FILE.getIntValue("a3");
        if (pitch < RANGE_LOW_END || pitch > RANGE_HIGH_END) {
            pitch = ToneGenerator.DEFAULT_PITCH;
        }
        if (duration < 0) {
            duration = ToneGenerator.DEFAULT_DURATION;
        }
        if (instrument < RANGE_LOW_END || instrument > RANGE_HIGH_END) {
            instrument = ToneGenerator.DEFAULT_INSTRUMENT;
        }
        if (volume < RANGE_LOW_END || volume > RANGE_HIGH_END) {
            volume = ToneGenerator.DEFAULT_VOLUME;
        }
        ToneGenerator.generateToneSynchronously((byte) pitch, duration, (byte) instrument, (byte) volume);
    }
}
