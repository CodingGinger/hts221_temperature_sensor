### HTS221 sensor driver for temperature and humidity
Driver is written for use within android thing using a raspberry pi 3 + sense hat


## Usage
# After adding HTS221.jar to your /libs folder of your poject.
HTS221 hts221 = new HTS221();
# Calling PowerOn function/method:
hts221.pOn(HTS221.MODE_ACTIVE);
# Get temperature data:
double temperature = hts221.getTemperature();
# Get humidity data:
double humidity = hts221.getHumidity();
