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
 * Initialize serial port
 * Initialize timers for LED and analog readings
 * Initialize sensor interfaces
 * Initialize communication
 */
void setup() {
	/** enable serial port for debug messages with Serial.println() */
	Serial.begin ( 9600 );

	/** set pin to output */
	pinMode ( activityLED, OUTPUT );
	pinMode ( accessLED, OUTPUT );

	/** Initiate call of getMeasures every 5 seconds */
	measureEvent = eventTimer.every ( messFreq, getMeasures, ( void * ) 0 );
	/** Initiate call of saveMeasures every 60 seconds */
	saveEvent = eventTimer.every ( saveFreq, saveData, ( void * ) 0 );

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
	} else {
		errorEvent = eventTimer.oscillate ( accessLED, 500, HIGH );
	}

	/** Configure the YHDC SCT013-000 current sensors */
	/* Initialise the current sensor 1 */
	emon1.current ( 0, calValue1 );
	/* Initialise the current sensor 2 */
	emon2.current ( 1, calValue2 );

	/* Get initial reading to setup the low pass filter */
	for ( unsigned int i = 0; i < 10; i++ ) {
		/* LED on */
		digitalWrite ( activityLED, HIGH );
		measuredCurr1 = emon1.calcIrms ( 1480 );
		measuredCurr2 = emon2.calcIrms ( 1480 );
		/* LED off */
		digitalWrite ( activityLED, LOW );
	}
}

