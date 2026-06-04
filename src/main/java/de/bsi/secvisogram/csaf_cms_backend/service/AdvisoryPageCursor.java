package de.bsi.secvisogram.csaf_cms_backend.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import javax.annotation.Nullable;

/**
 * Opaque pagination cursor for the advisory list, serialised as base64url-encoded JSON.
 *
 * <p>It records everything needed to resume the visible-layer scan exactly after the last emitted
 * row:
 * <ul>
 *   <li>{@code mangoBookmark} – the CouchDB Mango bookmark of the raw page the cursor stopped in;</li>
 *   <li>{@code skipWithinPage} – how many raw rows of that page were already emitted and must be
 *       skipped on resume;</li>
 *   <li>{@code stream} – which logical stream the cursor is positioned in (advisories first, then,
 *       for auditors, advisory versions);</li>
 *   <li>{@code fingerprint} – a SHA-256 hex hash of the normalised {@code expression} selector, used
 *       to reject a cursor that is reused after the query changed.</li>
 * </ul>
 *
 * <p>The cursor is opaque to clients; they echo it back verbatim. A token that cannot be decoded or
 * whose fingerprint does not match the active expression is rejected with HTTP 400.
 */
public final class AdvisoryPageCursor {

    /**
     * The logical stream a cursor is positioned in. Ordering follows the legacy concatenation:
     * advisories first, then advisory versions (auditor only).
     */
    public enum Stream {
        ADVISORY,
        ADVISORY_VERSION
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    @Nullable
    private String mangoBookmark;
    private int skipWithinPage;
    private Stream stream;
    private String fingerprint;

    /** Default constructor for Jackson deserialisation. */
    public AdvisoryPageCursor() {
    }

    public AdvisoryPageCursor(@Nullable String mangoBookmark, int skipWithinPage, Stream stream, String fingerprint) {
        this.mangoBookmark = mangoBookmark;
        this.skipWithinPage = skipWithinPage;
        this.stream = stream;
        this.fingerprint = fingerprint;
    }

    @Nullable
    public String getMangoBookmark() {
        return mangoBookmark;
    }

    public void setMangoBookmark(@Nullable String mangoBookmark) {
        this.mangoBookmark = mangoBookmark;
    }

    public int getSkipWithinPage() {
        return skipWithinPage;
    }

    public void setSkipWithinPage(int skipWithinPage) {
        this.skipWithinPage = skipWithinPage;
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Compute the fingerprint of an expression: a SHA-256 hex hash of the normalised selector. A
     * {@code null} or blank expression normalises to the empty string so an absent filter has a
     * stable fingerprint.
     *
     * @param expression the active filter expression (may be {@code null})
     * @return the lowercase hex SHA-256 of the normalised expression
     */
    public static String fingerprintOf(@Nullable String expression) {
        String normalised = (expression == null) ? "" : expression.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalised.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JLS for every JVM; this branch is unreachable.
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * Encode this cursor as a base64url JSON token.
     *
     * @return the opaque token
     */
    public String encode() {
        try {
            byte[] json = MAPPER.writeValueAsBytes(this);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException ex) {
            // Serialising a fixed-shape POJO cannot fail; treat as a programming error.
            throw new IllegalStateException("Could not encode pagination cursor", ex);
        }
    }

    /**
     * Decode a base64url JSON token and validate it against the active expression.
     *
     * @param token              the opaque cursor produced by a previous page
     * @param expectedExpression the active filter expression to validate the fingerprint against
     * @return the decoded cursor
     * @throws CsafException with HTTP 400 if the token cannot be decoded or the fingerprint does not
     *                       match the active expression
     */
    public static AdvisoryPageCursor decode(String token, @Nullable String expectedExpression) throws CsafException {
        final AdvisoryPageCursor cursor;
        try {
            byte[] json = Base64.getUrlDecoder().decode(token);
            cursor = MAPPER.readValue(json, AdvisoryPageCursor.class);
        } catch (IllegalArgumentException | java.io.IOException ex) {
            throw new CsafException("Invalid pagination cursor", CsafExceptionKey.InvalidFilterExpression, BAD_REQUEST);
        }
        if (cursor.stream == null || cursor.fingerprint == null) {
            throw new CsafException("Malformed pagination cursor", CsafExceptionKey.InvalidFilterExpression, BAD_REQUEST);
        }
        if (!cursor.fingerprint.equals(fingerprintOf(expectedExpression))) {
            throw new CsafException("Pagination cursor does not match the current filter expression",
                    CsafExceptionKey.InvalidFilterExpression, BAD_REQUEST);
        }
        return cursor;
    }
}
