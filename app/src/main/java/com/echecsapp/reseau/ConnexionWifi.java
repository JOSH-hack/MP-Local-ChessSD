package com.echecsapp.reseau;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation Wifi Direct de ConnexionPartie. Un appareil crée le groupe
 * (demarrerEnTantQuHote -- devient automatiquement propriétaire du groupe,
 * donc "serveur"), l'autre découvre les appareils à proximité puis se
 * connecte (seConnecterA -- devient "client"). Une fois le groupe formé,
 * une simple connexion TCP est ouverte entre les deux pour échanger les
 * messages de jeu.
 */
public class ConnexionWifi implements ConnexionPartie {

    private static final int PORT_APPLICATION = 8988;

    public interface EcouteurDecouverte {
        void onListeAppareilsMiseAJour(List<WifiP2pDevice> appareils);
    }

    private final Context contexte;
    private final WifiP2pManager gestionnaireP2P;
    private final WifiP2pManager.Channel canal;
    private final RecepteurWifiDirect recepteur;

    private EcouteurConnexion ecouteur;
    private EcouteurDecouverte ecouteurDecouverte;

    private Socket socket;
    private ServerSocket socketServeur;
    private volatile boolean connecte = false;

    public ConnexionWifi(Context contexte) {
        this.contexte = contexte.getApplicationContext();
        this.gestionnaireP2P = (WifiP2pManager) contexte.getSystemService(Context.WIFI_P2P_SERVICE);
        this.canal = gestionnaireP2P.initialize(contexte, contexte.getMainLooper(), null);
        this.recepteur = new RecepteurWifiDirect();
    }

    @Override
    public void definirEcouteur(EcouteurConnexion ecouteur) {
        this.ecouteur = ecouteur;
    }

    public void definirEcouteurDecouverte(EcouteurDecouverte ecouteurDecouverte) {
        this.ecouteurDecouverte = ecouteurDecouverte;
    }

    /** À appeler depuis onResume() de l'Activity : commence à écouter les événements système Wifi Direct. */
    public void enregistrerRecepteur() {
        IntentFilter filtre = new IntentFilter();
        filtre.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filtre.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filtre.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filtre.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        contexte.registerReceiver(recepteur, filtre);
    }

    /** À appeler depuis onPause() de l'Activity. */
    public void desenregistrerRecepteur() {
        try {
            contexte.unregisterReceiver(recepteur);
        } catch (IllegalArgumentException ignored) {
            // déjà désenregistré, rien à faire
        }
    }

    /**
     * Crée un groupe Wifi Direct en forçant CET appareil comme propriétaire
     * du groupe -- équivalent du rôle "hôte" côté Bluetooth. Évite de
     * dépendre de la négociation automatique, qui pourrait désigner
     * l'autre appareil comme propriétaire.
     */
    public void demarrerEnTantQuHote() {
        try {
            gestionnaireP2P.createGroup(canal, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    demarrerServeurTcp();
                }

                @Override
                public void onFailure(int raison) {
                    notifierErreur("Impossible de créer le groupe Wifi Direct (code " + raison + ").");
                }
            });
        } catch (SecurityException e) {
            notifierErreur("Permission Wifi manquante.");
        }
    }

    /** Lance la recherche des appareils à proximité ayant Wifi Direct actif. Les résultats arrivent via EcouteurDecouverte. */
    public void demarrerDecouverte() {
        try {
            gestionnaireP2P.discoverPeers(canal, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // résultats effectifs livrés via WIFI_P2P_PEERS_CHANGED_ACTION (capté par le récepteur)
                }

                @Override
                public void onFailure(int raison) {
                    notifierErreur("Recherche Wifi Direct impossible (code " + raison + ").");
                }
            });
        } catch (SecurityException e) {
            notifierErreur("Permission Wifi manquante.");
        }
    }

    /** Se connecte à un appareil trouvé par la découverte. L'appareil ciblé reste propriétaire du groupe (rôle "hôte"). */
    public void seConnecterA(WifiP2pDevice appareilCible) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = appareilCible.deviceAddress;

        try {
            gestionnaireP2P.connect(canal, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // connexion effective confirmée via WIFI_P2P_CONNECTION_CHANGED_ACTION
                }

                @Override
                public void onFailure(int raison) {
                    notifierErreur("Connexion Wifi Direct échouée (code " + raison + ").");
                }
            });
        } catch (SecurityException e) {
            notifierErreur("Permission Wifi manquante.");
        }
    }

    private void demarrerServeurTcp() {
        new Thread(() -> {
            try {
                socketServeur = new ServerSocket(PORT_APPLICATION);
                Socket socketAccepte = socketServeur.accept(); // bloquant jusqu'à connexion entrante
                demarrerSession(socketAccepte);
            } catch (IOException e) {
                notifierErreur("Impossible de démarrer le serveur Wifi : " + e.getMessage());
            }
        }).start();
    }

    private void seConnecterEnTcp(InetAddress adresseHote) {
        new Thread(() -> {
            try {
                Socket nouveauSocket = new Socket();
                nouveauSocket.connect(new InetSocketAddress(adresseHote, PORT_APPLICATION), 10000);
                demarrerSession(nouveauSocket);
            } catch (IOException e) {
                notifierErreur("Connexion TCP Wifi échouée : " + e.getMessage());
            }
        }).start();
    }

    private void demarrerSession(Socket socketConnecte) {
        this.socket = socketConnecte;
        this.connecte = true;

        if (ecouteur != null) {
            ecouteur.onConnecte();
        }

        new Thread(() -> {
            try {
                BufferedReader lecteur = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String ligne;
                while (connecte && (ligne = lecteur.readLine()) != null) {
                    try {
                        MessageReseau message = MessageReseau.depuis(ligne);
                        if (ecouteur != null) {
                            ecouteur.onMessageRecu(message);
                        }
                    } catch (JSONException e) {
                        notifierErreur("Message reçu invalide, ignoré.");
                    }
                }
                if (connecte) {
                    notifierDeconnexion("L'autre appareil a fermé la connexion.");
                }
            } catch (IOException e) {
                if (connecte) {
                    notifierDeconnexion("Connexion Wifi perdue.");
                }
            }
        }).start();
    }

    @Override
    public void envoyerMessage(MessageReseau message) {
        if (!connecte || socket == null) {
            notifierErreur("Impossible d'envoyer : pas de connexion active.");
            return;
        }

        new Thread(() -> {
            try {
                OutputStream sortie = socket.getOutputStream();
                String ligne = message.serialiser() + "\n";
                sortie.write(ligne.getBytes(StandardCharsets.UTF_8));
                sortie.flush();
            } catch (IOException e) {
                notifierDeconnexion("Échec d'envoi, connexion perdue.");
            }
        }).start();
    }

    @Override
    public void fermerConnexion() {
        connecte = false;
        try {
            if (socket != null) socket.close();
            if (socketServeur != null) socketServeur.close();
        } catch (IOException ignored) {
            // fermeture "best effort"
        }
        try {
            gestionnaireP2P.removeGroup(canal, null);
        } catch (SecurityException ignored) {
            // rien à faire si la permission n'est déjà plus là
        }
    }

    @Override
    public boolean estConnecte() {
        return connecte;
    }

    private void notifierErreur(String message) {
        if (ecouteur != null) {
            ecouteur.onErreur(message);
        }
    }

    private void notifierDeconnexion(String raison) {
        connecte = false;
        if (ecouteur != null) {
            ecouteur.onDeconnecte(raison);
        }
    }

    /** Capte les événements système Wifi Direct (découverte, changement de connexion...). */
    private class RecepteurWifiDirect extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                try {
                    gestionnaireP2P.requestPeers(canal, peers -> {
                        if (ecouteurDecouverte != null) {
                            ecouteurDecouverte.onListeAppareilsMiseAJour(new ArrayList<>(peers.getDeviceList()));
                        }
                    });
                } catch (SecurityException e) {
                    notifierErreur("Permission Wifi manquante pour lister les appareils.");
                }

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                try {
                    gestionnaireP2P.requestConnectionInfo(canal, info -> {
                        if (info.groupFormed && !info.isGroupOwner && info.groupOwnerAddress != null) {
                            // rôle "invité" : on ouvre la connexion TCP vers le propriétaire du groupe
                            seConnecterEnTcp(info.groupOwnerAddress);
                        }
                        // si isGroupOwner == true, le serveur TCP a déjà été démarré dans demarrerEnTantQuHote()
                    });
                } catch (SecurityException e) {
                    notifierErreur("Permission Wifi manquante.");
                }
            }
        }
    }
}