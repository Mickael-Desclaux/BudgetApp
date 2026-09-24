# Budget

App Android perso de suivi des dépenses mensuelles (Kotlin, Jetpack Compose, Room, Glance).

## Fonctionnalités
- **Accueil** : reste à dépenser dans le mois, jauge, reste par jour, récap revenus / fixes / épargne / dépensé.
- **Opérations** : dépenses et revenus du mois, filtres par catégorie, édition/suppression au tap.
- **Fixes** : dépenses et revenus récurrents (hebdo, mensuel, annuel). Les annuels comptent en entier le mois où ils tombent.
- **Historique** : solde +/− de chaque mois, écart à l'objectif d'épargne, détail par catégorie et opérations.
- **Réglages** : objectif d'épargne mensuel, heure du rappel quotidien, catégories, export/import JSON.
- **Rappel quotidien** : notification à l'heure choisie qui ouvre directement la saisie.
- **Widget** (3×1) : reste du mois, reste/jour et bouton « + » pour ajouter une dépense.
- **Total du jour** : saisir seulement un montant global, sans catégorie.

### Formule
```
dépense possible = revenus du mois − dépenses fixes du mois − épargne visée
reste            = dépense possible − dépenses ponctuelles saisies
```
Les récurrents sont générés comme opérations au début de chaque mois (à l'ouverture de l'app, au rappel ou à l'affichage du widget). Modifier un récurrent recalcule le mois en cours ; les mois passés restent tels quels.

## Build & installation
Prérequis déjà présents sur cette machine : Android SDK (`~/Android/Sdk`) et JDK 21 (chemin dans `gradle.properties`).

```bash
./gradlew testDebugUnitTest   # tests du calcul de budget
./gradlew assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
```

**Installer sur le téléphone**
1. Sur le téléphone : Paramètres → À propos → taper 7 fois sur « Numéro de build », puis activer *Débogage USB* dans les options pour les développeurs.
2. Brancher en USB (ou `adb pair` / `adb connect` pour le débogage Wi-Fi), puis :
   ```bash
   ./gradlew installDebug
   ```
Autre option : copier `app-debug.apk` sur le téléphone et l'ouvrir (autoriser l'installation d'apps inconnues).

Les mises à jour se réinstallent par-dessus sans perte de données (même clé de signature debug). Pense quand même à exporter une sauvegarde de temps en temps (Réglages → Exporter).

Le widget s'ajoute avec un appui long sur l'écran d'accueil → Widgets → Budget.
