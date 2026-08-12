package com.echecsapp.reseau;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Implémentation Bluetooth de ConnexionPartie, via un socket RFCOMM classique.
 * Un appareil doit démarrer en hôte (demarrerEnTantQuHote), l'autre se
 * connecte à lui via seConnecterA(appareil) -- l'appareil cible doit être
 * choisi par l'utilisateur parmi les appareils déjà appairés.
 */
public class ConnexionBluetooth implements ConnexionPartie {

    // UUID fixe et arbitraire : doit être strictement identique des deux côtés.
    private static final UUID UUID_APPLICATION = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String NOM_SERVICE = "EchecsAppBluetooth";

    private final BluetoothAdapter adaptateur;
    private EcouteurConnexion ecouteur;

    private BluetoothServerSocket socketServeur;
    private BluetoothSocket socket;
    private volatile boolean connecte = false;

    public ConnexionBluetooth(BluetoothAdapter adaptateur) {
        this.adaptateur = adaptateur;
    }

    @Override
    public void definirEcouteur(EcouteurConnexion ecouteur) {
        this.ecouteur = ecouteur;
    }

    /**
     * Démarre l'écoute en tant qu'hôte : bloque en arrière-plan jusqu'à ce
     * qu'un autre appareil se connecte. Nécessite BLUETOOTH_CONNECT (API 31+)
     * déjà accordée par l'utilisateur -- voir UtilitaireBluetooth.
     */
    public void demarrerEnTantQuHote() {
        new Thread(() -> {
            try {
                socketServeur = adaptateur.listenUsingRfcommWithServiceRecord(NOM_SERVICE, UUID_APPLICATION);
                BluetoothSocket socketAccepte = socketServeur.accept(); // bloquant
                socketServeur.close();
                demarrerSession(socketAccepte);
            } catch (IOException e) {
                notifierErreur("Impossible de démarrer l'écoute Bluetooth : " + e.getMessage());
            } catch (SecurityException e) {
                notifierErreur("Permission Bluetooth manquante.");
            }
        }).start();
    }

    /**
     * Se connecte à un appareil déjà appairé. La sélection de l'appareil
     * (via une liste affichée à l'utilisateur) se fait côté Activity, en
     * s'appuyant sur UtilitaireBluetooth.obtenirAppareilsApparies().
     */
    public void seConnecterA(BluetoothDevice appareilCible) {
        new Thread(() -> {
            try {
                adaptateur.cancelDiscovery(); // la découverte active ralentit fortement la connexion
                BluetoothSocket nouveauSocket = appareilCible.createRfcommSocketToServiceRecord(UUID_APPLICATION);
                nouveauSocket.connect(); // bloquant
                demarrerSession(nouveauSocket);
            } catch (IOException e) {
                notifierErreur("Connexion Bluetooth échouée : " + e.getMessage());
            } catch (SecurityException e) {
                notifierErreur("Permission Bluetooth manquante.");
            }
        }).start();
    }

    private void demarrerSession(BluetoothSocket socketConnecte) {
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
                    notifierDeconnexion("Connexion Bluetooth perdue.");
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
}