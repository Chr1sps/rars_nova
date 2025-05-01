package rars.util;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;

import java.util.List;

import static rars.riscv.hardware.memory.MemoryConfigurationKt.*;

// TODO: refactor this out of existance

public final class MemoryDump {
    public static final @NotNull
    @Unmodifiable List<@NotNull SegmentInfo> SEGMENTS;

    static {
        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        SEGMENTS = List.of(
            new SegmentInfo(
                ".text",
                getTextSegmentBaseAddress(memoryConfiguration),
                getTextSegmentLimitAddress(memoryConfiguration)
            ),
            new SegmentInfo(
                ".data",
                memoryConfiguration.getDataBaseAddress(),
                getDataSegmentLimitAddress(memoryConfiguration)
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
