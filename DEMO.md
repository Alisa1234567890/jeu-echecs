# Demo lundi - Jeu d'echecs

Script court (8-10 min) pour prouver toutes les fonctionnalites.

## 1) IA
- Lancer `Jouer contre l'IA`.
- Montrer les niveaux easy/medium/hard.
- Jouer 2-3 coups et montrer la reponse de l'IA.
- Code: `src/main/java/org/model/JIA.java`, `src/main/java/org/model/Jeu.java`.

## 2) Nulle par repetition (HashMap)
- Reproduire la meme position 3 fois (aller-retour des cavaliers).
- Montrer le statut de nulle par repetition.
- Code: `src/main/java/org/model/Jeu.java` (`repetitionCounts`, `buildPositionKey`).

## 3) PNG standard
- Jouer un coup.
- Montrer `partie_echecs.png` mis a jour dans le dossier du projet.
- Code: `src/main/java/org/model/Jeu.java` (`sauvegardePng`), `src/main/java/org/util/ImageGenerator.java`.

## 4) Editeur PNG
- Ouvrir `Outils -> Editeur PNG d'Echecs`.
- Charger `partie_echecs.png`, modifier un commentaire, enregistrer.
- Recharger pour prouver la persistance.
- Code: `src/main/java/org/editor/PngChessEditor.java`.

## 5) Vue 3D
- Montrer la fenetre 3D (ouverte au lancement ou via `Outils -> Ouvrir la vue 3D`).
- Jouer un coup et montrer la mise a jour (tour/statut/dernier coup).
- Code: `src/main/java/org/view/Simple3DView.java`, `src/main/java/org/Main.java`.

## 6) Connecteur reseau
- Ouvrir `Outils -> Test connecteur reseau`.
- Connecter `localhost:5000` et envoyer un message.
- Montrer la ligne recue dans le journal.
- Code: `src/main/java/org/tools/NetworkConnector.java`, `src/main/java/org/tools/NetworkConnectorDemoFrame.java`.

### Serveur local PowerShell (optionnel)

```powershell
$listener = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,5000)
$listener.Start()
Write-Host 'Echo server sur 5000'
$client = $listener.AcceptTcpClient()
$stream = $client.GetStream()
$reader = New-Object System.IO.StreamReader($stream)
$writer = New-Object System.IO.StreamWriter($stream)
$writer.AutoFlush = $true
while(($line = $reader.ReadLine()) -ne $null){ $writer.WriteLine('echo:' + $line) }
```

## 7) Extension choisie: export PGN standard
- Ouvrir l'editeur PNG et montrer le PGN stocke.
- Expliquer que le PGN est construit avec classes dediees.
- Code: `src/main/java/org/tools/PGN.java`, `src/main/java/org/tools/PgnExporter.java`, `src/main/java/org/model/Jeu.java` (`buildStandardPgnText`).

