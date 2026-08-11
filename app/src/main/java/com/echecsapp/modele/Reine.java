package com.echecsapp.modele;

import java.util.List;

public class Reine extends Piece {

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},   // comme la Tour
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}  // comme le Fou
    };

    public Reine(CouleurPiece couleur, Position position) {
        super(couleur, position);
    }

    @Override
    public List<Position> mouvementsPossibles(Plateau plateau) {
        return mouvementsEnLigne(plateau, DIRECTIONS);
    }

    @Override
    public char getSymbole() {
        return couleur == CouleurPiece.BLANC ? 'D' : 'd'; // D = Dame
    }
}