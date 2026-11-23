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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LinkTokenServiceImpl extends DefaultComponent implements LinkTokenService {
    private static final Logger logger = LoggerFactory.getLogger(LinkTokenServiceImpl.class);

    private static final String STORE_NAME = "onlyoffice";
    private static final String SECRET_KEY = "onlyoffice:link-token-secret";
    private static final int SECRET_LENGTH = 32;

    private static final String OPERATION_CLAIM = "op";
    private static final String DOCUMENT_CLAIM = "doc";
    private static final String KEY_CLAIM = "key";

    // Document Server fetches the file as soon as the editor or the conversion starts; a callback link has no expiry,
    // the save comes when the last user leaves the editor, however long the session lasted
    private static final long DOWNLOAD_TTL_MS = TimeUnit.HOURS.toMillis(1);

    // Earlier versions handed Document Server Nuxeo authentication tokens of this application, which never expire
    // and open the whole REST API
    private static final String LEGACY_TOKENS_DIRECTORY = "authTokens";
    private static final String LEGACY_TOKENS_APPLICATION = "onlyoffice-nuxeo";

    @Override
    public void start(final ComponentContext context) {
        revokeLegacyTokens();
    }

    @Override
    public String createToken(final Operation operation, final String documentId, final String userName,
                              final String documentKey) {
        JWTCreator.Builder builder = JWT.create()
                .withSubject(userName)
                .withClaim(OPERATION_CLAIM, operation.name())
                .withClaim(DOCUMENT_CLAIM, documentId)
                .withIssuedAt(new Date());

        if (operation == Operation.CALLBACK) {
            builder.withClaim(KEY_CLAIM, documentKey);
        } else {
            builder.withExpiresAt(new Date(System.currentTimeMillis() + DOWNLOAD_TTL_MS));
        }

        return builder.sign(getAlgorithm());
    }

    @Override
    public LinkToken verifyToken(final String token, final Operation operation, final String documentId) {
        if (token == null || token.isEmpty()) {
            throw new SecurityException("No link token");
        }

        DecodedJWT jwt;
        try {
            jwt = JWT.require(getAlgorithm())
                    .withClaim(OPERATION_CLAIM, operation.name())
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            throw new SecurityException("Invalid link token: " + e.getMessage());
        }

        if (!Objects.equals(jwt.getClaim(DOCUMENT_CLAIM).asString(), documentId)) {
            throw new SecurityException("Link token issued for another document");
        }
        if (jwt.getSubject() == null || jwt.getSubject().isEmpty()) {
            throw new SecurityException("Link token without user");
        }

        return new LinkToken(jwt.getSubject(), jwt.getClaim(KEY_CLAIM).asString());
    }

    private static Algorithm getAlgorithm() {
        return Algorithm.HMAC256(getSecret());
    }

    // Kept in the key/value store, shared by the nodes of a cluster and persisted by the database (SQL or MongoDB),
    // so that the links of open editing sessions stay valid across restarts; removing it invalidates all the links
    private static byte[] getSecret() {
        KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(STORE_NAME);

        byte[] secret = store.get(SECRET_KEY);
        if (secret == null) {
            byte[] generated = new byte[SECRET_LENGTH];
            new SecureRandom().nextBytes(generated);
            // another node may have generated one meanwhile
            store.compareAndSet(SECRET_KEY, null, generated);
            secret = store.get(SECRET_KEY);
        }
        return secret;
    }

    private void revokeLegacyTokens() {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        if (directoryService == null || directoryService.getDirectory(LEGACY_TOKENS_DIRECTORY) == null) {
            return;
        }

        try {
            int revoked = TransactionHelper.runInTransaction(() -> Framework.doPrivileged(() -> {
                try (Session session = directoryService.open(LEGACY_TOKENS_DIRECTORY)) {
                    int count = 0;
                    for (DocumentModel entry : session.query(Collections.singletonMap("applicationName",
                            (Serializable) LEGACY_TOKENS_APPLICATION))) {
                        session.deleteEntry(entry.getId());
                        count++;
                    }
                    return count;
                }
            }));
            if (revoked > 0) {
                logger.info("Revoked {} Nuxeo authentication tokens handed to Document Server by earlier versions",
                        revoked);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not revoke the Nuxeo authentication tokens handed to Document Server by earlier "
                    + "versions: {}", e.toString());
        }
    }
}
