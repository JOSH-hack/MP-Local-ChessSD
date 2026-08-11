package com.echecsapp.modele;

import java.util.List;

public class Fou extends Piece {

    private static final int[][] DIRECTIONS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Fou(CouleurPiece couleur, Position position) {
        super(couleur, position);
    }

    @Override
    public List<Position> mouvementsPossibles(Plateau plateau) {
        return mouvementsEnLigne(plateau, DIRECTIONS);
    }

    @Override
    public char getSymbole() {
        return couleur == CouleurPiece.BLANC ? 'F' : 'f';
    }
}