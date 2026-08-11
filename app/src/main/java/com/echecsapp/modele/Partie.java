package com.echecsapp.modele;

import java.util.ArrayList;
import java.util.List;

public class Partie {

    private final Plateau plateau;
    private CouleurPiece joueurActuel;
    private final List<Coup> historique;
    private final List<String> historiquePositions; // pour la règle de répétition triple

    private boolean droitPetitRoqueBlanc;
    private boolean droitGrandRoqueBlanc;
    private boolean droitPetitRoqueNoir;
    private boolean droitGrandRoqueNoir;

    private Position cibleEnPassant; // case "derrière" un pion qui vient d'avancer de 2 cases, null sinon
    private int compteurDemiCoups; // pour la règle des 50 coups (remis à zéro à chaque capture ou coup de pion)

    public Partie() {
        this.plateau = Plateau.creerPositionDepart();
        this.joueurActuel = CouleurPiece.BLANC;
        this.historique = new ArrayList<>();
        this.historiquePositions = new ArrayList<>();

        this.droitPetitRoqueBlanc = true;
        this.droitGrandRoqueBlanc = true;
        this.droitPetitRoqueNoir = true;
        this.droitGrandRoqueNoir = true;

        this.cibleEnPassant = null;
        this.compteurDemiCoups = 0;
    }

    public Plateau getPlateau() {
        return plateau;
    }

    public CouleurPiece getJoueurActuel() {
        return joueurActuel;
    }

    public void setJoueurActuel(CouleurPiece joueurActuel) {
        this.joueurActuel = joueurActuel;
    }

    public List<Coup> getHistorique() {
        return historique;
    }

    public List<String> getHistoriquePositions() {
        return historiquePositions;
    }

    public boolean isDroitPetitRoqueBlanc() {
        return droitPetitRoqueBlanc;
    }

    public void setDroitPetitRoqueBlanc(boolean valeur) {
        this.droitPetitRoqueBlanc = valeur;
    }

    public boolean isDroitGrandRoqueBlanc() {
        return droitGrandRoqueBlanc;
    }

    public void setDroitGrandRoqueBlanc(boolean valeur) {
        this.droitGrandRoqueBlanc = valeur;
    }

    public boolean isDroitPetitRoqueNoir() {
        return droitPetitRoqueNoir;
    }

    public void setDroitPetitRoqueNoir(boolean valeur) {
        this.droitPetitRoqueNoir = valeur;
    }

    public boolean isDroitGrandRoqueNoir() {
        return droitGrandRoqueNoir;
    }

    public void setDroitGrandRoqueNoir(boolean valeur) {
        this.droitGrandRoqueNoir = valeur;
    }

    public Position getCibleEnPassant() {
        return cibleEnPassant;
    }

    public void setCibleEnPassant(Position cibleEnPassant) {
        this.cibleEnPassant = cibleEnPassant;
    }

    public int getCompteurDemiCoups() {
        return compteurDemiCoups;
    }

    public void setCompteurDemiCoups(int compteurDemiCoups) {
        this.compteurDemiCoups = compteurDemiCoups;
    }
}