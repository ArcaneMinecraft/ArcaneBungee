package com.arcaneminecraft.bungee;

@FunctionalInterface
public interface ReturnRunnable<T> {
    void run(T args);
}
