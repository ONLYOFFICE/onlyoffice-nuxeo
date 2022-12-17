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

package org.onlyoffice.constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onlyoffice.model.DocumentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListFormats {
    public static final List<Format> formats = new ArrayList<Format>() {{
        add(new Format("djvu", DocumentType.WORD, new ArrayList<String>()));
        add(new Format("doc", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docm", DocumentType.WORD, Arrays.asList("docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docx", DocumentType.WORD, true, Arrays.asList("docm", "dotx", "dotm", "docxf", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("docxf", DocumentType.FORM, true, Arrays.asList("docm", "docx", "oform", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("oform", DocumentType.WORD, true, Arrays.asList( "pdf")));
        add(new Format("dot", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("dotm", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("dotx", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("epub", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("fb2", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("fodt", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("html", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("mht", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("odt", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "ott", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("ott", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "pdf", "pdfa", "rtf", "txt")));
        add(new Format("pdf", DocumentType.WORD, new ArrayList<String>()));
        add(new Format("rtf", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "txt")));
        add(new Format("txt", DocumentType.WORD, new ArrayList<String>()));
        add(new Format("xps", DocumentType.WORD, Arrays.asList("pdf", "pdfa")));
        add(new Format("oxps", DocumentType.WORD, Arrays.asList("pdf", "pdfa")));
        add(new Format("xml", DocumentType.WORD, Arrays.asList("docm", "docx", "docxf", "dotx", "dotm", "epub", "fb2", "html", "odt", "ott", "pdf", "pdfa", "rtf", "txt")));

        add(new Format("csv", DocumentType.CELL, new ArrayList<String>()));
        add(new Format("fods", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xltx", "xlsm", "xltm")));
        add(new Format("ods", DocumentType.CELL, Arrays.asList("csv", "ots", "pdf", "pdfa", "xlsx", "xltx", "xlsm", "xltm")));
        add(new Format("ots", DocumentType.CELL, Arrays.asList("csv", "ods", "pdf", "pdfa", "xlsx", "xltx", "xlsm", "xltm")));
        add(new Format("xls", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xltx", "xlsm", "xltm")));
        add(new Format("xlsm", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xltx", "xltm")));
        add(new Format("xlsx", DocumentType.CELL, true, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xltx", "xlsm", "xltm")));
        add(new Format("xlt", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xltx", "xlsm", "xltm")));
        add(new Format("xltm", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xltx", "xlsm")));
        add(new Format("xltx", DocumentType.CELL, Arrays.asList("csv", "ods", "ots", "pdf", "pdfa", "xlsx", "xlsm", "xltm")));

        add(new Format("fodp", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("odp", DocumentType.SLIDE, Arrays.asList("otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("otp", DocumentType.SLIDE, Arrays.asList("odp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("pot", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("potm", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm")));
        add(new Format("potx", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "pptx", "pptm", "potm")));
        add(new Format("pps", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("ppsm", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("ppsx", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("ppt", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "pptm", "potm")));
        add(new Format("pptm", DocumentType.SLIDE, Arrays.asList("odp", "otp", "pdf", "pdfa", "potx", "pptx", "potm")));
        add(new Format("pptx", DocumentType.SLIDE, true, Arrays.asList( "odp", "otp", "pdf", "pdfa", "potx", "pptm", "potm")));
    }};

    public static List<Format> getSupportedFormats() {
        return formats;
    }

    public static JSONArray getSupportedFormatsAsJson() throws JSONException {
        JSONArray array = new JSONArray();
        for (Format format : formats) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", format.name);
            jsonObject.put("type", format.type);
            jsonObject.put("edit", format.edit);
            jsonObject.put("convertTo", new JSONArray(format.convertTo));
            array.put(jsonObject);
        }
        return array;
    }
}
