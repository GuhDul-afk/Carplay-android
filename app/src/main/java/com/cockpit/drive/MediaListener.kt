package com.cockpit.drive

import android.service.notification.NotificationListenerService

/**
 * Service vide. Son rôle est seulement d'exister : Android demande qu'une app déclare
 * un "écouteur de notifications" pour l'autoriser à lire et piloter la musique en cours.
 */
class MediaListener : NotificationListenerService()
