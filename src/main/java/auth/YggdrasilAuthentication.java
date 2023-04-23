package auth;

public record YggdrasilAuthentication(
        String uuid,
        String challenge,
        String response,
        String publicKey,
        String signature,
        String instant
) {
}
