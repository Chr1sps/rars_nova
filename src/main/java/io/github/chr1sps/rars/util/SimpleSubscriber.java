package io.github.chr1sps.rars.util;

import java.util.concurrent.Flow;

public interface SimpleSubscriber<T> extends Flow.Subscriber<T> {

    @Override
    default void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    default void onComplete() {
    }
}
