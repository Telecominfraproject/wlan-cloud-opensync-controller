package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.equipment.LEDColour;

public class CEGWBlinkRequest extends CustomerEquipmentCommand {
	 /**
	 * 
	 */
	private static final long serialVersionUID = 3464950479960821571L;
	private int numCycles;
	    private int colour1DurationMs;
	    private int colour2DurationMs;
	    private LEDColour colour1;
	    private LEDColour colour2;

	    /**
	     * Constructor
	     * 
	     * @param equipmentQRCode
	     * @param equipmentId
	     */
	    public CEGWBlinkRequest(String equipmentQRCode, long equipmentId) {
	        super(CEGWCommandType.BlinkRequest, equipmentQRCode, equipmentId);
	    }

	    /**
	     * Constructor used by JSON
	     */
	    public CEGWBlinkRequest() {
	        super(CEGWCommandType.BlinkRequest, null, 0);
	    }

	    public int getNumCycles() {
	        return numCycles;
	    }

	    public void setNumCycles(int numCycles) {
	        this.numCycles = numCycles;
	    }

	    public int getColour1DurationMs() {
	        return colour1DurationMs;
	    }

	    public void setColour1DurationMs(int colourDurationMs) {
	        this.colour1DurationMs = colourDurationMs;
	    }

	    public int getColour2DurationMs() {
	        return colour2DurationMs;
	    }

	    public void setColour2DurationMs(int colourDurationMs) {
	        this.colour2DurationMs = colourDurationMs;
	    }

	    public LEDColour getColour1() {
	        return colour1;
	    }

	    public void setColour1(LEDColour colour1) {
	        this.colour1 = colour1;
	    }

	    public LEDColour getColour2() {
	        return colour2;
	    }

	    public void setColour2(LEDColour colour2) {
	        this.colour2 = colour2;
	    }
	    
	    @Override
	    public boolean hasUnsupportedValue() {
	        if (super.hasUnsupportedValue()) {
	            return true;
	        }
	        if (LEDColour.isUnsupported(colour1) || LEDColour.isUnsupported(colour2)) {
	            return true;
	        }
	        return false;
	    }
}
