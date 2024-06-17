package io.github.chr1sps.rars.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Flow;

public interface SimpleSubscriber<T> extends Flow.Subscriber<T> {
    Logger LOGGER = LogManager.getLogger(SimpleSubscriber.class);

    @Override
    default void onError(final Throwable throwable) {
        SimpleSubscriber.LOGGER.error("Error in subscriber", throwable);
    }

    @Override
    default void onComplete() {
    }
}
