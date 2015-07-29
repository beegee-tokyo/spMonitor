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
 * This function return a string with the time stamp
 *
 * thanks to https://www.arduino.cc/en/Tutorial/YunDatalogger
 *
 *@return *result
 *          Pointer to array to be filled with date and time as String
 */
String getTimeStamp() {
	String result;
	Process time;
	/* date is a command line utility to get the date and the time */
	/* in different formats depending on the additional parameter */
	time.begin ( "date" );
	time.addParameter ( "+%y,%m,%d,%H:%M" );
	time.run();  /* run the command */

	/* read the output of the command */
	while ( time.available() > 0 ) {
		char c = time.read();

		if ( c != '\n' ) {
			result += c;
		}
	}

	return result;
}

/**
 * This function saves the average of 1 minute of measurements to the log file on the SDcard
 */
void saveData ( void *context ) {
	/** Sensor log to be written to SDcard */
	String dataString;

	/* Write measurment to log file on SDcard */
	dataString = getTimeStamp();
	dataString += ",";
	dataString += String ( collLight / collCountLight );
	dataString += ",";
	dataString += String ( sqrt ( collCurr1 / collCount1 ) );
	dataString += ",";
	dataString += String ( sqrt ( collCurr2 / collCount2 ) );

	double powerCollected = sqrt ( collCurr1 / collCount1 ) * 230;
	powerCollected = sqrt ( collCurr2 / collCount2 ) * 230;

	/** Instance to the log file on the SDcard */
	String fileName = "/mnt/sda1/" + dataString.substring ( 0, 2 ) + "-" + dataString.substring ( 3, 5 ) + "-" + dataString.substring ( 6, 8 ) + "-datalog.txt";
	File dataFile = FileSystem.open ( fileName.c_str(), FILE_APPEND );

	if ( dataFile ) {
		dataFile.println ( dataString );
		dataFile.close();
	}

	Process sqLite;
	dataString = "sqlite3 -line /mnt/sda1/s.db 'insert into s (s,c,l) Values ("
	             + String ( sqrt ( collCurr1 / collCount1 ) ) + ","
	             + String ( sqrt ( collCurr2 / collCount2 ) ) + ","
	             + String ( collLight / collCountLight ) + ");'";

	sqLite.runShellCommand ( dataString );

	collCurr1 = collCurr2 = 0.0;
	collCount1 = collCount2 = 0;
}

/**
* Get supply voltage of the Arduino board
*
* thanks to http://hacking.majenko.co.uk/making-accurate-adc-readings-on-arduino
* and Jérôme who alerted us to http://provideyourown.com/2012/secret-arduino-voltmeter-measure-battery-voltage/
*
*@return <code>long</code>
*          Supply voltage in mV
*/
long readVcc() {
	/** Current supply voltage */
	long result;

	ADMUX = _BV ( REFS0 ) | _BV ( MUX3 ) | _BV ( MUX2 ) | _BV ( MUX1 );

	delay ( 2 );                                     /* Wait for Vref to settle */
	ADCSRA |= _BV ( ADSC );                          /* Convert */

	while ( bit_is_set ( ADCSRA, ADSC ) );

	result = ADCL;
	result |= ADCH << 8;
	result = READVCC_CALIBRATION_CONST / result;  /* 1100mV*1024 ADC steps http://openenergymonitor.org/emon/node/1186 */
	return result;
}

