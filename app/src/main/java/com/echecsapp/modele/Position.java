package com.echecsapp.modele;

import java.util.Objects;

/**
 * Représente une case du plateau.
 * colonne : 0 à 7 (correspond à a-h)
 * ligne   : 0 à 7 (correspond à 1-8)
 */
public class Position {

    private final int colonne;
    private final int ligne;

    public Position(int colonne, int ligne) {
        this.colonne = colonne;
        this.ligne = ligne;
    }

    public int getColonne() {
        return colonne;
    }

    public int getLigne() {
        return ligne;
    }

    public boolean estValide() {
        return colonne >= 0 && colonne <= 7 && ligne >= 0 && ligne <= 7;
    }

    /** Retourne une nouvelle position décalée, sans vérifier la validité. */
    public Position decaler(int deltaColonne, int deltaLigne) {
        return new Position(colonne + deltaColonne, ligne + deltaLigne);
    }

    /** Notation type "e4" pour affichage/debug. */
    public String enNotationAlgebrique() {
        char lettreColonne = (char) ('a' + colonne);
        int numeroLigne = ligne + 1;
        return "" + lettreColonne + numeroLigne;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return colonne == position.colonne && ligne == position.ligne;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colonne, ligne);
    }

    @Override
    public String toString() {
        return enNotationAlgebrique();
    }
}