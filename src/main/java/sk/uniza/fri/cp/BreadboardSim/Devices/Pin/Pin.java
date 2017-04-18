package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public abstract class Pin extends Circle {

	/** stavy pinu na cipe
	 * HIGH - +5V
	 * LOW - 0V
	 * HIGH_IMPEDANCE - kvazi NOT_CONNECTED, pin nema vplyv na potencial
	 * NOT_CONNECTED - nepripojene piny a vsetky piny ak cip nie je napajany
	**/
	public enum PinState { HIGH, LOW, HIGH_IMPEDANCE, NOT_CONNECTED	}

	/** typy budicov vystupnych pinov
	 * PUSH_PULL - klasicky vystup, pri napojeni vysupv +5V a GND nedefinovana hodnota
	 * TRI_STATE - pridáva tretí stav vysokej impedancie, kedy sa "odpája" od obvodu a nemá naň vplyv -> InputOutputPin
	 */
    // OPEN_COLLECTOR - pin pri hodnote LOW stiahne celu vetvu do LOW, pri HIGH je kvazi "odpojeny"
    public enum PinDriver {
        PUSH_PULL, TRI_STATE /*, OPEN_COLLECTOR*/
    }

	private Socket socket;
	private Device device;
	private PinState state = PinState.NOT_CONNECTED;

    private String name = "UNNAMED " + this.getClass().getSimpleName();

	public Pin(Device device){
		this.device = device;

		//"grafika" pre vytvorenie boundary na koliziu so soketmi
		this.setRadius(1);
		this.setFill(Color.RED);
	}

	public Pin(Device device, String name){
		this(device);

		this.name = name;
	}

	public Socket getSocket(){
		return socket;
	}

	public PinState getState(){
		return state;
	}

	/**
	 * Nastavenie stavu vystupneho pinu zmeni aj potencial pripojeneho soketu
	 * @param state Novy stav pinu - High, Low, High Impedance, Not connected
	 */
	public void setState(PinState state){
		this.state = state;
/*
		switch (state){
			case HIGH: this.socket.setPotential(Potential.Value.HIGH);
			break;
			case LOW: this.socket.setPotential(Potential.Value.LOW);
			break;
			default: this.socket.setPotential(Potential.Value.NC);
		}*/
	}

	/**
	 * Aktualizacia vstupneho stavu pinu na zaklade hodnoty potencialu pripojeneho soketu
	 */
	public void updateState(){
		//if(this instanceof InputPin || (this instanceof InputOutputPin && this.state == PinState.HIGH_IMPEDANCE)) {
			Potential.Value value = this.socket.getPotential().getValue();

			switch (value) {
				case HIGH:
					this.state = PinState.HIGH;
					break;
				case LOW:
					this.state = PinState.LOW;
					break;
				default:
					this.state = PinState.NOT_CONNECTED;
			}
		//}
	}

	public void setSocket(Socket socket){
		this.socket = socket;
		this.setFill(Color.GREEN);
	}

	public void removeSocket(){
		this.socket = null;
	}

	public Device getDevice(){
		return device;
	}

	public boolean isConnected(){
		return this.socket != null;
	}

	public void disconnect(){
		if(this.socket != null) this.socket.disconnect();
		this.setFill(Color.RED);
	}

    public String getName() {
        return this.name;
    }
}