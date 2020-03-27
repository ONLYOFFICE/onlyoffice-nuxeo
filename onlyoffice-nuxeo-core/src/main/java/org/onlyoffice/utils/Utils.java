package org.onlyoffice.utils;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface Utils {
    public String getDocumentKey(DocumentModel model);
    public String getFileExtension(String filename);
    public String getDocumentType(String ext);
    public String getChangeToken(String key);
}
