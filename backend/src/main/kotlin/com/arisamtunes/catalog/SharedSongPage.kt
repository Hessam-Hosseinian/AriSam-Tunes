package com.arisamtunes.catalog

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.host
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.net.URLEncoder
import java.util.UUID

fun Route.sharedSongRoutes(repository: CatalogRepository = CatalogRepository()) {
    get("/share/songs/{id}") {
        val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrElse {
            throw ApiException(HttpStatusCode.NotFound, ErrorCode.SONG_NOT_FOUND, "Song does not exist")
        }
        val song = repository.song(id)
            ?: throw ApiException(HttpStatusCode.NotFound, ErrorCode.SONG_NOT_FOUND, "Song does not exist")
        val origin = System.getenv("PUBLIC_BASE_URL")?.trimEnd('/')
            ?: "${call.request.headers["X-Forwarded-Proto"] ?: "http"}://${call.request.host()}"
        call.respondText(
            text = sharedSongHtml(song, origin),
            contentType = ContentType.parse("text/html; charset=utf-8"),
        )
    }
}

internal fun sharedSongHtml(song: SongResponse, origin: String): String {
    val title = song.title.html()
    val artist = song.artistName.html()
    val album = song.album?.takeIf(String::isNotBlank)?.html()
    val lyrics = song.lyrics?.takeIf(String::isNotBlank)?.html()
        ?: "متن ترانه برای این آهنگ موجود نیست."
    val audioPath = song.sourceRelativePath
        ?.let { "/media/audio/${it.urlPath()}" }
        ?: song.audioUrl
    val coverPath = song.coverFileName
        ?.let { "/media/covers/${it.urlPath()}" }
        ?: song.coverImageUrl
    val absoluteCover = if (coverPath.startsWith("/")) "$origin$coverPath" else coverPath
    val pageTitle = "${song.title} — ${song.artistName}".html()

    return """<!doctype html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <meta name="theme-color" content="#090b14">
  <title>$pageTitle | AriSam Tunes</title>
  <meta name="description" content="پخش $pageTitle همراه با متن ترانه">
  <meta property="og:type" content="music.song">
  <meta property="og:title" content="$pageTitle">
  <meta property="og:description" content="پخش آهنگ و متن ترانه در AriSam Tunes">
  <meta property="og:image" content="${absoluteCover.html()}">
  <meta name="twitter:card" content="summary_large_image">
  <style>
    :root { color-scheme: dark; --accent:#a78bfa; --accent2:#60a5fa; --ink:#f8fafc; --muted:rgba(255,255,255,.56); }
    * { box-sizing:border-box; }
    body { margin:0; min-height:100dvh; color:var(--ink); font-family:Vazirmatn,IRANSans,system-ui,-apple-system,sans-serif; background:#06060f; }
    body::before { content:""; position:fixed; inset:0; z-index:-2; background:linear-gradient(155deg,rgba(139,92,246,.28),transparent 42%),linear-gradient(25deg,rgba(37,99,235,.18),transparent 48%),#06060f; }
    .blur { position:fixed; inset:-35%; z-index:-1; background:url('$coverPath') center/cover; filter:blur(90px) saturate(1.35); opacity:.14; transform:scale(1.1); }
    main { width:min(100%,760px); margin:auto; padding:max(24px,env(safe-area-inset-top)) 20px max(38px,env(safe-area-inset-bottom)); }
    .player { direction:ltr; display:grid; grid-template-columns:minmax(180px,280px) 1fr; gap:30px; align-items:center; padding:24px; border:1px solid rgba(255,255,255,.11); border-radius:32px; background:rgba(18,18,35,.72); box-shadow:0 30px 90px rgba(0,0,0,.38); backdrop-filter:blur(24px); }
    .cover-wrap { position:relative; aspect-ratio:1; }
    .cover { width:100%; height:100%; object-fit:cover; border-radius:25px; box-shadow:0 22px 48px rgba(0,0,0,.45); }
    .cover-wrap::after { content:""; position:absolute; inset:0; border-radius:25px; border:1px solid rgba(255,255,255,.14); pointer-events:none; }
    .details { direction:rtl; min-width:0; }
    .eyebrow { margin:0 0 13px; color:rgba(255,255,255,.38); font-size:.72rem; font-weight:800; letter-spacing:.18em; text-transform:uppercase; }
    h1 { margin:0; font-size:clamp(1.55rem,5vw,2.35rem); line-height:1.25; overflow-wrap:anywhere; }
    .artist { margin:8px 0 0; color:var(--muted); font-size:1rem; }
    .album { margin:4px 0 0; color:rgba(255,255,255,.35); font-size:.82rem; }
    .timeline { direction:ltr; display:flex; align-items:center; gap:10px; margin-top:28px; }
    .time { width:38px; color:rgba(255,255,255,.4); font:600 .7rem ui-monospace,monospace; }
    .time:last-child { text-align:right; }
    input[type=range] { width:100%; height:4px; margin:0; accent-color:var(--accent); cursor:pointer; }
    .controls { direction:ltr; display:flex; justify-content:center; align-items:center; margin-top:19px; }
    button { width:60px; height:60px; display:grid; place-items:center; border:1px solid rgba(255,255,255,.22); border-radius:50%; color:#090b14; background:linear-gradient(135deg,#c4b5fd,#7dd3fc); box-shadow:0 12px 34px rgba(96,165,250,.22); cursor:pointer; transition:transform .15s ease; }
    button:active { transform:scale(.94); }
    button svg { grid-area:1/1; width:28px; height:28px; fill:currentColor; }
    button svg[hidden] { display:none; }
    .lyrics { margin-top:24px; padding:26px; border:1px solid rgba(255,255,255,.09); border-radius:28px; background:rgba(15,18,32,.67); backdrop-filter:blur(18px); }
    .lyrics-head { display:flex; align-items:center; gap:10px; margin-bottom:18px; }
    .lyrics-head span { display:grid; place-items:center; width:34px; height:34px; border-radius:12px; color:#c4b5fd; background:rgba(139,92,246,.16); }
    h2 { margin:0; font-size:1.05rem; }
    .lyrics-text { margin:0; color:rgba(255,255,255,.76); font-size:1rem; line-height:2.1; white-space:pre-wrap; unicode-bidi:plaintext; }
    @media(max-width:620px) { main{padding-inline:14px}.player{grid-template-columns:1fr;gap:22px;padding:18px}.cover-wrap{width:min(78vw,310px);justify-self:center}.details{text-align:center}.timeline{margin-top:23px}.lyrics{padding:21px 18px}.lyrics-head{justify-content:flex-start}.lyrics-text{text-align:start} }
    @media(prefers-reduced-motion:reduce) { *{scroll-behavior:auto!important;transition:none!important} }
  </style>
</head>
<body>
  <div class="blur" aria-hidden="true"></div>
  <main>
    <section class="player" aria-label="پخش‌کننده آهنگ">
      <div class="cover-wrap"><img class="cover" src="$coverPath" alt="کاور $title"></div>
      <div class="details">
        <p class="eyebrow">AriSam Tunes · Now Playing</p>
        <h1>$title</h1>
        <p class="artist">$artist</p>
        ${album?.let { "<p class=\"album\">$it</p>" }.orEmpty()}
        <audio id="audio" preload="metadata" src="$audioPath"></audio>
        <div class="timeline">
          <span class="time" id="current">0:00</span>
          <input id="seek" type="range" min="0" max="1000" value="0" aria-label="موقعیت پخش">
          <span class="time" id="duration">0:00</span>
        </div>
        <div class="controls">
          <button id="toggle" type="button" aria-label="پخش">
            <svg id="play" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
            <svg id="pause" viewBox="0 0 24 24" hidden><path d="M6 5h4v14H6zm8 0h4v14h-4z"/></svg>
          </button>
        </div>
      </div>
    </section>
    <section class="lyrics">
      <div class="lyrics-head"><span aria-hidden="true">♪</span><h2>متن ترانه</h2></div>
      <p class="lyrics-text">$lyrics</p>
    </section>
  </main>
  <script>
    const audio=document.querySelector('#audio'),seek=document.querySelector('#seek'),toggle=document.querySelector('#toggle');
    const play=document.querySelector('#play'),pause=document.querySelector('#pause'),current=document.querySelector('#current'),duration=document.querySelector('#duration');
    const clock=s=>Number.isFinite(s)?Math.floor(s/60)+':'+String(Math.floor(s%60)).padStart(2,'0'):'0:00';
    const state=()=>{const playing=!audio.paused;play.hidden=playing;pause.hidden=!playing;toggle.ariaLabel=playing?'توقف':'پخش'};
    toggle.addEventListener('click',()=>audio.paused?audio.play():audio.pause());
    audio.addEventListener('play',state);audio.addEventListener('pause',state);
    audio.addEventListener('loadedmetadata',()=>duration.textContent=clock(audio.duration));
    audio.addEventListener('timeupdate',()=>{current.textContent=clock(audio.currentTime);seek.value=audio.duration?Math.round(audio.currentTime/audio.duration*1000):0});
    seek.addEventListener('input',()=>{if(audio.duration)audio.currentTime=seek.value/1000*audio.duration});
  </script>
</body>
</html>"""
}

private fun String.html(): String = buildString(length) {
    this@html.forEach { character ->
        append(when (character) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&#39;"
            else -> character
        })
    }
}

private fun String.urlPath(): String = split('/', '\\').joinToString("/") {
    URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20")
}
