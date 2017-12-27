package mainwindow;

import javafx.event.ActionEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface ItemController {
    void onListItemMouseClick(MouseEvent e);
    void onFavoriteMouseClick(MouseEvent e, ItemView v);
    void onTagMouseClick(MouseEvent e, ItemView v);
    void onAddCaptionMousePressed(MouseEvent e, ItemView v);
    void onAddCaptionTextField(ActionEvent e, ItemView v);
}
