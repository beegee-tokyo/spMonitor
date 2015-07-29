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
 * Solar Panel Monitor
 * loop
 * required by Arduino IDE
 *
 * Main program loop
 */
void loop() {
	eventTimer.update();

	/** Get clients coming from server */
	YunClient client = server.accept();

	/* There is a new client? */
	if ( client ) {
		/* read the command */
		char command = client.read();

		if ( command == 'c' ) { /* Set the CT calibration value e.g. c133 => value 33 for sensor 1 */
			command = client.read(); /* get the sensor number */
			double readCal = client.parseFloat();

			if ( readCal < 2000 ) {
				if ( command == '1' ) {
					calValue1 = readCal;
					emon1.current ( 0, calValue1 );
				}

				if ( command == '2' ) {
					calValue2 = readCal;
					emon2.current ( 0, calValue2 );
				}

				client.println ( readCal, 6 );
			}
		} else if ( command == 'e' ) { /* Get the current settings */
			client.println ( "Freq " + String ( messFreq ) );
			client.println ( "Calib1 " + String ( calValue1, 6 ) );
			client.println ( "Calib2 " + String ( calValue2, 6 ) );
		}

		/* Close connection and free resources. */
		client.stop();
	}
}

