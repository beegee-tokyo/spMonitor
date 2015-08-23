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

/**
 * Puts values over the bridge for easy access from external
 *
 *@param index
 *          Index for sensor to read
 *          0 = solar CT
 *          1 = mains CT
 */
void getCTValues (int index) {
  /* Get the measured current from the solar panel */
  emon[index].calcVI(20, 2000);

  /** Measured power in W */
  double power = emon[index].realPower;
  /** String for prefix */
  String prefix = "c";
  if (index == 0) {
    prefix = "s";
    /** Sensor 1 is measuring the solar panel, if it is less than 20W then mostlikely that is the standby current drawn by the inverters */
    if ( emon[index].Irms < 0.55 ) {
      power = 0.0;
    }
  }
  collPower[index] = collPower[index] + power;
  collCount[index] += 1;

  Bridge.put ( prefix, String ( emon[index].Irms ) );
  Bridge.put ( prefix + "r", String ( power )  );
  Bridge.put ( prefix + "v", String ( emon[index].Vrms ) );
  Bridge.put ( prefix + "a", String ( emon[index].apparentPower ) );
  Bridge.put ( prefix + "p", String ( emon[index].powerFactor ) );
}

