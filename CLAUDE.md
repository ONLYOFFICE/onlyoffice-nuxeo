# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Nuxeo Marketplace plugin that integrates ONLYOFFICE Docs into the Nuxeo content management platform, enabling document creation, editing, and co-authoring directly in Nuxeo.

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
mvn test -Dtest=TestUtils#testService

# Build a specific module only
mvn clean install -pl onlyoffice-nuxeo-core
```

**Package output:** `onlyoffice-nuxeo-package/target/onlyoffice-nuxeo-package-*.zip`

Build requires Java 8 and Maven 3.8.6+. Tests use the Nuxeo testing framework (`NuxeoRunner` + `@Features(PlatformFeature.class)`).

## Module Structure

```
onlyoffice-nuxeo-core/       # Core Java: services, managers, operations, utils
onlyoffice-nuxeo-rest/       # JAX-RS REST API endpoints
onlyoffice-nuxeo-web-ui/     # Polymer web components and i18n resources
onlyoffice-nuxeo-package/    # Marketplace package assembly
```

`onlyoffice-nuxeo-rest` depends on `onlyoffice-nuxeo-core`. Both are bundled by `onlyoffice-nuxeo-package`.

## Architecture

### Integration Flow

1. User opens/creates a document in Nuxeo Web UI (Polymer components in `onlyoffice-nuxeo-web-ui`).
2. The browser navigates to the WebEngine route `/onlyedit/{id}` — handled by `Editor.java` in the core module.
3. `ConfigServiceImpl` builds the ONLYOFFICE editor configuration JSON (document URL, JWT token, permissions).
4. ONLYOFFICE Docs loads the file from Nuxeo via `GET /api/v1/onlyoffice/download/{id}`.
5. On save, ONLYOFFICE posts a callback to `POST /api/v1/onlyoffice/callback/{id}` — handled by `CallbackServiceImpl`, which saves the new blob back to the Nuxeo repository.

### Backend Key Components (all under `org.onlyoffice`)

| Package | Role |
|---|---|
| `sdk.manager.document` | Document type/format detection |
| `sdk.manager.security` | JWT token sign/verify |
| `sdk.manager.settings` | Read/write plugin configuration |
| `sdk.manager.url` | Generate download and callback URLs |
| `sdk.manager.request` | HTTP calls to ONLYOFFICE Docs server |
| `sdk.service.callback` | Process save callbacks, update Nuxeo blob |
| `sdk.service.config` | Build editor config JSON |
| `sdk.service.convert` | Document conversion requests |
| `service` | `PermissionService` — Nuxeo permission checks |
| `utils` | `Utils` — shared helpers |
| `operation` | `CreateOperation`, `ConvertOperation` — Nuxeo automation |

REST endpoints live in `org.nuxeo.ecm.restapi.server.jaxrs.OnlyofficeObject` (module `onlyoffice-nuxeo-rest`):
- `GET /settings`, `POST /settings` — admin configuration
- `GET /formats` — supported formats list
- `GET /filter/{id}` — edit/view capability check
- `POST /callback/{id}` — ONLYOFFICE save callback
- `GET /download/{id}` — authenticated file download

### Frontend

Web components are Polymer elements under `onlyoffice-nuxeo-web-ui/src/main/resources/web/nuxeo.war/ui/onlyoffice/`. They are injected into Nuxeo Web UI via the slot system defined in `onlyoffice.html`:

| Slot | Component |
|---|---|
| `DOCUMENT_ACTIONS` | `onlyoffice-editor-button.html` — open editor |
| `CREATE_POPUP_ITEMS/PAGES` | `onlyoffice-create.html` — new document dialog |
| `BLOB_ACTIONS` | `onlyoffice-convert-button.html` — format conversion |
| `ADMINISTRATION_MENU/PAGES` | `onlyoffice-settings.html` — admin settings |

Translations are in `i18n/messages.json` (English base) and `i18n/messages-{locale}.json` (20+ languages).

### OSGI / Nuxeo Component Registration

Each backend service is registered as a Nuxeo component via an XML descriptor in `onlyoffice-nuxeo-core/src/main/resources/OSGI-INF/`. When adding a new service, create a corresponding `*.xml` descriptor and register it in `MANIFEST.MF`.

### Configuration

Plugin settings are stored as Nuxeo properties. Required user configuration in `nuxeo.conf`:

```properties
onlyoffice.docserv.url=http://documentserver/
onlyoffice.jwt.secret=yoursecret  # optional, auto-generated if absent
```

Defaults are in `onlyoffice-nuxeo-package/src/main/resources/install/templates/onlyoffice-nuxeo/nuxeo.defaults`.

### Key External Dependencies

- `com.onlyoffice:docs-integration-sdk` — ONLYOFFICE SDK (managers/services base classes)
- `com.auth0:java-jwt` — JWT signing
- Nuxeo Platform (core, automation, web engine, REST API)
