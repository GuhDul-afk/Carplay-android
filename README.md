# Cockpit (Android)

Tableau de bord de conduite pour téléphone Android : horloge, destination vers Waze, favoris,
raccourcis d'apps, et un mini-lecteur Apple Musique qui flotte par-dessus Waze.

## Obtenir l'app sans installer Android Studio

1. Crée un compte gratuit sur github.com, puis un nouveau dépôt (bouton "New"), nom : `cockpit`.
2. Dans le dépôt : "uploading an existing file". Glisse tout le contenu de ce dossier
   (y compris le dossier `.github`) dans la page, puis "Commit changes".
3. Onglet **Actions** > **Build APK**. La compilation démarre toute seule (environ 3 à 5 minutes).
   Sinon : "Run workflow".
4. Quand la pastille est verte, ouvre l'exécution et télécharge **Cockpit-apk** en bas de page.
   C'est un zip qui contient `app-debug.apk`.
5. Passe `app-debug.apk` sur le téléphone et installe-le (Android demande d'autoriser
   l'installation depuis cette source).

## Première utilisation

1. Ouvre Cockpit. Accepte les notifications si Android le demande.
2. Dans la section **Réglages** de l'app, touche "Autoriser" pour :
   - le lecteur flottant (affichage par-dessus les autres apps) ;
   - le contrôle de la musique (accès aux notifications).
3. Si l'accès aux notifications est grisé : Réglages Android > Applications > Cockpit > menu ⋮ >
   "Autoriser les paramètres restreints", puis recommence.
4. Installe Waze et Apple Musique depuis le Play Store si ce n'est pas fait.

## Utilisation

- **Waze + musique** : lance la barre flottante puis ouvre Waze. Fais glisser la barre pour la
  placer où tu veux.
- **Arrêter** : retire la barre.
- **Go** : lance Waze vers l'adresse saisie. Les favoris font de même.
- **+ Ajouter** (applications) : ajoute n'importe quelle app installée. Appui long pour retirer.

## Limites connues

- Waze reste sa propre app : Cockpit la lance et se superpose, il ne l'intègre pas.
- La barre pilote l'app qui joue la musique. Si Apple Musique est fermée, lance un morceau d'abord.
- Le projet n'a pas été compilé avant livraison. Si la compilation échoue, copie le message
  d'erreur de l'onglet Actions pour qu'il soit corrigé.
