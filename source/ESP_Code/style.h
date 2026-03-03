const char CSS[] PROGMEM = R"=====(
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;background:#0d1117;color:#c9d1d9;line-height:1.6;padding:12px;max-width:960px;margin:0 auto;font-size:14px}
a{color:#58a6ff;text-decoration:none}a:hover{text-decoration:underline}
h1{font-size:1.4em;margin-bottom:.5em}h2{font-size:1.15em;margin-bottom:.5em}h3{font-size:1em}
.nav{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:10px 16px;margin-bottom:16px;display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.brand{font-weight:700;color:#3fb950;margin-right:auto;font-size:1.1em;white-space:nowrap}
.nav a{padding:4px 10px;border-radius:6px;font-size:.85em;color:#c9d1d9}.nav a:hover{background:#21262d;text-decoration:none}
.card{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:16px;margin-bottom:16px}
.card h2,.card h3{padding-bottom:8px;border-bottom:1px solid #30363d;margin-bottom:12px}
.btn{display:inline-block;padding:5px 14px;border-radius:6px;border:1px solid #30363d;background:#21262d;color:#c9d1d9;cursor:pointer;font-size:.85em;text-decoration:none;line-height:1.5}
.btn:hover{background:#30363d;text-decoration:none}
.btn-p{background:#238636;border-color:rgba(240,246,252,.1);color:#fff}.btn-p:hover{background:#2ea043}
.btn-d{background:#da3633;border-color:rgba(240,246,252,.1);color:#fff}.btn-d:hover{background:#f85149}
.btn-w{background:#9e6a03;border-color:rgba(240,246,252,.1);color:#fff}.btn-w:hover{background:#bb8009}
input[type=text],input[type=password],input[type=number],select,textarea{background:#0d1117;border:1px solid #30363d;color:#c9d1d9;padding:5px 12px;border-radius:6px;font-size:14px;width:100%;max-width:400px}
textarea{max-width:100%;font-family:Consolas,"Liberation Mono",monospace;min-height:80px}
input[type=submit]{padding:5px 14px;border-radius:6px;background:#238636;border:1px solid rgba(240,246,252,.1);color:#fff;font-size:.85em;cursor:pointer}
input[type=submit]:hover{background:#2ea043}
input[type=file]{color:#c9d1d9;font-size:.85em}
input[type=radio]{width:auto;margin-right:4px}
label{display:inline-block;margin-bottom:4px;font-weight:500;font-size:.85em;color:#8b949e}
table{width:100%;border-collapse:collapse;margin:.5em 0}
th,td{padding:8px 12px;text-align:left;border-bottom:1px solid #21262d}
th{color:#8b949e;font-weight:600;font-size:.8em;text-transform:uppercase;letter-spacing:.5px;background:#161b22}
tr:hover{background:rgba(56,139,253,.05)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:12px;margin:12px 0}
.gi{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:16px;text-align:center;transition:border-color .2s,transform .1s}
.gi:hover{border-color:#58a6ff;transform:translateY(-1px)}
.gi a{display:block;color:#c9d1d9;font-weight:500;text-decoration:none}
.gi .ico{font-size:1.4em;margin-bottom:6px}
.gi .lbl{font-size:.8em;color:#8b949e;margin-top:4px}
.stat{display:flex;gap:16px;flex-wrap:wrap;padding:8px 0;font-size:.85em;color:#8b949e}
.stat b{color:#c9d1d9}
pre{background:#0d1117;border:1px solid #30363d;border-radius:6px;padding:12px;overflow-x:auto;font-family:Consolas,monospace;font-size:.85em;color:#c9d1d9}
code{font-family:Consolas,monospace;background:#21262d;padding:2px 6px;border-radius:4px;font-size:.85em}
hr{border:none;border-top:1px solid #30363d;margin:16px 0}
.fr{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-bottom:10px}
.fr label{margin:0;white-space:nowrap;min-width:140px;color:#c9d1d9;font-size:.9em;font-weight:400}
.fr input,.fr select{flex:1}
.rl{display:inline-flex;align-items:center;gap:4px;margin-right:12px;font-size:.9em;cursor:pointer;color:#c9d1d9}
.rl input{width:auto;margin:0}
small{color:#8b949e;font-size:.8em}
.tag{display:inline-block;padding:1px 8px;border-radius:12px;font-size:.75em;font-weight:600}
.tg{background:rgba(63,185,80,.15);color:#3fb950}
.tr{background:rgba(248,81,73,.15);color:#f85149}
.ty{background:rgba(210,153,34,.15);color:#d29922}
.tb{background:rgba(88,166,255,.15);color:#58a6ff}
.flex{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
.mb{margin-bottom:12px}
.mt{margin-top:12px}
.muted{color:#8b949e}
.sect{margin-bottom:20px}
.sect h3{color:#58a6ff;margin-bottom:8px}
.bgroup{display:flex;gap:6px;flex-wrap:wrap;margin:8px 0}
.kbtn{padding:8px 12px;border-radius:6px;border:1px solid #30363d;background:#21262d;color:#c9d1d9;cursor:pointer;font-size:.82em;min-width:44px;text-align:center;transition:background .15s}
.kbtn:hover{background:#30363d}
.kbtn.w{min-width:80px}
.kpad{display:grid;grid-template-columns:repeat(3,1fr);gap:4px;max-width:180px}
.kpad .kbtn{padding:10px}
.warn{background:rgba(210,153,34,.1);border:1px solid rgba(210,153,34,.3);border-radius:6px;padding:12px;color:#d29922;margin-bottom:12px}
.info{background:rgba(88,166,255,.1);border:1px solid rgba(88,166,255,.3);border-radius:6px;padding:12px;color:#58a6ff;margin-bottom:12px}
.success{background:rgba(63,185,80,.1);border:1px solid rgba(63,185,80,.3);border-radius:6px;padding:12px;color:#3fb950;margin-bottom:12px}
iframe[name=iframe]{display:none}
@media(max-width:600px){
body{padding:8px}
.grid{grid-template-columns:1fr 1fr}
.nav{padding:8px 12px}
.fr{flex-direction:column;align-items:stretch}
.fr label{min-width:auto}
.card{padding:12px}
table{font-size:.85em}
th,td{padding:6px 8px}
.kpad{max-width:100%}
}
@media(max-width:380px){.grid{grid-template-columns:1fr}}
)=====";
