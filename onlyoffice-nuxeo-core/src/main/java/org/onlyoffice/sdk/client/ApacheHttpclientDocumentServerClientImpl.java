package org.onlyoffice.sdk.client;

import com.onlyoffice.client.ApacheHttpclientDocumentServerClient;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import org.nuxeo.runtime.api.Framework;

public class ApacheHttpclientDocumentServerClientImpl extends ApacheHttpclientDocumentServerClient {
    public ApacheHttpclientDocumentServerClientImpl() {
        super(
                Framework.getService(SettingsManager.class),
                Framework.getService(UrlManager.class)
        );
    }
}
