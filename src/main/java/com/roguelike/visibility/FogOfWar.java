package com.roguelike.visibility;

import java.util.*;
import com.roguelike.dungeon.DungeonGenerator;
import com.roguelike.dungeon.DungeonGenerator.Tile;

/**
 * Implements fog of war mechanics, tracking which tiles the player has explored
 * and which are currently visible.
 */
public class FogOfWar {
    
    // Visibility states for each tile
    public enum VisibilityState {
        UNEXPLORED,    // Never seen by the player
        EXPLORED,      // Seen before but not currently visible
        VISIBLE        // Currently visible to the player
    }
    
    private int width;
    private int height;
    private VisibilityState[][] visibility;
    private DungeonGenerator dungeonMap;
    private int playerVisionRadius;
    
    /**
     * Creates a new fog of war system for the specified dungeon.
     * 
     * @param dungeonMap The dungeon to apply fog of war to
     * @param playerVisionRadius The vision radius of the player in tiles
     */
    public FogOfWar(DungeonGenerator dungeonMap, int width, int height, int playerVisionRadius) {
        this.dungeonMap = dungeonMap;
        this.width = width;
        this.height = height;
        this.playerVisionRadius = playerVisionRadius;
        this.visibility = new VisibilityState[width][height];
        
        // Initialize all tiles as unexplored
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                visibility[x][y] = VisibilityState.UNEXPLORED;
            }
        }
    }
    
    /**
     * Updates the visibility based on the player's position.
     * 
     * @param playerX The player's X coordinate
     * @param playerY The player's Y coordinate
     */
    public void updateVisibility(int playerX, int playerY) {
        // Reset all currently visible tiles to explored
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (visibility[x][y] == VisibilityState.VISIBLE) {
                    visibility[x][y] = VisibilityState.EXPLORED;
                }
            }
        }
        
        // Calculate new visible tiles using shadowcasting
        calculateVisibility(playerX, playerY);
    }
    
    /**
     * Calculates which tiles are visible from the player's position
     * using a shadowcasting algorithm for field of view.
     * 
     * @param playerX Player's X coordinate
     * @param playerY Player's Y coordinate
     */
    private void calculateVisibility(int playerX, int playerY) {
        // Mark the player's position as visible
        if (isInBounds(playerX, playerY)) {
            visibility[playerX][playerY] = VisibilityState.VISIBLE;
        }
        
        // Cast visibility in all directions
        for (int octant = 0; octant < 8; octant++) {
            castLight(playerX, playerY, 1, 1.0, 0.0, playerVisionRadius, 
                      octant, 0, 0);
        }
    }
    
    /**
     * Recursive shadowcasting algorithm to determine visible tiles.
     * This implementation uses the recursive shadowcasting algorithm to handle
     * line of sight calculations.
     */
    private void castLight(int x, int y, int row, double startSlope, double endSlope, 
                          int radius, int octant, int deltaX, int deltaY) {
        if (startSlope < endSlope) {
            return;
        }
        
        double nextStartSlope = startSlope;
        boolean blocked = false;
        
        for (int distance = row; distance <= radius && !blocked; distance++) {
            int deltaY2 = -distance;
            for (int deltaX2 = -distance; deltaX2 <= 0; deltaX2++) {
                // Convert to map coordinates based on octant
                int mapX = x, mapY = y;
                
                switch (octant) {
                    case 0: mapX += deltaX2; mapY -= deltaY2; break;
                    case 1: mapX += -deltaY2; mapY += deltaX2; break;
                    case 2: mapX -= deltaX2; mapY += deltaY2; break;
                    case 3: mapX -= -deltaY2; mapY -= deltaX2; break;
                    case 4: mapX -= deltaX2; mapY -= deltaY2; break;
                    case 5: mapX -= -deltaY2; mapY += deltaX2; break;
                    case 6: mapX += deltaX2; mapY += deltaY2; break;
                    case 7: mapX += -deltaY2; mapY -= deltaX2; break;
                }
                
                // Calculate slopes for this cell
                double leftSlope = (deltaX2 - 0.5) / (deltaY2 + 0.5);
                double rightSlope = (deltaX2 + 0.5) / (deltaY2 - 0.5);
                
                // Skip if outside the view area
                if (rightSlope > startSlope) {
                    continue;
                } else if (leftSlope < endSlope) {
                    break;
                }
                
                // If in bounds and within the vision radius, mark as visible
                if (isInBounds(mapX, mapY) && 
                    calculateDistance(x, y, mapX, mapY) <= radius) {
                    visibility[mapX][mapY] = VisibilityState.VISIBLE;
                }
                
                // Check if this tile blocks sight
                if (isInBounds(mapX, mapY) && dungeonMap.getTile(mapX, mapY) == Tile.WALL) {
                    // If we hit a wall...
                    if (blocked) {
                        // Already in a blocked section, adjust start slope
                        nextStartSlope = rightSlope;
                    } else {
                        // Start a new section and recurse
                        blocked = true;
                        castLight(x, y, distance + 1, nextStartSlope, leftSlope, 
                                 radius, octant, deltaX, deltaY);
                        nextStartSlope = rightSlope;
                    }
                }
            }
            
            if (blocked) {
                break;
            }
        }
    }
    
    /**
     * Calculates the distance between two points.
     */
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Checks if the coordinates are within the map bounds.
     */
    private boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
    
    /**
     * Gets the visibility state of a tile.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return The visibility state
     */
    public VisibilityState getVisibility(int x, int y) {
        if (!isInBounds(x, y)) {
            return VisibilityState.UNEXPLORED;
        }
        return visibility[x][y];
    }
    
    /**
     * Checks if a tile is currently visible to the player.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if the tile is visible
     */
    public boolean isVisible(int x, int y) {
        return isInBounds(x, y) && visibility[x][y] == VisibilityState.VISIBLE;
    }
    
    /**
     * Checks if a tile has been explored by the player.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if the tile has been explored
     */
    public boolean isExplored(int x, int y) {
        return isInBounds(x, y) && 
               (visibility[x][y] == VisibilityState.EXPLORED || 
                visibility[x][y] == VisibilityState.VISIBLE);
    }
}