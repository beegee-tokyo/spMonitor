/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 * @author Bernd Giesecke
 * @version 0.1 July 02, 2015.
 */

/**
 * Called every minute by "eventTimer"
 * Reads values from analog input 0 (current produced by solar panel)
 * and analog input 3 (measured luminosity)
 *
 *@param context
 *          Pointer to application context
 */
void getMeasures ( void *context ) {
	/* Activity LED on */
	digitalWrite ( activityLED, HIGH );

	/* Get the light measurement if a sensor is attached */
	readLux();
	collLight = collLight + sunLux;
	collCountLight += 1;

	Bridge.put ( "Light", String ( sunLux ) );

	/* Get the measured current from the solar panel */
	/** Measured current from current sensor 1 */
	measuredCurr1 = emon1.calcIrms ( 1480 );

	/** Sensor 1 is measuring the solar panel, any value above 9A is nonsense */
	if ( measuredCurr1 > 9.0 ) {
		measuredCurr1 = prevCurr1;
	} else {
		collCurr1 = collCurr1 + ( measuredCurr1 * measuredCurr1 );
		collCount1 += 1;
	}

	prevCurr1 = measuredCurr1;

	Bridge.put ( "Curr1", String ( measuredCurr1 ) );

	/** Get the measured current from sensor 2 which can be attached to anything */
	/** Measured current from current sensor 2 */
	measuredCurr2 = emon2.calcIrms ( 1480 );

	/** Sensor 2 can measuring max 50A, any value above is nonsense */
	if ( measuredCurr2 > 50.0 ) {
		measuredCurr2 = prevCurr2;
	} else {
		collCurr2 = collCurr2 + ( measuredCurr2 * measuredCurr2 );
		collCurr2 += measuredCurr2;
		collCount2 += 1;
	}

	prevCurr2 = measuredCurr2;

	Bridge.put ( "Curr2", String ( measuredCurr2 ) );

	/* Activity LED off */
	digitalWrite ( activityLED, LOW );
}

