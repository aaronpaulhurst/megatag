package tablewindow;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import application.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.Database;
import model.Photo;

public class MenuController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialize MenuController");

        // OS X System Menu Bar
        if( System.getProperty("os.name","UNKNOWN").equals("Mac OS X")) {
            System.out.println("Adding Mac OS X menubar");
            mainMenuBar.setUseSystemMenuBar(true);
        }
    }

    // ----- Controller Hierarchy -----
    MainController parent;
    public void setParent(MainController p) { parent = p; }

    // ----- View -----
    @FXML MenuBar mainMenuBar;
    @FXML MenuItem removeMenuItem;
    @FXML MenuItem clearAllTagsMenuItem;
    @FXML CheckMenuItem filterFavoritesMenuItem;
    @FXML CheckMenuItem filterMissingMenuItem;
    @FXML CheckMenuItem filterTaggedMenuItem;
    @FXML CheckMenuItem filterUntaggedMenuItem;
    @FXML CheckMenuItem filterDuplicatedMenuItem;
    @FXML CheckMenuItem filterExtraCopiesMenuItem;
    @FXML CheckMenuItem filterHideExtrasMenuItem;

    @FXML private void onQuitMenuItem(ActionEvent e) {
        System.out.println("Event: onQuitMenuItem");

        // Ensure that we record any in-progress edits.
        parent.endCaptionEditing();

        if (getDatabase().wasChangedSinceSave()) {
            Alert alert = new Alert(
                    AlertType.CONFIRMATION,
                    "Save before quitting?",
                    ButtonType.YES, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() != ButtonType.YES) {
                parent.save();
            }
        }

        Main.exit();
    }

    @FXML private void onSortAscending(ActionEvent e) {
        System.out.println("Event: onSortAscending");
        getDatabaseView().setSortDirection(true);
    }

    @FXML private void onSortDescending(ActionEvent e) {
        System.out.println("Event: onSortDescending");
        getDatabaseView().setSortDirection(false);
    }

    @FXML private void onSelectAllMenuItem(ActionEvent e) {
        System.out.println("Event: onSelectAllMenuItem");
        parent.selectAll();
    }

    @FXML private void onSelectNoneMenuItem(ActionEvent e) {
        System.out.println("Event: onSelectNoneMenuItem");
        parent.selectNone();
    }

    @FXML private void onSortFilenameMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFilenameMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
            return x.getFile().compareTo(y.getFile());
        });
    }

    @FXML private void onSortLastModifiedMenuItem(ActionEvent e) {
        System.out.println("Event: onSortLastModifiedMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
            return new Long(x.getLastModified()).compareTo(y.getLastModified());
        });
    }

    @FXML private void onSortOriginalDateMenuItem(ActionEvent e) {
        System.out.println("Event: onSortOriginalDateMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
            return x.getOriginalDate().compareTo(y.getOriginalDate());
        });
    }

    @FXML private void onSortFileSizeMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFileSizedMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
            return new Long(x.getFileSize()).compareTo(y.getFileSize());
        });
    }

    @FXML private void onGroupDuplicatedMenuItem(ActionEvent e) {
        System.out.println("Event: onGroupDuplicatedMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
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
    }

    @FXML private void onSortFavoritesMenuItem(ActionEvent e) {
        System.out.println("Event: onSortFavoritesMenuItem");
        getDatabaseView().setSort((Photo x, Photo y) -> {
            return new Boolean(x.isFavorite()).compareTo(y.isFavorite());
        });
    }

    @FXML private void onSortTagsMenuItem(ActionEvent e) {
        System.out.println("Event: onSortTagsMenuItem");
        getDatabaseView().setSort( (Photo x, Photo y) -> {
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
    }

    @FXML private void onSortOrderAddedMenuItem(ActionEvent e) {
        System.out.println("Event: onSortOrderAddedMenuItem");
        getDatabaseView().setSort(null);
    }

    @FXML private void onRotateRightMenuItem(ActionEvent e) {
        System.out.println("Event: onRotateRightMenuItem");
        parent.applySelected( (Photo i) -> {
            i.setRotation(i.getRotation() + 90);
        });
    }

    @FXML private void onRotateLeftMenuItem(ActionEvent e) {
        System.out.println("Event: onRotateLeftMenuItem");
        parent.applySelected( (Photo i) -> {
            i.setRotation(i.getRotation() - 90);
        });
    }

    @FXML private void onOpenDirectoryMenuItem(ActionEvent e) {
        System.out.println("Event: onOpenDirMenuItem");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Image Folder");

        File root = chooser.showDialog(getWindow());

        getDatabase().addSearchRoot(root, parent.getMessageProperty());
    }

    @FXML private void onClearAllTagsMenuItem(ActionEvent e) {
        System.out.println("Event: onClearAllTagsMenuItem");

        // Multiple selections?
        int numSelected = parent.numSelected();
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

        parent.applySelected( (Photo i) -> {
            i.removeAllTags();
        });
    }

    @FXML private void onRemoveMenuItem(ActionEvent e) {
        System.out.println("Event: onRemoveMenuItem");
        parent.deletePhoto();
    }

    @FXML private void onOpenFilesMenuItem(ActionEvent e) {
        System.out.println("Event: onOpenFilesMenuItem");

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Images");
        List<File> list = chooser.showOpenMultipleDialog(getWindow());

        getDatabase().addFiles(list);

        parent.getMessageProperty().unbind();
        parent.getMessageProperty().set("Total = " + getDatabase().size() + " images.");
    }

    @FXML private void onSaveMenuItem(ActionEvent e) {
        System.out.println("Event: onSaveMenuItem");
        parent.save();
    }

    @FXML private void onFilterClearMenuItem(ActionEvent e) {
        // Unselect all menu items and reset DB view
        getDatabaseView().filterFavorites = false;
        filterFavoritesMenuItem.setSelected(false);

        getDatabaseView().filterMissing = false;
        filterMissingMenuItem.setSelected(false);

        getDatabaseView().filterOnlyDuplicated = false;
        filterDuplicatedMenuItem.setSelected(false);
        getDatabaseView().filterOnlyExtraCopies = false;
        filterExtraCopiesMenuItem.setSelected(false);
        getDatabaseView().filterHideExtraCopies = false;
        filterHideExtrasMenuItem.setSelected(false);

        getDatabaseView().filterTagged = false;
        filterTaggedMenuItem.setSelected(false);
        getDatabaseView().filterUntagged = false;
        filterUntaggedMenuItem.setSelected(false);

        getDatabaseView().update();
    }

    @FXML private void onFilterFavoritesMenuItem(ActionEvent e) {
        // Update DB view
        getDatabaseView().filterFavorites = filterFavoritesMenuItem.isSelected();
        getDatabaseView().update();
    }

    @FXML private void onFilterMissingMenuItem(ActionEvent e) {
        // Update DB view
        getDatabaseView().filterMissing = filterMissingMenuItem.isSelected();
        getDatabaseView().update();
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
        getDatabaseView().filterOnlyDuplicated  = filterDuplicatedMenuItem.isSelected();
        getDatabaseView().filterOnlyExtraCopies = filterExtraCopiesMenuItem.isSelected();
        getDatabaseView().filterHideExtraCopies = filterHideExtrasMenuItem.isSelected();
        getDatabaseView().update();
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
        getDatabaseView().filterTagged = filterTaggedMenuItem.isSelected();
        getDatabaseView().filterUntagged = filterUntaggedMenuItem.isSelected();
        getDatabaseView().update();
    }

    @FXML private void onFilterTaggedMenuItem(ActionEvent e) {
        doFilterTagsRadioGroup(e);
    }

    @FXML private void onFilterUntaggedMenuItem(ActionEvent e) {
        doFilterTagsRadioGroup(e);
    }

    // ----- Helpers -----

    public Database     getDatabase()     { return parent.getDatabase(); }
    public DatabaseView getDatabaseView() { return parent.getDatabaseView(); }

    private Window getWindow() {
        return mainMenuBar.getScene().getWindow();
    }
}
