package mainwindow;

import java.io.File;
import java.util.*;

import javafx.collections.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import model.Database;
import model.Photo;

public class ItemView extends ListCell<Photo> implements Observer {
    
    static final Image favoriteOnIcon = 
            new Image(application.Main.class.getResourceAsStream("/media/star.png"),
                      25, 25, true, true);
    static final Image favoriteOffIcon = 
            new Image(application.Main.class.getResourceAsStream("/media/dimstar.png"),
                      25, 25, true, true);

    static final Image tagImage = 
            new Image(application.Main.class.getResourceAsStream("/media/tag.png"),
                      32, 40, true, true);
    static final Image untaggedImage = 
            new Image(application.Main.class.getResourceAsStream("/media/untagged.png"),
                      32, 40, true, true);

    static final Image copiesImage = 
            new Image(application.Main.class.getResourceAsStream("/media/copies.png"),
                      25, 25, true, true);
    static final Image firstCopyImage = 
            new Image(application.Main.class.getResourceAsStream("/media/firstcopy.png"),
                      25, 25, true, true);
    
    static final int MAX_DISPLAY_LENGTH = 50;
    
    // ----- View components -----
    HBox panel = new HBox();
    ImageView imageView = new ImageView();
    Label path = new Label();
    ImageView favoriteIcon = new ImageView();
    ImageView tagIcon = new ImageView();
    ImageView copiesIcon = new ImageView();
    Label tagLabel = new Label();
    Tooltip tagTooltip = new Tooltip();

    // ----- Model linkage -----
    Photo model = null;

    // ----- Controller linkage -----
    ItemController controller;
    
    ItemView(ItemController c) {
        assert(favoriteOnIcon != null);
        assert(favoriteOffIcon != null);
        assert(tagImage != null);
        assert(untaggedImage != null);
        assert(firstCopyImage != null);
        assert(copiesImage != null);

        controller = c;
    
        // --- Install event handlers
        setOnMousePressed(controller::onListItemMouseClick);
        favoriteIcon.setOnMousePressed(e -> {
            controller.onFavoriteMouseClick(e, this); 
        });
        tagIcon.setOnMousePressed(e -> {
            controller.onTagMouseClick(e, this); 
        });
        
        // --- Build scene graph
        panel.setMaxWidth(Double.MAX_VALUE);

        imageView.setFitWidth(Photo.THUMB_WIDTH);
        imageView.setFitHeight(Photo.THUMB_HEIGHT);
        panel.getChildren().add(imageView);
  
        favoriteIcon.setFitWidth(25);
        favoriteIcon.setFitHeight(25);
        panel.getChildren().add(favoriteIcon);
        
        tagIcon.setFitWidth(32);
        tagIcon.setFitHeight(40);
        panel.getChildren().add(tagIcon);
        
        copiesIcon.setFitWidth(25);
        copiesIcon.setFitHeight(25);
        panel.getChildren().add(copiesIcon);
        
        /*
        // Print tags
        tagLabel.setStyle("-fx-font-size: 8");
        panel.getChildren().add(tagLabel);
        */
        Tooltip.install(tagIcon, tagTooltip);
        
        Pane ppp = new Pane();
        ppp.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().add(ppp);
        HBox.setHgrow(ppp, Priority.ALWAYS);

        path.setTextAlignment(TextAlignment.RIGHT);
        panel.getChildren().add(path);

        panel.setAlignment(Pos.CENTER_LEFT);
    }
    
    Photo getImageInfo() { return model; }
    
    @Override
    public void updateItem(Photo item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
            
            if (model != null) {
                model.deleteObserver(this);
            }
            model = null;
        } else {
            model = item;

            update(model, null);
            model.addObserver(this);
            
            setGraphic(panel);
        }
    }
    
    // Return a chopped display version of the file path.
    private String getDisplayName(File file) {                
        String rsl = file.getName();

        File parent = file.getParentFile();
        if (parent != null) {
            rsl = parent.getName() + "/" + rsl;
        }

        if (rsl.length() > MAX_DISPLAY_LENGTH) {
            rsl = "..." + rsl.substring(rsl.length() - MAX_DISPLAY_LENGTH);
        }

        return rsl;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == model) {
            imageView.setImage(model.getThumbnail());
            imageView.setRotate(model.getRotation());
            
            path.setText(getDisplayName(model.getFile()));

            favoriteIcon.setImage(
                model.isFavorite()
                    ? favoriteOnIcon
                    : favoriteOffIcon);   
            
            tagIcon.setImage(
                model.getTags().isEmpty()
                    ? untaggedImage
                    : tagImage);
            
            int copies = model.getDb().numDuplicates(model);
            if (copies <= 1) {
                copiesIcon.setImage(null);       
            }
            else if (model.getDb().isCanonicalCopy(model)) {
                copiesIcon.setImage(firstCopyImage);
            }
            else {
                copiesIcon.setImage(copiesImage);
            }
            /*
            // Print tags
            String tagStr = "";
            if (model.getTags().size() > 0) {
                tagStr += model.getTags().get(0);
            } else {
                tagStr = "<none>";
            }
            if (model.getTags().size() > 1) {
                tagStr += "\n" + model.getTags().get(1);
            }
            if (model.getTags().size() > 2) {
                tagStr += "\n...";
            }
            tagLabel.setText(tagStr);
            */
            
            if (model.getTags().isEmpty()) {
                tagTooltip.setText("No tags");
            } else {
                String tagStr = "Tags:";
                for(String t: model.getTags()) {
                    tagStr += "\n  " + t;
                }
                tagTooltip.setText(tagStr);
            }
        }
    }
}
    