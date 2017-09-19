package mainwindow;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface ItemController {
    void onListItemMouseClick(MouseEvent e);
    void onFavoriteMouseClick(MouseEvent e, ItemView v);
    void onTagMouseClick(MouseEvent e, ItemView v);
}
