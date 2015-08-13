/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 * @author Bernd Giesecke
 * @version 0.1 beta August 13, 2015.
 */

/**
 * Solar Panel Monitor
 * loop
 * required by Arduino IDE
 *
 * Main program loop
 */
void loop() {
  /** Actual time in milliseconds since start of spMonitor */
  unsigned long now = millis();
  if ( now - lastMeasure >= 1000 ) { /* initiate measurement every 1 seconds */
    lastMeasure = now;
    wdt_reset();
    getMeasures();
  }

  if ( now - lastSave >= 60000 ) { /* Save data every minute */
    lastSave = now;
    wdt_reset();
    saveData();
  }

  //if ( now - lastReset >= 600000 ) { /* Reset every hour */
  /* Wait for watchdog reset */
  //  wdt_disable();
  //  wdt_enable(WDTO_15MS);
  //  delay (500);
  //}

  /** Get clients coming from server */
  client = server.accept();

  /* There is a new client? */
  if ( client ) {
    wdt_reset();
    /** Character holding the command that was sent */
    char command = client.read();

    /** Only for claibration needed */
    //if ( command == 'c' ) { /* Set the CT calibration value e.g. c16.060606 => value 6.060606 for sensor 1 */
    //  command = client.read(); /* get the sensor number */
        /** Calibration factor that was sent */
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
      client.println ( "C1 " + String ( iCal1, 6 ) );
      client.println ( "C2 " + String ( iCal2, 6 ) );
      client.println ( "V " + String ( vCal ) );
    }

    /* Close connection and free resources. */
    wdt_reset();
    client.stop();
  }
}

