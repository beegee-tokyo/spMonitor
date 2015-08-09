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
  wdt_reset();
  unsigned long now = millis();
  if ( now - lastMeasure >= 5000 ) { /* initiate measurement every 5 seconds */
    lastMeasure = now;
    wdt_reset();
    getMeasures();
  }

  if ( now - lastSave >= 60000 ) { /* Save data every minute */
    lastSave = now;
    wdt_reset();
    saveData();
  }

  if ( now - lastReset >= 600000 ) { /* Reset every hour */
    /* Wait for watchdog reset */
    wdt_disable();
    wdt_enable(WDTO_15MS);
    delay (500);
  }
  
  /** Get clients coming from server */
  client = server.accept();

  /* There is a new client? */
  if ( client ) {
    /* read the command */
    char command = client.read();

    /** Only for claibration needed */
    //if ( command == 'c' ) { /* Set the CT calibration value e.g. c16.060606 => value 6.060606 for sensor 1 */
    //  command = client.read(); /* get the sensor number */
    //  double readCal = client.parseFloat();

    //  if ( command == '1' ) {
    //    iCal[0] = readCal;
    //    emon[0].current ( 0, iCal[0] );
    //  }

    //  if ( command == '2' ) {
    //    iCal[1] = readCal;
    //    emon[1].current ( 1, iCal[1] );
    //  }

    //  client.println ( readCal, 6 );
    //} else
    if ( command == 'e' ) { /* Get the actual settings */
      client.println ( "F 3s" );
      client.println ( "C1 " + String ( iCal[0], 6 ) );
      client.println ( "C2 " + String ( iCal[1], 6 ) );
      client.println ( "V " + String ( vCal, 6 ) );
    }

    /* Close connection and free resources. */
    wdt_reset();
    client.stop();
  }
}

