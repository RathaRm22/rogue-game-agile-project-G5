package com.roguelike.dungeon;

import java.util.*;

/**
 * Procedural dungeon generator that creates unique layouts for each game.
 * Implements a BSP (Binary Space Partitioning) algorithm to create rooms,
 * and then connects them with corridors.
 */
public class DungeonGenerator {
    
    private static final int MIN_ROOM_SIZE = 4;
    private static final int MIN_LEAF_SIZE = 10;
    private static final double ROOM_MAX_SIZE_RATIO = 0.8;
    
    private int width;
    private int height;
    private Tile[][] tiles;
    private Room[] rooms;
    private Random random;
    
    /**
     * Creates a new dungeon generator with the specified dimensions.
     * 
     * @param width The width of the dungeon
     * @param height The height of the dungeon
     * @param seed Random seed for reproducible generation
     */
    public DungeonGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        this.random = new Random(seed);
        
        // Initialize all tiles as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tile.WALL;
            }
        }
    }
    
    /**
     * Generates a new dungeon layout.
     * 
     * @return The generated dungeon as a 2D array of tiles
     */
    public Tile[][] generate() {
        // Create a root leaf that encompasses the entire dungeon
        Leaf rootLeaf = new Leaf(1, 1, width - 2, height - 2);
        List<Leaf> leafs = new ArrayList<>();
        leafs.add(rootLeaf);
        
        // Split leaves recursively
        boolean didSplit = true;
        while (didSplit) {
            didSplit = false;
            List<Leaf> newLeafs = new ArrayList<>();
            
            for (Leaf leaf : leafs) {
                if (leaf.leftChild == null && leaf.rightChild == null) {
                    // If this leaf is too big, split it
                    if (leaf.width > MIN_LEAF_SIZE || leaf.height > MIN_LEAF_SIZE) {
                        if (leaf.split(random)) {
                            newLeafs.add(leaf.leftChild);
                            newLeafs.add(leaf.rightChild);
                            didSplit = true;
                        }
                    }
                } else {
                    newLeafs.add(leaf);
                }
            }
            
            if (didSplit) {
                leafs = newLeafs;
            }
        }
        
        // Create rooms in the leaves
        rootLeaf.createRooms(random);
        
        // Get all rooms and store them
        List<Room> roomList = new ArrayList<>();
        rootLeaf.getRooms(roomList);
        rooms = roomList.toArray(new Room[0]);
        
        // Create rooms in the dungeon
        for (Room room : rooms) {
            createRoom(room);
        }
        
        // Connect rooms with corridors
        for (int i = 0; i < rooms.length - 1; i++) {
            connectRooms(rooms[i].center(), rooms[i + 1].center());
        }
        
        return tiles;
    }
    
    /**
     * Creates a room by setting all tiles within the room bounds to floor tiles.
     * 
     * @param room The room to create
     */
    private void createRoom(Room room) {
        for (int x = room.x1; x <= room.x2; x++) {
            for (int y = room.y1; y <= room.y2; y++) {
                tiles[x][y] = Tile.FLOOR;
            }
        }
    }
    
    /**
     * Connects two points in the dungeon with corridors.
     * 
     * @param start Starting point
     * @param end Ending point
     */
    private void connectRooms(Point start, Point end) {
        // Choose randomly between horizontal-first or vertical-first
        if (random.nextBoolean()) {
            // Horizontal first, vertical second
            createHorizontalCorridor(start.x, end.x, start.y);
            createVerticalCorridor(start.y, end.y, end.x);
        } else {
            // Vertical first, horizontal second
            createVerticalCorridor(start.y, end.y, start.x);
            createHorizontalCorridor(start.x, end.x, end.y);
        }
    }
    
    /**
     * Creates a horizontal corridor from x1 to x2 at the specified y-coordinate.
     */
    private void createHorizontalCorridor(int x1, int x2, int y) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                tiles[x][y] = Tile.FLOOR;
            }
        }
    }
    
    /**
     * Creates a vertical corridor from y1 to y2 at the specified x-coordinate.
     */
    private void createVerticalCorridor(int y1, int y2, int x) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                tiles[x][y] = Tile.FLOOR;
            }
        }
    }
    
    /**
     * Gets an array of all rooms generated in the dungeon.
     * 
     * @return Array of rooms
     */
    public Room[] getRooms() {
        return rooms;
    }
    
    /**
     * Gets the tile at the specified coordinates.
     * 
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return The tile at the given position
     */
    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return Tile.WALL;
        }
        return tiles[x][y];
    }
    
    /**
     * Represents a leaf in the Binary Space Partitioning tree.
     */
    private class Leaf {
        private int x, y;
        private int width, height;
        private Leaf leftChild;
        private Leaf rightChild;
        private Room room;
        
        public Leaf(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        /**
         * Splits this leaf into two child leaves.
         * 
         * @param random Random generator for split decisions
         * @return True if the split was successful
         */
        public boolean split(Random random) {
            if (leftChild != null || rightChild != null) {
                return false; // Already split
            }
            
            // Choose a direction - horizontal or vertical
            boolean splitHorizontally = random.nextBoolean();
            if (width > height && width / height >= 1.5) {
                splitHorizontally = false;
            } else if (height > width && height / width >= 1.5) {
                splitHorizontally = true;
            }
            
            int max = (splitHorizontally ? height : width) - MIN_LEAF_SIZE;
            if (max <= MIN_LEAF_SIZE) {
                return false; // Too small to split
            }
            
            int split = random.nextInt(max - MIN_LEAF_SIZE) + MIN_LEAF_SIZE;
            
            if (splitHorizontally) {
                leftChild = new Leaf(x, y, width, split);
                rightChild = new Leaf(x, y + split, width, height - split);
            } else {
                leftChild = new Leaf(x, y, split, height);
                rightChild = new Leaf(x + split, y, width - split, height);
            }
            
            return true;
        }
        
        /**
         * Creates a room within this leaf.
         * 
         * @param random Random generator for room dimensions
         */
        public void createRooms(Random random) {
            if (leftChild != null || rightChild != null) {
                // This leaf has been split, so create rooms in children
                if (leftChild != null) {
                    leftChild.createRooms(random);
                }
                if (rightChild != null) {
                    rightChild.createRooms(random);
                }
                
                // Connect the rooms of the children
                if (leftChild != null && rightChild != null) {
                    // If both children have rooms, connect them
                    Room leftRoom = leftChild.getRoom();
                    Room rightRoom = rightChild.getRoom();
                    
                    if (leftRoom != null && rightRoom != null) {
                        // Store the connecting points for corridor creation
                        leftRoom.setConnected(true);
                        rightRoom.setConnected(true);
                    }
                }
            } else {
                // This leaf is ready for a room
                int roomWidth = Math.max(MIN_ROOM_SIZE, (int)(width * ROOM_MAX_SIZE_RATIO));
                int roomHeight = Math.max(MIN_ROOM_SIZE, (int)(height * ROOM_MAX_SIZE_RATIO));
                
                // Random room size
                roomWidth = random.nextInt(width - roomWidth) + roomWidth;
                roomHeight = random.nextInt(height - roomHeight) + roomHeight;
                
                // Random room position
                int roomX = x + random.nextInt(width - roomWidth);
                int roomY = y + random.nextInt(height - roomHeight);
                
                // Create the room
                room = new Room(roomX, roomY, roomWidth, roomHeight);
            }
        }
        
        /**
         * Gets the room in this leaf.
         * 
         * @return The room, or null if no room exists
         */
        public Room getRoom() {
            if (room != null) {
                return room;
            } else {
                Room leftRoom = null;
                Room rightRoom = null;
                
                if (leftChild != null) {
                    leftRoom = leftChild.getRoom();
                }
                if (rightChild != null) {
                    rightRoom = rightChild.getRoom();
                }
                
                if (leftRoom == null && rightRoom == null) {
                    return null;
                } else if (rightRoom == null) {
                    return leftRoom;
                } else if (leftRoom == null) {
                    return rightRoom;
                } else if (random.nextBoolean()) {
                    return leftRoom;
                } else {
                    return rightRoom;
                }
            }
        }
        
        /**
         * Gets all rooms in this leaf and its children.
         * 
         * @param roomList List to add rooms to
         */
        public void getRooms(List<Room> roomList) {
            if (room != null) {
                roomList.add(room);
            }
            if (leftChild != null) {
                leftChild.getRooms(roomList);
            }
            if (rightChild != null) {
                rightChild.getRooms(roomList);
            }
        }
    }
    
    /**
     * Represents a room in the dungeon.
     */
    public static class Room {
        public int x1, y1, x2, y2;
        private boolean connected;
        
        public Room(int x, int y, int width, int height) {
            this.x1 = x;
            this.y1 = y;
            this.x2 = x + width - 1;
            this.y2 = y + height - 1;
            this.connected = false;
        }
        
        /**
         * Gets the center point of the room.
         * 
         * @return Center point
         */
        public Point center() {
            int centerX = (x1 + x2) / 2;
            int centerY = (y1 + y2) / 2;
            return new Point(centerX, centerY);
        }
        
        /**
         * Sets whether this room is connected to another room.
         * 
         * @param connected True if connected
         */
        public void setConnected(boolean connected) {
            this.connected = connected;
        }
        
        /**
         * Checks if this room is connected to another room.
         * 
         * @return True if connected
         */
        public boolean isConnected() {
            return connected;
        }
    }
    
    /**
     * Represents a point in the dungeon.
     */
    public static class Point {
        public int x, y;
        
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * Represents a tile type in the dungeon.
     */
    public enum Tile {
        FLOOR, WALL
    }
}