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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern CONFIG_LITERAL = Pattern.compile("JSON\\.parse\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)");
    private static final int HEX = 16;
    private static final int HEX_BYTE = 2;
    private static final int HEX_CHAR = 4;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Map<String, Object> config(final String fileName) {
        Map<String, Object> document = new HashMap<>();
        document.put("title", fileName);
        document.put("fileType", "docx");
        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        return config;
    }

    private String render(final String docTitle, final Map<String, Object> config) throws Exception {
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassForTemplateLoading(TestEditorTemplate.class, "/skin/views/onlyedit");
        Template template = configuration.getTemplate("index.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("skinPath", "/nuxeo/site/skin/onlyedit");
        model.put("docType", "word");
        model.put("docUrl", "http://docs.example.com/web-apps/apps/api/documents/api.js");
        model.put("docTitle", docTitle);
        model.put("config", objectMapper.writeValueAsString(config));

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    /**
     * Decodes a JavaScript string literal body the way the browser does before {@code JSON.parse} sees it.
     */
    private static String decodeJsString(final String literal) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c != '\\') {
                result.append(c);
                continue;
            }
            char escaped = literal.charAt(++i);
            switch (escaped) {
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'x':
                    result.append((char) Integer.parseInt(literal.substring(i + 1, i + 1 + HEX_BYTE), HEX));
                    i += HEX_BYTE;
                    break;
                case 'u':
                    result.append((char) Integer.parseInt(literal.substring(i + 1, i + 1 + HEX_CHAR), HEX));
                    i += HEX_CHAR;
                    break;
                default:
                    result.append(escaped);
            }
        }
        return result.toString();
    }

    private static int count(final String text, final String fragment) {
        int count = 0;
        for (int i = text.indexOf(fragment); i >= 0; i = text.indexOf(fragment, i + 1)) {
            count++;
        }
        return count;
    }

    @Test
    public void rendersEditorPage() throws Exception {
        String html = render("Quarterly report", config("report.docx"));

        assertTrue(html.contains("<title>Quarterly report - ONLYOFFICE</title>"));
        assertTrue(html.contains("report.docx"));
        assertTrue(html.contains("new DocsAPI.DocEditor(\"placeholder\", config)"));
    }

    /**
     * Stored XSS: the document title is inserted into the page without HTML escaping.
     */
    @Test
    public void documentTitleIsHtmlEscaped() throws Exception {
        String html = render(TITLE_PAYLOAD, config("report.docx"));

        assertFalse(html, html.contains("<script>alert('title')"));
        assertTrue(html, html.contains("<title>&lt;/title&gt;&lt;script&gt;alert('title')&lt;/script&gt;"
                + " - ONLYOFFICE</title>"));
    }

    /**
     * Stored XSS: the editor config JSON (it contains the file name) is inlined into a script block, so a file name
     * containing "&lt;/script&gt;" closes the block and injects markup.
     */
    @Test
    public void configCannotBreakOutOfScriptBlock() throws Exception {
        String html = render("report", config(CONFIG_PAYLOAD + ".docx"));

        assertFalse(html, html.contains("</script><script>alert('config')"));
        assertEquals("only the api script and the inline script are closed", 2, count(html, "</script"));
    }

    @Test
    public void configReachesEditorUnchanged() throws Exception {
        Map<String, Object> config = config(CONFIG_PAYLOAD + " \"quoted\" back\\slash <!-- ü中 .docx");
        String html = render("report", config);

        Matcher matcher = CONFIG_LITERAL.matcher(html);
        assertTrue(html, matcher.find());
        assertEquals(objectMapper.valueToTree(config), objectMapper.readTree(decodeJsString(matcher.group(1))));
    }
}
