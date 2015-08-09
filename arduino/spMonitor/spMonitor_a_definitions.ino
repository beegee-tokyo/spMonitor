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

#include <FileIO.h>

#include <YunServer.h>
#include <YunClient.h>

#include <avr/wdt.h>

/** Timer for measurements and saving */
unsigned long lastMeasure;
unsigned long lastSave;
unsigned long lastReset;
 
/** Constant value for activity LED port */
#define activityLED 8

/** Instance of the Adafruit TSL2561 sensor */
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified ( TSL2561_ADDR_FLOAT, 1 );
/** Currently used integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms */
int lightInteg = 2;
/** Collector for average per minute light sensor */
long collLight = 0;

/** Create an instance to emon for CT sensors */
EnergyMonitor emon[2];
/** Used current calibration value for current sensors */
double iCal[2];
/** Currently used solar calibration value */
double vCal = 255;
/** Currently used consumption calibration value */
#define iCalVal1 5.7
/** Currently used voltage calibration value */
#define iCalVal2 11.5
/** Collector for average power per minute CT sensors */
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

