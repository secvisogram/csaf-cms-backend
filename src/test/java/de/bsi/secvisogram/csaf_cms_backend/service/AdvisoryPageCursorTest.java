package de.bsi.secvisogram.csaf_cms_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for the opaque pagination cursor codec ({@link AdvisoryPageCursor}). These tests run
 * without Spring or CouchDB: the cursor is a standalone, pure codec by design.
 * They cover the round-trip, fingerprint binding to the active expression, and the
 * HTTP-400 mapped failure modes for tampered, garbage and stale tokens.
 */
class AdvisoryPageCursorTest {

    private static final String EXPRESSION = "{\"type\":\"Operator\",\"value\":\"title1\"}";

    @Test
    void encodeDecodeRoundTripPreservesAllFields() throws CsafException {
        String fingerprint = AdvisoryPageCursor.fingerprintOf(EXPRESSION);
        AdvisoryPageCursor original = new AdvisoryPageCursor(
                "g1AAAA-mango-bookmark", 7, AdvisoryPageCursor.Stream.ADVISORY_VERSION, fingerprint);

        String token = original.encode();
        AdvisoryPageCursor decoded = AdvisoryPageCursor.decode(token, EXPRESSION);

        assertEquals("g1AAAA-mango-bookmark", decoded.getMangoBookmark());
        assertEquals(7, decoded.getSkipWithinPage());
        assertEquals(AdvisoryPageCursor.Stream.ADVISORY_VERSION, decoded.getStream());
        assertEquals(fingerprint, decoded.getFingerprint());
    }

    @Test
    void roundTripPreservesNullMangoBookmark() throws CsafException {
        String fingerprint = AdvisoryPageCursor.fingerprintOf(EXPRESSION);
        AdvisoryPageCursor original =
                new AdvisoryPageCursor(null, 0, AdvisoryPageCursor.Stream.ADVISORY, fingerprint);

        AdvisoryPageCursor decoded = AdvisoryPageCursor.decode(original.encode(), EXPRESSION);

        assertNull(decoded.getMangoBookmark(), "a null mango bookmark must survive the round-trip");
        assertEquals(0, decoded.getSkipWithinPage());
        assertEquals(AdvisoryPageCursor.Stream.ADVISORY, decoded.getStream());
    }

    @Test
    void decodeRejectsGarbageToken() {
        // Not valid base64url at all -> base64 decode fails -> 400.
        CsafException ex = assertThrows(CsafException.class,
                () -> AdvisoryPageCursor.decode("!!! not base64 !!!", EXPRESSION));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    @Test
    void decodeRejectsValidBase64ButNotJson() {
        // Decodes as base64url but the bytes are not the cursor JSON -> 400.
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("this is not json".getBytes(StandardCharsets.UTF_8));
        CsafException ex = assertThrows(CsafException.class,
                () -> AdvisoryPageCursor.decode(token, EXPRESSION));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    @Test
    void decodeRejectsTamperedToken() throws CsafException {
        String fingerprint = AdvisoryPageCursor.fingerprintOf(EXPRESSION);
        String token = new AdvisoryPageCursor("mango", 3, AdvisoryPageCursor.Stream.ADVISORY, fingerprint)
                .encode();
        // Flip a character in the middle of the token so the decoded bytes are corrupt.
        char[] chars = token.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = (chars[mid] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);

        CsafException ex = assertThrows(CsafException.class,
                () -> AdvisoryPageCursor.decode(tampered, EXPRESSION));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    @Test
    void decodeRejectsFingerprintFromDifferentExpression() throws CsafException {
        // Cursor minted under one expression must not decode under a different expression.
        String fingerprint = AdvisoryPageCursor.fingerprintOf(EXPRESSION);
        String token = new AdvisoryPageCursor("mango", 0, AdvisoryPageCursor.Stream.ADVISORY, fingerprint)
                .encode();

        String otherExpression = "{\"type\":\"Operator\",\"value\":\"DIFFERENT\"}";
        CsafException ex = assertThrows(CsafException.class,
                () -> AdvisoryPageCursor.decode(token, otherExpression));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState(),
                "a cursor reused after the expression changed must be rejected with 400");
    }

    @Test
    void decodeRejectsMissingStreamOrFingerprint() {
        // A structurally-valid JSON object that lacks the required stream/fingerprint fields.
        String json = "{\"mangoBookmark\":\"m\",\"skipWithinPage\":1}";
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        CsafException ex = assertThrows(CsafException.class,
                () -> AdvisoryPageCursor.decode(token, EXPRESSION));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getRecommendedHttpState());
    }

    @Test
    void fingerprintNormalisesNullAndBlankToTheSameValue() {
        String nullFingerprint = AdvisoryPageCursor.fingerprintOf(null);
        String blankFingerprint = AdvisoryPageCursor.fingerprintOf("   ");
        String emptyFingerprint = AdvisoryPageCursor.fingerprintOf("");

        assertEquals(nullFingerprint, blankFingerprint,
                "null and blank expression must share a fingerprint (absent filter is stable)");
        assertEquals(nullFingerprint, emptyFingerprint);
    }

    @Test
    void fingerprintTrimsSurroundingWhitespace() {
        assertEquals(AdvisoryPageCursor.fingerprintOf(EXPRESSION),
                AdvisoryPageCursor.fingerprintOf("  " + EXPRESSION + "  "),
                "leading/trailing whitespace must not change the fingerprint");
    }

    @Test
    void differentExpressionsProduceDifferentFingerprints() {
        assertNotEquals(AdvisoryPageCursor.fingerprintOf(EXPRESSION),
                AdvisoryPageCursor.fingerprintOf("{\"type\":\"Operator\",\"value\":\"other\"}"));
    }

    @Test
    void cursorMintedForNullExpressionDecodesUnderNullAndBlank() throws CsafException {
        String fingerprint = AdvisoryPageCursor.fingerprintOf(null);
        String token = new AdvisoryPageCursor(null, 0, AdvisoryPageCursor.Stream.ADVISORY, fingerprint)
                .encode();

        // Both null and blank are the "no filter" case and must accept the same cursor.
        assertEquals(AdvisoryPageCursor.Stream.ADVISORY, AdvisoryPageCursor.decode(token, null).getStream());
        assertEquals(AdvisoryPageCursor.Stream.ADVISORY, AdvisoryPageCursor.decode(token, "  ").getStream());
    }
}
