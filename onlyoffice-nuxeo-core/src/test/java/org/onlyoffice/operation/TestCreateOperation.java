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

package org.onlyoffice.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.onlyoffice.test.OnlyofficeFeature;
import org.onlyoffice.test.TestHelper;

@RunWith(FeaturesRunner.class)
@Features(OnlyofficeFeature.class)
public class TestCreateOperation {
    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    @Inject
    private AutomationService automationService;

    @Inject
    private CoreSession session;

    @Inject
    private CoreFeature coreFeature;

    private DocumentModel create(final CoreSession userSession, final String title, final String type,
                                 final String language) throws OperationException {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/");
        params.put("title", title);
        params.put("type", type);
        if (language != null) {
            params.put("language", language);
        }

        try (OperationContext ctx = new OperationContext(userSession)) {
            String id = (String) automationService.run(ctx, CreateOperation.ID, params);
            return session.getDocument(new IdRef(id));
        }
    }

    private static Blob blobOf(final DocumentModel model) {
        return (Blob) model.getPropertyValue("file:content");
    }

    @Test
    public void createsWordDocumentFromTemplate() throws Exception {
        DocumentModel model = create(session, "My report", "word", "en");
        Blob blob = blobOf(model);

        assertEquals("File", model.getType());
        assertEquals("My report", model.getTitle());
        assertEquals("My report.docx", blob.getFilename());
        assertTrue("template is not empty", blob.getLength() > 0);
    }

    @Test
    public void createsDocumentOfEveryType() throws Exception {
        assertEquals("Sheet.xlsx", blobOf(create(session, "Sheet", "cell", null)).getFilename());
        assertEquals("Slides.pptx", blobOf(create(session, "Slides", "slide", null)).getFilename());
    }

    /**
     * BUG: {@code Utils.getMimeType} expects an extension but the operation passes the whole file name, so the
     * registry lookup always fails and new documents get {@code application/octet-stream}.
     */
    @Test
    public void createdDocumentHasMimeTypeOfItsFormat() throws Exception {
        assertEquals(DOCX_MIME_TYPE, blobOf(create(session, "Doc", "word", null)).getMimeType());
        assertEquals(XLSX_MIME_TYPE, blobOf(create(session, "Sheet", "cell", null)).getMimeType());
        assertEquals(PPTX_MIME_TYPE, blobOf(create(session, "Slides", "slide", null)).getMimeType());
    }

    @Test
    public void languageTagSelectsLocalizedTemplate() throws Exception {
        assertTrue(blobOf(create(session, "Doc", "word", "de-DE")).getLength() > 0);
        assertTrue(blobOf(create(session, "Doc unknown", "word", "xx-YY")).getLength() > 0);
    }

    @Test
    public void unknownTypeIsRejected() throws Exception {
        try {
            create(session, "Doc", "unknown", null);
            fail("Unknown document type must be rejected");
        } catch (OperationException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * BUG: {@code type.toUpperCase()} depends on the JVM default locale; with a Turkish locale "slide" becomes
     * "SLIDE" with a dotted capital I (U+0130) and no document type matches.
     */
    @Test
    public void typeParsingDoesNotDependOnDefaultLocale() throws Exception {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertEquals("Slides.pptx", blobOf(create(session, "Slides", "slide", null)).getFilename());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void titleIsStoredAsIs() throws Exception {
        DocumentModel model = create(session, "Budget: Q1 \"draft\"", "cell", null);

        assertEquals("Budget: Q1 \"draft\"", model.getPropertyValue("dc:title"));
    }

    /**
     * BUG: the title was used as the document name, which cannot contain a slash, so the operation failed.
     */
    @Test
    public void titleWithSlashCreatesDocument() throws Exception {
        DocumentModel model = create(session, "Q1/Q2 report", "word", null);

        assertEquals("Q1/Q2 report", model.getPropertyValue("dc:title"));
        assertFalse(model.getName(), model.getName().contains("/"));
        assertEquals("Q1_Q2 report.docx", blobOf(model).getFilename());
    }

    @Test
    public void fileNameIsSafe() throws Exception {
        // ':' in a file name breaks the Nuxeo conversions (full-text extraction), path separators break downloads
        assertEquals("a_b_c_ d.docx", blobOf(create(session, "a/b\\c: d", "word", null)).getFilename());
    }

    @Test
    public void sameTitleTwiceCreatesTwoDocuments() throws Exception {
        DocumentModel first = create(session, "Notes", "word", null);
        DocumentModel second = create(session, "Notes", "word", null);

        assertNotEquals(first.getId(), second.getId());
        assertEquals("Notes", second.getPropertyValue("dc:title"));
    }

    @Test(expected = DocumentSecurityException.class)
    public void userWithoutAddChildrenCannotCreate() throws Exception {
        DocumentModel root = session.getRootDocument();
        TestHelper.grant(session, root, "jdoe", SecurityConstants.READ);

        create(coreFeature.getCoreSession("jdoe"), "Doc", "word", null);
    }
}
