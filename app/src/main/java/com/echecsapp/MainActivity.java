package com.echecsapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.echecsapp.controleur.GestionnairePartie;
import com.echecsapp.modele.Coup;
import com.echecsapp.modele.CouleurPiece;
import com.echecsapp.modele.Partie;
import com.echecsapp.vue.EchiquierView;

public class MainActivity extends AppCompatActivity {

    private GestionnairePartie gestionnaire;
    private EchiquierView vueEchiquier;
    private TextView texteStatut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texteStatut = findViewById(R.id.texteStatut);
        vueEchiquier = findViewById(R.id.vueEchiquier);

        demarrerNouvellePartie();
    }

    private void demarrerNouvellePartie() {
        Partie partie = new Partie();
        gestionnaire = new GestionnairePartie(partie);

        vueEchiquier.setGestionnaire(gestionnaire);
        vueEchiquier.setOnCoupSelectionneListener(this::onCoupSelectionne);

        mettreAJourTexteStatut();
    }

    private void onCoupSelectionne(Coup coup) {
        if (coup.isEstPromotion()) {
            demanderTypePromotion(coup);
        } else {
            finaliserCoup(coup);
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
                    finaliserCoup(coup);
                })
                .show();
    }

    private void finaliserCoup(Coup coup) {
        gestionnaire.jouerCoup(coup);
        vueEchiquier.reinitialiserSelection();
        mettreAJourTexteStatut();
        verifierFinDePartie();
    }

    private void mettreAJourTexteStatut() {
        CouleurPiece joueurActuel = gestionnaire.getPartie().getJoueurActuel();
        boolean enEchec = com.echecsapp.controleur.ValidateurCoups.estRoiEnEchec(
                gestionnaire.getPartie().getPlateau(), joueurActuel);

        String nomJoueur = (joueurActuel == CouleurPiece.BLANC) ? "Blancs" : "Noirs";
        String texte = "Trait aux " + nomJoueur;
        if (enEchec) {
            texte += " — Échec !";
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
            case ECHEC_ET_MAT_BLANCS_GAGNENT:
                message = "Échec et mat — les Blancs gagnent !";
                break;
            case ECHEC_ET_MAT_NOIRS_GAGNENT:
                message = "Échec et mat — les Noirs gagnent !";
                break;
            case PAT:
                message = "Pat — partie nulle.";
                break;
            case NULLE_REPETITION:
                message = "Partie nulle par répétition de position.";
                break;
            case NULLE_CINQUANTE_COUPS:
                message = "Partie nulle — règle des 50 coups.";
                break;
            case NULLE_MATERIEL_INSUFFISANT:
                message = "Partie nulle — matériel insuffisant.";
                break;
            default:
                message = "Partie terminée.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Fin de partie")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Revanche", (dialog, which) -> demarrerNouvellePartie())
                .show();
    }
}