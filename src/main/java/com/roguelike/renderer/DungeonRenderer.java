package com.roguelike.renderer;


import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.roguelike.dungeon.DungeonGenerator;
import com.roguelike.dungeon.DungeonGenerator.Tile;
import com.roguelike.visibility.FogOfWar;
import com.roguelike.visibility.FogOfWar.VisibilityState;

/**
 * Handles rendering the dungeon with fog of war effects.
 */
public class DungeonRenderer {

    private int tileSize;
    private DungeonGenerator dungeonMap;
    private FogOfWar fogOfWar;
    
    // Colors for different tile types and visibility states
    private static final Color FLOOR_COLOR = Color.LIGHTGRAY;
    private static final Color WALL_COLOR = Color.DARKGRAY;
    private static final Color PLAYER_COLOR = Color.BLUE;
    private static final Color EXPLORED_COLOR_MODIFIER = Color.rgb(0, 0, 0, 0.5); // Semi-transparent black
    private static final Color UNEXPLORED_COLOR = Color.BLACK;
    
    /**
     * Creates a new dungeon renderer.
     * 
     * @param dungeonMap The dungeon to render
     * @param fogOfWar The fog of war system
     * @param tileSize The size of each tile in pixels
     */
    public DungeonRenderer(DungeonGenerator dungeonMap, FogOfWar fogOfWar, int tileSize) {
        this.dungeonMap = dungeonMap;
        this.fogOfWar = fogOfWar;
        this.tileSize = tileSize;
    }
    
    /**
     * Renders the dungeon on the specified graphics context.
     * 
     * @param gc The graphics context to render on
     * @param playerX The player's X coordinate
     * @param playerY The player's Y coordinate
     */
    public void render(GraphicsContext gc, int playerX, int playerY) {
        int width = (int) (gc.getCanvas().getWidth() / tileSize);
        int height = (int) (gc.getCanvas().getHeight() / tileSize);
        
        // Center the view on the player
        int startX = Math.max(0, playerX - width / 2);
        int startY = Math.max(0, playerY - height / 2);
        
        // Update the fog of war based on player position
        fogOfWar.updateVisibility(playerX, playerY);
        
        // Render each tile
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int mapX = startX + x;
                int mapY = startY + y;
                
                renderTile(gc, mapX, mapY, x * tileSize, y * tileSize);
            }
        }
        
        // Render player
        int playerScreenX = (playerX - startX) * tileSize;
        int playerScreenY = (playerY - startY) * tileSize;
        gc.setFill(PLAYER_COLOR);
        gc.fillOval(playerScreenX, playerScreenY, tileSize, tileSize);
    }
    
    /**
     * Renders a single tile with appropriate visibility.
     * 
     * @param gc Graphics context
     * @param mapX Map X coordinate
     * @param mapY Map Y coordinate
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     */
    private void renderTile(GraphicsContext gc, int mapX, int mapY, int screenX, int screenY) {
        VisibilityState visibility = fogOfWar.getVisibility(mapX, mapY);
        
        if (visibility == VisibilityState.UNEXPLORED) {
            // Unexplored tiles are black
            gc.setFill(UNEXPLORED_COLOR);
            gc.fillRect(screenX, screenY, tileSize, tileSize);
            return;
        }
        
        // Get the base color for the tile
        Tile tile = dungeonMap.getTile(mapX, mapY);
        Color baseColor = (tile == Tile.FLOOR) ? FLOOR_COLOR : WALL_COLOR;
        
        if (visibility == VisibilityState.EXPLORED) {
            // Darken explored but not visible tiles
            gc.setFill(baseColor);
            gc.fillRect(screenX, screenY, tileSize, tileSize);
            gc.setFill(EXPLORED_COLOR_MODIFIER);
            gc.fillRect(screenX, screenY, tileSize, tileSize);
        } else {
            // Visible tiles use their normal color
            gc.setFill(baseColor);
            gc.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }
    
    /**
     * Sets the size of each tile in pixels.
     * 
     * @param tileSize The new tile size
     */
    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }
}