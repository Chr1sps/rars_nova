package rars.util;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;

import java.util.List;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

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
// TODO: refactor this out of existance

public final class MemoryDump {
    public static final @NotNull
    @Unmodifiable List<@NotNull SegmentInfo> SEGMENTS;

    static {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        SEGMENTS = List.of(
            new SegmentInfo(".text", memoryConfiguration.textBaseAddress, memoryConfiguration.textLimitAddress),
            new SegmentInfo(
                ".data",
                memoryConfiguration.dataBaseAddress,
                memoryConfiguration.dataSegmentLimitAddress
            )
        );
    }

    private MemoryDump() {
    }

    /**
     * Return array with segment address bounds for specified segment.
     *
     * @param segment
     *     String with segment name (initially ".text" and ".data")
     * @return array of two Integer, the base and limit address for that segment.
     * Null if parameter
     * name does not match a known segment name.
     */
    public static @Nullable Pair<Integer, Integer> getSegmentBounds(final @NotNull String segment) {
        return SEGMENTS.stream()
            .filter(s -> s.name().equals(segment))
            .findFirst()
            .map(s -> new Pair<>(s.baseAddress(), s.limitAddress()))
            .orElse(null);
    }

    public record SegmentInfo(@NotNull String name, int baseAddress, int limitAddress) {
    }

}
