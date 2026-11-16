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

package org.onlyoffice.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestUtils {

    @Inject
    private Utils utils;

    @Inject
    private CoreSession session;

    @Inject
    private RepositoryService repositoryService;

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void getChangeTokenExtractsTokenFromDocumentKey() {
        assertEquals("1-0", utils.getChangeToken(encode("0b6e2c4a-uuid__1-0")));
    }

    @Test
    public void getChangeTokenReturnsNullForKeyWithoutSeparator() {
        assertNull(utils.getChangeToken(encode("no-separator")));
    }

    @Test
    public void getChangeTokenReturnsNullForInvalidInput() {
        assertNull(utils.getChangeToken("%%% not base64 %%%"));
        assertNull(utils.getChangeToken(null));
    }

    @Test
    public void resolveReferenceByIdAndPath() {
        DocumentModel model = TestHelper.createFile(session, "doc", "doc.docx", "application/octet-stream", "x");
        Session repoSession = repositoryService.getSession(model.getRepositoryName());

        assertEquals(model.getId(), utils.resolveReference(repoSession, new IdRef(model.getId())).getUUID());
        assertEquals(model.getId(), utils.resolveReference(repoSession, new PathRef("/doc")).getUUID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveReferenceRejectsNullRef() {
        utils.resolveReference(repositoryService.getSession(session.getRepositoryName()), null);
    }

    @Test
    public void getMimeTypeByExtension() {
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                utils.getMimeType("docx"));
    }

    @Test
    public void getMimeTypeFallsBackToOctetStream() {
        assertEquals("application/octet-stream", utils.getMimeType("unknown-extension"));
    }
}
