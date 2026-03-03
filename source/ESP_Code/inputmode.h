/*
 * inputmode.h - Input Mode page: keyboard and mouse control interface for ESPloitV2.
 *
 * Stored in PROGMEM (flash) via a raw string literal. References /style.css
 * for the shared theme so no inline styles are duplicated across pages.
 *
 * A JS helper K(id, cmd, label, cls) calls document.write() to emit a
 * complete <form> + submit button in one line, reducing the page from
 * ~429 lines of hand-written HTML to ~171 lines.
 *
 * Every form button POSTs to /runlivepayload with a HID command string
 * (Print:, PrintLine:, Press:<keycode>, MouseMoveUp:, MouseClickLEFT:, etc.).
 * Forms target a hidden <iframe> at the bottom of the page so submissions
 * happen asynchronously without navigating away.
 *
 * Sections:
 *   - Text Input      : Print (no Enter) and PrintLine (with Enter) textareas
 *   - Mouse           : D-pad movement (20-unit steps) and left/right click
 *   - Arrow Keys      : Directional arrows, Enter, Tab, Alt+Tab, Shift+Tab
 *   - Function Keys   : F1 through F12
 *   - Misc Keys       : ESC, HOME, END, INSERT, DEL, BACKSPACE, SPACE, PAGE UP/DOWN
 *   - Windows         : GUI, GUI+r, cmd, osk, Alt+F4, Ctrl+Alt+Del, Ctrl+Shift+Esc
 *   - Mac             : z and / key helpers (keyboard-layout aids)
 *   - Linux           : Alt+F2, gnome-terminal, Ctrl+c, Ctrl+x
 *   - BIOS            : Common BIOS-entry keys (F1, F2, F8, F12, DEL, ESC)
 */
const char InputModePage[] PROGMEM = R"=====(
<!DOCTYPE HTML>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Input Mode - Cactus WHID</title>
<link rel="stylesheet" href="/style.css">
</head>
<body>

<div class="nav">
<a href="/esploit">&larr; Back</a>
<span class="brand">Input Mode</span>
</div>

<div class="info">Set <b>Delay Before Starting a Live Payload</b> to <b>0</b> on the configuration page to avoid delays in Input Mode.</div>

<script>
function K(id,cmd,label,cls){
document.write('<form action="/runlivepayload" method="post" id="'+id+'" target="iframe"><input type="radio" name="livepayloadpresent" value="1" hidden checked><textarea name="livepayload" hidden>'+cmd+'</textarea><input type="submit" form="'+id+'" value="'+label+'" class="'+(cls||'kbtn')+'"></form>')
}
</script>

<div class="card sect">
<h3>Text Input</h3>
<form action="/runlivepayload" method="post" id="print" target="iframe">
<input type="radio" name="livepayloadpresent" value="1" hidden checked>
<div class="mb">
<label>Print (no Enter)</label>
<textarea form="print" rows="2" name="livepayload" style="min-height:40px">Print:</textarea>
</div>
<input type="submit" form="print" value="Send Text" class="btn btn-p">
</form>
<hr>
<form action="/runlivepayload" method="post" id="println" target="iframe">
<input type="radio" name="livepayloadpresent" value="1" hidden checked>
<div class="mb">
<label>PrintLine (with Enter)</label>
<textarea form="println" rows="2" name="livepayload" style="min-height:40px">PrintLine:</textarea>
</div>
<input type="submit" form="println" value="Send Text + Enter" class="btn btn-p">
</form>
</div>

<div class="card sect">
<h3>Mouse</h3>
<div class="flex" style="gap:24px;flex-wrap:wrap;align-items:start">
<div>
<div class="muted mb" style="text-align:center;font-size:.8em">Movement</div>
<div class="kpad">
<div></div>
<script>K('up','MouseMoveUp:20','\u25B2')</script>
<div></div>
<script>K('left','MouseMoveLeft:20','\u25C0')</script>
<div></div>
<script>K('right','MouseMoveRight:20','\u25B6')</script>
<div></div>
<script>K('down','MouseMoveDown:20','\u25BC')</script>
<div></div>
</div>
</div>
<div>
<div class="muted mb" style="text-align:center;font-size:.8em">Clicks</div>
<div class="bgroup">
<script>K('clickleft','MouseClickLEFT:','Left Click','kbtn w')</script>
<script>K('rightclick','MouseClickRIGHT:','Right Click','kbtn w')</script>
</div>
</div>
</div>
</div>

<div class="card sect">
<h3>Arrow Keys</h3>
<div class="flex" style="gap:24px;flex-wrap:wrap;align-items:start">
<div>
<div class="muted mb" style="text-align:center;font-size:.8em">Arrows</div>
<div class="kpad">
<div></div>
<script>K('a_up','Press:218','\u25B2')</script>
<div></div>
<script>K('a_left','Press:216','\u25C0')</script>
<script>K('a_enter','Press:176','Enter')</script>
<script>K('a_right','Press:215','\u25B6')</script>
<div></div>
<script>K('a_down','Press:217','\u25BC')</script>
<div></div>
</div>
</div>
<div>
<div class="muted mb" style="text-align:center;font-size:.8em">Navigation</div>
<div class="bgroup">
<script>K('a_tab','Press:179','Tab','kbtn w')</script>
<script>K('a_alttab','Press:130+179','Alt+Tab','kbtn w')</script>
<script>K('a_shifttab','Press:133+179','Shift+Tab','kbtn w')</script>
</div>
</div>
</div>
</div>

<div class="card sect">
<h3>Function Keys</h3>
<div class="bgroup">
<script>
K('f1','Press:194','F1');K('f2','Press:195','F2');K('f3','Press:196','F3');
K('f4','Press:197','F4');K('f5','Press:198','F5');K('f6','Press:199','F6');
K('f7','Press:200','F7');K('f8','Press:201','F8');K('f9','Press:202','F9');
K('f10','Press:203','F10');K('f11','Press:204','F11');K('f12','Press:205','F12');
</script>
</div>
</div>

<div class="card sect">
<h3>Misc Keys</h3>
<div class="bgroup">
<script>
K('esc','Press:177','ESC');K('home','Press:210','HOME');K('end','Press:213','END');
K('insert','Press:209','INSERT');K('del','Press:212','DEL');K('bs','Press:178','BACKSPACE','kbtn w');
K('space','Press:32','SPACE','kbtn w');K('pu','Press:211','PAGE UP','kbtn w');K('pd','Press:214','PAGE DOWN','kbtn w');
</script>
</div>
</div>

<div class="card sect">
<h3>Windows</h3>
<div class="bgroup">
<script>
K('wingui','Press:131','GUI');K('runprompt','Press:131+114','GUI+r');
K('cmd','PrintLine:cmd','cmd + Enter','kbtn w');K('osk','PrintLine:osk','osk + Enter','kbtn w');
K('altf4','Press:130+197','Alt+F4');K('ctrlaltdel','Press:128+130+212','Ctrl+Alt+Del','kbtn w');
K('ctrlshiftesc','Press:128+129+177','Ctrl+Shift+Esc','kbtn w');
</script>
</div>
</div>

<div class="card sect">
<h3>Mac</h3>
<div class="bgroup">
<script>
K('z','Press:122','z (right of L-Shift)','kbtn w');
K('fwdslash','Press:47','/ (left of R-Shift)','kbtn w');
</script>
</div>
</div>

<div class="card sect">
<h3>Linux</h3>
<div class="bgroup">
<script>
K('altf2','Press:134+195','Alt+F2','kbtn w');
K('gterm','PrintLine:gnome-terminal','gnome-terminal + Enter','kbtn w');
K('ctrlc','Press:128+99','Ctrl+c');K('ctrlx','Press:128+120','Ctrl+x');
</script>
</div>
</div>

<div class="card sect">
<h3>BIOS</h3>
<div class="bgroup">
<script>
K('bf1','Press:194','F1');K('bf2','Press:195','F2');K('bf8','Press:201','F8');
K('bf12','Press:205','F12');K('bdel','Press:212','DEL');K('besc','Press:177','ESC');
</script>
</div>
</div>

<iframe src="/runlivepayload" name="iframe"></iframe>

</body>
</html>
)=====";
