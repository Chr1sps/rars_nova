package rars.riscv.syscalls

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.sound.midi.*

/** The default pitch value for the tone: 60 / middle C. */
const val DEFAULT_PITCH = 60

/** The default duration of the tone: 1000 milliseconds. */
const val DEFAULT_DURATION = 1000;

/** The default instrument of the tone: 0 / piano. */
const val DEFAULT_INSTRUMENT = 0

/** The default volume of the tone: 100 (of 127). */
const val DEFAULT_VOLUME = 100

// private static final Executor threadPool = Executors.newCachedThreadPool();

/**
 * Produces a Tone with the specified pitch, duration, and instrument,
 * and volume.
 *
 * @param pitch      the desired pitch in semitones - 0-127 where 60 is
 *                   middle C.
 * @param duration   the desired duration in milliseconds.
 * @param instrument the desired instrument (or patch) represented
 *                   by a positive byte value (0-127). See the <a href=
 *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
 *                   MIDI instrument patch map</a> for more instruments
 *                   associated with
 *                   each value.
 * @param volume     the desired volume of the initial attack of the
 *                   Tone (MIDI velocity) represented by a positive byte value
 *                   (0-127).
 */

fun CoroutineScope.generateToneAsync(
    pitch: Int,
    duration: Int,
    instrument: Int,
    volume: Int,
) {
    launch {
        playTone(pitch, duration, instrument, volume)
    }
}

/**
 * Produces a Tone with the specified pitch, duration, and instrument,
 * and volume, waiting for it to finish playing.
 *
 * @param pitch      the desired pitch in semitones - 0-127 where 60 is
 *                   middle C.
 * @param duration   the desired duration in milliseconds.
 * @param instrument the desired instrument (or patch) represented
 *                   by a positive byte value (0-127). See the <a href=
 *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
 *                   MIDI instrument patch map</a> for more instruments
 *                   associated with
 *                   each value.
 * @param volume     the desired volume of the initial attack of the
 *                   Tone (MIDI velocity) represented by a positive byte value
 *                   (0-127).
 */
suspend fun CoroutineScope.generateToneSync(
    pitch: Int,
    duration: Int,
    instrument: Int,
    volume: Int,
) {
    playTone(pitch, duration, instrument, volume)
}

/** Tempo of the tone is in milliseconds: 1000 beats per second. */
const val TEMPO = 1000.0f

/** The default MIDI channel of the tone: 0 (channel 1). */
const val DEFAULT_CHANNEL = 0

/**
 * Plays the tone
 *
 * @param pitch      the pitch in semitones. Pitch is represented by
 *                   a positive byte value - 0-127 where 60 is middle C.
 * @param duration   the duration of the tone in milliseconds.
 * @param instrument a positive byte value (0-127) which represents
 *                   the instrument (or patch) of the tone. See the <a href=
 *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
 *                   MIDI instrument patch map</a> for more instruments
 *                   associated with
 *                   each value.
 * @param volume     a positive byte value (0-127) which represents the
 *                   volume of the initial attack of the note (MIDI velocity).
 *                   127 being
 *                   loud, and 0 being silent.
 */
suspend fun CoroutineScope.playTone(
    pitch: Int,
    duration: Int,
    instrument: Int,
    volume: Int,
) {
    try {
        val sequence = Sequence(Sequence.PPQ, 1).apply {
            buildTrack {
                addMessage(
                    ShortMessage.PROGRAM_CHANGE,
                    DEFAULT_CHANNEL,
                    instrument,
                    0
                )
                addMessage(
                    ShortMessage.NOTE_ON,
                    DEFAULT_CHANNEL,
                    pitch,
                    volume
                )
                addMessage(
                    ShortMessage.NOTE_OFF,
                    Tone.DEFAULT_CHANNEL,
                    pitch,
                    volume
                )
            }
        }
        MidiSystem.getSequencer().use { player ->
            with(player) {
                open()
                tempoInMPQ = TEMPO
                this.sequence = sequence
                val endListener = EndOfTrackListenerNew()
                addMetaEventListener(endListener)
                start()
                try {
                    endListener.awaitEndOfTrack()
                } catch (_: InterruptedException) {
                }
            }
        }
    } catch (_: MidiUnavailableException) {
    } catch (_: InvalidMidiDataException) {
    }
}

private fun Sequence.buildTrack(builderFunc: Track.() -> Unit): Track =
    createTrack().apply(builderFunc)

private fun Track.addMessage(
    command: Int,
    channel: Int,
    data1: Int,
    data2: Int,
) {
    val message = ShortMessage().apply {
        setMessage(command, channel, data1, data2)
    }
    val event = MidiEvent(message, /* tick = */ 0)
    add(event)
}

private class EndOfTrackListenerNew : MetaEventListener {
    var endedYet = false
    override fun meta(meta: MetaMessage) {
        if (meta.type == 47) {
            endedYet = true
            // TODO: replace notifyAll() with something else
            (this as Object).notifyAll()
        }
    }

    @Synchronized
    fun awaitEndOfTrack() {
        while (!endedYet) {
            // TODO: replace wait() with something else
            (this as Object).wait()
        }
    }
}