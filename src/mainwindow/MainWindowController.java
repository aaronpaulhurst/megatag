package mainwindow;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import application.Main;
import javafx.application.Platform;
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
import javafx.stage.*;
import java.util.*;
import java.io.*;
import model.Database;
import model.Photo;
import imagewindow.ImageWindowController;

public class MainWindowController implements Initializable, ItemController {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialize MainWindow");

        dbview.bind(listView, this);

        // Read database
        try (FileInputStream is = new FileInputStream(Main.DB_FILE)) {
            db.read(is);
        } catch (IOException ex) {
            // Ignore
            System.out.println("Could not read existing database: " + ex.toString());
        }

        loadChildViews();

        // Allow multiple items to be selected
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // OS X System Menu Bar
        if( System.getProperty("os.name","UNKNOWN").equals("Mac OS X")) {
            System.out.println("Adding Mac OS X menubar");
            mainMenuBar.setUseSystemMenuBar(true);
        }
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
        } catch (IOException e) {
            // Fail hard
            System.out.println("FXML Error: " + e.toString());
            Main.exit();
        }

        // ----

        tagWindow = new Popup();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("mainwindow/TagWindow.fxml"));
            tagWindow.getContent().add(loader.load());
            tagWindow.setAutoHide( true );
            tagWindow.setHideOnEscape( true );
            tagWindow.setOpacity(1.0);

            tagWindowController = (TagWindowController)loader.getController();
            tagWindowController.setParent(this);
            tagWindowController.initEvents();
        } catch (IOException e) {
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
    @FXML MenuBar mainMenuBar;
    @FXML Button cancelFindTagButton;
    @FXML MenuButton tagMenuButton;
    @FXML MenuItem removeMenuItem;
    @FXML MenuItem clearAllTagsMenuItem;
    @FXML CheckMenuItem filterFavoritesMenuItem;
    @FXML CheckMenuItem filterMissingMenuItem;
    @FXML CheckMenuItem filterTaggedMenuItem;
    @FXML CheckMenuItem filterUntaggedMenuItem;
    @FXML CheckMenuItem filterDuplicatedMenuItem;
    @FXML CheckMenuItem filterExtraCopiesMenuItem;
    @FXML CheckMenuItem filterHideExtrasMenuItem;
    @FXML Label messageLabel;

    // ----- Parent/Child Views -----
    Stage imageWindow;
    ImageWindowController imageWindowController;

    Popup tagWindow;
    TagWindowController tagWindowController;

    // ----- Controller -----

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

    @FXML private void onQuitMenuItem(ActionEvent e) {
        System.out.println("Event: onQuitMenuItem");

        // Ensure that we record any in-progress edits.
        endCaptionEditing();

        if (db.wasChangedSinceSave()) {
            Alert alert = new Alert(
                    AlertType.CONFIRMATION,
                    "Save before quitting?",
                    ButtonType.YES, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() != ButtonType.YES) {
                save();
            }
        }

        Main.exit();
    }

    @FXML private void onSortAscending(ActionEvent e) {
        System.out.println("Event: onSortAscending");
        dbview.setSortDirection(true);
        dbview.update(db, null);
    }

    @FXML private void onSortDescending(ActionEvent e) {
        System.out.println("Event: onSortDescending");
        dbview.setSortDirection(false);
        dbview.update(db, null);
    }

    @FXML private void onSelectAllMenuItem(ActionEvent e) {
        System.out.println("Event: onSelectAllMenuItem");
        listView.getSelectionModel().selectAll();
    }

    @FXML private void onSelectNoneMenuItem(ActionEvent e) {
        System.out.println("Event: onSelectNoneMenuItem");
        listView.getSelectionModel().clearSelection();
    }

    @FXML private void onSortFilenameMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFilenameMenuItem");
        dbview.setSort((Photo x, Photo y) -> {
            return x.getFile().compareTo(y.getFile());
        });
        dbview.update(db, null);
    }

    @FXML private void onSortLastModifiedMenuItem(ActionEvent e) {
        System.out.println("Event: onSortLastModifiedMenuItem");
        dbview.setSort((Photo x, Photo y) -> {
            return new Long(x.getLastModified()).compareTo(y.getLastModified());
        });
        dbview.update(db, null);
    }

    @FXML private void onSortFileSizeMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFileSizedMenuItem");
        dbview.setSort((Photo x, Photo y) -> {
            return new Long(x.getFileSize()).compareTo(y.getFileSize());
        });
        dbview.update(db, null);
    }

    @FXML private void onGroupDuplicatedMenuItem(ActionEvent e) {
        System.out.println("Event: onGroupDuplicatedMenuItem");
        dbview.setSort((Photo x, Photo y) -> {
            int xhash = x.getHash();
            int yhash = y.getHash();
            // Sort by hash
            if (xhash != yhash) {
                return xhash - yhash;
            }
            // Sort by canonical copy
            if (x.getDb().isCanonicalCopy(x)) {
                return 1;
            }
            if (y.getDb().isCanonicalCopy(y)) {
                return -1;
            }
            // Same
            return 0;
        });
        dbview.update(db, null);
    }

    @FXML private void onSortFavoritesMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFavoritesMenuItem");
        dbview.setSort((Photo x, Photo y) -> {
            return new Boolean(x.isFavorite()).compareTo(y.isFavorite());
        });
        dbview.update(db, null);
    }

    @FXML private void onSortTagsMenuItem(ActionEvent e) {
        System.out.println("Event: onSortTagsMenuItem");
        dbview.setSort( (Photo x, Photo y) -> {
            for(int i = 0; true; i++) {
                if (i == x.getTags().size() && i == y.getTags().size()) {
                    return 0;
                }
                if (i >= x.getTags().size()) {
                    return -1;
                }
                if (i >= y.getTags().size()) {
                    return 1;
                }
                String xtag = x.getTags().get(i);
                String ytag = y.getTags().get(i);
                if (xtag != ytag) {
                    return xtag.compareTo(ytag);
                }
            }
        });
        dbview.update(db, null);
    }

    @FXML private void onSortOrderAddedMenuItem(ActionEvent e) {
        System.out.println("Event: onSortOrderAddedMenuItem");
        dbview.setSort(null);
    }

    @FXML private void onTagMenuMouseEntered(MouseEvent e) {
        System.out.println("Event: mouse entered");

        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        for(String tag : db.getAllTags().keySet()) {
            MenuItem item = new MenuItem(tag);
            item.setOnAction(this::onTagMenuButton);
            menuItems.add(item);
        }

        System.out.println("Adding " + menuItems.size() + " items");

        tagMenuButton.getItems().clear();
        tagMenuButton.getItems().addAll(menuItems);
    }

    @FXML private void onTagMenuButton(ActionEvent e) {
        System.out.println("Event: tagMenuButton from "+e.getSource().toString());

        if (e.getSource() instanceof MenuItem) {
            MenuItem item = (MenuItem)e.getSource();

            dbview.filterTag = item.getText();
            tagMenuButton.setText(item.getText());
            cancelFindTagButton.setVisible(true);
            dbview.update(db,  null);
        }
    }

    @FXML private void onCancelFindTagButton(ActionEvent e) {
        System.out.println("Event: onCancelFindTagButton");

        dbview.filterTag = null;
        tagMenuButton.setText("...");
        cancelFindTagButton.setVisible(false);
        dbview.update(db,  null);
    }

    @FXML private void onRotateRightMenuItem(ActionEvent e) {
        System.out.println("Event: onRotateRightMenuItem");
        for(Photo i : listView.getSelectionModel().getSelectedItems()) {
            i.setRotation(i.getRotation() + 90);
        }
    }

    @FXML private void onRotateLeftMenuItem(ActionEvent e) {
        System.out.println("Event: onRotateLeftMenuItem");
        for(Photo i : listView.getSelectionModel().getSelectedItems()) {
            i.setRotation(i.getRotation() - 90);
        }
    }

    @FXML private void onOpenDirectoryMenuItem(ActionEvent e) {
        System.out.println("Event: onOpenDirMenuItem");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Image Folder");

        File root = chooser.showDialog(getWindow());

        db.addSearchRoot(root, messageLabel.textProperty());
    }

    @FXML private void onClearAllTagsMenuItem(ActionEvent e) {
        System.out.println("Event: onClearAllTagsMenuItem");

        // Multiple selections?  Confirm.
        int numSelected = listView.getSelectionModel().getSelectedItems().size();
        if (numSelected > 1) {
            Alert alert = new Alert(
                    AlertType.CONFIRMATION,
                    "Change " + numSelected + " selected photos?",
                    ButtonType.YES, ButtonType.CANCEL);
            alert.showAndWait();

            if (alert.getResult() != ButtonType.YES) {
                return;
            }
        }

        for(Photo i : listView.getSelectionModel().getSelectedItems()) {
            i.removeAllTags();
        }
    }

    @FXML private void onRemoveMenuItem(ActionEvent e) {
        System.out.println("Event: onRemoveMenuItem");
        deletePhoto();
    }

    @FXML private void onOpenFilesMenuItem(ActionEvent e) {
        System.out.println("Event: onOpenFilesMenuItem");

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Images");
        List<File> list = chooser.showOpenMultipleDialog(getWindow());

        db.addFiles(list);

        messageLabel.textProperty().unbind();
        messageLabel.textProperty().set("Total = " + db.size() + " images.");
    }

    @FXML private void onSaveMenuItem(ActionEvent e) {
        System.out.println("Event: onSaveMenuItem");
        save();
    }

    @FXML private void onFilterClearMenuItem(ActionEvent e) {
        // Unselect all menu items and reset DB view
        dbview.filterFavorites = false;
        filterFavoritesMenuItem.setSelected(false);

        dbview.filterFavorites = false;
        filterFavoritesMenuItem.setSelected(false);

        dbview.filterOnlyDuplicated = false;
        filterDuplicatedMenuItem.setSelected(false);
        dbview.filterOnlyExtraCopies = false;
        filterExtraCopiesMenuItem.setSelected(false);
        dbview.filterHideExtraCopies = false;
        filterHideExtrasMenuItem.setSelected(false);

        dbview.filterTagged = false;
        filterTaggedMenuItem.setSelected(false);
        dbview.filterUntagged = false;
        filterUntaggedMenuItem.setSelected(false);

        dbview.update(db, null);
    }

    @FXML private void onFilterFavoritesMenuItem(ActionEvent e) {
        // Update DB view
        dbview.filterFavorites = filterFavoritesMenuItem.isSelected();
        dbview.update(db, null);
    }

    @FXML private void onFilterMissingMenuItem(ActionEvent e) {
        // Update DB view
        dbview.filterMissing = filterMissingMenuItem.isSelected();
        dbview.update(db, null);
    }

    // Helper
    private void doFilterDuplicatesRadioGroup(ActionEvent e) {
        // Behave as a unselectable radio group
        if (filterDuplicatedMenuItem != e.getSource()) {
            filterDuplicatedMenuItem.setSelected(false);
        }
        if (filterExtraCopiesMenuItem != e.getSource()) {
            filterExtraCopiesMenuItem.setSelected(false);
        }
        if (filterHideExtrasMenuItem != e.getSource()) {
            filterHideExtrasMenuItem.setSelected(false);
        }
        // Update DB view
        dbview.filterOnlyDuplicated  = filterDuplicatedMenuItem.isSelected();
        dbview.filterOnlyExtraCopies = filterExtraCopiesMenuItem.isSelected();
        dbview.filterHideExtraCopies = filterHideExtrasMenuItem.isSelected();
        dbview.update(db, null);
    }

    @FXML private void onFilterDuplicatedMenuItem(ActionEvent e) {
        doFilterDuplicatesRadioGroup(e);
    }

    @FXML private void onFilterExtraCopiesMenuItem(ActionEvent e) {
        doFilterDuplicatesRadioGroup(e);
    }

    @FXML private void onFilterHideExtrasMenuItem(ActionEvent e) {
        doFilterDuplicatesRadioGroup(e);
    }

    // Helper
    private void doFilterTagsRadioGroup(ActionEvent e) {
        // Behave as a unselectable radio group
        if (filterTaggedMenuItem != e.getSource()) {
            filterTaggedMenuItem.setSelected(false);
        }
        if (filterUntaggedMenuItem != e.getSource()) {
            filterUntaggedMenuItem.setSelected(false);
        }

        // Update DB view
        dbview.filterTagged = filterTaggedMenuItem.isSelected();
        dbview.filterUntagged = filterUntaggedMenuItem.isSelected();
        dbview.update(db, null);
    }

    @FXML private void onFilterTaggedMenuItem(ActionEvent e) {
        doFilterTagsRadioGroup(e);
    }

    @FXML private void onFilterUntaggedMenuItem(ActionEvent e) {
        doFilterTagsRadioGroup(e);
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
        tagWindowController.setModel(view.getPhoto());
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
            db.deleteItems(listView.getSelectionModel().getSelectedItems());
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

    public List<Photo> getSelected() {
        // Make a copy
        return new ArrayList<Photo>(
                listView.getSelectionModel().getSelectedItems() );
    }

    // ----- Helpers -----

    private void save() {
        try (FileOutputStream os = new FileOutputStream(Main.DB_FILE)) {
            db.write(os);
            db.clearChangedSinceSave();
        } catch (IOException ex) {
            Alert a = new Alert(AlertType.ERROR);
            a.setContentText("Failed to save database");
            a.showAndWait();
        }
    }

    private Window getWindow() {
        return mainMenuBar.getScene().getWindow();
    }

    public Database getModel() {
        return db;
    }
}
