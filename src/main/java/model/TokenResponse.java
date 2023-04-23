package model;

public record TokenResponse<T> (
        T data,
        String token
) {}
