package com.echecsapp.modele;

import java.util.List;

public class Roi extends Piece {

    private static final int[][] DELTAS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1},
            {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };

    public Roi(CouleurPiece couleur, Position position) {
        super(couleur, position);
    }

    /**
     * Retourne les déplacements d'une case dans toutes les directions.
     * ATTENTION : ceci ne filtre pas les cases attaquées par l'adversaire
     * (un roi ne peut pas se déplacer en échec), et n'inclut pas le roque.
     * Ces deux règles nécessitent une vue d'ensemble du plateau et seront
     * gérées au niveau du contrôleur de partie.
     */
    @Override
    public List<Position> mouvementsPossibles(Plateau plateau) {
        return mouvementsFixes(plateau, DELTAS);
    }

    @Override
    public char getSymbole() {
        return couleur == CouleurPiece.BLANC ? 'R' : 'r'; // R = Roi
    }
}