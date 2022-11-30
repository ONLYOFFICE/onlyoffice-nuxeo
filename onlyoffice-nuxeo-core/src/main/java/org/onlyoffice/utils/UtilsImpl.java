/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.DefaultComponent;
import org.onlyoffice.model.DocumentType;

public class UtilsImpl extends DefaultComponent implements Utils {

    private static final Map<String, String> PathLocale = new HashMap<String, String>(){{
        put("az", "az-Latn-AZ");
        put("bg", "bg-BG");
        put("cs", "cs-CZ");
        put("de", "de-DE");
        put("el", "el-GR");
        put("en-GB", "en-GB");
        put("en", "en-US");
        put("es", "es-ES");
        put("fr", "fr-FR");
        put("it", "it-IT");
        put("ja", "ja-JP");
        put("ko", "ko-KR");
        put("lv", "lv-LV");
        put("nl", "nl-NL");
        put("pl", "pl-PL");
        put("pt-BR", "pt-BR");
        put("pt", "pt-PT");
        put("ru", "ru-RU");
        put("sk", "sk-SK");
        put("sv", "sv-SE");
        put("uk", "uk-UA");
        put("vi", "vi-VN");
        put("zh", "zh-CN");
    }};

    @Override
    public String getPathLocale(String language) {
        return PathLocale.get(language);
    }

    @Override
    public String getDocumentKey(DocumentModel model) {
        try {
            String key = model.getId() + "__" + model.getChangeToken();
            return Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDocumentType(String ext) {
        if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.pdf.djvu.fb2.epub.xps.docxf.oform".indexOf(ext) != -1) return "text";
        if (".xls.xlsx.xlsm.xlt.xltx.xltm.ods.fods.ots.csv".indexOf(ext) != -1) return "spreadsheet";
        if (".pps.ppsx.ppsm.ppt.pptx.pptm.pot.potx.potm.odp.fodp.otp".indexOf(ext) != -1) return "presentation";
        return null;
    }

    @Override
    public String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1).trim().toLowerCase();
    }

    @Override
    public String getChangeToken(String key) {
        try {
            String decoded = new String(Base64.getDecoder().decode(key), "UTF-8");
            return decoded.split("__")[1];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDefaultExtensionByType(DocumentType documentType) {
        switch (documentType) {
            case WORD:
                return "docx";
            case CELL:
                return "xlsx";
            case SLIDE:
                return "pptx";
            case FORM:
                return "docxf";
        }

        return null;
    }
}
