/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */

#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_TSL2561_U.h>
#include <pgmspace.h>

#include <EmonLib.h>

#include <FileIO.h>

#include <YunServer.h>
#include <YunClient.h>

#include <avr/wdt.h>

/** Timer for measurements */
unsigned long lastMeasure;
/** Timer for saving */
unsigned long lastSave;
/** Timer for reset */
//unsigned long lastReset;

/** Constant value for activity LED port */
#define activityLED 8

/** Constant value for measurement frequency in ms */
#define measureFreq 1000

/** Instance of the Adafruit TSL2561 sensor */
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified ( TSL2561_ADDR_FLOAT, 1 );
/** Currently used integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms */
int lightInteg = 2;
/** Collector for average per minute light sensor */
long collLight = 0;

/** Create an instance to emon for CT sensors */
EnergyMonitor emon[2];

/** iCal definition for CT 1 (solar) */
#define iCal1 5.7
/** iCal definition for CT 2 (mains) */
#define iCal2 11.5
/** Phase shift definition for voltage (solar) */
#define pShift1 1.3
/** Phase shift definition for voltage (mains) */
#define pShift2 6.1
/** vCal definition for voltage measurement */
#define vCal 255

/** Collector for average power per minute CT sensors
 [0] = solar CT sensor
 [1] = mains CT sensor */
double collPower[2];
/** Counter for measurements per minute from sensors
 [0] = solar CT sensor
 [1] = mains CT sensor
 [2] = light sensor */
int collCount[3];

/** Listen to the default port 5555, the Yún webserver
   will forward there all the HTTP requests you send */
YunServer server;
/** Instance of client for incoming and outgoing HTTP requests */
YunClient client;

