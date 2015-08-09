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
void getMeasures () {
  /* Activity LED on */
  digitalWrite ( activityLED, HIGH );

  wdt_reset();
  /* Get the light measurement if a sensor is attached */
  //readLux();

  wdt_reset();
  /* Get the measured current from the solar panel */
  /** Measured current from current sensor 1 */
  emon[0].calcVI(20, 2000);

  /** Sensor 1 is measuring the solar panel, if it is less than 20W then mostlikely that is the standby current drawn by the inverters */
  if ( emon[0].Irms < 0.5 ) {
    collPower[0] = collPower[0] + 0;
    Bridge.put ( "sr", "0" );
  } else {
    collPower[0] = collPower[0] + emon[0].realPower;
    Bridge.put ( "sr", String ( emon[0].realPower ) );
  }
  collCount[0] += 1;

  Bridge.put ( "s", String ( emon[0].Irms ) );
  Bridge.put ( "sv", String ( emon[0].Vrms ) );
  Bridge.put ( "sa", String ( emon[0].apparentPower ) );
  Bridge.put ( "sp", String ( emon[0].powerFactor ) );

  wdt_reset();
  /** Get the measured current from sensor 2 which can be attached to anything */
  /** Measured current from current sensor 2 */
  emon[1].calcVI(20, 2000);

  collPower[1] = collPower[1] + emon[1].realPower;
  collCount[1] += 1;

  Bridge.put ( "c", String ( emon[1].Irms ) );
  Bridge.put ( "cr", String ( emon[1].realPower ) );
  Bridge.put ( "cv", String ( emon[1].Vrms ) );
  Bridge.put ( "ca", String ( emon[1].apparentPower ) );
  Bridge.put ( "cp", String ( emon[1].powerFactor ) );

  /* Activity LED off */
  digitalWrite ( activityLED, LOW );
}


