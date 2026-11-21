# TV Player (Player Lamine S Bouzareah)

Structure modulaire (version 7.4.0)

```
app/src/main/java/com/example/tvplayer/
  MainActivity.kt        ecrans et interactions
  core/design/           Design.kt : couleurs, espacements, rayons, typographie, themes
  data/model/            Models.kt : Channel, Vod, Episode, Playlist, Account
  data/                  Presets.kt : chaines de test, sources EPG gratuites
  data/source/           M3uParser.kt, EpgParser.kt
  pvr/                   Recorder.kt : enregistrement et planification
  portal/                PortalServer.kt : portail web (page dans assets/portal.html)
app/src/main/assets/     portal.html
app/src/main/res/        icones et banniere
.github/workflows/       build.yml : compile, publie l'APK
```

Depot : copier tout le contenu de ce dossier a la racine du depot GitHub.
