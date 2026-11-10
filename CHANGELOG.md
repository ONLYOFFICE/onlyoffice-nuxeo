# Change Log

##
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
