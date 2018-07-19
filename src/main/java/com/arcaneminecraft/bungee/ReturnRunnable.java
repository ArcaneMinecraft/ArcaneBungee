package com.arcaneminecraft.bungee;

@FunctionalInterface
public interface ReturnRunnable<T> {
    void run(T arg);

    @FunctionalInterface
    interface More<T, R> {
        void run(T arg1, R arg2);
    }
}
