# spMonitor
Solar Panel Monitor with Arduino Uno, current &amp; light sensors and WiFi module to transfer data to a PC or Android device

## Solar Panel Monitor
 
Uses current sensor to measure output of solar panels.  
Optional additional measurement of luminosity.  
Optional additional measurement of in/output to electricity grid  

### Arduino part:
#### Hardware:
Arduino Yun  
Sensor shield --> schematic in subfolder hardware  
#### Required libraries:
--> in subfolder arduino/libraries  
##### Timer library by JChristensen  
-- Link: https://github.com/JChristensen/Timer/tree/v2.1  
-- License: GNU General Public License as published by the Free Software Foundation Version 2  

##### Adafruit Sensor library  
-- Link: https://github.com/adafruit/Adafruit_Sensor  
-- License: Apache License, Version 2.0  

##### Adafruit TSL2561 library  
-- Link: https://github.com/adafruit/Adafruit_TSL2561  
-- License: BSD License  

##### OpenEnergyMonitor CT sensor library  
-- Link: https://github.com/openenergymonitor/EmonLib  
-- License: Licence GNU GPL V3  
#### Software:
--> in subfolder arduino/spMonitor
### Android part:
#### Software:
--> This repository
### PC part:
#### Work in progress
--> PHP script in arduino/spMonitor/www