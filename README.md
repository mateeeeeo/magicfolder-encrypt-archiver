# MagicFolder - A JavaFX Encryptor App
A desktop application for **securely encrypting and managing files and folders**, built with JavaFX and AES-GCM encryption. Designed with usability and security in mind, it provides a streamlined UI for organizing and securing sensitive data locally.
 
## Features
- **AES-GCM Encryption** with layered keys
- **Custom archive format** with block-based file encryption
- **Simple Drag & Drop file management** using a JavaFX TreeTableView
- **Editable folders** and inline renaming
- **Encrypts basic file metadata** using JSON (`org.json`)
- **Nested folder support**
- **Easy file importing/exporting**

## Encryption Details
- **Passwords hashed using BCrypt**
- **Two-tiered encryption**:
  -  Hashed user password used to generate `key2`, which encrypts `key1`
  - `key1` then encrypts and decrypts the file contents and dictionary
- **Dictionary based approach**
  - Serialized as minified JSON, which is encrypted for added protection
 
## UI Highlights
- **Archive FileSystem similar to native**:
  - Drag-and-drop files & folders from OS
  - Supports complex nesting
  - Supports easy editing
 
## Libraries & Assets Used
### Dependencies
| Library | Purpose |
|--------|---------|
| [`Bcrypt`](https://github.com/djmdjm/jBCrypt/blob/master/src/org/mindrot/jbcrypt/BCrypt.java) | Password hashing |
| [`Apache Commons IO`](https://commons.apache.org/proper/commons-io/) | File manipulation utilities |
| [`org.json`](https://github.com/stleary/JSON-java) | JSON serialization of metadata |
| [`svgSalamander`](https://github.com/blackears/svgSalamander) | SVG rendering within JavaFX via SwingNode |
| [`JavaFX`](https://openjfx.io/) | Main GUI framework |
 
### Icons
- [Remix Icon](https://remixicon.com/) 
- [Google Material Design Icons](https://fonts.google.com/icons)
 

*All third-party assets are used under their respective licenses.*
