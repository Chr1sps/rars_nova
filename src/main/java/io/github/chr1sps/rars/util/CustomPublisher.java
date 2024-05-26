package io.github.chr1sps.rars.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class CustomPublisher<T> extends SubmissionPublisher<T> {
    private final List<CustomSubscription> subscriptions = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        lock.lock();
        try {
            CustomSubscription subscription = new CustomSubscription(subscriber);
            subscriptions.add(subscription);
            subscriber.onSubscribe(subscription);
        } finally {
            lock.unlock();
        }
    }

    public void deleteSubscriber(Flow.Subscriber<? super T> subscriber) {
        lock.lock();
        try {
            subscriptions.removeIf(subscription -> subscription.getSubscriber().equals(subscriber));
        } finally {
            lock.unlock();
        }
    }

    public void publish(T item) {
        lock.lock();
        try {
            for (CustomSubscription subscription : subscriptions) {
                subscription.sendNext(item);
            }
        } finally {
            lock.unlock();
        }
    }

    public void complete() {
        lock.lock();
        try {
            for (CustomSubscription subscription : subscriptions) {
                subscription.complete();
            }
            subscriptions.clear();
        } finally {
            lock.unlock();
        }
    }

    public void error(Throwable throwable) {
        lock.lock();
        try {
            for (CustomSubscription subscription : subscriptions) {
                subscription.error(throwable);
            }
            subscriptions.clear();
        } finally {
            lock.unlock();
        }
    }

    private class CustomSubscription implements Flow.Subscription {
        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicLong requested = new AtomicLong(0);
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final ReentrantLock subscriptionLock = new ReentrantLock();

        public CustomSubscription(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public Flow.Subscriber<? super T> getSubscriber() {
            return subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                error(new IllegalArgumentException("Non-positive subscription request"));
                return;
            }
            subscriptionLock.lock();
            try {
                requested.addAndGet(n);
            } finally {
                subscriptionLock.unlock();
            }
        }

        @Override
        public void cancel() {
            subscriptionLock.lock();
            try {
                completed.set(true);
                subscriptions.remove(this);
            } finally {
                subscriptionLock.unlock();
            }
        }

        public void sendNext(T item) {
            subscriptionLock.lock();
            try {
                if (requested.get() > 0 && !completed.get()) {
                    subscriber.onNext(item);
                    System.out.println("value sent");
                    requested.decrementAndGet();
                }
            } finally {
                subscriptionLock.unlock();
            }
        }

        public void complete() {
            subscriptionLock.lock();
            try {
                if (!completed.getAndSet(true)) {
                    subscriber.onComplete();
                }
            } finally {
                subscriptionLock.unlock();
            }
        }

        public void error(Throwable throwable) {
            subscriptionLock.lock();
            try {
                if (!completed.getAndSet(true)) {
                    subscriber.onError(throwable);
                }
            } finally {
                subscriptionLock.unlock();
            }
        }
    }
}
