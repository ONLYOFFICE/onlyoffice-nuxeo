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

package org.onlyoffice.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Test;

/**
 * Renders the editor page with the same FreeMarker defaults WebEngine uses (no auto-escaping).
 */
public class TestEditorTemplate {
    private static final String TITLE_PAYLOAD = "</title><script>alert('title')</script>";
    private static final String CONFIG_PAYLOAD = "</script><script>alert('config')</script>";

    private String render(final String docTitle, final String fileName) throws Exception {
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassForTemplateLoading(TestEditorTemplate.class, "/skin/views/onlyedit");
        Template template = configuration.getTemplate("index.ftl");

        Map<String, Object> document = new HashMap<>();
        document.put("title", fileName);
        document.put("fileType", "docx");
        Map<String, Object> config = new HashMap<>();
        config.put("document", document);

        Map<String, Object> model = new HashMap<>();
        model.put("skinPath", "/nuxeo/site/skin/onlyedit");
        model.put("docType", "word");
        model.put("docUrl", "http://docs.example.com/web-apps/apps/api/documents/api.js");
        model.put("docTitle", docTitle);
        model.put("config", new ObjectMapper().writeValueAsString(config));

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    @Test
    public void rendersEditorPage() throws Exception {
        String html = render("Quarterly report", "report.docx");

        assertTrue(html.contains("<title>Quarterly report - ONLYOFFICE</title>"));
        assertTrue(html.contains("report.docx"));
        assertTrue(html.contains("new DocsAPI.DocEditor(\"placeholder\", config)"));
    }

    /**
     * BUG (stored XSS): the document title is inserted into the page without HTML escaping.
     */
    @Test
    public void documentTitleIsHtmlEscaped() throws Exception {
        String html = render(TITLE_PAYLOAD, "report.docx");

        assertFalse(html, html.contains("<script>alert('title')"));
    }

    /**
     * BUG (stored XSS): the editor config JSON (it contains the file name) is inlined into a script block, so a
     * file name containing "&lt;/script&gt;" closes the block and injects markup.
     */
    @Test
    public void configCannotBreakOutOfScriptBlock() throws Exception {
        String html = render("report", CONFIG_PAYLOAD + ".docx");

        assertFalse(html, html.contains("</script><script>alert('config')"));
    }
}
