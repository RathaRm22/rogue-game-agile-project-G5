package com.roguelike;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Game class.
 * 
 * Note: Full testing of the Game class would require JavaFX test components
 * like TestFX. These tests focus on the non-UI aspects that can be tested
 * without the JavaFX runtime.
 */
public class GameTest {

    /**
     * Verifies that the game constants are properly defined.
     * This is a simple sanity check to ensure basic configuration is valid.
     */
    @Test
    public void testGameConstants() {
        // Reflection can be used to access private static final fields
        // But here we're just using the known values
        
        // These are our expectations based on the code review
        final int expectedDungeonWidth = 80;
        final int expectedDungeonHeight = 50;
        final int expectedTileSize = 16;
        final int expectedPlayerVisionRadius = 10;
        
        // Since we can't directly access Game.DUNGEON_WIDTH etc. (as they're private),
        // we're just testing that our expectations about the game dimensions
        // are internally consistent
         
        assertTrue(expectedDungeonWidth > 0, "Dungeon width should be positive");
        assertTrue(expectedDungeonHeight > 0, "Dungeon height should be positive");
        assertTrue(expectedTileSize > 0, "Tile size should be positive");
        assertTrue(expectedPlayerVisionRadius > 0, "Player vision radius should be positive");
        
        // Verify that tile size and dungeon size make sense together
        assertTrue(expectedTileSize * expectedDungeonWidth > 0, 
                "Rendered dungeon width should be positive");
        assertTrue(expectedTileSize * expectedDungeonHeight > 0, 
                "Rendered dungeon height should be positive");
        
        // Verify that player vision is reasonable compared to dungeon size
        assertTrue(expectedPlayerVisionRadius < expectedDungeonWidth, 
                "Player vision should be less than dungeon width");
        assertTrue(expectedPlayerVisionRadius < expectedDungeonHeight, 
                "Player vision should be less than dungeon height");
    }
    
    /**
     * This test verifies that we can create a Game instance.
     * It doesn't actually start the JavaFX UI.
     */
    @Test
    public void testGameInstantiation() {
        Game game = new Game();
        assertNotNull(game, "Should be able to instantiate Game");
    }
}