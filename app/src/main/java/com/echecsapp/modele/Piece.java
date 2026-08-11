package com.echecsapp.modele;

import java.util.ArrayList;
import java.util.List;

public abstract class Piece {

    protected CouleurPiece couleur;
    protected Position position;
    protected boolean aDejaBouge;

    public Piece(CouleurPiece couleur, Position position) {
        this.couleur = couleur;
        this.position = position;
        this.aDejaBouge = false;
    }

    /**
     * Retourne la liste des cases atteignables par cette pièce selon son
     * schéma de déplacement, en tenant compte des pièces bloquantes sur le
     * plateau. Ce sont des coups "pseudo-légaux" : ils respectent le
     * déplacement de la pièce mais ne vérifient pas encore si le roi se
     * retrouve en échec après le coup (ça, c'est le rôle du contrôleur/
     * Plateau à un niveau supérieur).
     */
    public abstract List<Position> mouvementsPossibles(Plateau plateau);

    /** Symbole utilisé pour la notation FEN/SAN (majuscule = blanc, minuscule = noir géré ailleurs). */
    public abstract char getSymbole();

    public CouleurPiece getCouleur() {
        return couleur;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
        this.aDejaBouge = true;
    }

    public boolean isADejaBouge() {
        return aDejaBouge;
    }

    /**
     * Change la position SANS marquer la pièce comme ayant bougé.
     * Réservé à la simulation de coups par le validateur (test d'échec),
     * qui doit pouvoir annuler un coup sans corrompre les droits de roque.
     * Ne jamais utiliser ceci pour un coup réellement joué : voir setPosition().
     */
    public void definirPositionSansHistorique(Position position) {
        this.position = position;
    }

    /** Réservé à la simulation de coups par le validateur, pour restaurer l'état d'origine. */
    public void definirADejaBouge(boolean valeur) {
        this.aDejaBouge = valeur;
    }

    /**
     * Utilitaire pour les pièces "glissantes" (Tour, Fou, Reine) : avance
     * dans une direction donnée jusqu'à sortir du plateau, rencontrer une
     * pièce alliée (arrêt avant), ou une pièce adverse (arrêt après capture).
     */
    protected List<Position> mouvementsEnLigne(Plateau plateau, int[][] directions) {
        List<Position> mouvements = new ArrayList<>();

        for (int[] direction : directions) {
            Position curseur = position.decaler(direction[0], direction[1]);

            while (curseur.estValide()) {
                Piece pieceSurCase = plateau.getPiece(curseur);

                if (pieceSurCase == null) {
                    mouvements.add(curseur);
                } else {
                    if (pieceSurCase.getCouleur() != this.couleur) {
                        mouvements.add(curseur); // capture possible
                    }
                    break; // bloqué dans cette direction, alliée ou ennemie
                }

                curseur = curseur.decaler(direction[0], direction[1]);
            }
        }

        return mouvements;
    }

    /**
     * Utilitaire pour les pièces à mouvement fixe (Roi, Cavalier) : teste
     * chaque décalage donné une seule fois.
     */
    protected List<Position> mouvementsFixes(Plateau plateau, int[][] deltas) {
        List<Position> mouvements = new ArrayList<>();

        for (int[] delta : deltas) {
            Position cible = position.decaler(delta[0], delta[1]);

            if (!cible.estValide()) {
                continue;
            }

            Piece pieceSurCase = plateau.getPiece(cible);
            if (pieceSurCase == null || pieceSurCase.getCouleur() != this.couleur) {
                mouvements.add(cible);
            }
        }

        return mouvements;
    }
}