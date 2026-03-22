# Elevator

`Elevator` ist ein Minecraft-Paper-Plugin, das aus vertikal angeordneten Druckplatten einfache Fahrstuehle erstellt. Sobald an derselben `X/Z`-Position mehrere Druckplatten uebereinander stehen und ueber den Platten genug Platz vorhanden ist, erkennt das Plugin diese Struktur automatisch als Fahrstuhl. Es sind keine Befehle, keine Konfigurationsdateien und keine besonderen Blockkombinationen notwendig.

Das Plugin ist darauf ausgelegt, Fahrstuehle direkt im normalen Bauen nutzbar zu machen. Spieler stellen sich auf eine gueltige Druckplatte, springen fuer eine Etage nach oben oder ducken sich fuer eine Etage nach unten. Dadurch kann ein Fahrstuhl mit beliebig vielen Etagen gebaut werden, solange weitere gueltige Druckplatten vertikal darueber oder darunter vorhanden sind.

## Was das Plugin macht

Das Plugin prueft Druckplatten in einer senkrechten Linie und verbindet sie zu einem Fahrstuhl.

Ein Fahrstuhl ist gueltig, wenn:
- an derselben `X/Z`-Position mindestens zwei Druckplatten uebereinander existieren
- ueber jeder beteiligten Druckplatte mindestens ein Block Luft ist
- der Bereich darueber fuer den Spieler begehbar ist

Der Block unter der Druckplatte ist egal.
Der Typ der Druckplatte ist ebenfalls egal.
Entscheidend ist nur, dass Druckplatten vertikal uebereinander stehen.

## Funktionen

- Automatische Fahrstuhl-Erkennung ohne Commands
- Beliebig viele Etagen auf derselben senkrechten Linie
- Springen transportiert genau eine Etage nach oben
- Ducken transportiert genau eine Etage nach unten
- BossBar zeigt an, dass man auf einem Fahrstuhl steht
- BossBar zeigt die aktuelle Etage und die Gesamtzahl der Etagen
- Gueltige Fahrstuhlplatten zeigen `END_ROD`-Partikel
- Beim Teleport bleiben violette `PORTAL`-Partikel sichtbar
- Beim Wechseln erscheint ein farbiger Richtungs-Pfeil auf der vorherigen Etage

## Nutzung im Spiel

### Fahrstuhl bauen

1. Setze eine Druckplatte an eine Stelle.
2. Baue ueber oder unter dieser Position weitere Druckplatten auf exakt derselben `X/Z`-Achse.
3. Achte darauf, dass ueber jeder Druckplatte genug Platz fuer einen Spieler vorhanden ist.

Beispiel:
- Druckplatte bei `X 100 / Z 100 / Y 64`
- weitere Druckplatte bei `X 100 / Z 100 / Y 80`
- weitere Druckplatte bei `X 100 / Z 100 / Y 96`

Dann entstehen drei Etagen eines Fahrstuhls.

### Fahrstuhl benutzen

1. Stelle dich auf eine gueltige Fahrstuhl-Druckplatte.
2. Beobachte die BossBar.

Die BossBar zeigt:
- dass du auf einem Fahrstuhl stehst
- auf welcher Etage du gerade bist
- wie viele Etagen dieser Fahrstuhl insgesamt hat

3. Springe, um genau eine Etage hoeher zu fahren.
4. Ducke dich, um genau eine Etage tiefer zu fahren.

Wenn es in die gewaehlte Richtung keine weitere Etage gibt, passiert nichts.

## Visuelle Rueckmeldungen

Das Plugin nutzt mehrere visuelle Hinweise, damit Spieler sofort erkennen, was gerade passiert.

### BossBar

Wenn du auf einer gueltigen Fahrstuhlplatte stehst, erscheint eine BossBar.

Sie zeigt:
- `Fahrstuhl`
- deine aktuelle Etage
- die Gesamtzahl der vorhandenen Etagen

Falls die Bukkit-API eine passende Segmentanzahl unterstuetzt, wird die BossBar segmentiert angezeigt. Andernfalls bleibt sie durchgehend.

### End Rod Partikel

Gueltige Fahrstuhlplatten erzeugen `END_ROD`-Partikel. Dadurch kann man schnell sehen, welche Druckplatten zu einem erkannten Fahrstuhl gehoeren.

### Teleport-Partikel

Beim eigentlichen Etagenwechsel erscheinen violette `PORTAL`-Partikel. Diese markieren Start und Ziel des Teleports.

### Richtungs-Pfeile

Beim Etagenwechsel erscheint auf der vorherigen Etage ein Partikel-Pfeil:
- hellgruen fuer nach oben
- rot fuer nach unten

Der Pfeil ist an der Ausrichtung des Spielers orientiert, damit er aus der Blickrichtung besser als Pfeil erkennbar ist.

## Wichtige Regeln

- Es muessen mindestens zwei Druckplatten in einer senkrechten Linie vorhanden sein.
- Die Druckplatten muessen dieselbe `X/Z`-Position haben.
- Ueber den Druckplatten muss ausreichend Platz sein.
- Der Block unter der Druckplatte spielt keine Rolle.
- Der Druckplatten-Typ spielt keine Rolle.

## Kurzfassung

`Elevator` macht aus uebereinanderliegenden Druckplatten einen einfachen, intuitiven Mehrstockwerks-Fahrstuhl. Springen bewegt nach oben, Ducken nach unten, und Partikel sowie BossBar zeigen jederzeit klar an, ob ein Fahrstuhl erkannt wurde und auf welcher Etage man steht.
