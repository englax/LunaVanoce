# Luna Vánoce

A Kiosk Satellite plugin for Luna, our kitchen voice assistant.

* **Voice skin "Luna Vánoce":** a violet glass bauble with a ring of fairy lights. The lights twinkle while Luna listens, chase each other while she thinks (with a gold star circling) and pulse while she answers. Full screen it dims the room and lets snow fall; docked it is a small card at the bottom.
* **Screensaver "Adventní kalendář":** 24 doors, where only today's door glows from 1 to 24 December, an advent wreath that lights one more candle each Advent Sunday, a countdown to Štědrý den, and the live weather falling over it all (rain on the glass, snow, fog, storms). After Christmas it counts down to the New Year, wishes PF on 1 January and keeps the calendar up until Tři králové. From 7 January to 31 October it shows only the clock and weather; on 1 November the calendar comes back with closed doors.

## Install

1. Publish a release with a tag such as `v1.0.0`. The Release workflow builds the plugin and attaches it.
2. On the kiosk: Settings → Plugin Manager → turn on Enable Plugins → Add plugin → paste this repository's address → Trust and install → turn the plugin on.
3. In the plugin's settings pick the weather entity under **Počasí**.
4. Settings → Screensaver → Screensaver mode → **Adventní kalendář (Luna Vánoce)**.
5. Settings → Voice Satellite → Appearance → Skin → **Luna Vánoce**.

The bundled Nunito font is under the SIL Open Font License (`assets/fonts/OFL.txt`). The SDK in `sdk/` and the build tools in `tools/` come from the Kiosk Satellite plugin template (Apache-2.0).
