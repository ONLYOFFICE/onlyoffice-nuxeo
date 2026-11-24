/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onlyoffice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;

import jakarta.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.service.LinkTokenService.LinkToken;
import org.onlyoffice.service.LinkTokenService.Operation;
import org.onlyoffice.test.OnlyofficeFeature;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestLinkTokenService {
    private static final String USER = "jdoe";
    private static final String DOCUMENT = "8f1c5c2e-3d2a-4b8e-9a51-2f7d0c6b1a10";
    private static final String OTHER_DOCUMENT = "1b2e7f40-5c9d-4e1a-8f3b-6d0a9c2e7b54";
    private static final String KEY = "a2V5X18x";
    private static final long HOUR_MS = 3600000L;
    private static final long CLOCK_AHEAD_MS = 30000L;

    @Inject
    private LinkTokenService linkTokenService;

    @Inject
    private TokenAuthenticationService tokenAuthenticationService;

    private void assertRejected(final String token, final Operation operation, final String documentId) {
        try {
            linkTokenService.verifyToken(token, operation, documentId);
            fail("Token must not be valid for " + operation + " of " + documentId);
        } catch (SecurityException e) {
            // expected
        }
    }

    private static Algorithm storedSecret() {
        return Algorithm.HMAC256(Framework.getService(KeyValueService.class).getKeyValueStore("onlyoffice")
                .get("onlyoffice:link-token-secret"));
    }

    @Test
    public void downloadTokenIsValidOnlyForTheDownloadOfItsDocument() {
        String token = linkTokenService.createToken(Operation.DOWNLOAD, DOCUMENT, USER, null);

        assertEquals(USER, linkTokenService.verifyToken(token, Operation.DOWNLOAD, DOCUMENT).getUserName());
        assertRejected(token, Operation.CALLBACK, DOCUMENT);
        assertRejected(token, Operation.TEST_FILE, null);
        assertRejected(token, Operation.DOWNLOAD, OTHER_DOCUMENT);
    }

    @Test
    public void callbackTokenIsValidOnlyForTheCallbackOfItsDocument() {
        String token = linkTokenService.createToken(Operation.CALLBACK, DOCUMENT, USER, KEY);

        LinkToken link = linkTokenService.verifyToken(token, Operation.CALLBACK, DOCUMENT);
        assertEquals(USER, link.getUserName());
        assertEquals(KEY, link.getDocumentKey());
        assertRejected(token, Operation.DOWNLOAD, DOCUMENT);
        assertRejected(token, Operation.TEST_FILE, null);
        assertRejected(token, Operation.CALLBACK, OTHER_DOCUMENT);
    }

    @Test
    public void testFileTokenIsNotValidForDocuments() {
        String token = linkTokenService.createToken(Operation.TEST_FILE, null, USER, null);

        assertEquals(USER, linkTokenService.verifyToken(token, Operation.TEST_FILE, null).getUserName());
        assertRejected(token, Operation.DOWNLOAD, DOCUMENT);
        assertRejected(token, Operation.CALLBACK, DOCUMENT);
    }

    @Test
    public void downloadTokenExpiresButCallbackTokenLastsTheWholeEditingSession() {
        assertNotNull(JWT.decode(linkTokenService.createToken(Operation.DOWNLOAD, DOCUMENT, USER, null))
                .getExpiresAt());
        assertNull(JWT.decode(linkTokenService.createToken(Operation.CALLBACK, DOCUMENT, USER, KEY))
                .getExpiresAt());
    }

    @Test
    public void expiredTokenIsRejected() {
        linkTokenService.createToken(Operation.DOWNLOAD, DOCUMENT, USER, null);
        String expired = JWT.create()
                .withSubject(USER)
                .withClaim("op", Operation.DOWNLOAD.name())
                .withClaim("doc", DOCUMENT)
                .withExpiresAt(new Date(System.currentTimeMillis() - HOUR_MS))
                .sign(storedSecret());

        assertRejected(expired, Operation.DOWNLOAD, DOCUMENT);
    }

    /**
     * BUG: a token issued by a node of the cluster whose clock is a little ahead was refused by the others ("can't be
     * used before"), so the editor did not open when Document Server's request landed on another node.
     */
    @Test
    public void tokenIssuedByNodeWithClockAheadIsAccepted() {
        linkTokenService.createToken(Operation.DOWNLOAD, DOCUMENT, USER, null);
        long now = System.currentTimeMillis();
        String ahead = JWT.create()
                .withSubject(USER)
                .withClaim("op", Operation.DOWNLOAD.name())
                .withClaim("doc", DOCUMENT)
                .withIssuedAt(new Date(now + CLOCK_AHEAD_MS))
                .withExpiresAt(new Date(now + HOUR_MS))
                .sign(storedSecret());

        assertEquals(USER, linkTokenService.verifyToken(ahead, Operation.DOWNLOAD, DOCUMENT).getUserName());
    }

    @Test
    public void tokenNotSignedByThisServerIsRejected() {
        String forged = JWT.create()
                .withSubject(USER)
                .withClaim("op", Operation.DOWNLOAD.name())
                .withClaim("doc", DOCUMENT)
                .sign(Algorithm.HMAC256("document server secret"));
        String token = linkTokenService.createToken(Operation.DOWNLOAD, DOCUMENT, USER, null);
        int signature = token.lastIndexOf('.') + 1;
        char changed = token.charAt(signature) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signature) + changed + token.substring(signature + 1);

        assertRejected(forged, Operation.DOWNLOAD, DOCUMENT);
        assertRejected(tampered, Operation.DOWNLOAD, DOCUMENT);
        assertRejected(null, Operation.DOWNLOAD, DOCUMENT);
        assertRejected("", Operation.DOWNLOAD, DOCUMENT);
    }

    @Test
    public void nuxeoTokensHandedOutByEarlierVersionsAreRevokedOnStart() {
        String legacy = tokenAuthenticationService.acquireToken(USER, "onlyoffice-nuxeo", "document-server", "", "rw");
        String other = tokenAuthenticationService.acquireToken(USER, "other-application", "device", "", "rw");

        ((LinkTokenServiceImpl) linkTokenService).start(null);

        assertNull(tokenAuthenticationService.getUserName(legacy));
        assertEquals(USER, tokenAuthenticationService.getUserName(other));
    }
}
