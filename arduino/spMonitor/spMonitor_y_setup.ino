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
 * setup
 * required by Arduino IDE
 *
 * Initialize timers for LED and analog readings
 * Initialize sensor interfaces
 * Initialize communication
 */
void setup() {
  /** set pin to output */
  pinMode ( activityLED, OUTPUT );

  /** Initialize bridge connection */
  Bridge.begin();
  /* Listen for incoming connection only from localhost */
  /* (no one from the external network could connect) */
  server.listenOnLocalhost();
  server.begin();

  /** Initialize access to SDcard */
  FileSystem.begin();

  /** Configure the Adafruit TSL2561 light sensor */
  /* Initialise the sensor */
  if ( tsl.begin() ) {
    /* Setup the sensor gain and integration time */
    configureSensor();
  }

  /** Preset values for current sensing */
  iCal[0] = iCalVal1;
  /** Currently used current calibration value for current sensor 2 */
  iCal[1] = iCalVal2;
  collPower[0] = collPower[1] = 0.0;
  collCount[0] = collCount[1] = collCount[2] = 0;

  /** Configure the YHDC SCT013-000 current sensors */
  /* Initialise the current sensor 1 */
  emon[0].voltage( 2, vCal, 1.3 );
  emon[0].current ( 0, iCal[0] );
  /* Initialise the current sensor 2 */
  emon[1].voltage( 2, vCal, 2 );
  emon[1].current ( 1, iCal[1] );

  /* Get initial reading to setup the low pass filter */
  unsigned int i = 0;
  while (i<20) {
    /* LED on */
    digitalWrite ( activityLED, HIGH );
    emon[0].calcVI ( 20, 2000 );
    emon[1].calcVI ( 20, 2000 );
    /* LED off */
    digitalWrite ( activityLED, LOW );
    i++;
  }

  /* For debug only */
  //String fileName = "/mnt/sda1/debug.txt";

  //File dataFile = FileSystem.open ( fileName.c_str(), FILE_APPEND );

  //if ( dataFile ) {
  //  dataFile.println ( "Restart\n" );
  //  dataFile.close();
  //}
  /* End of For debug only */

  /** Initiate call of getMeasures and saveData every 5 seconds / 60 seconds */
  lastMeasure = lastSave = lastReset = millis();

  /** Activate the watchdog */
  wdt_enable(WDTO_8S);
}

