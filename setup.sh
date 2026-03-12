#!/bin/bash
# Script setup.sh - Prépare et lance l'application

set -e  # Exit on error

cd "$(dirname "$0")"

echo "================================"
echo "Jeu d'Échecs - Configuration"
echo "================================"
echo ""

echo "1️⃣  Compilation du projet..."
mvn clean compile -q
echo "   ✅ Compilation réussie"
echo ""

echo "2️⃣  Téléchargement des dépendances..."
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
echo "   ✅ Dépendances téléchargées"
echo ""

echo "3️⃣  Vérification des ressources..."
SVGCOUNT=$(find src/main/resources/Pieces -name "*.svg" | wc -l)
echo "   ✅ $SVGCOUNT fichiers SVG trouvés"
echo ""

echo "4️⃣  Lancement de l'application..."
echo "   (Cliquez sur la fenêtre pour interagir avec le jeu)"
echo "   (Fermez la fenêtre pour quitter)"
echo ""

java -cp "target/classes:target/dependency/*" org.Main
