package model;

public record RunForm<T> (
    String runToken,
    T data
) {}
