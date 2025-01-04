package rars.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CustomPublisher<T> implements Flow.Publisher<T> {
    private final List<@NotNull CustomSubscription> subscriptions = new ArrayList<>();
    private final Lock lock = new ReentrantReadWriteLock().writeLock();

    @Override
    public void subscribe(final @NotNull Flow.Subscriber<? super T> subscriber) {
        this.lock.lock();
        try {
            final CustomSubscription subscription = new CustomSubscription(subscriber);
            this.subscriptions.add(subscription);
            subscriber.onSubscribe(subscription);
        } finally {
            this.lock.unlock();
        }
    }

    public void deleteSubscriber(final @NotNull Flow.Subscriber<? super T> subscriber) {
        this.lock.lock();
        try {
            this.subscriptions.removeIf(subscription -> subscription.getSubscriber().equals(subscriber));
        } finally {
            this.lock.unlock();
        }
    }

    public void submit(final T item) {
        this.lock.lock();
        try {
            this.subscriptions.forEach(subscription -> subscription.sendNext(item));
            this.subscriptions.removeIf(subscription -> subscription.completed.get());
        } finally {
            this.lock.unlock();
        }
    }

    public void complete() {
        this.lock.lock();
        try {
            this.subscriptions.forEach(CustomSubscription::complete);
            this.subscriptions.clear();
        } finally {
            this.lock.unlock();
        }
    }

    public void error(final Throwable throwable) {
        this.lock.lock();
        try {
            this.subscriptions.forEach(subscription -> subscription.error(throwable));
            this.subscriptions.clear();
        } finally {
            this.lock.unlock();
        }
    }

    private class CustomSubscription implements Flow.Subscription {
        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicLong requested = new AtomicLong(0);
        private final AtomicBoolean completed = new AtomicBoolean(false),
            cancelled = new AtomicBoolean(false);
        private final ReentrantLock subscriptionLock = new ReentrantLock();

        public CustomSubscription(final Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public Flow.Subscriber<? super T> getSubscriber() {
            return this.subscriber;
        }

        @Override
        public void request(final long n) {
            if (n <= 0) {
                this.error(new IllegalArgumentException("Non-positive subscription request"));
                return;
            }
            this.subscriptionLock.lock();
            try {
                this.requested.addAndGet(n);
            } finally {
                this.subscriptionLock.unlock();
            }
        }

        @Override
        public void cancel() {
            this.subscriptionLock.lock();
            try {
                this.completed.set(true);
//                CustomPublisher.this.subscriptions.remove(this);
            } finally {
                this.subscriptionLock.unlock();
            }
        }

        public void sendNext(final T item) {
            this.subscriptionLock.lock();
            try {
                if (this.requested.get() > 0 && !this.completed.get()) {
                    this.subscriber.onNext(item);
                    this.requested.decrementAndGet();
                }
            } finally {
                this.subscriptionLock.unlock();
            }
        }

        public void complete() {
            this.subscriptionLock.lock();
            try {
                if (!this.completed.getAndSet(true)) {
                    this.subscriber.onComplete();
                }
            } finally {
                this.subscriptionLock.unlock();
            }
        }

        public void error(final Throwable throwable) {
            this.subscriptionLock.lock();
            try {
                if (!this.completed.getAndSet(true)) {
                    this.subscriber.onError(throwable);
                }
            } finally {
                this.subscriptionLock.unlock();
            }
        }
    }
}
