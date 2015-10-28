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
 * This function return a string with the time stamp
 *
 * thanks to https://www.arduino.cc/en/Tutorial/YunDatalogger
 *
 *@return *result
 *          Pointer to String with date and time
 *          format "yy,mm,dd,hh:mm"
 */
String getTimeStamp() {
  /** String for date time to be returned */
  String result;
  /** Instance to Linino process */
  Process time;

  /* date is a command line utility to get the date and the time */
  /* in different formats depending on the additional parameter */
  time.begin ( "date" );
  time.addParameter ( "+%y,%m,%d,%H:%M" );
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
  /** Sensor log to be saved */
  String dataString;
  /** Time stamp of sensor log */
  String timeString;

  /** Light value collected since last saving */
  long light = 0;
  if ( collCount[2] != 0 ) {
    light = collLight / collCount[2];
  }
  /** Solar power value collected since last saving */
  double solar = collPower[0] / collCount[0];
  /** Consumed power value collected since last saving */
  double cons = collPower[1] / collCount[1];

  Bridge.put ( "L", String ( light ) );
  Bridge.put ( "S", String ( solar ) );
  Bridge.put ( "C", String ( cons ) );

  /* Write current data into sqlite database */
  /** Instance to Linino process */
  Process dataSave;
  timeString = getTimeStamp();
  timeString.replace(',', '-');
  /** Month as integer */
  int nowMonth = timeString.substring(3, 5).toInt();

  /* Check for month change or new start of app */
  if (thisMonth != nowMonth) {
    // new start of app or new month started
    thisMonth = nowMonth;
    // create new database for the new month
    dataString = "sqlite3 /mnt/sda1/"
                 + timeString.substring(0, 5)
                 + ".db < /mnt/sda1/create.sql";
    dataSave.runShellCommand ( dataString );
  }

  dataString = "sqlite3 /mnt/sda1/"
               + timeString.substring(0, 5)
               + ".db 'insert into s (d,s,c,l) Values (\""
               + timeString + "\","
               + String ( solar ) + ","
               + String ( cons ) + ","
               + String ( light ) + ");' &";

  dataSave.runShellCommand ( dataString );

//  dataString = "sqlite3 /mnt/sda1/s.db 'insert into s (d,s,c,l) Values (\""
//               + timeString + "\","
//               + String ( solar ) + ","
//               + String ( cons ) + ","
//               + String ( light ) + ");' &";

//  dataSave.runShellCommand ( dataString );

  wdt_reset();
  /* Send current data to emonCMS */
  dataString = "curl \"http://emoncms.org/api/post?apikey=e778b92fc1f06d7e94a94bcc2a969664&json={s:";
  dataString += String ( solar );
  dataString += ",c:";
  dataString += String ( cons );
  dataString += ",l:";
  dataString += String ( light );
  dataString += "}\" &";
  dataSave.runShellCommand ( dataString );

  /* Send current data to mySQL database */
//  dataString = "curl \"http://desire.giesecke.tk/s/i.php?d=";
//  dataString += timeString + "&s=";
//  dataString += String ( solar );
//  dataString += "&c=";
//  dataString += String ( cons );
//  dataString += "&l=";
//  dataString += String ( light );
//  dataString += "\" &";
//  dataSave.runShellCommand ( dataString );

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

