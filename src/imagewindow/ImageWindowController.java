package imagewindow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.*;

import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import mainwindow.MainWindowController;
import model.Photo;

public class ImageWindowController implements Initializable, Observer {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialize ImageWindowController");

        // Original size
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        maxWidth = primaryScreenBounds.getWidth() - 200;
        maxHeight = primaryScreenBounds.getHeight() - 200;
    }

    ScheduledFuture resizeFuture = null;

    public void initEvents() {
        Scene scene = mainImageView.getScene();
        Window window =scene.getWindow();

        window.setOnCloseRequest(e -> { window.hide(); });

        window.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (resizeFuture != null) {
                resizeFuture.cancel(false);
            }

            resizeFuture = application.Main.getThreadPool().schedule(() -> {
                maxHeight = window.getHeight();
                maxWidth = window.getWidth();
                resize();
            }, 100, TimeUnit.MILLISECONDS);
        });

        window.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (resizeFuture != null) {
                resizeFuture.cancel(false);
            }

            resizeFuture = application.Main.getThreadPool().schedule(() -> {
                maxHeight = window.getHeight();
                maxWidth = window.getWidth();
                resize();
            }, 100, TimeUnit.MILLISECONDS);
        });
    }

    double maxWidth = -1, maxHeight = -1;

    @FXML ImageView mainImageView;

    // ----- Model -----
    Photo current;

    // ----- Parent/Child Views -----
    MainWindowController parent;
    public void setParent(MainWindowController p) { parent = p; }

    // ----- Controller -----

    private void resize() {
        Image img = mainImageView.getImage();
        if (img == null) {
            return;
        }

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        int rot = (int)mainImageView.getRotate();
        if ((rot % 180) == 90 ||
            (rot % 180) == -90)
        {
            double swap = imgWidth;
            imgWidth = imgHeight;
            imgHeight = swap;
        }

        // Constraint 1: Bound by window size
        // Constraint 2: Shrink to image
        double newWidth = Math.min(imgWidth, maxWidth);
        double newHeight = Math.min(imgHeight, maxHeight);
        // Constraint 3: Shrink to aspect ratio
        double shrink = 0;
        if ((shrink = newWidth - newHeight*img.getWidth()/img.getHeight()) > 1.0) {
            newWidth -= shrink;
            mainImageView.setX(shrink*0.5);
        } else {
            mainImageView.setX(0);
        }
        if ((shrink = newHeight - newWidth*img.getHeight()/img.getWidth()) > 1.0) {
            newHeight -= shrink;
            mainImageView.setY(shrink*0.5);
        } else {
            mainImageView.setY(0);
        }

        // Resize image view
        mainImageView.setFitWidth(newWidth);
        mainImageView.setFitHeight(newHeight);
    }

    public void setImage(Photo img) {

        if (img != null) {
            img.deleteObserver(this);
        }

        current = img;
        current.addObserver(this);

        try (FileInputStream fis = new FileInputStream(current.getFile())) {
            mainImageView.setImage(new Image(fis));
            mainImageView.setRotate(img.getRotation());
        } catch (FileNotFoundException ex) {
            // Do nothing
        }catch (IOException ex) {
            // Do nothing
        }

        resize();
    }

    @FXML void onKeyPressed(KeyEvent e) {
        System.out.println("Key pressed in image window");

        KeyCode key = e.getCode();
        switch(key) {
        case LEFT:
        case KP_LEFT:
        case UP:
        case KP_UP:
            parent.prevPhoto();
            e.consume();
            break;
        case RIGHT:
        case KP_RIGHT:
        case DOWN:
        case KP_DOWN:
        case SPACE:
            parent.nextPhoto();
            e.consume();
            break;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == current) {
            // Refresh in case rotation etc. changed
            setImage(current);
        }
    }
}
