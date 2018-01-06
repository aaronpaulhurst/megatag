package mainwindow;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

import javafx.collections.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import model.Database;
import model.Photo;

public class DatabaseView implements Observer {

    private Database db;

    ObservableList<Photo> rows = FXCollections.observableArrayList();

    public boolean filterFavorites = false;
    public boolean filterMissing = false;
    public boolean filterTagged = false;
    public boolean filterUntagged = false;
    public boolean filterOnlyDuplicated = false;
    public boolean filterOnlyExtraCopies = false;
    public boolean filterHideExtraCopies = false;
    public String filterTag = null;

    private Comparator<Photo> sort = null;
    private boolean ascendingSort = true;

    DatabaseView(Database db) {
        this.db = db;

        db.addObserver(this);
    }

    void bind(ListView<Photo> listView, ItemController controller) {
        assert(listView != null);

        listView.setItems(rows);

        listView.setCellFactory(new Callback<ListView<Photo>,
            ListCell<Photo>>() {
                @Override
                public ListCell<Photo> call(ListView<Photo> list) {
                    ItemView iv = new ItemView(controller);
                    return iv;
                }
            }
        );
    }

    private boolean isFiltered(Photo p) {
        // Apply filters
        if (filterFavorites && !p.isFavorite()) {
            return true;
        }
        if (filterTagged && p.getTags().isEmpty()) {
            return true;
        }
        if (filterUntagged && !p.getTags().isEmpty()) {
            return true;
        }
        if (filterTag != null && !p.getTags().contains(filterTag)) {
            return true;
        }
        if (filterOnlyDuplicated && (p.getDb().numDuplicates(p) <= 1)) {
            return true;
        }
        if (filterMissing && !p.isMissing()) {
            return true;
        }
        if (filterOnlyExtraCopies && p.getDb().isCanonicalCopy(p)) {
            return true;
        }
        if (filterHideExtraCopies && !p.getDb().isCanonicalCopy(p)) {
            return true;
        }
        return false;
    }

    public void setSort(Comparator<Photo> sort) {
        System.out.println("DatabaseView: changed sort");
        this.sort = sort;
    }
    public void setSortDirection(boolean ascending) {
        System.out.println("DatabaseView: changed sort");
        this.ascendingSort = ascending;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == db) {
            // Database changed.
            System.out.println("DatabaseView: notified of change");

            ArrayList<Photo> toAdd = new ArrayList<Photo>();

            for(Photo i : db.get()) {
                if (!isFiltered(i)) {
                    toAdd.add(i);
                }
            }

            if (sort != null) {
                toAdd.sort(sort);
            }
            if (!ascendingSort) {
                Collections.reverse(toAdd);
            }

            rows.clear();
            rows.addAll(toAdd);
        }
    }
}
