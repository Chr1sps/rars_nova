package rars.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Flow;

public interface SimpleSubscriber<T> extends Flow.Subscriber<T> {
    Logger LOGGER = LogManager.getLogger();

    @Override
    default void onError(final @NotNull Throwable throwable) {
        SimpleSubscriber.LOGGER.error("Error in subscriber", throwable);
    }

    @Override
    default void onComplete() {
    }
}
