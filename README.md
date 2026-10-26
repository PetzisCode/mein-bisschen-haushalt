# Mein bisschen Haushalt 🌿

Eine ruhige Android-App für Haushaltsroutinen – mit Energie-Modus, frei einstellbaren
Erinnerungen und freundlichen Benachrichtigungen. Ohne Druck, ohne schlechtes Gewissen.

## So bekommst du die APK (ohne Programmierkenntnisse)

1. Auf github.com ein neues, leeres Repository anlegen (z. B. `mein-bisschen-haushalt`).
2. Den Inhalt dieses Ordners hochladen – **inklusive** des versteckten Ordners `.github`
   und der Datei `app/haushalt.keystore`.
3. GitHub baut die App automatisch (Reiter **Actions**, dauert ca. 3–5 Minuten).
4. Danach unter **Releases** auf `MeinBisschenHaushalt.apk` tippen, herunterladen, öffnen.
   Beim ersten Mal „Installation aus unbekannten Quellen“ für den Browser erlauben.

Jede neue Version wird einfach über die alte installiert – deine Aufgaben bleiben erhalten.

## Was die App kann

- **Heute**: Aufgaben des Tages abhaken, verschieben, heute auslassen. Energie-Modus
  🌙 wenig · 🌿 normal · ☀️ viel – zeigt nur, was zu deiner Tagesform passt.
- **Aufgaben**: regelmäßig (täglich oder an gewählten Wochentagen) oder einmalig
  (mit selbst gewähltem Datum oder ganz ohne). Keine erzwungenen Intervalle.
- **Erinnerungen**: beliebig viele Uhrzeiten pro Aufgabe, Tage wählbar, an/aus, löschen,
  einzelne Tage aussetzen, „Später“-Knopf in der Benachrichtigung (Dauer einstellbar),
  Hauptschalter. Änderungen gelten sofort – alte Uhrzeiten werden vollständig entfernt.
- Startvorschläge: Katzenklos 13:00 Uhr, Staubsaugen & Abendrunde 19:30 Uhr – frei änderbar.
- Erinnerungen laufen über den System-Wecker und funktionieren auch bei geschlossener App
  und nach einem Neustart des Handys.

## Technik

Java, nur Android-Bordmittel (keine Fremdbibliotheken), minSdk 26 (Android 8),
targetSdk 34. Daten liegen lokal in `haushalt.json` im App-Speicher.
