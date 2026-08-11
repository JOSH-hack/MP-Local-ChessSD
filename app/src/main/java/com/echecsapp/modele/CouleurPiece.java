package com.echecsapp.modele;

public enum CouleurPiece {
    BLANC,
    NOIR;

    public CouleurPiece adverse() {
        return this == BLANC ? NOIR : BLANC;
    }
}