package com.echecsapp.reseau;

/**
 * Abstraction commune à tous les modes de connexion (Bluetooth, Wifi Direct...).
 * MainActivity ne dépend que de cette interface, pas des implémentations
 * concrètes -- ça permet de brancher Wifi plus tard sans toucher à la
 * logique de synchronisation des coups.
 */
public interface ConnexionPartie {

    interface EcouteurConnexion {
        /** Appelé une fois la connexion établie avec l'autre appareil. */
        void onConnecte();

        /** Appelé si la connexion est perdue ou fermée, avec une raison lisible. */
        void onDeconnecte(String raison);

        /** Appelé à chaque message reçu de l'autre appareil. */
        void onMessageRecu(MessageReseau message);

        /** Appelé en cas d'erreur non fatale (ex: échec d'envoi ponctuel). */
        void onErreur(String message);
    }

    /**
     * IMPORTANT : ces callbacks sont invoqués depuis un thread réseau, PAS le
     * thread principal. Toute mise à jour d'UI dans l'implémentation de
     * EcouteurConnexion doit être enveloppée dans runOnUiThread(...).
     */
    void definirEcouteur(EcouteurConnexion ecouteur);

    void envoyerMessage(MessageReseau message);

    void fermerConnexion();

    boolean estConnecte();
}