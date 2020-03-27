package org.onlyoffice.utils;

import java.util.Base64;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.DefaultComponent;

public class UtilsImpl extends DefaultComponent implements Utils {

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
        if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.pdf.djvu.fb2.epub.xps".indexOf(ext) != -1) return "text";
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

}
