package com.roguelike;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import com.roguelike.dungeon.DungeonGenerator;
import com.roguelike.dungeon.DungeonGenerator.Tile;
import com.roguelike.dungeon.DungeonGenerator.Room;
import com.roguelike.dungeon.DungeonGenerator.Point;
import com.roguelike.visibility.FogOfWar;
import com.roguelike.visibility.FogOfWar.VisibilityState;
import com.roguelike.renderer.DungeonRenderer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import java.util.Set;

/**
 * Test class for the roguelike dungeon generator project.
 * Tests the core functionality of the dungeon generator, fog of war,
 * and related components.
 */
public class DungeonGeneratorTest {
    
    private static final int TEST_WIDTH = 50;
    private static final int TEST_HEIGHT = 30;
    private static final int VISION_RADIUS = 8;
    private static final long TEST_SEED = 12345L;
    
    private DungeonGenerator dungeonMap;
    private FogOfWar fogOfWar;
    
    @BeforeEach
    public void setUp() {
        // Create a new dungeon with a fixed seed for reproducible tests
        dungeonMap = new DungeonGenerator(TEST_WIDTH, TEST_HEIGHT, TEST_SEED);
        dungeonMap.generate();
        
        // Create fog of war instance
        fogOfWar = new FogOfWar(dungeonMap, TEST_WIDTH, TEST_HEIGHT, VISION_RADIUS);
    }
    
    @Test
    public void testDungeonGeneration() {
        // Verify that the dungeon was generated
        assertNotNull(dungeonMap, "Dungeon map should not be null");
        
        // Check that rooms were created
        Room[] rooms = dungeonMap.getRooms();
        assertNotNull(rooms, "Rooms array should not be null");
        assertTrue(rooms.length > 0, "At least one room should be generated");
        
        // Verify that rooms have valid dimensions
        for (Room room : rooms) {
            assertTrue(room.x1 < room.x2, "Room x1 should be less than x2");
            assertTrue(room.y1 < room.y2, "Room y1 should be less than y2");
            assertTrue(room.x1 >= 0, "Room x1 should be non-negative");
            assertTrue(room.y1 >= 0, "Room y1 should be non-negative");
            assertTrue(room.x2 < TEST_WIDTH, "Room x2 should be within map bounds");
            assertTrue(room.y2 < TEST_HEIGHT, "Room y2 should be within map bounds");
        }
        
        // Check that at least some floor tiles were created
        int floorCount = 0;
        for (int x = 0; x < TEST_WIDTH; x++) {
            for (int y = 0; y < TEST_HEIGHT; y++) {
                if (dungeonMap.getTile(x, y) == Tile.FLOOR) {
                    floorCount++;
                }
            }
        }
        assertTrue(floorCount > 0, "Dungeon should have floor tiles");
        
        // Verify that the rooms are connected (by checking floor tiles between rooms)
        // This is a simple check that assumes corridors exist between consecutive rooms
        for (int i = 0; i < rooms.length - 1; i++) {
            Point start = rooms[i].center();
            Point end = rooms[i + 1].center();
            
            // Check for a path of floor tiles between the centers
            boolean pathExists = pathExists(start.x, start.y, end.x, end.y);
            assertTrue(pathExists, "There should be a path between room " + i + " and room " + (i + 1));
        }
    }
    
    /**
     * Helper method to check if a path of floor tiles exists between two points.
     * Uses a breadth-first search to find any valid path between the points.
     */
    private boolean pathExists(int startX, int startY, int endX, int endY) {
        // Ensure start and end points are within bounds
        if (startX < 0 || startY < 0 || startX >= TEST_WIDTH || startY >= TEST_HEIGHT ||
            endX < 0 || endY < 0 || endX >= TEST_WIDTH || endY >= TEST_HEIGHT) {
            return false;
        }
        
        // Check if start or end points are walls
        if (dungeonMap.getTile(startX, startY) == Tile.WALL || 
            dungeonMap.getTile(endX, endY) == Tile.WALL) {
            return false;
        }
        
        // Direction vectors for moving in 4 directions (up, right, down, left)
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        
        // Queue for BFS
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        
        // Set to keep track of visited tiles
        Set<String> visited = new HashSet<>();
        visited.add(startX + "," + startY);
        
        // BFS to find path
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];
            
            // Check if we've reached the destination
            if (x == endX && y == endY) {
                return true;
            }
            
            // Try all four directions
            for (int i = 0; i < 4; i++) {
                int newX = x + dx[i];
                int newY = y + dy[i];
                String key = newX + "," + newY;
                
                // Check if the new position is valid, is a floor, and hasn't been visited
                if (newX >= 0 && newY >= 0 && newX < TEST_WIDTH && newY < TEST_HEIGHT &&
                    dungeonMap.getTile(newX, newY) == Tile.FLOOR && 
                    !visited.contains(key)) {
                    queue.add(new int[]{newX, newY});
                    visited.add(key);
                }
            }
        }
        
        // No path found
        return false;
    }
    
    @Test
    public void testFogOfWar() {
        // Find a floor tile to use as the player position
        int playerX = -1, playerY = -1;
        for (int x = 0; x < TEST_WIDTH && playerX == -1; x++) {
            for (int y = 0; y < TEST_HEIGHT && playerY == -1; y++) {
                if (dungeonMap.getTile(x, y) == Tile.FLOOR) {
                    playerX = x;
                    playerY = y;
                }
            }
        }
        
        assertTrue(playerX >= 0 && playerY >= 0, "Should find a floor tile for player position");
        
        // Initially all tiles should be unexplored
        for (int x = 0; x < TEST_WIDTH; x++) {
            for (int y = 0; y < TEST_HEIGHT; y++) {
                assertEquals(VisibilityState.UNEXPLORED, fogOfWar.getVisibility(x, y),
                           "All tiles should start as unexplored");
            }
        }
        
        // Update visibility from player position
        fogOfWar.updateVisibility(playerX, playerY);
        
        // Check that player position is visible
        assertEquals(VisibilityState.VISIBLE, fogOfWar.getVisibility(playerX, playerY),
                   "Player position should be visible");
        assertTrue(fogOfWar.isVisible(playerX, playerY), "Player position should be visible");
        assertTrue(fogOfWar.isExplored(playerX, playerY), "Player position should be explored");
        
        // Check that some tiles are now visible
        int visibleCount = 0;
        for (int x = 0; x < TEST_WIDTH; x++) {
            for (int y = 0; y < TEST_HEIGHT; y++) {
                if (fogOfWar.getVisibility(x, y) == VisibilityState.VISIBLE) {
                    visibleCount++;
                }
            }
        }
        assertTrue(visibleCount > 1, "More than just the player position should be visible");
        
        // Move player to a new position and check that previously visible tiles become explored
        int newPlayerX = playerX + 5;
        int newPlayerY = playerY + 5;
        
        // Make sure the new position is within bounds
        newPlayerX = Math.min(newPlayerX, TEST_WIDTH - 1);
        newPlayerY = Math.min(newPlayerY, TEST_HEIGHT - 1);
        
        // If new position is a wall, find a floor tile nearby
        if (dungeonMap.getTile(newPlayerX, newPlayerY) == Tile.WALL) {
            for (int x = Math.max(0, newPlayerX - 3); x < Math.min(TEST_WIDTH, newPlayerX + 3); x++) {
                for (int y = Math.max(0, newPlayerY - 3); y < Math.min(TEST_HEIGHT, newPlayerY + 3); y++) {
                    if (dungeonMap.getTile(x, y) == Tile.FLOOR) {
                        newPlayerX = x;
                        newPlayerY = y;
                        break;
                    }
                }
            }
        }
        
        // Update visibility from new player position
        fogOfWar.updateVisibility(newPlayerX, newPlayerY);
        
        // Check that old position is now explored but not visible
        if (calculateDistance(playerX, playerY, newPlayerX, newPlayerY) > VISION_RADIUS) {
            assertNotEquals(VisibilityState.VISIBLE, fogOfWar.getVisibility(playerX, playerY),
                         "Old player position should not be visible");
            assertTrue(fogOfWar.isExplored(playerX, playerY), "Old player position should be explored");
        }
        
        // Check that new position is visible
        assertEquals(VisibilityState.VISIBLE, fogOfWar.getVisibility(newPlayerX, newPlayerY),
                   "New player position should be visible");
    }
    
    @Test
    public void testOutOfBoundsTiles() {
        // Test that out-of-bounds tiles return WALL
        assertEquals(Tile.WALL, dungeonMap.getTile(-1, -1), "Out of bounds should return WALL");
        assertEquals(Tile.WALL, dungeonMap.getTile(TEST_WIDTH, TEST_HEIGHT), "Out of bounds should return WALL");
        
        // Test that out-of-bounds visibility returns UNEXPLORED
        assertEquals(VisibilityState.UNEXPLORED, fogOfWar.getVisibility(-1, -1),
                   "Out of bounds visibility should be UNEXPLORED");
        assertEquals(VisibilityState.UNEXPLORED, fogOfWar.getVisibility(TEST_WIDTH, TEST_HEIGHT),
                   "Out of bounds visibility should be UNEXPLORED");
        
        // Test that out-of-bounds visibility checks return false
        assertFalse(fogOfWar.isVisible(-1, -1), "Out of bounds should not be visible");
        assertFalse(fogOfWar.isExplored(-1, -1), "Out of bounds should not be explored");
    }
    
    @Test
    public void testDungeonRendererInitialization() {
        // Test that we can create a DungeonRenderer instance
        // We don't test the actual rendering which requires JavaFX
        int tileSize = 16;
        DungeonRenderer renderer = new DungeonRenderer(dungeonMap, fogOfWar, tileSize);
        assertNotNull(renderer, "Should be able to create a renderer");
        
        // We can still test that the tile size can be set
        renderer.setTileSize(tileSize * 2);
        // No assertion needed, just testing that it doesn't throw an exception
    }
    
    /**
     * Calculates the distance between two points.
     */
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}