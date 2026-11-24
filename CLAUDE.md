# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Keeping the docs current

After every change, check that `README.md` and this `CLAUDE.md` still match the code (build requirements, settings keys, endpoints, components, module structure) and update them in the same change if they don't.

## Project Overview

This is a Nuxeo Marketplace plugin that integrates ONLYOFFICE Docs into the Nuxeo content management platform, enabling document creation, editing, and co-authoring directly in Nuxeo. It targets Nuxeo LTS 2025 (`nuxeo-parent` 2025.x, Jakarta EE).

## Build & Test Commands

```bash
# Full build (produces installable ZIP package)
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=TestUtils

# Run a specific test method
mvn test -Dtest=TestUtils#getChangeTokenExtractsTokenFromDocumentKey

# Build a specific module only
mvn clean install -pl onlyoffice-nuxeo-core
```

**Package output:** `onlyoffice-nuxeo-package/target/onlyoffice-nuxeo-package-*.zip`

**After every code change**, before reporting it done or committing, run the tests of the affected modules and the style check, and fix whatever they report:

```bash
mvn test -pl <module>
mvn checkstyle:check -pl <module>
```

Run them from the repository root (`checkstyle.xml` is resolved from there). Checkstyle is bound to the `verify` phase, so `mvn test` alone does not run it; it also covers test sources. Typical violations: magic numbers (extract a named constant), lines over 120 characters, missing `final` on parameters.

Build requires **Java 21** (CI uses Temurin 21) and Maven 3.8.6+. Some Nuxeo artifacts come from the `maven-private` repository at packages.nuxeo.com, so the build needs credentials for it in `~/.m2/settings.xml`.

Tests use `FeaturesRunner` with `@Features(OnlyofficeFeature.class)` (`onlyoffice-nuxeo-core/src/test/java/org/onlyoffice/test/`). REST tests in `onlyoffice-nuxeo-rest` call `OnlyofficeObject` methods directly with a mocked `WebContext`; they do not go through HTTP, JAX-RS routing or the authentication filter. Tests change global `Framework` properties and the `onlyoffice.properties` file, so they must run sequentially.

## Module Structure

```
onlyoffice-nuxeo-core/       # Core Java: services, managers, operations, utils, WebEngine editor page
onlyoffice-nuxeo-rest/       # JAX-RS REST API endpoints
onlyoffice-nuxeo-web-ui/     # Polymer web components and i18n resources
onlyoffice-nuxeo-package/    # Marketplace package assembly
```

`onlyoffice-nuxeo-rest` depends on `onlyoffice-nuxeo-core`. Both are bundled by `onlyoffice-nuxeo-package`. The ONLYOFFICE SDK and httpclient5/httpcore5 are shaded into the core jar (`org.apache.hc` is relocated); the package ships no jars to `nxserver/lib`.

## Architecture

### Integration Flow

1. User opens/creates a document in Nuxeo Web UI (Polymer components in `onlyoffice-nuxeo-web-ui`).
2. The browser navigates to the WebEngine route `/site/onlyedit/{id}` — handled by `Editor.java` in the core module, rendered with `skin/views/onlyedit/index.ftl`.
3. `ConfigServiceImpl` builds the ONLYOFFICE editor configuration JSON (document URL, callback URL, JWT token, permissions).
4. ONLYOFFICE Docs loads the file from Nuxeo via `GET /api/v1/onlyoffice/download/{id}?token=…`.
5. On editing events and save, ONLYOFFICE posts callbacks to `POST /api/v1/onlyoffice/callback/{id}?token=…` — handled by `CallbackServiceImpl`, which locks the document while it is being edited and saves the new blob back to the Nuxeo repository.

The download, callback and test-file URLs are open URLs (no Nuxeo authentication, see `OSGI-INF/link-token-service.xml`). They are protected by a **link token** of the connector (`LinkTokenService`): a JWT signed with a secret generated on first use and kept in the KeyValue store, bound to one operation, one document and the user, and for the callback to the editing session key. The request runs as that user (`loginAs`). When a JWT secret is configured, the Document Server JWT is verified on top of it.

### Backend Key Components (all under `org.onlyoffice`)

| Package | Role |
|---|---|
| `sdk.manager.document` | Document type/format detection, document key (`docId__userChangeToken`, base64) |
| `sdk.manager.security` | Document Server JWT sign/verify |
| `sdk.manager.settings` | Read/write plugin configuration |
| `sdk.manager.url` | Generate download, callback and editor URLs |
| `sdk.client` | HTTP calls to ONLYOFFICE Docs server (Apache httpclient5) |
| `sdk.service.callback` | Process callbacks (lock/unlock, save the new blob) |
| `sdk.service.config` | Build editor config JSON |
| `sdk.service.convert` | Document conversion requests |
| `sdk.service.settings` | Connection checks of the settings page (`SettingsValidationService`) |
| `service` | `PermissionService` (Nuxeo permission checks), `LinkTokenService` (tokens of Document Server links), `LockService` (locks of editing sessions) |
| `utils` | `Utils` — shared helpers |
| `operation` | `CreateOperation` (`ONLYOFFICE.Create`), `ConvertOperation` (`ONLYOFFICE.Convert`) — Nuxeo automation |
| `web` | `Editor` — WebEngine editor page |

REST endpoints live in `org.nuxeo.ecm.restapi.server.jaxrs.OnlyofficeObject` (module `onlyoffice-nuxeo-rest`), under `/api/v1/onlyoffice`:
- `GET /settings`, `POST /settings` — admin configuration (administrators only)
- `GET /formats` — supported formats list
- `GET /filter/{id}` — edit/view capability check
- `POST /callback/{id}` — ONLYOFFICE callback (open URL, link token)
- `GET /download/{id}` — file download for Document Server (open URL, link token)
- `GET /test-txt` — test file for the connection check of the settings page (open URL, link token)

### Frontend

Web components are Polymer elements under `onlyoffice-nuxeo-web-ui/src/main/resources/web/nuxeo.war/ui/onlyoffice/`. They are injected into Nuxeo Web UI via the slot system defined in `onlyoffice.html`:

| Slot | Component |
|---|---|
| `DOCUMENT_ACTIONS` | `onlyoffice-editor-button.html` — open editor |
| `CREATE_POPUP_ITEMS/PAGES` | `onlyoffice-create.html` — new document dialog |
| `BLOB_ACTIONS` | `onlyoffice-convert-button.html` — format conversion |
| `ADMINISTRATION_MENU/PAGES` | `onlyoffice-settings.html` — admin settings |

Translations are in `ui/i18n/messages.json` (English base) and `ui/i18n/messages-{locale}.json`. Each locale file must also be added as an `<append>` in `onlyoffice-nuxeo-web-ui/src/main/resources/OSGI-INF/deployment-fragment.xml`, otherwise it is not installed.

### OSGI / Nuxeo Component Registration

Each backend service is registered as a Nuxeo component via an XML descriptor in `onlyoffice-nuxeo-core/src/main/resources/OSGI-INF/`. When adding a new service, create a corresponding `*.xml` descriptor and register it in the `Nuxeo-Component` header of `META-INF/MANIFEST.MF`.

### Configuration

Plugin settings are Nuxeo framework properties with the `onlyoffice.` prefix, set in `nuxeo.conf` or from the admin settings page. Saving the settings page writes them to `nxserver/config/onlyoffice.properties`.

```properties
onlyoffice.url=http://documentserver/          # Document Server address, reachable from browsers and Nuxeo
onlyoffice.innerUrl=http://documentserver/     # optional: Document Server address used by Nuxeo
onlyoffice.productInnerUrl=http://nuxeo:8080/  # optional: Nuxeo address used by Document Server
onlyoffice.security.key=yoursecret             # JWT secret; empty = JWT disabled (no auto-generation)
onlyoffice.security.header=Authorization       # optional
onlyoffice.security.prefix=Bearer              # optional
```

Other settings (`demo`, `ignoreSSLCertificate`, `customization.*`, `lossyEdit`) come from the SDK `Settings` model. `onlyoffice-nuxeo-package/.../nuxeo.defaults` holds no plugin defaults (only the template include); defaults come from the SDK.

### Key External Dependencies

- `com.onlyoffice:docs-integration-sdk` — ONLYOFFICE SDK (managers/services base classes), shaded into the core jar
- `com.auth0:java-jwt` — JWT signing (provided by Nuxeo)
- Nuxeo Platform (core, automation, web engine, REST API, KeyValue store)
