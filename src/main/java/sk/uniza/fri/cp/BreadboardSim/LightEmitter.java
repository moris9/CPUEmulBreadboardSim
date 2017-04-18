package sk.uniza.fri.cp.BreadboardSim;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Je mozne ho riadit iba pomocou jednej simulacie z jedneho boardu!
 * potrebne mazanie cez delete
 * <p>
 * Created by Moris on 16.4.2017.
 */
public class LightEmitter {

    //Board podla ktoreho sa spusta/zastavuje simulacia
    private static Board board;
    //"skrtiace" vlakno RunLater poziadaviek
    private static Thread thread;
    //pocitadlo pre pristup k RunLater
    private AtomicLong counter = new AtomicLong(-1);
    //vytvorene instancie emittorov
    private static final ConcurrentLinkedQueue<LightEmitter> instances = new ConcurrentLinkedQueue<>();

    private final Shape shape;
    private final Color col_turnedOn;
    private final Color col_turnedOff;
    private final AtomicInteger turnedOn = new AtomicInteger(0); //kolko krat bol zapnuty od poslednej aktualizacie
    private final AtomicBoolean state;
    private final int minUpdateDelayMs;
    private long lastUpdate = 0;

    public LightEmitter(Board paBoard, Shape shape, Color colorTurnedOn, Color colorTurnedOff, int minUpdateDelayMs) {
        this.shape = shape;
        this.col_turnedOn = colorTurnedOn;
        this.col_turnedOff = colorTurnedOff;
        this.minUpdateDelayMs = minUpdateDelayMs;
        this.state = new AtomicBoolean(false);

        instances.add(this);

        if (board == null) {
            board = paBoard;
            board.simRunningProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        counter.set(-1);

                        thread = new Thread(() -> {
                            long count = 0;
                            while (!Thread.currentThread().isInterrupted()) {
                                count++;
                                if (counter.getAndSet(count) == -1) {
                                    updateUI(counter);
                                }
                            }

                            counter.set(-1); //oznam o ukonceni
                            updateUI(counter); //posledny update -> vypnutie
                        });

                        thread.setDaemon(true);
                        thread.start();
                    } else {
                        if (thread != null) thread.interrupt();
                    }
                }
            });
        }
    }

    public LightEmitter(Board paBoard, Shape shape, Color colorTurnedOn, Color colorTurnedOff) {
        this(paBoard, shape, colorTurnedOn, colorTurnedOff, 0);
    }

    public void turnOn() {
        turnedOn.getAndIncrement();
        state.set(true);
    }

    public void turnOff() {
        state.set(false);
    }

    public boolean getState() {
        return state.get();
    }

    public void delete() {
        instances.remove(this);
        if (instances.size() == 0) board = null;
    }

    private static void updateUI(final AtomicLong counter) {

        Platform.runLater(() -> {
            //pre kazdy emitter na ploche
            for (LightEmitter emitter :
                    instances) {


                if ((System.currentTimeMillis() - emitter.lastUpdate > emitter.minUpdateDelayMs)) {
                    //ak je cas od posledneho update-u vacsi ako minimalny nastaveny

                    if (emitter.minUpdateDelayMs > 0 && emitter.turnedOn.get() > 2) {
                        //ak ma emitter nastaveny minimalny update a bol zopnuty viac ako dva krat za dany cas, zapni ho
                        emitter.shape.setFill(emitter.col_turnedOn);
                    } else {
                        //inak sa riad posla akutalne nastavenej hodnoty
                        if (emitter.state.get()) {
                            emitter.shape.setFill(emitter.col_turnedOn);
                        } else {
                            emitter.shape.setFill(emitter.col_turnedOff);
                        }
                    }

                    emitter.turnedOn.set(0);
                    emitter.lastUpdate = System.currentTimeMillis();
                } else if (counter.get() == -1) {
                    //prikaz na ukoncenie -> vypnutie
                    emitter.shape.setFill(emitter.col_turnedOff);
                }
            }

            counter.set(-1);
        });

    }
}