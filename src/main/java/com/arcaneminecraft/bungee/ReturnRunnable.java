package com.arcaneminecraft.bungee;

@FunctionalInterface
@Deprecated
public interface ReturnRunnable<T> {
    void run(T arg);

    @FunctionalInterface
    @Deprecated
    interface More<T, R> {

        void run(T arg1, R arg2);
    }
}
