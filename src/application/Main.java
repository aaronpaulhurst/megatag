package application;
	
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;


public class Main extends Application {
    
    private static final ScheduledExecutorService scheduler = 
            Executors.newScheduledThreadPool(1);
    
    public static ScheduledExecutorService getThreadPool() {
        return scheduler;
    }
    
    public static File DB_FILE = new File("db.json");
    
	@Override
	public void start(Stage primaryStage) {
	    try {
	        primaryStage.setScene(
	            new Scene(
	                FXMLLoader.load(getClass().getResource("../mainwindow/MainWindow.fxml")),
	                900, 600) );
	    } catch (IOException e) {
	        // Fail hard
	        System.out.println("FXML Error: " + e.toString());
	        Main.exit();
	    }
	    
        primaryStage.setOnCloseRequest(e -> Platform.exit());
        primaryStage.setTitle("Megans Photos");
        primaryStage.show();
	}
	
	@Override
	public void stop() {
	    Main.exit();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static void exit() {
	    System.out.println("Exiting");
        Platform.exit();
        scheduler.shutdownNow();
        System.exit(0);
	}
}
