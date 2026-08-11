package com.echecsapp.modele;

import java.util.ArrayList;
import java.util.List;

public class Pion extends Piece {

    public Pion(CouleurPiece couleur, Position position) {
        super(couleur, position);
    }

    /**
     * Retourne les avancées et captures diagonales standard.
     * ATTENTION : la prise en passant n'est pas incluse ici, car elle
     * dépend de l'historique de la partie (le dernier coup joué), pas
     * seulement de l'état du plateau. Elle sera ajoutée par le contrôleur.
     * La promotion (transformation en Dame/Tour/Fou/Cavalier) est aussi
     * gérée par le contrôleur une fois la case d'arrivée connue.
     */
    @Override
    public List<Position> mouvementsPossibles(Plateau plateau) {
        List<Position> mouvements = new ArrayList<>();

        // Le pion blanc avance vers les lignes croissantes, le noir vers les décroissantes
        int direction = (couleur == CouleurPiece.BLANC) ? 1 : -1;

        // Avancée d'une case
        Position uneCase = position.decaler(0, direction);
        if (uneCase.estValide() && plateau.getPiece(uneCase) == null) {
            mouvements.add(uneCase);

            // Avancée de deux cases, seulement si le pion n'a pas encore bougé
            if (!aDejaBouge) {
                Position deuxCases = position.decaler(0, direction * 2);
                if (deuxCases.estValide() && plateau.getPiece(deuxCases) == null) {
                    mouvements.add(deuxCases);
                }
            }
        }

        // Captures diagonales
        int[] deltasColonne = {-1, 1};
        for (int deltaColonne : deltasColonne) {
            Position diagonale = position.decaler(deltaColonne, direction);
            if (diagonale.estValide()) {
                Piece pieceAdverse = plateau.getPiece(diagonale);
                if (pieceAdverse != null && pieceAdverse.getCouleur() != this.couleur) {
                    mouvements.add(diagonale);
                }
            }
        }

        return mouvements;
    }

    @Override
    public char getSymbole() {
        return couleur == CouleurPiece.BLANC ? 'P' : 'p';
    }
}