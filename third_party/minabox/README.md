# MinaBox attribution

The sources under `android/app/src/main/java/eu/wewox/minabox` originate from
[oleksandrbalan/minabox](https://github.com/oleksandrbalan/minabox), release `v1.10.0`
(commit `35398894f648255a8844599d92528e5dbe939693`).

`MinaBox.kt` coalesces rapid drag deltas and resets per-gesture velocity tracking.
`MinaBoxState.kt` exposes active programmatic motion so the repeating song plane can defer wrapping
until a fling or center animation has finished. `MinaBoxItemProvider.kt` avoids chained temporary
maps while resolving visible items on each frame. All modified files carry modification notices.

`MusicSuggestionsScreen.kt` adapts the upstream advanced hexagon example to render AriSam Tunes
song artwork and metadata. MinaBox is licensed under Apache License 2.0; see `LICENSE.md` here.
