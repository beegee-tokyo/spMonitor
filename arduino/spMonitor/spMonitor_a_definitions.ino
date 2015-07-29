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

#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_TSL2561_U.h>
#include <pgmspace.h>

#include <EmonLib.h>

#include <Timer.h>

#include <FileIO.h>

#include <YunServer.h>
#include <YunClient.h>

#define READVCC_CALIBRATION_CONST 1126400L

/* Enable this to debug with Serial.print outputs */
/*#define DEBUG */

/** Timer for measurements */
Timer eventTimer;
/** Pointer to timer for measurements */
int measureEvent = 0;
/** Pointer to timer for save records */
int saveEvent = 0;
/** Pointer to timer for error */
int errorEvent = 0;

/** Constant value for activity LED port */
#define activityLED 8
/** Constant value for error & access LED port */
#define accessLED 13

/** Flag for write to debug log */
boolean isDebug = false;
/** Counter for write to debug log */
int debugWriteCount = 0;

/** Measurement frequence in ms */
unsigned int messFreq = 5000;
/** Recording frequence in ms */
unsigned int saveFreq = 60000;

/** Instance of the Adafruit TSL2561 sensor */
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified ( TSL2561_ADDR_FLOAT, 12345 );
/** Currently used integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms 3 = no sensor available */
int lightInteg = 2;
/** Int value read from AD conv for sun measurement */
unsigned int sunLux;
/** Collector for average per minute light sensor */
long collLight = 0;
/** Counter for measurements per minute from current sensor 1 */
int collCountLight = 0;

/** Create an instance to emon for sensor 1*/
EnergyMonitor emon1;
/** Create an instance to emon for sensor 2*/
EnergyMonitor emon2;
/** Currently used calibration value for sensor 1 */
double calValue1 = 6.060606;
/** Currently used calibration value for sensor 2 */
double calValue2 = 12.195121;
/** Measured current from CT current sensor 1 */
double measuredCurr1;
/** Measured current from CT current sensor 2 */
double measuredCurr2;
/** Previous measurement result from current sensor 1 */
double prevCurr1;
/** Previous measurement result from current sensor 2 */
double prevCurr2;
/** Collector for average per minute sensor 1 */
double collCurr1 = 0.0;
/** Collector for average per minute sensor 2 */
double collCurr2 = 0.0;
/** Counter for measurements per minute from current sensor 1 */
int collCount1 = 0;
/** Counter for measurements per minute from current sensor 2 */
int collCount2 = 0;

/** Listen to the default port 5555, the Yún webserver
   will forward there all the HTTP requests you send */
YunServer server;

