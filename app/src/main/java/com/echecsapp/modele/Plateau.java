package com.echecsapp.modele;

public class Plateau {

    private final Piece[][] cases; // [colonne][ligne]

    public Plateau() {
        cases = new Piece[8][8];
    }

    /** Crée un plateau vide (utile pour les tests unitaires du moteur). */
    public static Plateau creerPlateauVide() {
        return new Plateau();
    }

    /** Crée un plateau avec la position de départ standard des échecs. */
    public static Plateau creerPositionDepart() {
        Plateau plateau = new Plateau();

        // Pions
        for (int colonne = 0; colonne < 8; colonne++) {
            plateau.placerPiece(new Pion(CouleurPiece.BLANC, new Position(colonne, 1)));
            plateau.placerPiece(new Pion(CouleurPiece.NOIR, new Position(colonne, 6)));
        }

        // Pièces majeures et mineures, blancs (ligne 0) puis noirs (ligne 7)
        int[] colonnesTours = {0, 7};
        int[] colonnesCavaliers = {1, 6};
        int[] colonnesFous = {2, 5};

        for (int colonne : colonnesTours) {
            plateau.placerPiece(new Tour(CouleurPiece.BLANC, new Position(colonne, 0)));
            plateau.placerPiece(new Tour(CouleurPiece.NOIR, new Position(colonne, 7)));
        }
        for (int colonne : colonnesCavaliers) {
            plateau.placerPiece(new Cavalier(CouleurPiece.BLANC, new Position(colonne, 0)));
            plateau.placerPiece(new Cavalier(CouleurPiece.NOIR, new Position(colonne, 7)));
        }
        for (int colonne : colonnesFous) {
            plateau.placerPiece(new Fou(CouleurPiece.BLANC, new Position(colonne, 0)));
            plateau.placerPiece(new Fou(CouleurPiece.NOIR, new Position(colonne, 7)));
        }

        plateau.placerPiece(new Reine(CouleurPiece.BLANC, new Position(3, 0)));
        plateau.placerPiece(new Reine(CouleurPiece.NOIR, new Position(3, 7)));

        plateau.placerPiece(new Roi(CouleurPiece.BLANC, new Position(4, 0)));
        plateau.placerPiece(new Roi(CouleurPiece.NOIR, new Position(4, 7)));

        return plateau;
    }

    public Piece getPiece(Position position) {
        if (!position.estValide()) {
            return null;
        }
        return cases[position.getColonne()][position.getLigne()];
    }

    public void placerPiece(Piece piece) {
        Position position = piece.getPosition();
        cases[position.getColonne()][position.getLigne()] = piece;
    }

    public void retirerPiece(Position position) {
        if (position.estValide()) {
            cases[position.getColonne()][position.getLigne()] = null;
        }
    }

    public boolean estCaseVide(Position position) {
        return getPiece(position) == null;
    }

    public boolean estCaseOccupeeParAdversaire(Position position, CouleurPiece couleur) {
        Piece piece = getPiece(position);
        return piece != null && piece.getCouleur() != couleur;
    }

    /**
     * Déplace une pièce d'une case à une autre, sans aucune validation de
     * légalité (échec, coup autorisé...). Cette méthode "brute" est utilisée
     * par le contrôleur de partie, qui se charge lui de vérifier la légalité
     * avant d'appeler ce déplacement, et de gérer les cas spéciaux (roque,
     * prise en passant, promotion).
     */
    public void deplacerPieceBrut(Position depart, Position arrivee) {
        Piece piece = getPiece(depart);
        if (piece == null) {
            return;
        }
        retirerPiece(depart);
        piece.setPosition(arrivee);
        placerPiece(piece);
    }
}