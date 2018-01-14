package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import imagewindow.ImageWindowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import tablewindow.MainController;
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

    public static File DB_FILE = new File("megatagDB.json");

    @Override
    public void start(Stage primaryStage) {

        // Where is the database located?
        // 1. On Windows, use the APPDATA environment variable
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            DB_FILE = new File(appData, DB_FILE.toString());
        }
        // 2. Otherwise... home directory
        else {
            appData = System.getProperty("user.home");
            DB_FILE = new File(appData, DB_FILE.toString());
        }

        MainController primaryController = null;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("tablewindow/MainWindow.fxml"));
            primaryStage.setScene(new Scene(loader.load(), 1000, 600));
            primaryStage.setOnCloseRequest(e -> Platform.exit());
            primaryStage.setTitle("Megatag");
            primaryStage.show();
            primaryController = (MainController)loader.getController();
        } catch (Exception e) {
            // Fail hard
            System.out.println("Error: " + e.toString() + " at " + e.getStackTrace().toString());
            Main.exit();
        }

        // Load database in another thread
        final MainController primaryController2 = primaryController;
        Task<Void> task = new Task<Void>() {
            @Override public Void call() {
                primaryController2.load();
                return null;
            }
        };
        new Thread(task).start();
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
