package sk.uniza.fri.cp.App;

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakSetChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import org.fxmisc.richtext.CodeArea;

import java.util.function.IntFunction;

/**
 * TODO prist na lepsi sposob spravy breakpointov
 *
 * Created by Moris on 10.2.2017.
 */
public class BreakpointFactory implements IntFunction<Node> {

    private final String tagBreak = "break";
    private final CodeArea area;

    private ObservableSet<Integer> list;    //indexy paragrafov s breakami

    /**
     * Konstruktor faktorky na breakpointy v kode.
     * Metoda apply vracia symbol breakpointu pre paragraf.
     * Trieda sa stara aj o spravu breakpointov a aktualizaciu listu s indexami paragrafov obsahujucich breakpoint.
     * Pri kazdej zmene obsahu parametra area prebieha update listu breakpointov.
     *
     * @param area CodeArea kodu, kam sa pridavaju preakpointy
     * @param list Zoznam indexov paragrafov, na ktorych je nastaveny breakpoint
     */
    public BreakpointFactory(CodeArea area, ObservableSet<Integer> list){
        this.area = area;
        this.list = list;

        //zavolaj pri pridani / odstraneni / vymazani obsahu riadku
        area.plainTextChanges()
                .filter(ptc -> ptc.getRemoved().contains("\n")
                        || ptc.getInserted().contains("\n")
                        || area.getParagraph(area.getCurrentParagraph()).getText().trim().isEmpty())
                .subscribe((a) -> updateBreakpointsListByParagraphs());

        //listener volany pri vycisteni vsetkych breakpointov
        list.addListener(new SetChangeListener<Integer>() {
            @Override
            public void onChanged(Change<? extends Integer> change) {
                if(change.wasRemoved() && list.isEmpty()){
                    clearBreakpoints();
                }
            }
        });
    }

    @Override
    public Node apply(int value) {
        Circle circ = new Circle(5);
        circ.setFill(Color.RED);
        if(!list.contains(value))
            circ.setVisible(false);

        StackPane pane = new StackPane();
        StackPane.setAlignment(circ, Pos.CENTER);
        pane.setPrefSize(20,20);
        pane.getChildren().add(circ);
        pane.setCursor(Cursor.HAND);

        //po kliknuti na panel s breakpointom
        pane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(circ.isVisible()) { //odstranenie breakpointu
                    //odober styl paragrafu
                    RichTextFXHelpers.tryRemoveParagraphStyle(area, value, tagBreak);

                    //odober break z listu
                    list.remove(value);
                }
                else { //zavedenie breakpointu
                    //pridaj breakpoint tag k stylom paragrafu
                    RichTextFXHelpers.addParagraphStyle(area , value, tagBreak);

                    //pridaj paragraf do listu
                    list.add(value);
                }
            }
        });

        //listener na list indexov, pri jeho zmene sa zobrazi / neozbrazi zn. breakpointu
        list.addListener(
                new WeakSetChangeListener<Integer>(
                        change -> {
                            if(list.contains(value))
                                circ.setVisible(true);
                            else
                                circ.setVisible(false);
                        }));

        return pane;
    }

    /**
     * Aktualizacia listu breakpointov na zaklade stylu paragrafov v editore kodu
     */
    private void updateBreakpointsListByParagraphs(){
        //pomocne cislo aby sa nezmazali vsetky breaky
        // (ak je iba jeden, pri jeho presuvani sa zavola clearBreakpoints lebo list je prazdny)
        list.add(-1);

        //prehladaj kazdy paragraph
        for (int i = 0; i < area.getParagraphs().size(); i++) {
            //ak obsahuje styl pre break, pridaj jeho index do listu
            if( area.getParagraph(i).getParagraphStyle().stream().anyMatch(style -> style.equals(tagBreak)) ) {
                list.add(i);
            } else {
                list.remove(i);
            }
        }

        //odstranenie pomocnej
        list.remove(-1);
    }

    /**
     * Zmazanie stylov paragrafov pri vymazani vestkych breakpointov
     */
    private void clearBreakpoints(){
        //prejdi kazdy paragraph
        for (int i = 0; i < area.getParagraphs().size(); i++) {
            //ak obsahuje break, odober ho
            if(area.getParagraph(i).getParagraphStyle().stream().anyMatch(style -> style.equals(tagBreak))){
                RichTextFXHelpers.tryRemoveParagraphStyle(area, i, tagBreak);
            }
        }
    }
}
