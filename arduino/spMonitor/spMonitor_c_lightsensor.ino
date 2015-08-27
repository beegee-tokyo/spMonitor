/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 *@author Bernd Giesecke
 *@version 0.2 beta August 19, 2015.
 */

/**************************************************************************/
/*
 *  Adafruit sensor routines taken from "sensorapi.pde"
 */
/**************************************************************************/
/**
 * Configures the gain and integration time for the TSL2561
 */
void configureSensor () {
  /* You can also manually set the gain or enable auto-gain support */
  tsl.enableAutoRange ( true );         /* Auto-gain ... switches automatically between 1x and 16x */

  /* Changing the integration time gives you better sensor resolution (402ms = 16-bit data) */
  tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_402MS ); /* 16-bit data but slowest conversions */
}

/**
 * Get current light measurement.
 * Function makes 5 measurements and returns the average value.
 * Function adapts integration time in case of sensor overload
 *
 * Result is stored in global variable sunLux
 */
void readLux () {
  /** Accumulated sensor values */
  long accLux = 0;
  /** Sensor event reads value from the sensor */
  sensors_event_t event;

  /** Counter for successfull readings, used to adjust the integration time */
  int lightOk = 0; /* In case of saturation we retry 5 times */

  for ( int i = 0; i < 5; i++ ) {
    wdt_reset();
    tsl.getEvent ( &event );

    /* Display the results (light is measured in lux) */
    if ( event.light ) {
      /** Int value read from AD conv for sun measurement */
      accLux += event.light;
      lightOk++; /* Increase counter of successful measurements */

      if ( lightInteg == 1 ) { /* we are at medium integration time, try a higher one */
        tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_402MS ); /* 16-bit data but slowest conversions */
        				/* Test new integration time */
        				tsl.getEvent ( &event );
        
        				if ( event.light == 0 ) {
        					/* Satured, switch back to medium integration time */
        					tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_101MS ); /* medium resolution and speed   */
        				} else {
                            lightInteg = 2;
        				}
      } else if ( lightInteg == 0 ) { /* we are at lowest integration time, try a higher one */
        tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_101MS ); /* medium resolution and speed   */
        				/* Test new integration time */
        				tsl.getEvent ( &event );
        
        				if ( event.light == 0 ) {
        					/* Satured, switch back to low integration time */
        					tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_13MS ); /* fast but low resolution */
        				} else {
                            lightInteg = 1;
        				}
      }
    } else {
      /* If event.light = 0 lux the sensor is probably saturated
                                               and no reliable data could be generated! */
      if ( lightInteg == 2 ) { /* we are at highest integration time, try a lower one */
        tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_101MS ); /* medium resolution and speed   */
        wdt_reset();
        tsl.getEvent ( &event );

        if ( event.light == 0 ) { /* Still saturated? */
          lightInteg = 0;
          tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_13MS ); /* fast but low resolution */
          wdt_reset();
          tsl.getEvent ( &event );

          if ( event.light != 0 ) { /* Got a result now? */
            accLux += event.light;
            lightOk++; /* Increase counter of successful measurements */
          }
        } else {
          lightInteg = 1;
          accLux += event.light;
          lightOk++; /* Increase counter of successful measurements */
        }
      } else if ( lightInteg == 1 ) { /* we are at medium integration time, try a lower one */
        lightInteg = 0;
        tsl.setIntegrationTime ( TSL2561_INTEGRATIONTIME_13MS ); /* fast but low resolution */
        wdt_reset();
        tsl.getEvent ( &event );
        if ( event.light != 0 ) { /* Got a result now? */
          accLux += event.light;
          lightOk++; /* Increase counter of successful measurements */
        }
      }
    }
  }

  if ( lightOk != 0 ) {
    collLight = collLight + (accLux / lightOk);
    collCount[2] += 1;
    Bridge.put ( "l", String ( accLux / lightOk ) );
  } else {
    Bridge.put ( "l", "0" );
  }
}

