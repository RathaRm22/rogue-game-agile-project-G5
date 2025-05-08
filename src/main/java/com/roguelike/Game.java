package com.roguelike;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;

import com.roguelike.dungeon.DungeonGenerator;
import com.roguelike.visibility.FogOfWar;
import com.roguelike.renderer.DungeonRenderer;

/**
 * Main game class that demonstrates the procedural dungeon generation
 * and fog of war features.
 */
public class Game extends Application {

    // Game configuration
    private static final int DUNGEON_WIDTH = 80;
    private static final int DUNGEON_HEIGHT = 50;
    private static final int TILE_SIZE = 16;
    private static final int PLAYER_VISION_RADIUS = 10;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    
    // Game components
    private DungeonGenerator dungeonMap;
    private FogOfWar fogOfWar;
    private DungeonRenderer renderer;
    private int playerX, playerY;
    
    @Override
    public void start(Stage primaryStage) {
        // Create the game canvas
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Create a StackPane as the root and add the canvas
        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        
        // Create the scene
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Generate a new dungeon with a random seed
        long seed = System.currentTimeMillis();
        generateNewDungeon(seed);
        
        // Set up key handling
        setupKeyHandling(scene);
        
        // Set up the game loop
        setupGameLoop(gc);
        
        // Set up the stage
        primaryStage.setTitle("Roguelike Dungeon Demo");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    /**
     * Generates a new dungeon and places the player in the first room.
     * 
     * @param seed Random seed for dungeon generation
     */
    private void generateNewDungeon(long seed) {
        // Create the dungeon
        dungeonMap = new DungeonGenerator(DUNGEON_WIDTH, DUNGEON_HEIGHT, seed);
        dungeonMap.generate();
        
        // Create the fog of war system
        fogOfWar = new FogOfWar(dungeonMap, DUNGEON_WIDTH, DUNGEON_HEIGHT, PLAYER_VISION_RADIUS);
        
        // Create the renderer
        renderer = new DungeonRenderer(dungeonMap, fogOfWar, TILE_SIZE);
        
        // Place the player in the first room
        DungeonGenerator.Room startRoom = dungeonMap.getRooms()[0];
        playerX = startRoom.center().x;
        playerY = startRoom.center().y;
        
        // Update initial visibility
        fogOfWar.updateVisibility(playerX, playerY);
    }
    
    /**
     * Sets up keyboard input handling.
     * 
     * @param scene The JavaFX scene to add key handlers to
     */
    private void setupKeyHandling(Scene scene) {
        scene.setOnKeyPressed(e -> {
            KeyCode key = e.getCode();
            
            // Handle movement
            int newX = playerX;
            int newY = playerY;
            
            switch (key) {
                case UP:
                case W:
                    newY--;
                    break;
                case DOWN:
                case S:
                    newY++;
                    break;
                case LEFT:
                case A:
                    newX--;
                    break;
                case RIGHT:
                case D:
                    newX++;
                    break;
                case R:
                    // Generate a new dungeon with a different seed
                    generateNewDungeon(System.currentTimeMillis());
                    break;
                default:
                    break;
            }
            
            // Check if the new position is valid (not a wall)
            if (dungeonMap.getTile(newX, newY) != DungeonGenerator.Tile.WALL) {
                playerX = newX;
                playerY = newY;
            }
        });
    }
    
    /**
     * Sets up the game loop using JavaFX's AnimationTimer.
     * 
     * @param gc The graphics context to render on
     */
    private void setupGameLoop(GraphicsContext gc) {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Clear the canvas
                gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                
                // Render the dungeon
                renderer.render(gc, playerX, playerY);
            }
        }.start();
    }
    
    /**
     * Main method to launch the application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}