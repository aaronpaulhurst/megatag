package mainwindow;

import java.io.File;
import java.util.*;

import javafx.collections.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
    
    static final Image editCaptionImage = 
            new Image(application.Main.class.getResourceAsStream("/media/write.png"),
                      25, 25, true, true);
    
    static final int MAX_DISPLAY_LENGTH = 50;
    
    // ----- View components -----
    HBox panel = new HBox();
    ImageView imageView = new ImageView();
    
    Label   path = new Label();
    Tooltip pathTooltip = new Tooltip();
    
    ImageView favoriteIcon = new ImageView();
    Tooltip   favoriteTooltip = new Tooltip();
    
    ImageView tagIcon = new ImageView();
    Tooltip   tagTooltip = new Tooltip();
    Label     tagLabel = new Label();

    ImageView copiesIcon = new ImageView();
    Tooltip   copiesTooltip = new Tooltip();

    ImageView editCaptionIcon = new ImageView();
    Tooltip   editCaptionTooltip = new Tooltip();
    Label     captionLabel = new Label();
    TextArea  captionTextArea = new TextArea();
    boolean   editingCaption = false;
    final int CAPTION_TEXT_WIDTH = 200;
    
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
        assert(editCaptionImage != null);

        controller = c;
        final int ROW_HEIGHT = Photo.THUMB_HEIGHT;
    
        // --- Install event handlers
        setOnMousePressed(controller::onListItemMouseClick);
        favoriteIcon.setOnMousePressed(e -> {
            controller.onFavoriteMouseClick(e, this); 
        });
        tagIcon.setOnMousePressed(e -> {
            controller.onTagMouseClick(e, this); 
        });
        editCaptionIcon.setOnMousePressed(e -> {
            controller.onEditCaptionMousePressed(e, this); 
        });
        
        // --- Build scene graph
        String css = this.getClass().getClassLoader().getResource("application/application.css").toExternalForm();
        panel.getStylesheets().add(css);

        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setSpacing(10.0);

        copiesIcon.setFitWidth(25);
        copiesIcon.setFitHeight(25);
        panel.getChildren().add(copiesIcon);

        Tooltip.install(copiesIcon, copiesTooltip);
        
        imageView.setFitWidth(Photo.THUMB_WIDTH);
        imageView.setFitHeight(Photo.THUMB_HEIGHT);
        panel.getChildren().add(imageView);
  
        favoriteIcon.setFitWidth(25);
        favoriteIcon.setFitHeight(25);
        favoriteIcon.getStyleClass().add("glow-on-hover");
        panel.getChildren().add(favoriteIcon);
                        
        favoriteTooltip.setText("Click to toggle favorite status");
        Tooltip.install(favoriteIcon, favoriteTooltip);
                              
        tagIcon.setFitWidth(32);
        tagIcon.setFitHeight(40);
        tagIcon.getStyleClass().add("glow-on-hover");
        panel.getChildren().add(tagIcon);
                
        editCaptionIcon.setImage(editCaptionImage);
        editCaptionIcon.setFitWidth(25);
        editCaptionIcon.setFitHeight(25);
        editCaptionIcon.getStyleClass().add("glow-on-hover");
        panel.getChildren().add(editCaptionIcon);
        
        captionLabel.setMaxHeight(Double.MAX_VALUE);
        captionLabel.setPrefHeight(ROW_HEIGHT);
        captionLabel.setPrefWidth(0);
        captionLabel.setPadding(new Insets(10.0));
        captionLabel.setAlignment(Pos.TOP_LEFT);
        panel.getChildren().add(captionLabel);
        
        editCaptionTooltip.setText("Click to add/edit caption");
        Tooltip.install(editCaptionIcon, editCaptionTooltip);
     
        captionTextArea.setMaxHeight(Double.MAX_VALUE);
        captionTextArea.setPrefWidth(CAPTION_TEXT_WIDTH);
        captionTextArea.setPrefHeight(ROW_HEIGHT);
        captionTextArea.promptTextProperty().set("Enter caption..");
        panel.getChildren().add(captionTextArea);
        
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
        Tooltip.install(path, pathTooltip);

        panel.getChildren().add(path);

        panel.setAlignment(Pos.CENTER_LEFT);
    }
    
    Photo getPhoto() { return model; }
    
    public void setEditCaptionEnabled(boolean en) {
        this.editingCaption = en;
        
        if (en == false) {
            getPhoto().setCaption(captionTextArea.getText());
        }
        update(model, null);
    }
    
    public boolean isEditCaptionEnabled() {
        return editingCaption;
    }
    
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
            rsl = parent.getName() + "/\n" + rsl;
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
            // Show the full path as a tooltip
            pathTooltip.setText(model.getFile().toString());

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
                copiesTooltip.setText("Unique image");
            }
            else if (model.getDb().isCanonicalCopy(model)) {
                copiesIcon.setImage(firstCopyImage);
                copiesTooltip.setText(
                        String.format("%d copies exist\n(This is the main copy)", copies));
            }
            else {
                copiesIcon.setImage(copiesImage);
                copiesTooltip.setText(
                        String.format("%d copies exist\n(This it NOT the main copy)", copies));
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
                        
            if (editingCaption) {
                captionLabel.setVisible(false);
                captionLabel.setPrefWidth(0);
                captionLabel.setMinWidth(0);
                
                captionTextArea.setVisible(true);
                captionTextArea.setPrefWidth(CAPTION_TEXT_WIDTH);
            }
            else if (model.getCaption() == null || model.getCaption().isEmpty()) {
                captionTextArea.setVisible(false);
                captionTextArea.setPrefWidth(0);
                
                captionLabel.setVisible(false);
                captionLabel.setPrefWidth(0);
            }
            else {
                captionTextArea.setVisible(false);
                captionTextArea.setPrefWidth(0);
                captionTextArea.setText(model.getCaption());
                
                captionLabel.setPrefWidth(CAPTION_TEXT_WIDTH);
                captionLabel.setVisible(true);
                captionLabel.setText(model.getCaption());
            }
        }
    }
}
    