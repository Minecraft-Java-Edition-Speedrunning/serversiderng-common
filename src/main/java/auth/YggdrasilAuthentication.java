package auth;

public record YggdrasilAuthentication (
        String uuid,
        String publicKey,
        Long publicKeyExpiration,
        String publicKeySignature,
        String challenge,
        Long challengeExpiration,
        String challengeSignature
) {}
