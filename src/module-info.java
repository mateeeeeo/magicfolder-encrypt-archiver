module EncryptArchiver {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.swing;
    requires java.desktop;
    requires org.json;
    requires com.kitfox.svgSalamander;
    requires org.apache.commons.io;
    opens com.magicfolder;
    exports com.magicfolder.components to javafx.fxml;
    opens com.magicfolder.components;
}