# HTS221 sensor driver for temperature and humidity
Driver is written for use within android thing using a raspberry pi 3 + sense hat.
**__Note this driver isnt ready for production, it is published as a sampl. No guarantee or warranty is extended.__**


## Usage
### After adding HTS221.jar to your /libs folder of your poject and import package.
HTS221 hts221 = new HTS221();
### Calling PowerOn function/method:
hts221.pOn(HTS221.MODE_ACTIVE);
### Get temperature data:
double temperature = hts221.getTemperature();
### Get humidity data:
double humidity = hts221.getHumidity();

## License

 Copyright 2018 Daniel Larsson

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
