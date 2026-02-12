# ONLYOFFICE addon package for Nuxeo

Bring full office editing to your [Nuxeo](https://www.nuxeo.com/) content platform — edit, co-author, and manage office files without leaving Nuxeo. Powered by [ONLYOFFICE Docs](https://www.onlyoffice.com/docs). 

## Features 🌟

The package allows to:

* Create and edit text documents, spreadsheets, presentations, and PDFs.
* Share documents with other users.
* Collaborate on documents in real time using two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

<p align="center">
  <a href="https://www.onlyoffice.com/office-for-nuxeo">
    <img width="840" src="https://static-site.onlyoffice.com/public/images/templates/office-for-nuxeo/hero/screen3@2x.png" alt="ONLYOFFICE for Nuxeo">
  </a>
</p>

### Supported formats

**For viewing:**
* **WORD:** DJVU, DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OTT, OXPS, PDF, RTF, TXT, XML, XPS
* **CELL:** CSV, FODS, ODS, OTS, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX

**For editing:**

* **WORD:** DOCM, DOCX, DOTM, DOTX, HTM, XML
* **CELL:** XLSM, XLSX, XLTM, XLTX
* **SLIDE:** POTM, POTX, PPSM, PPSX, PPTM, PPTX
* **PDF:** PDF

**For filling:**

* **PDF:** PDF

**For converting to Office Open XML formats:**

* **WORD:** DOC, DOCM, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OTT, OXPS, PDF, RTF, XML, XPS
* **CELL:** FODS, ODS, OTS, XLS, XLSM, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM

## Installing ONLYOFFICE Docs

You’ll need a running instance of ONLYOFFICE Docs (Document Server) that can communicate both ways with your Nuxeo server and is reachable by end-user browsers.

#### ☁️ Option 1: ONLYOFFICE Docs Cloud  
No installation needed — just [register here](https://www.onlyoffice.com/docs-registration) and get instant access.  
Your registration email includes all required connection details, including the **Document Server address** and **JWT credentials**.

#### 🏠 Option 2: Self-hosted ONLYOFFICE Docs  
Install ONLYOFFICE Docs on your own infrastructure for full control.    
You have two main choices for the ONLYOFFICE Document Server:

* **Community Edition (Free)**: Ideal for small teams and personal use.  
  * The **recommended** installation method is [Docker](https://github.com/onlyoffice/Docker-DocumentServer).  
  * To install it on Debian, Ubuntu, or other derivatives, click [here](https://helpcenter.onlyoffice.com/docs/installation/docs-community-install-ubuntu.aspx).   
* **Enterprise Edition**: Packed with professional features and scalability for larger organizations. To install, click [here](https://helpcenter.onlyoffice.com/docs/installation/enterprise).

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

> **Note:**  Document Server ↔ Nuxeo must be mutually reachable. The Document Server must be able to **POST** to Nuxeo (callback flow).

## Installing the addon package 📥

You have two options:

### Option A: Nuxeo Marketplace (recommended)
Install directly from [Nuxeo Marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace)  

### Option B: [nuxeoctl](https://doc.nuxeo.com/nxdoc/installing-a-new-package-on-your-instance/)

```
nuxeoctl mp-install /path/to/onlyoffice-nuxeo-package-x.x.zip
```

## Configuring the addon package ⚙️

Open the [nuxeo.conf](https://doc.nuxeo.com/nxdoc/configuration-parameters-index-nuxeoconf/) file and enter the name of the server with ONLYOFFICE Docs installed:

```
onlyoffice.docserv.url=http://documentserver/
```
where the **documentserver** is the name of the server with **ONLYOFFICE Docs** installed. 
The address must be accessible from the user browser and from the Nuxeo server. 
The Nuxeo server address must also be accessible from **ONLYOFFICE Docs** for correct work.

Starting from version 7.2, JWT is enabled by default and the secret key is generated automatically to restrict the access to ONLYOFFICE Docs and for security reasons and data integrity. 
Specify your own secret key by adding the `onlyoffice.jwt.secret=yoursecret` line to the **nuxeo.conf** file. 
In the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/), specify the same secret key and enable the validation.

## Compiling the ONLYOFFICE addon package for Nuxeo

To build Nuxeo package, the following steps must be performed for Ubuntu:

1. The stable Java version is necessary for the successful build. If you do not have it installed, use the following commands to install Open JDK 8:
    ```bash
    sudo apt-get update
    sudo apt-get install openjdk-8-jdk
    ```

2. Install latest Maven:
Installation process is described [here](https://maven.apache.org/install.html)

3. Download the ONLYOFFICE addon package for Nuxeo source code:
    ```bash
    git clone https://github.com/onlyoffice/onlyoffice-nuxeo.git
    ```

4. Compile ONLYOFFICE addon package for Nuxeo:
    ```bash
    cd onlyoffice-nuxeo/
    mvn clean install
    ```

5. Built package is located here `./onlyoffice-nuxeo-package/target/onlyoffice-nuxeo-package-x.x.zip`

## How it works ✍️ 

**Create a new file**

1. Open the target workspace/folder.
2. Click Create new (ONLYOFFICE icon).
3. Choose Document / Spreadsheet / Presentation, name it, and confirm.
4. The editor opens — start working immediately.

**Open an existing file**

1. Locate your file in *Nuxeo*.
2. Click **Open with ONLYOFFICE** (or the editor icon).
3. The file opens in the embedded editor for viewing or editing (per your permissions).

**Auto-save & versioning**: Changes are sent back to Nuxeo; Nuxeo handles repository updates/versioning.

The ONLYOFFICE integration follows the API documented [here](https://api.onlyoffice.com/docs/docs-api/get-started/basic-concepts/). 

## ONLYOFFICE Docs editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

* Community Edition 🆓 (`onlyoffice-documentserver` package)
* Enterprise Edition 🏢 (`onlyoffice-documentserver-ee` package)

The table below will help you to make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** |
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/community) | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/enterprise) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | 1 or 3 years support included |
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                  | + | + |
| Dark theme                        | + | + |
| 125%, 150%, 175%, 200% scaling    | + | + |
| White Label                       | - | - |
| Integrated test example (node.js) | + | + |
| Mobile web editors                | - | +* |
| **Plugins & Macros** | **Community Edition** | **Enterprise Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Enterprise Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | + | + |
| Editing Content control         | + | + |
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | + | + |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation                 | + | + |
| Conditional formatting          | + | + |
| Sparklines                      | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields              | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| **PDF Editor features**      | **Community Edition** | **Enterprise Edition** |
| Text editing and co-editing                                | + | + |
| Work with pages (adding, deleting, rotating)               | + | + |
| Inserting objects (shapes, images, hyperlinks, etc.)       | + | + |
| Text annotations (highlight, underline, cross out, stamps) | + | + |
| Comments                        | + | + |
| Freehand drawings               | + | + |
| Form filling                    | + | + |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubNuxeo#docs-enterprise)  |

\* If supported by DMS.

## Need help? User Feedback and Support 💡

* **🐞 Found a bug?** Please report it by creating an [issue](https://github.com/ONLYOFFICE/onlyoffice-nuxeo/issues).
* **❓ Have a question?** Ask our community and developers on the [ONLYOFFICE Forum](https://community.onlyoffice.com).
* **👨‍💻 Need help for developers?** Check our [API documentation](https://api.onlyoffice.com).
* **💡 Want to suggest a feature?** Share your ideas on our [feedback platform](https://feedback.onlyoffice.com/forums/966080-your-voice-matters).