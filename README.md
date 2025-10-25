# 🎬 AlphaStream
<image-card alt="AlphaStream Logo" src="https://github.com/pecorio-dev/AlphaStream/blob/master/image.jpg?raw=true" ></image-card>
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-25%2B-brightgreen.svg)](https://android-arsenal.com/api?level=25)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Application Android moderne de streaming pour films et séries TV**

AlphaStream est une application Android native développée en Kotlin qui offre une expérience de streaming fluide et intuitive pour regarder films et séries. Avec une interface utilisateur moderne et des fonctionnalités avancées, elle propose un accès facile à un large catalogue de contenus.

## ✨ Fonctionnalités

### 🎯 **Fonctionnalités principales**
- **Streaming haute qualité** : Lecture de films et séries en HD/FHD
- **Interface TV** : Support complet pour Android TV avec navigation optimisée
- **Recherche avancée** : Recherche rapide et efficace dans tout le catalogue
- **Favoris** : Sauvegarde de vos contenus préférés
- **Progression de visionnage** : Reprise automatique là où vous vous êtes arrêté
- **Mode sombre/clair** : Interface adaptable selon vos préférences

### 📱 **Expérience utilisateur**
- **Navigation intuitive** : Interface Material Design moderne
- **Animations fluides** : Transitions et effets visuels soignés
- **Responsive design** : Optimisé pour smartphones et tablettes
- **Gestion hors-ligne** : Sauvegarde locale des favoris et préférences

### 🎮 **Support multi-plateforme**
- **Android Mobile** : Smartphones et tablettes (API 25+)
- **Android TV** : Interface dédiée pour télévisions
- **Orientation adaptative** : Support portrait et paysage

## 🏗️ Architecture technique

### **Stack technologique**
- **Langage** : Kotlin 100%
- **Architecture** : MVVM avec Repository Pattern
- **Injection de dépendances** : Hilt (Dagger)
- **Base de données** : Room Database
- **Réseau** : Retrofit + OkHttp
- **Interface** : View Binding + Data Binding
- **Navigation** : Navigation Component
- **Lecteur vidéo** : ExoPlayer (Media3)
- **Images** : Glide
- **Animations** : Lottie

### **Fonctionnalités techniques**
- **Coroutines** : Programmation asynchrone moderne
- **LiveData** : Observation réactive des données
- **Pagination** : Chargement optimisé des listes
- **Cache intelligent** : Gestion efficace de la mémoire
- **Gestion d'erreurs** : Retry automatique et fallbacks
- **Sécurité** : Validation des données et protection des API

## 📦 Installation

### **Prérequis**
- Android Studio Arctic Fox ou plus récent
- SDK Android 25+ (Android 7.1)
- Kotlin 1.8+
- Gradle 8.0+

### **Étapes d'installation**

1. **Cloner le repository**
```bash
git clone https://github.com/pecorio-dev/AlphaStream.git
cd AlphaStream
```

2. **Ouvrir dans Android Studio**
```bash
# Ouvrir le projet dans Android Studio
# File > Open > Sélectionner le dossier AlphaStream
```

3. **Synchroniser les dépendances**
```bash
# Android Studio synchronisera automatiquement
# Ou manuellement : Tools > Sync Project with Gradle Files
```

4. **Compiler et installer**
```bash
# Via Android Studio : Run > Run 'app'
# Ou via ligne de commande :
./gradlew assembleDebug
./gradlew installDebug
```

## 🚀 Utilisation

### **Première utilisation**
1. Lancez l'application
2. Explorez le catalogue depuis l'accueil
3. Utilisez la recherche pour trouver du contenu spécifique
4. Ajoutez vos contenus préférés aux favoris
5. Profitez du streaming !

### **Navigation**
- **Accueil** : Découvrez les tendances et nouveautés
- **Films** : Parcourez le catalogue de films
- **Séries** : Explorez les séries disponibles
- **Recherche** : Trouvez rapidement ce que vous cherchez
- **Favoris** : Accédez à vos contenus sauvegardés
- **Paramètres** : Personnalisez votre expérience

### **Fonctionnalités avancées**
- **Lecture continue** : Reprise automatique de la lecture
- **Qualité adaptative** : Sélection automatique de la meilleure qualité
- **Interface TV** : Navigation avec télécommande sur Android TV
- **Thèmes** : Basculez entre mode sombre et clair

## 🛠️ Développement

### **Structure du projet**
```
app/
├── src/main/java/dev/pecorio/alphastream/
│   ├── data/           # Couche de données
│   │   ├── api/        # Services API
│   │   ├── dao/        # Accès base de données
│   │   ├── database/   # Configuration Room
│   │   ├── model/      # Modèles de données
│   │   └── repository/ # Repositories
│   ├── di/             # Injection de dépendances
│   ├── ui/             # Interface utilisateur
│   │   ├── adapters/   # Adapters RecyclerView
│   │   ├── components/ # Composants réutilisables
│   │   ├── details/    # Écrans de détails
│   │   ├── home/       # Écran d'accueil
│   │   ├── movies/     # Section films
│   │   ├── series/     # Section séries
│   │   ├── search/     # Recherche
│   │   ├── player/     # Lecteur vidéo
│   │   ├── favorites/  # Favoris
│   │   ├── settings/   # Paramètres
│   │   └── tv/         # Interface TV
│   └── utils/          # Utilitaires
└── src/main/res/       # Ressources
```

### **Contribution**
Les contributions sont les bienvenues ! Pour contribuer :

1. Fork le projet
2. Créez une branche feature (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

### **Standards de code**
- Suivre les conventions Kotlin
- Utiliser ktlint pour le formatage
- Documenter les fonctions publiques
- Écrire des tests unitaires
- Respecter l'architecture MVVM

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 🤝 Support

### **Soutenir le projet**
Si vous appréciez AlphaStream et souhaitez soutenir son développement :

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20me-red?style=for-the-badge&logo=ko-fi)](https://ko-fi.com/pecorio)

Votre soutien nous aide à :
- 🚀 **Améliorer les performances** de l'application
- 🆓 **Maintenir l'app gratuite** et sans publicité
- ✨ **Développer de nouvelles fonctionnalités**
- 🔧 **Corriger les bugs** rapidement
- 📱 **Supporter plus de plateformes**

### **Contact**
- **Développeur** : [Pecorio](https://github.com/pecorio-dev)
- **Issues** : [GitHub Issues](https://github.com/pecorio-dev/AlphaStream/issues)
- **Discussions** : [GitHub Discussions](https://github.com/pecorio-dev/AlphaStream/discussions)

## 🔄 Changelog

### Version 1.0.0 (Actuelle)
- 🎉 Version initiale
- 📱 Interface mobile complète
- 📺 Support Android TV
- 🎬 Streaming films et séries
- ⭐ Système de favoris
- 🔍 Recherche avancée
- 🎨 Thèmes sombre/clair
- 💾 Sauvegarde progression

---

<div align="center">

**Fait avec ❤️ par [Pecorio](https://github.com/pecorio-dev)**

[⬆ Retour en haut](#-alphastream)

