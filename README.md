# spMonitor
Solar Panel Monitor with Arduino Yun, current and light sensors and WiFi module to transfer data to a PC or Android device<br />

## Solar Panel Monitor

Uses current sensor to measure output of solar panels.<br />
Optional additional measurement of luminosity.<br />
Optional additional measurement of in/output to electricity grid<br />

## More details about the project:
-- http://desire.giesecke.tk/solar-panel-monitoring-part-1-background/<br />
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
###### MPAndroidChart - open source graph plotting library for Android
-- Link: https://github.com/PhilJay/MPAndroidChart/     <br />
-- License: Apache License, Version 2.0<br />
###### OkHttp - an HTTP & SPDY client for Android and Java applications
-- Link: http://square.github.io/okhttp/<br />
-- License: Apache License, Version 2.0<br />
###### Okio - a modern I/O API for Java http://square.github.io/okio
-- Link: https://github.com/square/okio/<br />
-- License: Apache License, Version 2.0<br />
###### Java documentation
-- Link: http://desire.giesecke.tk/docs/spmonitor/<br />
### PC part:
#### Work in progress
--> PHP script in arduino/spMonitor/www<br />
