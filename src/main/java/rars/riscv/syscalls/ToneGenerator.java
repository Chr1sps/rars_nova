package rars.riscv.syscalls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.sound.midi.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//  The ToneGenerator and Tone classes were developed by Otterbein College
//  student Tony Brock in July 2007. They simulate MIDI output through the
//  computers soundcard using classes and methods of the javax.sound.midi
//  package.
//
//  Max Hailperin <max@gustavus.edu> changed the interface of the
//  ToneGenerator class 2009-10-19 in order to
//  (1) provide a reliable way to wait for the completion of a
//       synchronous tone,
//  and while he was at it,
//  (2) improve the efficiency of asynchronous tones by using a thread
//      pool executor, and
//  (3) simplify the interface by removing all the unused versions
//       that provided default values for various parameters

public final class ToneGenerator {

    /**
     * The default pitch value for the tone: 60 / middle C.
     */
    public final static byte DEFAULT_PITCH = 60;

    /**
     * The default duration of the tone: 1000 milliseconds.
     */
    public final static int DEFAULT_DURATION = 1000;

    /**
     * The default instrument of the tone: 0 / piano.
     */
    public final static byte DEFAULT_INSTRUMENT = 0;

    /**
     * The default volume of the tone: 100 (of 127).
     */
    public final static byte DEFAULT_VOLUME = 100;

    private static final Executor threadPool = Executors.newCachedThreadPool();

    private ToneGenerator() {
    }

    /**
     * Produces a Tone with the specified pitch, duration, and instrument,
     * and volume.
     *
     * @param pitch
     *     the desired pitch in semitones - 0-127 where 60 is
     *     middle C.
     * @param duration
     *     the desired duration in milliseconds.
     * @param instrument
     *     the desired instrument (or patch) represented
     *     by a positive byte value (0-127). See the <a href=
     *     http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *     MIDI instrument patch map</a> for more instruments
     *     associated with
     *     each value.
     * @param volume
     *     the desired volume of the initial attack of the
     *     Tone (MIDI velocity) represented by a positive byte value
     *     (0-127).
     */
    public static void generateTone(
        final byte pitch, final int duration,
        final byte instrument, final byte volume
    ) {
        ToneGenerator.threadPool.execute(() -> Tone.play(pitch, duration, instrument, volume));
    }

    /**
     * Produces a Tone with the specified pitch, duration, and instrument,
     * and volume, waiting for it to finish playing.
     *
     * @param pitch
     *     the desired pitch in semitones - 0-127 where 60 is
     *     middle C.
     * @param duration
     *     the desired duration in milliseconds.
     * @param instrument
     *     the desired instrument (or patch) represented
     *     by a positive byte value (0-127). See the <a href=
     *     http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *     MIDI instrument patch map</a> for more instruments
     *     associated with
     *     each value.
     * @param volume
     *     the desired volume of the initial attack of the
     *     Tone (MIDI velocity) represented by a positive byte value
     *     (0-127).
     */
    public static void generateToneSynchronously(
        final byte pitch, final int duration,
        final byte instrument, final byte volume
    ) {
        Tone.play(pitch, duration, instrument, volume);
    }

}

/**
 * Contains important variables for a MIDI Tone: pitch, duration
 * instrument (patch), and volume. The tone can be passed to a thread
 * and will be played using MIDI.
 */
final class Tone {
    /**
     * Tempo of the tone is in milliseconds: 1000 beats per second.
     */

    public final static int TEMPO = 1000;
    /**
     * The default MIDI channel of the tone: 0 (channel 1).
     */
    public final static int DEFAULT_CHANNEL = 0;
    private static final Logger LOGGER = LogManager.getLogger(Tone.class);
    /**
     * The following lock and the code which locks and unlocks it
     * around the opening of the Sequencer were added 2009-10-19 by
     * Max Hailperin <max@gustavus.edu> in order to work around a
     * bug in Sun's JDK which causes crashing if two threads race:
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6888117">http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6888117</a> .
     * This routinely manifested native-code crashes when tones
     * were played asynchronously, on dual-core machines with Sun's
     * JDK (but not on one core or with OpenJDK). Even when tones
     * were played only synchronously, crashes sometimes occurred.
     * This is likely due to the fact that Thread.sleep was used
     * for synchronization, a role it cannot reliably serve. In
     * any case, this one lock seems to make all the crashes go
     * away, and the sleeps are being eliminated (since they can
     * cause other, less severe, problems), so that case should be
     * double covered.
     */
    private static final Lock openLock = new ReentrantLock();

    private Tone() {
    }

    /**
     * Plays the tone
     *
     * @param pitch
     *     the pitch in semitones. Pitch is represented by
     *     a positive byte value - 0-127 where 60 is middle C.
     * @param duration
     *     the duration of the tone in milliseconds.
     * @param instrument
     *     a positive byte value (0-127) which represents
     *     the instrument (or patch) of the tone. See the <a href=
     *     http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *     MIDI instrument patch map</a> for more instruments
     *     associated with
     *     each value.
     * @param volume
     *     a positive byte value (0-127) which represents the
     *     volume of the initial attack of the note (MIDI velocity).
     *     127 being
     *     loud, and 0 being silent.
     */
    public static void play(final byte pitch, final int duration, final byte instrument, final byte volume) {

        try {
            Tone.openLock.lock();
            Sequencer player;
            try {
                player = MidiSystem.getSequencer();
                player.open();
            } finally {
                Tone.openLock.unlock();
            }

            final Sequence seq = new Sequence(Sequence.PPQ, 1);
            player.setTempoInMPQ(Tone.TEMPO);
            final Track t = seq.createTrack();

            // select instrument
            final ShortMessage inst = new ShortMessage();
            inst.setMessage(ShortMessage.PROGRAM_CHANGE, Tone.DEFAULT_CHANNEL, instrument, 0);
            final MidiEvent instChange = new MidiEvent(inst, 0);
            t.add(instChange);

            final ShortMessage on = new ShortMessage();
            on.setMessage(ShortMessage.NOTE_ON, Tone.DEFAULT_CHANNEL, pitch, volume);
            final MidiEvent noteOn = new MidiEvent(on, 0);
            t.add(noteOn);

            final ShortMessage off = new ShortMessage();
            off.setMessage(ShortMessage.NOTE_OFF, Tone.DEFAULT_CHANNEL, pitch, volume);
            final MidiEvent noteOff = new MidiEvent(off, duration);
            t.add(noteOff);

            player.setSequence(seq);

            /*
             * The EndOfTrackListener was added 2009-10-19 by Max
             * Hailperin <max@gustavus.edu> so that its
             * awaitEndOfTrack method could be used as a more reliable
             * replacement for Thread.sleep. (Given that the tone
             * might not start playing right away, the sleep could end
             * before the tone, clipping off the end of the tone.)
             */
            final EndOfTrackListener eot = new EndOfTrackListener();
            player.addMetaEventListener(eot);

            player.start();

            try {
                eot.awaitEndOfTrack();
            } catch (final InterruptedException ignored) {
            } finally {
                player.close();
            }

        } catch (final MidiUnavailableException |
            InvalidMidiDataException mue) {
            Tone.LOGGER.error("Error playing tone.", mue);
        }
    }

}

class EndOfTrackListener implements javax.sound.midi.MetaEventListener {

    private boolean endedYet = false;

    @Override
    public synchronized void meta(final javax.sound.midi.@NotNull MetaMessage m) {
        if (m.getType() == 47) {
            this.endedYet = true;
            this.notifyAll();
        }
    }

    /**
     * <p>awaitEndOfTrack.</p>
     *
     * @throws java.lang.InterruptedException
     *     if any.
     */
    public synchronized void awaitEndOfTrack() throws InterruptedException {
        while (!this.endedYet) {
            this.wait();
        }
    }
}
