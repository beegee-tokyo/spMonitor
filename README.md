# spMonitor
Solar Panel Monitor with Arduino Yun, current and light sensors and WiFi module to transfer data to a PC or Android device<br />

## Solar Panel Monitor

Uses current sensor to measure output of solar panels.<br />
Optional additional measurement of luminosity.<br />
Optional additional measurement of in/output to electricity grid<br />

### Arduino part:
#### Hardware:
Arduino Yun<br />
Sensor shield --> schematic in subfolder hardware<br />
#### Required libraries:
##### Adafruit Sensor library
-- Link: https://github.com/adafruit/Adafruit_Sensor<br />
-- License: Apache License, Version 2.0<br />

##### Adafruit TSL2561 library
-- Link: https://github.com/adafruit/Adafruit_TSL2561<br />
-- License: BSD License<br />

##### OpenEnergyMonitor CT sensor library
-- Link: https://github.com/openenergymonitor/EmonLib<br />
-- License: Licence GNU GPL V3<br />
#### Software:
--> in subfolder arduino/spMonitor<br />
### Android part:
#### Software:
--> This repository<br />
#### Required libraries:
###### GraphView - open source graph plotting library for Android
-- Link: http://www.android-graphview.org/     <br />
-- License: GNU GENERAL PUBLIC LICENSE Version 2, June 1991<br />
### PC part:
#### Work in progress
--> PHP script in arduino/spMonitor/www<br />
