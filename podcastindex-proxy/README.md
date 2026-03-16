# Podcastindex-Proxy

Eigenständiger Proxy-Server für die Audiothek-Online-Suche als separates Go-Projekt.  
Dieses Projekt ist bewusst vom Desktop-Client getrennt und enthält die Podcastindex-Zugangsdaten nur zur Laufzeit über Umgebungsvariablen oder eine Server-Konfigurationsdatei.

## Zweck

Der Proxy nimmt Suchanfragen der App entgegen, ruft damit Podcastindex auf und liefert ein kompaktes JSON-Format zurück, das sich leicht in `AudioEntry`-ähnliche Daten mappen lässt.

## Konfiguration

Erforderlich, jeweils mit Priorität:

- zuerst Environment:
  - `PODCASTINDEX_API_KEY`
  - `PODCASTINDEX_API_SECRET`
- falls dort nicht gesetzt: Konfigurationsdatei `/etc/podcastindex-proxy.conf`

Optional:

- `PODCASTINDEX_PROXY_HOST`
  Standard: `0.0.0.0`
- `PODCASTINDEX_PROXY_PORT`
  Standard: `8080`
- `PODCASTINDEX_PROXY_USER_AGENT`
  Standard: `MediathekView-Podcastindex-Proxy`
- `PODCASTINDEX_FEED_LIMIT`
  Standard: `10`
- `PODCASTINDEX_EPISODE_LIMIT`
  Standard: `100`
- `PODCASTINDEX_PROXY_CONFIG_FILE`
  Standard: `/etc/podcastindex-proxy.conf`

### Format der Konfigurationsdatei

Beispiel für `/etc/podcastindex-proxy.conf`:

```ini
PODCASTINDEX_API_KEY=dein-key
PODCASTINDEX_API_SECRET=dein-secret
```

Leere Zeilen und Kommentarzeilen mit `#` werden ignoriert.

## Starten

```sh
export PODCASTINDEX_API_KEY='...'
export PODCASTINDEX_API_SECRET='...'

go run ./cmd/podcastindex-proxy
```

Alternativ ohne Environment nur mit Konfigurationsdatei:

```sh
sudo install -m 600 /dev/stdin /etc/podcastindex-proxy.conf <<'EOF'
PODCASTINDEX_API_KEY=...
PODCASTINDEX_API_SECRET=...
EOF

go run ./cmd/podcastindex-proxy
```

Optionaler Build:

```sh
go build -o ./.bin/podcastindex-proxy ./cmd/podcastindex-proxy
```

## Betrieb als Dienst

Beispieldateien liegen unter:

- `deploy/caddy/Caddyfile`
- `deploy/launchd/de.mediathekview.podcastindex-proxy.plist`
- `deploy/nginx/podcastindex-proxy.conf`
- `deploy/systemd/podcastindex-proxy.service`

### macOS mit `launchd`

1. Binary bauen und nach `/usr/local/bin/podcastindex-proxy` kopieren.
2. Arbeits- und Log-Verzeichnisse anlegen:

```sh
mkdir -p /usr/local/var/podcastindex-proxy
mkdir -p /usr/local/var/log
```

3. Die Beispiel-`plist` anpassen und nach `~/Library/LaunchAgents/` oder `/Library/LaunchDaemons/` kopieren.
4. Dienst laden:

```sh
launchctl load ~/Library/LaunchAgents/de.mediathekview.podcastindex-proxy.plist
```

### Linux mit `systemd`

1. Binary nach `/opt/podcastindex-proxy/podcastindex-proxy` kopieren.
2. Eigenen Dienstnutzer anlegen, falls gewünscht.
3. Optional `/etc/podcastindex-proxy.conf` mit Key und Secret anlegen.
4. Für Betrieb hinter einem Reverse Proxy am besten intern nur auf `127.0.0.1:18080` lauschen:

```ini
PODCASTINDEX_PROXY_HOST=127.0.0.1
PODCASTINDEX_PROXY_PORT=18080
```

5. Die Beispiel-Datei anpassen und nach `/etc/systemd/system/podcastindex-proxy.service` kopieren.
6. Dienst aktivieren und starten:

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now podcastindex-proxy
```

### Reverse Proxy mit `caddy`

Eine Vorlage liegt unter `deploy/caddy/Caddyfile`.

Vorgehen:

1. Den Go-Dienst lokal auf `127.0.0.1:18080` binden.
2. `podcastindex.example.org` in der Caddy-Datei durch den echten Hostnamen ersetzen.
3. Die Datei nach `/etc/caddy/Caddyfile` oder als eigene Site-Konfiguration übernehmen.
4. Caddy neu laden:

```sh
sudo systemctl reload caddy
```

Die Vorlage veröffentlicht bewusst nur:

- `/health`
- `/api/audiothek/podcast-search`

Alle anderen Pfade liefern `404`.

### Reverse Proxy mit `nginx`

Eine Vorlage liegt unter `deploy/nginx/podcastindex-proxy.conf`.

Vorgehen:

1. Den Go-Dienst lokal auf `127.0.0.1:18080` binden.
2. `podcastindex.example.org` in der Nginx-Datei durch den echten Hostnamen ersetzen.
3. Die Datei nach `/etc/nginx/conf.d/` oder `/etc/nginx/sites-available/` übernehmen.
4. Nginx-Konfiguration prüfen und neu laden:

```sh
sudo nginx -t
sudo systemctl reload nginx
```

Die Vorlage veröffentlicht bewusst nur:

- `/health`
- `/api/audiothek/podcast-search`

Alle anderen Pfade liefern `404`.

## Endpunkte

### Gesundheitscheck

```http
GET /health
```

Antwort:

```json
{
  "status": "ok"
}
```

### Podcast-Suche

```http
GET /api/audiothek/podcast-search?q=hörspiel
```

Optionale Query-Parameter:

- `feedLimit`
- `episodeLimit`

Beispiel:

```http
GET /api/audiothek/podcast-search?q=hörspiel&feedLimit=5&episodeLimit=50
```

Antwort:

```json
{
  "results": [
    {
      "channel": "Podcastindex",
      "genre": "Comedy",
      "theme": "Kalk & Welk",
      "title": "Die fabelhaften Boomer Boys",
      "description": "Bereinigte Beschreibung ohne HTML",
      "audioUrl": "https://...",
      "websiteUrl": "https://...",
      "publishedAt": "2026-03-11T12:00:00",
      "durationMinutes": 55,
      "sizeMb": 48,
      "isPodcast": true
    }
  ]
}
```

## Projektstruktur

- `cmd/podcastindex-proxy`
  Startpunkt des HTTP-Servers
- `internal/config`
  Konfiguration über Umgebungsvariablen
- `internal/podcastindex`
  Authentifizierte Zugriffe auf Podcastindex
- `internal/search`
  Feed- und Episoden-Suche sowie Datenbereinigung
- `internal/api`
  HTTP-Handler und JSON-Modelle
