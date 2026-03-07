package com.cognizant.lms.userservice.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenTypeTest {
    @Test
    void testTokenTypeValues() {
        TokenType[] values = TokenType.values();
        assertEquals(2, values.length);
        assertEquals(TokenType.id, values[0]);
        assertEquals(TokenType.access, values[1]);
    }

    @Test
    void testTokenTypeValueOf() {
        assertEquals(TokenType.id, TokenType.valueOf("id"));
        assertEquals(TokenType.access, TokenType.valueOf("access"));
    }
}
