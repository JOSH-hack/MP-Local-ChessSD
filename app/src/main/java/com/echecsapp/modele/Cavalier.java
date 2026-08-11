package com.echecsapp.modele;

import java.util.List;

public class Cavalier extends Piece {

    private static final int[][] DELTAS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    public Cavalier(CouleurPiece couleur, Position position) {
        super(couleur, position);
    }

    @Override
    public List<Position> mouvementsPossibles(Plateau plateau) {
        return mouvementsFixes(plateau, DELTAS);
    }

    @Override
    public char getSymbole() {
        return couleur == CouleurPiece.BLANC ? 'C' : 'c';
    }
}