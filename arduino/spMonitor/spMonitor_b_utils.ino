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
 * This function return a string with the time stamp
 *
 * thanks to https://www.arduino.cc/en/Tutorial/YunDatalogger
 *
 * @return *result
 *          Pointer to String with date and time
 *          format "yy,mm,dd,hh:mm,ss"
 */
String getTimeStamp() {
  /** String for date time to be returned */
  String result;
  /** Instance to Linino process */
  Process time;

  /* date is a command line utility to get the date and the time */
  /* in different formats depending on the additional parameter */
  time.begin ( "date" );
  time.addParameter ( "+%y,%m,%d,%H:%M,%S" );
  wdt_reset();
  time.run();  /* run the command */

  /* read the output of the command */
  while ( time.available() > 0 ) {
    /** Character for data returned from Linino */
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

  /** Light value collected since last saving */
  long light = 0;
  if ( collCount[2] != 0 ) {
    light = collLight / collCount[2];
  }
  /** Solar power value collected since last saving */
  double solar = collPower[0] / collCount[0];
  /** Consumed power value collected since last saving */
  double cons = collPower[1] / collCount[1];

  collEnergy[0] += solar / 60;
  collEnergy[1] += cons / 60;

  Bridge.put ( "L", String ( light ) );
  Bridge.put ( "S", String ( solar ) );
  Bridge.put ( "C", String ( cons ) );

  /* Write measurment to log file on SDcard */
  dataString = getTimeStamp().substring(0, 14);
  dataString += ",";
  dataString += String ( light );
  dataString += ",";
  dataString += String ( solar );
  dataString += ",";
  dataString += String ( cons );

  /** Instance to the log file on the SDcard */
  String fileName = "/mnt/sda1/";
  fileName += dataString.substring ( 0, 8 );
  fileName.replace(',', '-');
  fileName += ".txt";

  /** Pointer to log file */
  File dataFile = FileSystem.open ( fileName.c_str(), FILE_APPEND );

  if ( dataFile ) {
    dataFile.println ( dataString );
    dataFile.close();
  }

  if ( dataString.substring( 9, 14).equalsIgnoreCase("23:59") ) {
    collEnergy[0] = collEnergy[1] = 0.0;
  }

  /* Write current data into sqlite database */
  /** Instance to Linino process */
  //Process sqLite;
  //fileName = getTimeStamp();
  //dataString = "sqlite3 -line /mnt/sda1/s.db 'insert into s (d,s,c,l) Values ("
  //             + fileName + ","
  //             + String ( solar ) + ","
  //             + String ( cons ) + ","
  //             + String ( light ) + ");'";

  //sqLite.runShellCommand ( dataString );

  wdt_reset();
  // Emoncms configurations
  //  fileName = "emoncms.org";
  //  if (client.connect(fileName.c_str(), 80)) {
  // send the HTTP GET request:
  //    client.print("GET /api/post?apikey=e778b92fc1f06d7e94a94bcc2a969664&json={s:");
  //    client.print(solar);
  //    client.print(",c:");
  //    client.print(cons);
  //    client.print(",l:");
  //    client.print(light);
  //    client.println("} HTTP/1.1");
  //    client.println("Host:emoncms.org");
  //    client.println("User-Agent: Arduino-ethernet");
  //    client.println("Connection: close");
  //    client.println();
  //  }

  /* Send current data to emonCMS */
  /** Instance to Linino process */
  Process emonCMS;
  dataString = "curl \"http://emoncms.org/api/post?apikey=e778b92fc1f06d7e94a94bcc2a969664&json={s:";
  dataString += String ( solar );
  dataString += ",c:";
  dataString += String ( cons );
  dataString += ",l:";
  dataString += String ( light );
  dataString += "}\"";
  emonCMS.runShellCommand ( dataString );

  collPower[0] = collPower[1] = 0.0;
  collCount[0] = collCount[1] = collCount[2] = 0;
  collLight = 0;
}

/**
 * This function writes a debug info into /mnt/sda1/debug.txt
 *
 * @param debugMessage
 *      String with message to be written into debug file
 */
//void writeDebug(String message) {
  /** File name for debug info */
//  String fileName = "/mnt/sda1/debug.txt";
//
  /** Pointer to debug file */
//  File dataFile = FileSystem.open ( fileName.c_str(), FILE_APPEND );
//
//  if ( dataFile ) {
//    dataFile.println ( message );
//    dataFile.close();
//  }
//
//}

