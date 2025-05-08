module com.roguelike {
    // Export your packages to make them accessible to JavaFX
    exports com.roguelike;
    exports com.roguelike.dungeon;
    exports com.roguelike.renderer;
    exports com.roguelike.visibility;
    
    // Require the JavaFX modules you need with 'requires transitive' for modules that need to be exported
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    
    // For testing with JUnit
    opens com.roguelike to org.junit.platform.commons;
}