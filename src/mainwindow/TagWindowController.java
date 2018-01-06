package mainwindow;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import application.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.geometry.Orientation;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import java.util.*;
import java.io.*;
import model.Database;
import model.Photo;
import imagewindow.ImageWindowController;

public class TagWindowController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    // ----- Model -----
    Photo model;

    // ----- Parent/Child Views -----
    MainWindowController parent;
    public void setParent(MainWindowController p) { parent = p; }

    // ----- View Elements -----

    @FXML AnchorPane rootPane;
    @FXML VBox curTags;
    @FXML VBox newTags;
    @FXML TextField addNewTagField;

    public void initEvents() {
    }

    public void setModel(Photo model) {

        this.model = model;

        curTags.getChildren().clear();
        if (model.getTags().isEmpty()) {
            curTags.getChildren().add(new Label("No tags"));
        } else {
            newTags.getChildren().add(new Label("Current tags..."));
        }

        for(String tag: model.getTags()) {
            HBox hbox = new HBox();
            hbox.setSpacing(5);

            Button removeButton = new Button("-");
            removeButton.getStyleClass().add("minus-button");
            removeButton.setOnAction(e -> {
                // Untag specific photo
                model.removeTag(tag);

                // Untag other selected items?
                List<Photo> selected = parent.getSelected();
                selected.remove(model);
                if (!selected.isEmpty()) {
                    Alert alert = new Alert(
                            AlertType.CONFIRMATION,
                            "Untag " + selected.size() + " other selected photos?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait();

                    if (alert.getResult() == ButtonType.YES) {
                        for(Photo p: selected) {
                            p.removeTag(tag);
                        }
                    }
                }

                getWindow().hide();
            });

            Label tagLabel = new Label(tag);

            hbox.getChildren().addAll(removeButton, tagLabel);
            curTags.getChildren().add(hbox);
        }

        newTags.getChildren().clear();
        List<String> topTags = parent.getModel().getTopTags(5);
        // Don't suggest tags we already have
        for(String tag: model.getTags()) {
            topTags.remove(tag);
        }
        if (!topTags.isEmpty()) {
            newTags.getChildren().add(new Separator(Orientation.HORIZONTAL));
            newTags.getChildren().add(new Label("Recent tags..."));
        }
        for (String tag : topTags) {
            HBox hbox = new HBox();
            hbox.setSpacing(5);

            Button addButton = new Button("+");
            addButton.getStyleClass().add("plus-button");
            addButton.setOnAction(e -> {
                tagModelAndMaybeOtherSelections(tag);
                getWindow().hide();
            });
            Label tagLabel = new Label(tag);

            hbox.getChildren().addAll(addButton, tagLabel);
            newTags.getChildren().add(hbox);
        }
    }

    private Window getWindow() {
        return rootPane.getScene().getWindow();
    }

    @FXML
    public void onAddNewTagButton(ActionEvent e) {
        String tag = addNewTagField.getText();
        System.out.println("Adding tag: "+tag);
        tagModelAndMaybeOtherSelections(tag);

        addNewTagField.clear();
        getWindow().hide();
    }

    @FXML
    public void onAddNewTagField(ActionEvent e) {
        String tag = addNewTagField.getText();
        System.out.println("Adding tag: "+tag);
        tagModelAndMaybeOtherSelections(tag);

        addNewTagField.clear();
        getWindow().hide();
    }

    // --- Helpers ---

    private void tagModelAndMaybeOtherSelections(String tag) {
        // Add tag to specific row
        model.addTag(tag);

        // Add tag to other selected items?
        List<Photo> selected = parent.getSelected();
        selected.remove(model);
        if (!selected.isEmpty()) {
            Alert alert = new Alert(
                    AlertType.CONFIRMATION,
                    "Tag " + selected.size() + " other selected photos?",
                    ButtonType.YES, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                for(Photo p: selected) {
                    p.addTag(tag);
                }
            }
        }
    }

}
