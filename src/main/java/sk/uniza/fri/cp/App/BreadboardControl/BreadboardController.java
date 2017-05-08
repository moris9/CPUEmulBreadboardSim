package sk.uniza.fri.cp.App.BreadboardControl;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.*;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Created by Moris on 4.3.2017.
 */

public class BreadboardController implements Initializable {

    //@FXML private BorderPane sceneRoot;
    //@FXML private HBox hbPicker;

    //súbor
    private File currentFile;

    @FXML private VBox root;
    @FXML private AnchorPane boardPane;

    @FXML private VBox toolsBox;
    @FXML private ColorPicker wireColorPicker;
    @FXML private SplitPane toolsSplitPane;
    private ItemPicker newItemPicker;
    private DescriptionPane descriptionPane;

    @FXML private Label lbCoordinates;
    @FXML
    private Label lbZoom;

    @FXML
    private ToggleSwitch tsPower;

    private Board board;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //tool box -> napravo - colorPicker, itemPicker, description
        this.wireColorPicker.setValue(Wire.getDefaultColor());
        this.wireColorPicker.setOnAction(event -> Wire.setDefaultColor(wireColorPicker.getValue()));

        this.descriptionPane = new DescriptionPane();

        this.newItemPicker = new ItemPicker();
        this.newItemPicker.setPanelForDescription(this.descriptionPane);
        registerItems();

        toolsSplitPane.getItems().addAll(this.newItemPicker, this.descriptionPane);

        board = new Board(2000, 2000, 10);
        board.setDescriptionPane(this.descriptionPane);
        board.setOnMouseMoved(event -> {
            Point2D gridPoint = board.getMousePositionOnGrid(event);
            lbCoordinates.setText(((int) gridPoint.getX()) + "x" + ((int) gridPoint.getY()));
        });

        board.zoomScaleProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                lbZoom.setText(((int) (newValue.doubleValue() * 100)) + "%");
            }
        });


        //ak sa zmeni stav simulacie, zmen aj tlacitko spustania simulacie
        board.simRunningProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != this.tsPower.isSelected()) {
                this.tsPower.setSelected(newValue);
            }
        });

        this.tsPower.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                board.powerOn();
            else
                board.powerOff();
        });

        this.boardPane.getChildren().add(board);

        AnchorPane.setTopAnchor(this.board, 0.0);
        AnchorPane.setRightAnchor(this.board, 0.0);
        AnchorPane.setBottomAnchor(this.board, 0.0);
        AnchorPane.setLeftAnchor(this.board, 0.0);
        this.board.setHvalue(0.5);
        this.board.setVvalue(0.5);
//        this.board.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
//        this.board.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    }

    public void callDelete(){
        this.board.deleteSelect();
    }

    public boolean powerOn() {
        if (this.board.isSimulationRunning()) {
            return true;
        }
        this.board.powerOn();
        return false;
    }

    private void registerItems(){
        this.newItemPicker.registerItem(new Gen7400());
        this.newItemPicker.registerItem(new Gen7402());
        this.newItemPicker.registerItem(new Gen7404());
        this.newItemPicker.registerItem(new Gen7408());
        this.newItemPicker.registerItem(new Gen7410());
        this.newItemPicker.registerItem(new Gen7430());
        this.newItemPicker.registerItem(new Gen7432());
        this.newItemPicker.registerItem(new Gen7486());
        this.newItemPicker.registerItem(new SN74125());
        this.newItemPicker.registerItem(new SN74138());
        this.newItemPicker.registerItem(new SN74148());
        this.newItemPicker.registerItem(new SN74151());
        this.newItemPicker.registerItem(new SN74153());
        this.newItemPicker.registerItem(new SN74164());
        this.newItemPicker.registerItem(new SN74573());
        this.newItemPicker.registerItem(new U6264B());

        this.newItemPicker.registerItem(new SchoolBreadboard());
        this.newItemPicker.registerItem(new Breadboard());
        this.newItemPicker.registerItem(new HexSegment());
        this.newItemPicker.registerItem(new NumKeys());
        this.newItemPicker.registerItem(new Probe());

    }

    @FXML
    private void handlePowerAction(){
        System.out.println("power");
        if (board.isSimulationRunning())
            board.powerOff();
        else
            board.powerOn();
    }

    @FXML
    private void handleSaveAction() {
        saveCircuit(false);
    }

    @FXML
    private void handleSaveAsAction() {
        saveCircuit(true);
    }

    @FXML
    private void handleLoadAction() {
        if (!continueIfUnsavedFile()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Načítať obvod...");
        chooser.setInitialDirectory(
                currentFile != null
                        ? currentFile.getParentFile()
                        : new File(Paths.get("").toAbsolutePath().toString()));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SCHX", "*.schx"),
                new FileChooser.ExtensionFilter("SCH", "*.sch"));

        File file = chooser.showOpenDialog(root.getScene().getWindow());

        if (file != null) {
            if (board.load(file)) {
                ((Stage) root.getScene().getWindow()).setTitle("Simulátor - " + file.getName());
                currentFile = file;
                board.clearChange();
            }
        }
    }

    @FXML
    private void handleClearBoardAction() {
        if (!continueIfUnsavedFile()) return;

        board.clearBoard();
        ((Stage) root.getScene().getWindow()).setTitle("Simulátor - Nový obvod");
        currentFile = null;
        board.clearChange();
    }

    /**
     * Výstraha pre užívateľa s otázkou na ďalší postup, ak aktuálny obvod nie je uložený.
     *
     * @return true - volajúca procedúra môže pokračovať, false - užívateľ nechce pokračovať
     */
    public boolean continueIfUnsavedFile() {
        if (!board.hasChanged()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potvrdenie");
        alert.setHeaderText("Zmeny vo vašom obovde neboli uložené");
        alert.setContentText("Prajete si uložiť zmeny?");

        ButtonType btnTypeSave = new ButtonType("Uložiť");
        ButtonType btnTypeSaveAs = new ButtonType("Uložiť ako");
        ButtonType btnTypeNo = new ButtonType("Nie");
        ButtonType btnTypeCancel = new ButtonType("Zrušiť");

        alert.getButtonTypes().clear();
        if (currentFile != null) alert.getButtonTypes().add(btnTypeSave);
        alert.getButtonTypes().addAll(btnTypeSaveAs, btnTypeNo, btnTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnTypeCancel) {
                return false;
            } else if (result.get() == btnTypeSaveAs) {
                return saveCircuit(true);
            } else if (result.get() == btnTypeSave) {
                return saveCircuit(false);
            }
        }

        return true;
    }

    private boolean saveCircuit(boolean saveAs) {
        File file = saveAs ? null : currentFile;

        if (currentFile == null || saveAs) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Uložiť obvod" + (saveAs ? " ako" : "") + "..");
            chooser.setInitialDirectory(new File(Paths.get("").toAbsolutePath().toString()));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SCHX", "*.schx"));

            if (currentFile != null)
                chooser.setInitialFileName(currentFile.getName());

            file = chooser.showSaveDialog(root.getScene().getWindow());
        }

        if (file != null) {
            if (!saveAs && currentFile != null && currentFile.getName().equals(file.getName())) {
                //opytanie sa ci chce naozaj prepisat subor
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Potvrdenie");
                alert.setHeaderText("Naozaj si prajete prepísať súbor " + file.getName() + "?");

                ButtonType btnTypeYes = new ButtonType("Áno");
                ButtonType btnTypeNo = new ButtonType("Nie");

                alert.getButtonTypes().clear();
                alert.getButtonTypes().addAll(btnTypeYes, btnTypeNo);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == btnTypeNo) return false;
            }

            if (board.save(file)) {
                board.clearChange();
                ((Stage) root.getScene().getWindow()).setTitle("Simulátor - " + file.getName());
                currentFile = file;
                return true;
            }
        }

        return false;
    }
}
