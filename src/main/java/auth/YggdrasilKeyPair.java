package auth;

public record YggdrasilKeyPair(
        String publicKeySignature,
        long expiresAt,
        String privateKey,
        String publicKey
) { }
