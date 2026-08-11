package com.echecsapp.controleur;

import com.echecsapp.modele.*;

import java.util.ArrayList;
import java.util.List;

public class ValidateurCoups {

    private ValidateurCoups() {
        // classe utilitaire, pas d'instanciation
    }

    /** Retourne vrai si le roi de la couleur donnée est actuellement attaqué. */
    public static boolean estRoiEnEchec(Plateau plateau, CouleurPiece couleur) {
        Position positionRoi = trouverRoi(plateau, couleur);
        if (positionRoi == null) {
            return false; // ne devrait pas arriver en jeu normal
        }
        return caseEstAttaquee(plateau, positionRoi, couleur.adverse());
    }

    private static Position trouverRoi(Plateau plateau, CouleurPiece couleur) {
        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                Position position = new Position(colonne, ligne);
                Piece piece = plateau.getPiece(position);
                if (piece instanceof Roi && piece.getCouleur() == couleur) {
                    return position;
                }
            }
        }
        return null;
    }

    /** Retourne vrai si la case cible est attaquée par au moins une pièce de la couleur donnée. */
    public static boolean caseEstAttaquee(Plateau plateau, Position cible, CouleurPiece parCouleur) {
        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                Position position = new Position(colonne, ligne);
                Piece piece = plateau.getPiece(position);
                if (piece != null && piece.getCouleur() == parCouleur) {
                    if (piece.mouvementsPossibles(plateau).contains(cible)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Retourne les mouvements légaux d'une pièce : les mouvements pseudo-légaux
     * (schéma de déplacement seul) filtrés pour exclure ceux qui laisseraient
     * son propre roi en échec.
     */
    public static List<Position> mouvementsLegaux(Piece piece, Plateau plateau) {
        List<Position> resultat = new ArrayList<>();

        for (Position destination : piece.mouvementsPossibles(plateau)) {
            if (!coupLaisseRoiEnEchec(plateau, piece.getPosition(), destination, piece.getCouleur())) {
                resultat.add(destination);
            }
        }

        return resultat;
    }

    /**
     * Simule un coup standard (déplacement + capture éventuelle sur la case
     * d'arrivée) puis l'annule immédiatement, pour tester s'il expose le roi.
     * Ne clone pas le plateau entier : déplace, teste, restaure.
     */
    private static boolean coupLaisseRoiEnEchec(Plateau plateau, Position depart, Position arrivee, CouleurPiece couleur) {
        Piece pieceDeplacee = plateau.getPiece(depart);
        Piece pieceCapturee = plateau.getPiece(arrivee);
        boolean etatADejaBougeAvant = pieceDeplacee.isADejaBouge();

        plateau.retirerPiece(depart);
        plateau.retirerPiece(arrivee);
        pieceDeplacee.definirPositionSansHistorique(arrivee);
        plateau.placerPiece(pieceDeplacee);

        boolean roiEnEchec = estRoiEnEchec(plateau, couleur);

        // Annulation de la simulation
        plateau.retirerPiece(arrivee);
        pieceDeplacee.definirPositionSansHistorique(depart);
        pieceDeplacee.definirADejaBouge(etatADejaBougeAvant);
        plateau.placerPiece(pieceDeplacee);
        if (pieceCapturee != null) {
            plateau.placerPiece(pieceCapturee);
        }

        return roiEnEchec;
    }

    /**
     * Génère les coups de roque légaux pour le joueur au trait.
     * Conditions vérifiées : roi et tour concernés n'ont jamais bougé,
     * cases entre eux vides, roi pas actuellement en échec, et aucune
     * case traversée par le roi n'est attaquée.
     */
    public static List<Coup> genererCoupsRoque(Partie partie) {
        List<Coup> coups = new ArrayList<>();
        Plateau plateau = partie.getPlateau();
        CouleurPiece couleur = partie.getJoueurActuel();
        int ligne = (couleur == CouleurPiece.BLANC) ? 0 : 7;

        if (estRoiEnEchec(plateau, couleur)) {
            return coups; // interdit de roquer en étant en échec
        }

        Position positionRoi = new Position(4, ligne);
        Piece roi = plateau.getPiece(positionRoi);
        if (!(roi instanceof Roi) || roi.isADejaBouge()) {
            return coups;
        }

        boolean droitPetit = (couleur == CouleurPiece.BLANC)
                ? partie.isDroitPetitRoqueBlanc() : partie.isDroitPetitRoqueNoir();
        boolean droitGrand = (couleur == CouleurPiece.BLANC)
                ? partie.isDroitGrandRoqueBlanc() : partie.isDroitGrandRoqueNoir();

        CouleurPiece adversaire = couleur.adverse();

        // Petit roque (côté roi : colonnes f et g)
        if (droitPetit) {
            Position caseF = new Position(5, ligne);
            Position caseG = new Position(6, ligne);
            Piece tour = plateau.getPiece(new Position(7, ligne));

            if (tour instanceof Tour && !tour.isADejaBouge()
                    && plateau.estCaseVide(caseF) && plateau.estCaseVide(caseG)
                    && !caseEstAttaquee(plateau, caseF, adversaire)
                    && !caseEstAttaquee(plateau, caseG, adversaire)) {

                Coup coup = new Coup(positionRoi, caseG, roi, null);
                coup.setEstPetitRoque(true);
                coups.add(coup);
            }
        }

        // Grand roque (côté dame : colonnes b, c, d)
        if (droitGrand) {
            Position caseB = new Position(1, ligne);
            Position caseC = new Position(2, ligne);
            Position caseD = new Position(3, ligne);
            Piece tour = plateau.getPiece(new Position(0, ligne));

            if (tour instanceof Tour && !tour.isADejaBouge()
                    && plateau.estCaseVide(caseB) && plateau.estCaseVide(caseC) && plateau.estCaseVide(caseD)
                    && !caseEstAttaquee(plateau, caseC, adversaire)
                    && !caseEstAttaquee(plateau, caseD, adversaire)) {

                Coup coup = new Coup(positionRoi, caseC, roi, null);
                coup.setEstGrandRoque(true);
                coups.add(coup);
            }
        }

        return coups;
    }

    /**
     * Génère tous les coups légaux d'un pion donné : avancées/captures
     * standard (avec drapeau de promotion si la dernière rangée est
     * atteinte), plus la prise en passant si applicable.
     */
    public static List<Coup> genererCoupsPion(Partie partie, Pion pion) {
        List<Coup> coups = new ArrayList<>();
        Plateau plateau = partie.getPlateau();

        for (Position destination : mouvementsLegaux(pion, plateau)) {
            Piece pieceCapturee = plateau.getPiece(destination);
            Coup coup = new Coup(pion.getPosition(), destination, pion, pieceCapturee);
            if (destination.getLigne() == 0 || destination.getLigne() == 7) {
                coup.setEstPromotion(true);
            }
            coups.add(coup);
        }

        Position cible = partie.getCibleEnPassant();
        if (cible != null) {
            int direction = (pion.getCouleur() == CouleurPiece.BLANC) ? 1 : -1;
            boolean adjacentAGauche = pion.getPosition().equals(cible.decaler(-1, -direction));
            boolean adjacentADroite = pion.getPosition().equals(cible.decaler(1, -direction));

            if (adjacentAGauche || adjacentADroite) {
                Position positionPionAdverse = new Position(cible.getColonne(), pion.getPosition().getLigne());
                Piece pionAdverse = plateau.getPiece(positionPionAdverse);

                if (pionAdverse instanceof Pion && pionAdverse.getCouleur() != pion.getCouleur()
                        && !coupEnPassantLaisseRoiEnEchec(plateau, pion, cible, positionPionAdverse)) {

                    Coup coup = new Coup(pion.getPosition(), cible, pion, pionAdverse);
                    coup.setEstPriseEnPassant(true);
                    coups.add(coup);
                }
            }
        }

        return coups;
    }

    /**
     * Simulation dédiée à la prise en passant : la pièce capturée n'est pas
     * sur la case d'arrivée mais à côté, il faut donc une logique distincte
     * de coupLaisseRoiEnEchec().
     */
    private static boolean coupEnPassantLaisseRoiEnEchec(Plateau plateau, Piece pion, Position destination, Position positionPionAdverse) {
        Position depart = pion.getPosition();
        Piece pionAdverse = plateau.getPiece(positionPionAdverse);
        CouleurPiece couleur = pion.getCouleur();

        plateau.retirerPiece(depart);
        plateau.retirerPiece(positionPionAdverse);
        pion.definirPositionSansHistorique(destination);
        plateau.placerPiece(pion);

        boolean roiEnEchec = estRoiEnEchec(plateau, couleur);

        // Annulation
        plateau.retirerPiece(destination);
        pion.definirPositionSansHistorique(depart);
        plateau.placerPiece(pion);
        plateau.placerPiece(pionAdverse);

        return roiEnEchec;
    }
}