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
  wdt_reset();
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
void saveData () {
  /** Sensor log to be written to SDcard */
  String dataString;

  long light = 0;
  if ( collCount[2] != 0 ) {
    light = collLight / collCount[2];
  }
  double solar = collPower[0] / collCount[0];
  double cons = collPower[1] / collCount[1];

  wdt_reset();
  Bridge.put ( "L", String ( light ) );
  Bridge.put ( "S", String ( solar ) );
  Bridge.put ( "C", String ( cons ) );

  wdt_reset();
  /* Write measurment to log file on SDcard */
  dataString = getTimeStamp();
  dataString += ",";
  dataString += String ( light );
  dataString += ",";
  dataString += String ( solar );
  dataString += ",";
  dataString += String ( cons );
  /** Instance to the log file on the SDcard */
  String fileName = "/mnt/sda1/";
  fileName += dataString.substring ( 0, 2 );
  fileName += "-";
  fileName += dataString.substring ( 3, 5 );
  fileName += "-";
  fileName += dataString.substring ( 6, 8 );
  fileName += "-datalog.txt";

  File dataFile = FileSystem.open ( fileName.c_str(), FILE_APPEND );

  wdt_reset();
  if ( dataFile ) {
    dataFile.println ( dataString );
    dataFile.close();
  }

  /* Write current data into sqlite database */
  Process sqLite;
  dataString = "sqlite3 -line /mnt/sda1/s.db 'insert into s (s,c,l) Values ("
               + String ( solar ) + ","
               + String ( cons ) + ","
               + String ( light ) + ");'";

  sqLite.runShellCommand ( dataString );

  wdt_reset();
  // Emoncms configurations
  //fileName = "emoncms.org";
  //if (client.connect(fileName.c_str(), 80)) {
    // send the HTTP GET request:
  //  client.print("GET /api/post?apikey=e778b92fc1f06d7e94a94bcc2a969664&json={s:");
  //  client.print(solar);
  //  client.print(",c:");
  //  client.print(cons);
  //  client.print(",l:");
  //  client.print(light);
  //  client.println("} HTTP/1.1");
  //  client.println("Host:emoncms.org");
  //  client.println("User-Agent: Arduino-ethernet");
  //  client.println("Connection: close");
  //  client.println();
  //}

  collPower[0] = collPower[1] = 0.0;
  collCount[0] = collCount[1] = collCount[2] = 0;
  collLight = 0;
}


