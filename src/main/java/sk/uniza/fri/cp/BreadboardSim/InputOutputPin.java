package sk.uniza.fri.cp.BreadboardSim;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class InputOutputPin extends OutputPin {

	public InputOutputPin(Device device){
		super(device, PinDriver.TRI_STATE);
	}

	public InputOutputPin(Device device, PinDriver pinDriver){
		super(device, pinDriver);
	}

}