package com.echecsapp.controleur;

import com.echecsapp.modele.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestionnairePartie {

    public enum ResultatPartie {
        EN_COURS,
        ECHEC_ET_MAT_BLANCS_GAGNENT,
        ECHEC_ET_MAT_NOIRS_GAGNENT,
        PAT,
        NULLE_REPETITION,
        NULLE_CINQUANTE_COUPS,
        NULLE_MATERIEL_INSUFFISANT
    }

    private final Partie partie;

    public GestionnairePartie(Partie partie) {
        this.partie = partie;
    }

    public Partie getPartie() {
        return partie;
    }

    /**
     * Retourne tous les coups légaux du joueur au trait. Utile pour l'IHM
     * (surligner les cases jouables) et pour détecter mat/pat (liste vide).
     */
    public List<Coup> genererTousLesCoupsLegaux() {
        List<Coup> tousLesCoups = new ArrayList<>();
        Plateau plateau = partie.getPlateau();
        CouleurPiece couleur = partie.getJoueurActuel();

        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                Position position = new Position(colonne, ligne);
                Piece piece = plateau.getPiece(position);

                if (piece == null || piece.getCouleur() != couleur) {
                    continue;
                }

                if (piece instanceof Pion) {
                    tousLesCoups.addAll(ValidateurCoups.genererCoupsPion(partie, (Pion) piece));
                } else {
                    for (Position destination : ValidateurCoups.mouvementsLegaux(piece, plateau)) {
                        Piece pieceCapturee = plateau.getPiece(destination);
                        tousLesCoups.add(new Coup(position, destination, piece, pieceCapturee));
                    }
                }
            }
        }

        tousLesCoups.addAll(ValidateurCoups.genererCoupsRoque(partie));
        return tousLesCoups;
    }

    /**
     * Joue un coup : applique tous les effets de bord (roque, prise en
     * passant, promotion, mise à jour des droits de roque et de la cible
     * en passant), enregistre l'historique, et passe la main.
     * IMPORTANT : ne vérifie pas la légalité du coup. N'appelez cette
     * méthode qu'avec un Coup obtenu via genererTousLesCoupsLegaux().
     */
    public void jouerCoup(Coup coup) {
        Plateau plateau = partie.getPlateau();

        if (coup.estCapture() || coup.getPieceDeplacee() instanceof Pion) {
            partie.setCompteurDemiCoups(0);
        } else {
            partie.setCompteurDemiCoups(partie.getCompteurDemiCoups() + 1);
        }

        if (coup.isEstPriseEnPassant()) {
            Position positionPionCapture = new Position(coup.getArrivee().getColonne(), coup.getDepart().getLigne());
            plateau.retirerPiece(positionPionCapture);
        }

        plateau.deplacerPieceBrut(coup.getDepart(), coup.getArrivee());

        if (coup.isEstPetitRoque() || coup.isEstGrandRoque()) {
            int ligne = coup.getDepart().getLigne();
            if (coup.isEstPetitRoque()) {
                plateau.deplacerPieceBrut(new Position(7, ligne), new Position(5, ligne));
            } else {
                plateau.deplacerPieceBrut(new Position(0, ligne), new Position(3, ligne));
            }
        }

        if (coup.isEstPromotion()) {
            appliquerPromotion(coup);
        }

        mettreAJourDroitsDeRoque(coup);
        mettreAJourCibleEnPassant(coup);

        partie.getHistorique().add(coup);
        partie.getHistoriquePositions().add(genererSignaturePosition());
        partie.setJoueurActuel(partie.getJoueurActuel().adverse());
    }

    private void appliquerPromotion(Coup coup) {
        Plateau plateau = partie.getPlateau();
        CouleurPiece couleur = coup.getPieceDeplacee().getCouleur();
        Position position = coup.getArrivee();

        Piece nouvellePiece;
        switch (coup.getTypePiecePromotion()) {
            case 'T':
                nouvellePiece = new Tour(couleur, position);
                break;
            case 'F':
                nouvellePiece = new Fou(couleur, position);
                break;
            case 'C':
                nouvellePiece = new Cavalier(couleur, position);
                break;
            default:
                nouvellePiece = new Reine(couleur, position); // Dame par défaut
        }

        plateau.retirerPiece(position);
        plateau.placerPiece(nouvellePiece);
    }

    private void mettreAJourDroitsDeRoque(Coup coup) {
        Piece piece = coup.getPieceDeplacee();

        if (piece instanceof Roi) {
            if (piece.getCouleur() == CouleurPiece.BLANC) {
                partie.setDroitPetitRoqueBlanc(false);
                partie.setDroitGrandRoqueBlanc(false);
            } else {
                partie.setDroitPetitRoqueNoir(false);
                partie.setDroitGrandRoqueNoir(false);
            }
        }

        if (piece instanceof Tour) {
            Position depart = coup.getDepart();
            if (depart.equals(new Position(0, 0))) partie.setDroitGrandRoqueBlanc(false);
            if (depart.equals(new Position(7, 0))) partie.setDroitPetitRoqueBlanc(false);
            if (depart.equals(new Position(0, 7))) partie.setDroitGrandRoqueNoir(false);
            if (depart.equals(new Position(7, 7))) partie.setDroitPetitRoqueNoir(false);
        }
    }

    private void mettreAJourCibleEnPassant(Coup coup) {
        Piece piece = coup.getPieceDeplacee();
        boolean avanceDeDeuxCases = piece instanceof Pion
                && Math.abs(coup.getArrivee().getLigne() - coup.getDepart().getLigne()) == 2;

        if (avanceDeDeuxCases) {
            int ligneIntermediaire = (coup.getDepart().getLigne() + coup.getArrivee().getLigne()) / 2;
            partie.setCibleEnPassant(new Position(coup.getDepart().getColonne(), ligneIntermediaire));
        } else {
            partie.setCibleEnPassant(null);
        }
    }

    /** Signature de la position (placement + trait + droits de roque + cible en passant), pour la règle de répétition triple. */
    private String genererSignaturePosition() {
        StringBuilder sb = new StringBuilder();
        Plateau plateau = partie.getPlateau();

        for (int ligne = 7; ligne >= 0; ligne--) {
            for (int colonne = 0; colonne < 8; colonne++) {
                Piece piece = plateau.getPiece(new Position(colonne, ligne));
                sb.append(piece == null ? '.' : piece.getSymbole());
            }
        }
        sb.append(partie.getJoueurActuel());
        sb.append(partie.isDroitPetitRoqueBlanc()).append(partie.isDroitGrandRoqueBlanc());
        sb.append(partie.isDroitPetitRoqueNoir()).append(partie.isDroitGrandRoqueNoir());
        sb.append(partie.getCibleEnPassant());

        return sb.toString();
    }

    /** Évalue l'état de la partie APRÈS le dernier coup joué : en cours, mat, pat, ou nulle. */
    public ResultatPartie evaluerResultat() {
        boolean roiEnEchec = ValidateurCoups.estRoiEnEchec(partie.getPlateau(), partie.getJoueurActuel());
        boolean aucunCoupLegal = genererTousLesCoupsLegaux().isEmpty();

        if (aucunCoupLegal) {
            if (roiEnEchec) {
                return (partie.getJoueurActuel() == CouleurPiece.BLANC)
                        ? ResultatPartie.ECHEC_ET_MAT_NOIRS_GAGNENT
                        : ResultatPartie.ECHEC_ET_MAT_BLANCS_GAGNENT;
            }
            return ResultatPartie.PAT;
        }

        if (partie.getCompteurDemiCoups() >= 100) { // 50 coups complets = 100 demi-coups
            return ResultatPartie.NULLE_CINQUANTE_COUPS;
        }

        if (repetitionTripleAtteinte()) {
            return ResultatPartie.NULLE_REPETITION;
        }

        if (materielInsuffisant()) {
            return ResultatPartie.NULLE_MATERIEL_INSUFFISANT;
        }

        return ResultatPartie.EN_COURS;
    }

    private boolean repetitionTripleAtteinte() {
        Map<String, Integer> occurrences = new HashMap<>();
        for (String signature : partie.getHistoriquePositions()) {
            int compte = occurrences.getOrDefault(signature, 0) + 1;
            occurrences.put(signature, compte);
            if (compte >= 3) {
                return true;
            }
        }
        return false;
    }

    /** Version simplifiée : couvre les cas les plus courants (K seul, K+F, K+C contre K seul). */
    private boolean materielInsuffisant() {
        Plateau plateau = partie.getPlateau();
        List<Piece> autresPieces = new ArrayList<>();

        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                Piece piece = plateau.getPiece(new Position(colonne, ligne));
                if (piece != null && !(piece instanceof Roi)) {
                    autresPieces.add(piece);
                }
            }
        }

        if (autresPieces.isEmpty()) {
            return true; // roi contre roi
        }
        if (autresPieces.size() == 1) {
            Piece seulePiece = autresPieces.get(0);
            return (seulePiece instanceof Fou) || (seulePiece instanceof Cavalier);
        }

        return false;
    }
}