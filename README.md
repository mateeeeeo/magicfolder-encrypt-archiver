# MagicFolder - A JavaFX Encryptor App
 
A desktop application for **securely encrypting and managing files and folders**, built with JavaFX and AES-GCM encryption. Designed with usability and security in mind, it provides a streamlined UI for organizing and securing sensitive data locally.
 
---
 
## Features
 
- ✅ **AES-GCM Encryption** with layered keys
- 📦 **Custom archive format** with block-based file encryption
- 📁 **Drag & drop file management** using a JavaFX TreeTableView
- ✏️ **Editable folders** and inline renaming
- 💾 **Easy drag & drop archive updates**
- 📄 **Encrypts basic file metadata** using JSON (`org.json`)
- 📚 **Nested folder support**
- 📂 **Easy file importing/exporting
---
 
## Encryption Details
 
- **Two-tiered encryption**:
  -  Hashed user password encrypts `key1/iv1`
  -  `key1/iv1` encrypts the file contents and dictionary
- **Files are encrypted in blocks** using `Cipher.update()` and `doFinal()`
- The **dictionary (file/folder structure)** is stored at the top of the archive:
  - Serialized as minified JSON
  - Preceded by a fixed-size `int` indicating its length
 
---
 
## 🖥️ UI Highlights
 
- **FileSystem similar to native**:
  - Drag-and-drop files & folders from OS
  - Supports complex nesting
 
---
 
## 📚 Libraries & Assets Used
 
### 🔧 Dependencies
 
| Library | Purpose |
|--------|---------|
| [`Bcrypt`]( (https://github.com/djmdjm/jBCrypt/blob/master/src/org/mindrot/jbcrypt/BCrypt.java) | Password hashing |
| [`Apache Commons IO`](https://commons.apache.org/proper/commons-io/) | File manipulation utilities |
| [`org.json`](https://github.com/stleary/JSON-java) | JSON serialization of metadata |
| [`svgSalamander`](https://github.com/blackears/svgSalamander) | SVG rendering within JavaFX via SwingNode |
| [`JavaFX`](https://openjfx.io/) | Main GUI framework |
 
### 🎨 Icons
 
- [Remix Icon](https://remixicon.com/) — for file, folder, and action icons  
- [Google Material Design Icons](https://fonts.google.com/icons) — for UI controls and dialogs
 
*All third-party assets are used under their respective licenses.*
