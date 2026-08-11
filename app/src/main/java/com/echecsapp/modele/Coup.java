package com.echecsapp.modele;

public class Coup {

    private final Position depart;
    private final Position arrivee;
    private final Piece pieceDeplacee;
    private final Piece pieceCapturee; // null si pas de capture

    private boolean estPetitRoque;
    private boolean estGrandRoque;
    private boolean estPriseEnPassant;
    private boolean estPromotion;
    private char typePiecePromotion; // 'D', 'T', 'F', 'C' -- pertinent seulement si estPromotion

    public Coup(Position depart, Position arrivee, Piece pieceDeplacee, Piece pieceCapturee) {
        this.depart = depart;
        this.arrivee = arrivee;
        this.pieceDeplacee = pieceDeplacee;
        this.pieceCapturee = pieceCapturee;
    }

    public Position getDepart() {
        return depart;
    }

    public Position getArrivee() {
        return arrivee;
    }

    public Piece getPieceDeplacee() {
        return pieceDeplacee;
    }

    public Piece getPieceCapturee() {
        return pieceCapturee;
    }

    public boolean estCapture() {
        return pieceCapturee != null;
    }

    public boolean isEstPetitRoque() {
        return estPetitRoque;
    }

    public void setEstPetitRoque(boolean estPetitRoque) {
        this.estPetitRoque = estPetitRoque;
    }

    public boolean isEstGrandRoque() {
        return estGrandRoque;
    }

    public void setEstGrandRoque(boolean estGrandRoque) {
        this.estGrandRoque = estGrandRoque;
    }

    public boolean isEstPriseEnPassant() {
        return estPriseEnPassant;
    }

    public void setEstPriseEnPassant(boolean estPriseEnPassant) {
        this.estPriseEnPassant = estPriseEnPassant;
    }

    public boolean isEstPromotion() {
        return estPromotion;
    }

    public void setEstPromotion(boolean estPromotion) {
        this.estPromotion = estPromotion;
    }

    public char getTypePiecePromotion() {
        return typePiecePromotion;
    }

    public void setTypePiecePromotion(char typePiecePromotion) {
        this.typePiecePromotion = typePiecePromotion;
    }

    @Override
    public String toString() {
        return depart.enNotationAlgebrique() + " -> " + arrivee.enNotationAlgebrique();
    }
}