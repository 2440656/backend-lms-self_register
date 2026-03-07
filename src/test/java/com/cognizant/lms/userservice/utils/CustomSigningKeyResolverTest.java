package com.cognizant.lms.userservice.utils;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomSigningKeyResolverTest {
    private CustomSigningKeyResolver customSigningKeyResolver;
    private JwsHeader jwsHeader;
    private Claims claims;
    private JwkProvider jwkProvider;
    private Jwk jwk;
    private RSAPublicKey rsaPublicKey;

    @BeforeEach
    void setUp() throws JwkException, MalformedURLException {
        jwkProvider = mock(UrlJwkProvider.class);
        customSigningKeyResolver = new CustomSigningKeyResolver("http://localhost");
        jwsHeader = mock(JwsHeader.class);
        claims = mock(Claims.class);
        jwk = mock(Jwk.class);
        rsaPublicKey = mock(RSAPublicKey.class);

        when(jwsHeader.getKeyId()).thenReturn("testKeyId");
        when(claims.getIssuer()).thenReturn("testIssuer");
        when(jwkProvider.get("testKeyId")).thenReturn(jwk);
        when(jwk.getPublicKey()).thenReturn(rsaPublicKey);
    }

    @Test
    void resolveSigningKey_invalidUrl_returnsNull() {
        CustomSigningKeyResolver resolverWithInvalidUrl = new CustomSigningKeyResolver("invalid-url");
        PublicKey publicKey = (PublicKey) resolverWithInvalidUrl.resolveSigningKey(jwsHeader, claims);
        assertNull(publicKey);
    }

    @Test
    void resolveSigningKey_jwkException_returnsNull() throws JwkException {
        when(jwkProvider.get("testKeyId")).thenThrow(new JwkException("JWK error"));
        PublicKey publicKey = (PublicKey) customSigningKeyResolver.resolveSigningKey(jwsHeader, claims);
        assertNull(publicKey);
    }
}
