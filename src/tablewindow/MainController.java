package tablewindow;

import java.io.IOException;
import java.net.URL;

import application.Main;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.*;
import java.util.*;
import java.util.function.Consumer;
import java.io.*;
import model.Database;
import model.Photo;
import imagewindow.ImageWindowController;

public class MainController implements Initializable, ItemController {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialize MainController");

        getDatabaseView().bind(listView, this);

        loadChildViews();

        menuBarPaneController.setParent(this);

        // Allow multiple items to be selected
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadChildViews() {
        imageWindow = new Stage();
        imageWindow.setResizable(true);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("imagewindow/ImageWindow.fxml"));
            imageWindow.setScene(new Scene(loader.load(), 1000, 600));

            imageWindowController = (ImageWindowController)loader.getController();
            imageWindowController.setParent(this);
            imageWindowController.initEvents();
        } catch (Exception e) {
            // Fail hard
            System.out.println("FXML Error: " + e.toString());
            Main.exit();
        }

        // ----

        tagWindow = new Popup();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("tablewindow/TagPopup.fxml"));
            tagWindow.getContent().add(loader.load());
            tagWindow.setAutoHide( true );
            tagWindow.setHideOnEscape( true );
            tagWindow.setOpacity(1.0);

            tagPopupController = (TagPopupController)loader.getController();
            tagPopupController.setParent(this);
            tagPopupController.initEvents();
        } catch (Exception e) {
            // Fail hard
            System.out.println("FXML Error: " + e.toString());
            Main.exit();
        }
    }

    // ----- Model -----
    Database db = new Database();

    // ----- View -----
    DatabaseView dbview = new DatabaseView(db);

    @FXML ListView<Photo> listView;
    @FXML Label messageLabel;
    @FXML AnchorPane menuBarPane;
    @FXML Button cancelFindTagButton;
    @FXML MenuButton tagMenuButton;

    // ----- Controller Hierarchy -----
    Stage imageWindow;
    ImageWindowController imageWindowController;

    Popup tagWindow;
    TagPopupController tagPopupController;

    @FXML MenuController menuBarPaneController;

    // ----- Event handlers -----

    @FXML private void onTagMenuButton(ActionEvent e) {
        System.out.println("Event: tagMenuButton from "+e.getSource().toString());

        if (e.getSource() instanceof MenuItem) {
            MenuItem item = (MenuItem)e.getSource();

            getDatabaseView().filterTag = item.getText();
            tagMenuButton.setText(item.getText());
            cancelFindTagButton.setVisible(true);
            getDatabaseView().update(db,  null);
        }
    }

    @FXML private void onCancelFindTagButton(ActionEvent e) {
        System.out.println("Event: onCancelFindTagButton");

        getDatabaseView().filterTag = null;
        tagMenuButton.setText("...");
        cancelFindTagButton.setVisible(false);
        getDatabaseView().update(db,  null);
    }

    @FXML private void onKeyPressed(KeyEvent e) {
        System.out.println("Event: onKeyPressed");

        switch(e.getCode()) {
        case BACK_SPACE:
        case DELETE:
            deletePhoto();
            e.consume();
            break;
        }
    }


    // ---- ItemController interface ----

    @Override
    public void onListItemMouseClick(MouseEvent e) {
        if (e.isConsumed()) {
            return;
        }

        if (e.getSource() instanceof ItemView) {
            ItemView iv = (ItemView)e.getSource();

            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if(e.getClickCount() == 2) {
                    imageWindowController.setImage(iv.getPhoto());
                    imageWindow.show();
                }
            }
        }
    }
    @Override
    public void onFavoriteMouseClick(MouseEvent e, ItemView view) {

        // Get current button state
        boolean newState = !view.getPhoto().isFavorite();

        // Multiple selections?  Confirm.
        int numSelected = listView.getSelectionModel().getSelectedItems().size();
        if (numSelected > 1) {
            Alert alert = new Alert(
                    AlertType.CONFIRMATION,
                    "Change " + numSelected + " selected photos?",
                    ButtonType.YES, ButtonType.CANCEL);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                for(Photo i : listView.getSelectionModel().getSelectedItems()) {
                    i.setFavorite(newState);
                }
            }
        }

        else {
            // Modify only image that the button belonged to.
            view.getPhoto().setFavorite(newState);
        }

        e.consume();
    }

    @Override
    public void onTagMouseClick(MouseEvent e, ItemView view) {
        tagPopupController.setModel(view.getPhoto());
        tagWindow.show(getWindow(), e.getScreenX(), e.getScreenY());
        e.consume();
    }

    // When non-null, the caption that is currently being edited.
    ItemView captionUnderEdit = null;

    @Override
    public void onEditCaptionMousePressed(MouseEvent e, ItemView view) {

        if (view.isEditCaptionEnabled()) {
            view.setEditCaptionEnabled(false);
            captionUnderEdit = null;
        } else {
            if (captionUnderEdit != null) {
                captionUnderEdit.setEditCaptionEnabled(false);
            }
            view.setEditCaptionEnabled(true);
            captionUnderEdit = view;
        }

        e.consume();
    }

    public void endCaptionEditing() {
        if (captionUnderEdit != null) {
            captionUnderEdit.setEditCaptionEnabled(false);
        }
        captionUnderEdit = null;
    }

    @Override
    public void onEditCaptionTextField(ActionEvent e, ItemView view) {
        if (e.getSource() instanceof TextField) {
            TextField src = (TextField)e.getSource();

            view.getPhoto().setCaption(src.getText());
            view.setEditCaptionEnabled(false);

            e.consume();
        }
    }

    @FXML private void onTagMenuMouseEntered(MouseEvent e) {
        System.out.println("Event: mouse entered");

        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        for(String tag : getDatabase().getAllTags().keySet()) {
            MenuItem item = new MenuItem(tag);
            item.setOnAction(this::onTagMenuButton);
            menuItems.add(item);
        }

        System.out.println("Adding " + menuItems.size() + " items");

        tagMenuButton.getItems().clear();
        tagMenuButton.getItems().addAll(menuItems);
    }

    // ---- Actions ----

    public void deletePhoto() {
        int numSelected = listView.getSelectionModel().getSelectedItems().size();
        if (numSelected == 0) {
            return;
        }

        // Confirm always.
        Alert alert = new Alert(
                AlertType.CONFIRMATION,
                "Remove " + numSelected + " selected photos?\n" +
                        "They will not be deleted on disk.",
                        ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            getDatabase().deleteItems(listView.getSelectionModel().getSelectedItems());
        }
    }

    public void nextPhoto() {
        if (listView.getSelectionModel().isEmpty()) {
            return;
        }
        int idx = listView.getSelectionModel().getSelectedIndex() + 1;
        if (idx >= listView.getItems().size()) {
            return;
        }
        listView.getSelectionModel().clearAndSelect(idx);
        imageWindowController.setImage(listView.getSelectionModel().getSelectedItem());
    }

    public void prevPhoto() {
        if (listView.getSelectionModel().isEmpty()) {
            return;
        }
        int idx = listView.getSelectionModel().getSelectedIndex() - 1;
        if (idx < 0) {
            return;
        }
        listView.getSelectionModel().clearAndSelect(idx);
        imageWindowController.setImage(listView.getSelectionModel().getSelectedItem());
    }

    public int numSelected() {
        return listView.getSelectionModel().getSelectedItems().size();
    }

    public List<Photo> getSelected() {
        // Make a copy
        return new ArrayList<Photo>(
                listView.getSelectionModel().getSelectedItems() );
    }

    public void applySelected(Consumer<Photo> f) {
        for(Photo p : listView.getSelectionModel().getSelectedItems()) {
            f.accept(p);
        }
    }

    public void selectAll() {
        listView.getSelectionModel().selectAll();
    }

    public void selectNone() {
        listView.getSelectionModel().clearSelection();
    }

    public void save() {
        try (FileOutputStream os = new FileOutputStream(Main.DB_FILE)) {
            getDatabase().write(os);
            getDatabase().clearChangedSinceSave();
        } catch (IOException ex) {
            Alert a = new Alert(AlertType.ERROR);
            a.setContentText("Failed to save database");
            a.showAndWait();
        }
    }

    // Load a database.
    // Does not need to be run
    public void load(File dbFile) {
        db.clear();

        // Run in a separate thread
        Task<Void> task = new Task<Void>() {
            @Override public Void call() {

                try (FileInputStream is = new FileInputStream(dbFile)) {
                    getDatabase().read(is, (String msg) -> { updateMessage(msg); });
                } catch (IOException ex) {
                    // Ignore
                    System.out.println("Could not read existing database: " + ex.toString());
                }

                return null;
            }
        };

        getMessageProperty().unbind();
        getMessageProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            // Database.read already notifies observers
            getMessageProperty().unbind();
            getMessageProperty().set("Total = " + getDatabase().size() + " images.");
        });

        new Thread(task).start();
    }

    // ----- Helpers -----

    private Window getWindow() {
        return menuBarPane.getScene().getWindow();
    }

    public Database     getDatabase()     { return db; }
    public DatabaseView getDatabaseView() { return dbview; }

    public StringProperty getMessageProperty() { return messageLabel.textProperty(); }
}
