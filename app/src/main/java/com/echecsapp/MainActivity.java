package com.echecsapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.echecsapp.controleur.GestionnairePartie;
import com.echecsapp.controleur.ValidateurCoups;
import com.echecsapp.modele.Coup;
import com.echecsapp.modele.CouleurPiece;
import com.echecsapp.modele.Partie;
import com.echecsapp.modele.Position;
import com.echecsapp.reseau.ConnexionBluetooth;
import com.echecsapp.reseau.ConnexionPartie;
import com.echecsapp.reseau.ConnexionWifi;
import com.echecsapp.reseau.MessageReseau;
import com.echecsapp.reseau.UtilitaireBluetooth;
import com.echecsapp.reseau.UtilitaireWifi;
import com.echecsapp.vue.EchiquierView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConnexionPartie.EcouteurConnexion {

    private GestionnairePartie gestionnaire;
    private EchiquierView vueEchiquier;
    private TextView texteStatut;

    private BluetoothAdapter adaptateurBluetooth;
    private ConnexionWifi connexionWifi;

    /** Connexion active du moment (Bluetooth ou Wifi) -- null en mode local. */
    private ConnexionPartie connexion;

    private String monPseudo = "Joueur";
    private String pseudoAdversaire = "Adversaire";
    private CouleurPiece couleurLocale; // null = mode local (pass-and-play)

    private boolean dialogueAppareilsWifiAffiche = false;

    private Runnable actionEnAttentePermission;

    private final ActivityResultLauncher<String[]> lanceurPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultats -> {
                boolean toutesAccordees = true;
                for (Boolean accordee : resultats.values()) {
                    toutesAccordees = toutesAccordees && Boolean.TRUE.equals(accordee);
                }

                if (toutesAccordees && actionEnAttentePermission != null) {
                    actionEnAttentePermission.run();
                } else if (!toutesAccordees) {
                    Toast.makeText(this, "Permissions refusées, mode réseau indisponible.", Toast.LENGTH_LONG).show();
                }
                actionEnAttentePermission = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texteStatut = findViewById(R.id.texteStatut);
        vueEchiquier = findViewById(R.id.vueEchiquier);
        vueEchiquier.setOnCoupSelectionneListener(this::onCoupSelectionne);

        findViewById(R.id.boutonAbandonner).setOnClickListener(v -> abandonner());
        findViewById(R.id.boutonProposerNulle).setOnClickListener(v -> proposerNulle());

        BluetoothManager gestionnaireBluetooth = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adaptateurBluetooth = gestionnaireBluetooth.getAdapter();
        connexionWifi = new ConnexionWifi(this);

        demarrerPartieLocale();
        afficherMenuPrincipal();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connexionWifi.enregistrerRecepteur();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connexionWifi.desenregistrerRecepteur();
    }

    // ---------- Démarrage / menu ----------

    private void demarrerPartieLocale() {
        Partie partie = new Partie();
        gestionnaire = new GestionnairePartie(partie);
        couleurLocale = null;

        vueEchiquier.setGestionnaire(gestionnaire);
        vueEchiquier.setCouleurLocale(null);
        mettreAJourTexteStatut();
    }

    private void afficherMenuPrincipal() {
        String[] options = {
                "Bluetooth — Héberger",
                "Bluetooth — Rejoindre",
                "Wifi Direct — Héberger",
                "Wifi Direct — Rejoindre",
                "Jouer en local"
        };

        new AlertDialog.Builder(this)
                .setTitle("Mode de jeu")
                .setItems(options, (d, index) -> {
                    switch (index) {
                        case 0:
                            demarrerAvecPermissions(() -> demanderPseudoPuis(this::demarrerEnTantQuHoteBluetooth),
                                    UtilitaireBluetooth.permissionsRequises());
                            break;
                        case 1:
                            demarrerAvecPermissions(() -> demanderPseudoPuis(this::afficherListeAppareilsApparies),
                                    UtilitaireBluetooth.permissionsRequises());
                            break;
                        case 2:
                            demarrerAvecPermissions(() -> demanderPseudoPuis(this::demarrerEnTantQuHoteWifi),
                                    UtilitaireWifi.permissionsRequises());
                            break;
                        case 3:
                            demarrerAvecPermissions(() -> demanderPseudoPuis(this::rejoindreEnWifi),
                                    UtilitaireWifi.permissionsRequises());
                            break;
                        case 4:
                            demarrerPartieLocale();
                            break;
                    }
                })
                .show();
    }

    private void demarrerAvecPermissions(Runnable action, String[] permissions) {
        actionEnAttentePermission = action;
        lanceurPermissions.launch(permissions);
    }

    private void demanderPseudoPuis(Runnable suite) {
        EditText champPseudo = new EditText(this);
        champPseudo.setHint("Votre pseudo");
        champPseudo.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Votre profil")
                .setView(champPseudo)
                .setCancelable(false)
                .setPositiveButton("Valider", (d, w) -> {
                    String saisie = champPseudo.getText().toString().trim();
                    monPseudo = saisie.isEmpty() ? "Joueur" : saisie;
                    suite.run();
                })
                .show();
    }

    // ---------- Bluetooth ----------

    private void demarrerEnTantQuHoteBluetooth() {
        if (!UtilitaireBluetooth.bluetoothDisponibleEtActive(adaptateurBluetooth)) {
            Toast.makeText(this, "Veuillez activer le Bluetooth puis réessayer.", Toast.LENGTH_LONG).show();
            return;
        }

        couleurLocale = CouleurPiece.BLANC; // convention : l'hôte joue les Blancs
        ConnexionBluetooth connexionBluetooth = new ConnexionBluetooth(adaptateurBluetooth);
        connexion = connexionBluetooth;
        connexion.definirEcouteur(this);
        connexionBluetooth.demarrerEnTantQuHote();

        Toast.makeText(this, "En attente d'un adversaire (Bluetooth)...", Toast.LENGTH_LONG).show();
    }

    private void afficherListeAppareilsApparies() {
        if (!UtilitaireBluetooth.bluetoothDisponibleEtActive(adaptateurBluetooth)) {
            Toast.makeText(this, "Veuillez activer le Bluetooth puis réessayer.", Toast.LENGTH_LONG).show();
            return;
        }

        List<BluetoothDevice> appareils = new ArrayList<>(UtilitaireBluetooth.obtenirAppareilsApparies(adaptateurBluetooth));

        if (appareils.isEmpty()) {
            Toast.makeText(this, "Aucun appareil appairé. Appairez d'abord l'autre téléphone dans les réglages Bluetooth.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] noms = new String[appareils.size()];
        for (int i = 0; i < appareils.size(); i++) {
            try {
                noms[i] = appareils.get(i).getName();
            } catch (SecurityException e) {
                noms[i] = "Appareil inconnu";
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choisir un appareil (Bluetooth)")
                .setItems(noms, (d, index) -> seConnecterEnBluetooth(appareils.get(index)))
                .show();
    }

    private void seConnecterEnBluetooth(BluetoothDevice appareil) {
        couleurLocale = CouleurPiece.NOIR; // convention : celui qui rejoint joue les Noirs
        ConnexionBluetooth connexionBluetooth = new ConnexionBluetooth(adaptateurBluetooth);
        connexion = connexionBluetooth;
        connexion.definirEcouteur(this);
        connexionBluetooth.seConnecterA(appareil);

        Toast.makeText(this, "Connexion Bluetooth en cours...", Toast.LENGTH_SHORT).show();
    }

    // ---------- Wifi Direct ----------

    private void demarrerEnTantQuHoteWifi() {
        couleurLocale = CouleurPiece.BLANC; // convention : l'hôte joue les Blancs
        connexion = connexionWifi;
        connexionWifi.definirEcouteur(this);
        connexionWifi.demarrerEnTantQuHote();

        Toast.makeText(this, "Création du groupe Wifi Direct, en attente d'un adversaire...", Toast.LENGTH_LONG).show();
    }

    private void rejoindreEnWifi() {
        couleurLocale = CouleurPiece.NOIR; // convention : celui qui rejoint joue les Noirs
        connexion = connexionWifi;
        connexionWifi.definirEcouteur(this);
        dialogueAppareilsWifiAffiche = false;

        connexionWifi.definirEcouteurDecouverte(appareils -> runOnUiThread(() -> afficherListeAppareilsWifi(appareils)));
        connexionWifi.demarrerDecouverte();

        Toast.makeText(this, "Recherche d'appareils Wifi Direct à proximité...", Toast.LENGTH_LONG).show();
    }

    private void afficherListeAppareilsWifi(List<WifiP2pDevice> appareils) {
        if (appareils.isEmpty() || dialogueAppareilsWifiAffiche) {
            return; // on attend d'autres résultats, ou la liste est déjà affichée
        }

        dialogueAppareilsWifiAffiche = true;
        String[] noms = new String[appareils.size()];
        for (int i = 0; i < appareils.size(); i++) {
            noms[i] = appareils.get(i).deviceName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Choisir un appareil (Wifi Direct)")
                .setItems(noms, (d, index) -> connexionWifi.seConnecterA(appareils.get(index)))
                .setOnCancelListener(d -> dialogueAppareilsWifiAffiche = false)
                .show();
    }

    // ---------- Callbacks réseau (communs Bluetooth + Wifi) ----------
    // ATTENTION : appelés depuis un thread réseau, tout ce qui touche l'UI passe par runOnUiThread.

    @Override
    public void onConnecte() {
        runOnUiThread(() -> {
            Partie partie = new Partie();
            gestionnaire = new GestionnairePartie(partie);
            vueEchiquier.setGestionnaire(gestionnaire);
            vueEchiquier.setCouleurLocale(couleurLocale);
            mettreAJourTexteStatut();

            Toast.makeText(this, "Connecté ! Vous jouez les " +
                    (couleurLocale == CouleurPiece.BLANC ? "Blancs" : "Noirs"), Toast.LENGTH_LONG).show();

            try {
                connexion.envoyerMessage(MessageReseau.pourProfil(monPseudo, "pion"));
            } catch (JSONException e) {
                // non bloquant : l'échange de profil est accessoire au déroulement du jeu
            }
        });
    }

    @Override
    public void onDeconnecte(String raison) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Connexion perdue")
                .setMessage(raison)
                .setCancelable(false)
                .setPositiveButton("Revenir au menu", (d, w) -> afficherMenuPrincipal())
                .show());
    }

    @Override
    public void onErreur(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onMessageRecu(MessageReseau message) {
        runOnUiThread(() -> traiterMessageRecu(message));
    }

    private void traiterMessageRecu(MessageReseau message) {
        try {
            switch (message.getType()) {
                case PROFIL:
                    pseudoAdversaire = message.getDonnees().getString("nom");
                    Toast.makeText(this, pseudoAdversaire + " a rejoint la partie", Toast.LENGTH_SHORT).show();
                    mettreAJourTexteStatut();
                    break;

                case COUP:
                    traiterCoupRecu(message);
                    break;

                case ABANDON:
                    new AlertDialog.Builder(this)
                            .setTitle("Partie terminée")
                            .setMessage(pseudoAdversaire + " a abandonné. Vous gagnez !")
                            .setPositiveButton("OK", null)
                            .show();
                    break;

                case PROPOSITION_NULLE:
                    new AlertDialog.Builder(this)
                            .setTitle("Proposition de nulle")
                            .setMessage(pseudoAdversaire + " propose de faire nulle. Accepter ?")
                            .setPositiveButton("Accepter", (d, w) -> {
                                connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.ACCEPTATION_NULLE));
                                afficherFinDePartieManuelle("Partie nulle, acceptée d'un commun accord.");
                            })
                            .setNegativeButton("Refuser", (d, w) ->
                                    connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.REFUS_NULLE)))
                            .show();
                    break;

                case ACCEPTATION_NULLE:
                    afficherFinDePartieManuelle("Partie nulle, acceptée d'un commun accord.");
                    break;

                case REFUS_NULLE:
                    Toast.makeText(this, pseudoAdversaire + " a refusé la nulle.", Toast.LENGTH_SHORT).show();
                    break;

                case PROPOSITION_REVANCHE:
                    new AlertDialog.Builder(this)
                            .setTitle("Revanche ?")
                            .setMessage(pseudoAdversaire + " propose une revanche.")
                            .setPositiveButton("Accepter", (d, w) -> {
                                connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.ACCEPTATION_REVANCHE));
                                redemarrerPartieReseau();
                            })
                            .setNegativeButton("Refuser", null)
                            .show();
                    break;

                case ACCEPTATION_REVANCHE:
                    redemarrerPartieReseau();
                    break;
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Message reçu corrompu, ignoré.", Toast.LENGTH_SHORT).show();
        }
    }

    private void traiterCoupRecu(MessageReseau message) throws JSONException {
        int departColonne = message.getDonnees().getInt("departColonne");
        int departLigne = message.getDonnees().getInt("departLigne");
        int arriveeColonne = message.getDonnees().getInt("arriveeColonne");
        int arriveeLigne = message.getDonnees().getInt("arriveeLigne");
        Character promotion = message.getDonnees().has("promotion")
                ? message.getDonnees().getString("promotion").charAt(0) : null;

        Position depart = new Position(departColonne, departLigne);
        Position arrivee = new Position(arriveeColonne, arriveeLigne);

        Coup coupValide = gestionnaire.trouverCoupLegal(depart, arrivee, promotion);
        if (coupValide == null) {
            Toast.makeText(this, "Coup reçu invalide : désynchronisation détectée.", Toast.LENGTH_LONG).show();
            return;
        }

        jouerEtRafraichir(coupValide);
    }

    // ---------- Jeu local ----------

    private void onCoupSelectionne(Coup coup) {
        if (coup.isEstPromotion()) {
            demanderTypePromotion(coup);
        } else {
            jouerCoupLocalEtEnvoyer(coup);
        }
    }

    private void demanderTypePromotion(Coup coup) {
        String[] options = {"Dame", "Tour", "Fou", "Cavalier"};
        char[] codes = {'D', 'T', 'F', 'C'};

        new AlertDialog.Builder(this)
                .setTitle("Promotion du pion")
                .setCancelable(false)
                .setItems(options, (dialog, index) -> {
                    coup.setTypePiecePromotion(codes[index]);
                    jouerCoupLocalEtEnvoyer(coup);
                })
                .show();
    }

    private void jouerCoupLocalEtEnvoyer(Coup coup) {
        jouerEtRafraichir(coup);

        if (connexion != null && connexion.estConnecte()) {
            try {
                connexion.envoyerMessage(MessageReseau.pourCoup(coup));
            } catch (JSONException e) {
                Toast.makeText(this, "Échec d'envoi du coup à l'adversaire.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void jouerEtRafraichir(Coup coup) {
        gestionnaire.jouerCoup(coup);
        vueEchiquier.reinitialiserSelection();
        mettreAJourTexteStatut();
        verifierFinDePartie();
    }

    // ---------- Abandon / nulle ----------

    private void abandonner() {
        if (connexion == null || !connexion.estConnecte()) {
            Toast.makeText(this, "Fonction disponible en partie réseau uniquement pour l'instant.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Abandonner")
                .setMessage("Confirmer l'abandon de la partie ?")
                .setPositiveButton("Abandonner", (d, w) -> {
                    connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.ABANDON));
                    afficherFinDePartieManuelle("Vous avez abandonné.");
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void proposerNulle() {
        if (connexion == null || !connexion.estConnecte()) {
            Toast.makeText(this, "Fonction disponible en partie réseau uniquement pour l'instant.", Toast.LENGTH_SHORT).show();
            return;
        }
        connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.PROPOSITION_NULLE));
        Toast.makeText(this, "Proposition de nulle envoyée.", Toast.LENGTH_SHORT).show();
    }

    // ---------- Statut / fin de partie ----------

    private void mettreAJourTexteStatut() {
        CouleurPiece joueurActuel = gestionnaire.getPartie().getJoueurActuel();
        boolean enEchec = ValidateurCoups.estRoiEnEchec(gestionnaire.getPartie().getPlateau(), joueurActuel);

        String nomJoueur = (joueurActuel == CouleurPiece.BLANC) ? "Blancs" : "Noirs";
        String texte = "Trait aux " + nomJoueur;
        if (enEchec) {
            texte += " — Échec !";
        }
        if (couleurLocale != null) {
            texte += (joueurActuel == couleurLocale) ? "  (à vous de jouer)" : "  (en attente de " + pseudoAdversaire + ")";
        }

        texteStatut.setText(texte);
    }

    private void verifierFinDePartie() {
        GestionnairePartie.ResultatPartie resultat = gestionnaire.evaluerResultat();
        if (resultat == GestionnairePartie.ResultatPartie.EN_COURS) {
            return;
        }

        String message;
        switch (resultat) {
            case ECHEC_ET_MAT_BLANCS_GAGNENT: message = "Échec et mat — les Blancs gagnent !"; break;
            case ECHEC_ET_MAT_NOIRS_GAGNENT: message = "Échec et mat — les Noirs gagnent !"; break;
            case PAT: message = "Pat — partie nulle."; break;
            case NULLE_REPETITION: message = "Partie nulle par répétition de position."; break;
            case NULLE_CINQUANTE_COUPS: message = "Partie nulle — règle des 50 coups."; break;
            case NULLE_MATERIEL_INSUFFISANT: message = "Partie nulle — matériel insuffisant."; break;
            default: message = "Partie terminée.";
        }

        afficherFinDePartieAvecRevanche(message);
    }

    private void afficherFinDePartieAvecRevanche(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Fin de partie")
                .setMessage(message)
                .setCancelable(false);

        if (connexion != null && connexion.estConnecte()) {
            builder.setPositiveButton("Proposer une revanche", (d, w) ->
                    connexion.envoyerMessage(MessageReseau.simple(MessageReseau.Type.PROPOSITION_REVANCHE)));
        } else {
            builder.setPositiveButton("Revanche", (d, w) -> demarrerPartieLocale());
        }

        builder.show();
    }

    private void afficherFinDePartieManuelle(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Fin de partie")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .show();
    }

    private void redemarrerPartieReseau() {
        Partie partie = new Partie();
        gestionnaire = new GestionnairePartie(partie);
        couleurLocale = couleurLocale.adverse(); // on alterne les couleurs à chaque revanche

        vueEchiquier.setGestionnaire(gestionnaire);
        vueEchiquier.setCouleurLocale(couleurLocale);
        mettreAJourTexteStatut();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connexion != null) {
            connexion.fermerConnexion();
        }
    }
}