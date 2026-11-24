# Change Log

##
## Fixed
- Nuxeo failing to start alongside other addons (e.g. OpenSearch) because the package replaced httpclient5 in nxserver/lib
- users opening the same document ending up in separate editing sessions after a background update of the document (thumbnail, fulltext, ...), with all but the first one to save losing their changes (Invalid change token)
- the file of a document replaced by other means than the editor (Blob.AttachOnDocument, WebDAV, Drive, ONLYOFFICE.Convert, ...) keeping the same document key: Document Server served the previous file from its cache, and an editing session saved over the new file; the key now also carries a hash of the file, so that any change of the file (including restoring a version) changes it
- a save of an editing session failing for a transient reason (e.g. Document Server unreachable while downloading the file) releasing the lock of the document anyway: while Document Server retried, the document could be locked or changed by others and the changes of the session were lost; the lock is now released only once the file is saved, and nothing is committed when the callback answers 5xx
- ONLYOFFICE.Convert replacing the file without keeping the original: unless the document type had an automatic versioning policy, the file in its original format was lost; the original is now kept as a version, and the converted file is saved as a minor version
- ONLYOFFICE.Convert replacing the file of a document open in ONLYOFFICE (the changes of the editing session were then lost), of a version, of a document in the trash or under retention; it now answers 409
- the download of a document by Document Server answering 500 instead of 401 when JWT is enabled and the request has no valid token
- saving the settings page writing to nxserver/config/onlyoffice.properties every property whose name starts with "onlyoffice", including those of other addons (e.g. "onlyoffice-...")
- the convert button shown for every attachment of a document, while the conversion always replaced the main file; it is now shown for the main file only
- the settings page reporting "An error occurred while saving the settings" for saved settings when a connection check failed with an error code the SDK does not know; it now reports the check as failed with "Unknown error"
- the Basque translation not installed; the Dutch translation of the download error of a conversion and the details of the conversion check error in Basque not shown; the "size limit exceeded" and "output format" conversion errors shown as their key or as "Unknown error"
- the conversion dialog going on polling after being closed with Esc or a click outside, opening again when the conversion ended, polling twice (and reporting a false error) after Cancel and Convert in quick succession, and polling for ever a conversion Document Server never finished; a conversion is now given up after 5 minutes
- callback answering 500 with the text of the exception to malformed requests (not a JSON object, unknown or missing status, header token without payload, save without file url), now 400 without the exception text
- versions, proxies, documents in the trash and documents under retention or legal hold opened for editing: the changes could not be saved and were lost (or saved into a deleted document); they now open for viewing
- ONLYOFFICE.Convert answering 500 for a document that cannot be converted (unsupported format, no content) and ONLYOFFICE.Create for an unknown document type, now 400 with the reason
- data of the settings page (path to api.js) and unknown fields of the request stored as the "onlyoffice.extra" setting; a value stored by earlier versions is removed on the next save

## Security
- the document key was built from the document id, its change token and the digest of its file, which anyone knowing the document could compute: without JWT, Document Server opens the editing session of whatever key it is given; the key is now a hash of these with a secret of the connector, and the callback accepts only the key of the current revision
- the links handed to Document Server (download, callback, test file of the connection check) carried a Nuxeo authentication token of the user, which never expired and gave access to the whole REST API as that user; they now carry a token of the connector, valid only for one operation on one document (and, for the callback, one editing session); the tokens handed out by earlier versions are revoked on startup. Close all editing sessions before upgrading: the changes of a session left open during the upgrade cannot be saved back to Nuxeo
- the download link handed to Document Server served any file of the document, inline when "inline=true" was added to it: an html or svg file uploaded by a user ran its scripts in the Nuxeo session of anyone who opened the link; the file is now always sent as an attachment with "X-Content-Type-Options: nosniff" and "Content-Security-Policy: sandbox", and only in a format known to Document Server

## 3.1.0
## Added
- support for Nuxeo 2025.*

## Changed
- updated document type icons (word, cell, slide, pdf)
- updated com.onlyoffice:docs-integration-sdk to v1.11.0, see [ONLYOFFICE/docs-integration-sdk-java v1.11.0](https://github.com/ONLYOFFICE/docs-integration-sdk-java/releases/tag/v1.11.0)
- download and test-txt endpoints verify the JWT operation claim (download)
- documents locked by another user in Nuxeo open in view mode, other Nuxeo security policies are respected

## Fixed
- escaping of the document title and the editor config on the editor page (XSS)
- lock set by another user outside ONLYOFFICE removed when saving the document
- mime type of created and converted documents
- special characters in settings lost after restart
- "null" in the user name shown in the editor
- creating presentations and diagrams with a Turkish server locale
- filter error for a document without content
- first validation of the convert service failing on a new instance
- creating a document with "/" in the title

## 3.0.0
## Added
- core of the package has been moved to com.onlyoffice.docs-integration-sdk (https://github.com/ONLYOFFICE/docs-integration-sdk-java)
- improved connection settings validation
- shardkey parameter
- default empty file templates
- editing pdf

## Changed
- apache httpclient 5
- default token lifetime is 5 minutes
- address of the convert service, /converter instead /ConvertService.ashx
- create pdf instead docxf
- remove support docxf and oform

## 2.1.0
## Added
- improvement of jwt signature
- settings validation
- button to open file location
- documents conversion
- change favicon in editor by document type
- advanced server settings for specifying internal addresses
- setting authorization header
- settings page
- ability to create documents

## Changed
- document server v6.0 and earlier is no longer supported
- fixed package compatibility issue with SAML

## 2.0.0
## Added
- support for version 2021

## 1.1.0
## Added
- support docxf and oform formats

## 1.0.0
- Initial release
