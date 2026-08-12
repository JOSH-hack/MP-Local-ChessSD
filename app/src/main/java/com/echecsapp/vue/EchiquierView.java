package com.echecsapp.vue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.echecsapp.controleur.GestionnairePartie;
import com.echecsapp.modele.*;

import java.util.ArrayList;
import java.util.List;

public class EchiquierView extends View {

    /** Callback vers l'Activity : lui transmet le coup choisi par l'utilisateur (dont elle vérifie/finalise la légalité et la promotion). */
    public interface OnCoupSelectionneListener {
        void onCoupSelectionne(Coup coup);
    }

    private GestionnairePartie gestionnaire;
    private OnCoupSelectionneListener listener;

    /**
     * Couleur jouable sur CET appareil. null = mode local (pass-and-play,
     * les deux couleurs sont jouables sur le même écran). En mode réseau,
     * fixée à BLANC ou NOIR selon le rôle (hôte/invité) : on ne peut alors
     * sélectionner que ses propres pièces, même quand c'est son tour.
     */
    private CouleurPiece couleurLocale;

    public void setCouleurLocale(CouleurPiece couleurLocale) {
        this.couleurLocale = couleurLocale;
    }

    private final Paint paintCaseClaire = new Paint();
    private final Paint paintCaseFoncee = new Paint();
    private final Paint paintSelection = new Paint();
    private final Paint paintCoupLegal = new Paint();
    private final Paint paintPiece = new Paint();

    private float tailleCase;
    private Position caseSelectionnee;
    private List<Coup> coupsLegauxDepuisSelection = new ArrayList<>();

    public EchiquierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiserPinceaux();
    }

    private void initialiserPinceaux() {
        paintCaseClaire.setColor(Color.parseColor("#EEEED2"));
        paintCaseFoncee.setColor(Color.parseColor("#769656"));

        paintSelection.setColor(Color.parseColor("#88F6F669"));
        paintCoupLegal.setColor(Color.parseColor("#8877AACC"));

        paintPiece.setTextAlign(Paint.Align.CENTER);
        paintPiece.setAntiAlias(true);
        paintPiece.setColor(Color.BLACK);
    }

    public void setGestionnaire(GestionnairePartie gestionnaire) {
        this.gestionnaire = gestionnaire;
        invalidate();
    }

    public void setOnCoupSelectionneListener(OnCoupSelectionneListener listener) {
        this.listener = listener;
    }

    /** Réinitialise la sélection en cours (à appeler après qu'un coup a été joué). */
    public void reinitialiserSelection() {
        caseSelectionnee = null;
        coupsLegauxDepuisSelection = new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        tailleCase = Math.min(w, h) / 8f;
        paintPiece.setTextSize(tailleCase * 0.7f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gestionnaire == null) {
            return;
        }

        dessinerCases(canvas);
        dessinerSurbrillances(canvas);
        dessinerPieces(canvas);
    }

    private void dessinerCases(Canvas canvas) {
        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                boolean estClaire = (colonne + ligne) % 2 != 0;
                Paint paint = estClaire ? paintCaseClaire : paintCaseFoncee;

                float x = colonne * tailleCase;
                // ligne 0 = rang 1 (en bas de l'écran), donc on inverse l'axe Y à l'affichage
                float y = (7 - ligne) * tailleCase;

                canvas.drawRect(x, y, x + tailleCase, y + tailleCase, paint);
            }
        }
    }

    private void dessinerSurbrillances(Canvas canvas) {
        if (caseSelectionnee != null) {
            dessinerSurbrillanceCase(canvas, caseSelectionnee, paintSelection);
        }
        for (Coup coup : coupsLegauxDepuisSelection) {
            dessinerSurbrillanceCase(canvas, coup.getArrivee(), paintCoupLegal);
        }
    }

    private void dessinerSurbrillanceCase(Canvas canvas, Position position, Paint paint) {
        float x = position.getColonne() * tailleCase;
        float y = (7 - position.getLigne()) * tailleCase;
        canvas.drawRect(x, y, x + tailleCase, y + tailleCase, paint);
    }

    private void dessinerPieces(Canvas canvas) {
        Plateau plateau = gestionnaire.getPartie().getPlateau();

        for (int colonne = 0; colonne < 8; colonne++) {
            for (int ligne = 0; ligne < 8; ligne++) {
                Piece piece = plateau.getPiece(new Position(colonne, ligne));
                if (piece == null) {
                    continue;
                }

                float centreX = colonne * tailleCase + tailleCase / 2f;
                float centreY = (7 - ligne) * tailleCase + tailleCase / 2f - (paintPiece.ascent() + paintPiece.descent()) / 2f;

                paintPiece.setColor(piece.getCouleur() == CouleurPiece.BLANC ? Color.WHITE : Color.BLACK);
                canvas.drawText(glyphePourPiece(piece), centreX, centreY, paintPiece);

                // léger contour pour que les pièces blanches restent visibles sur case claire
                if (piece.getCouleur() == CouleurPiece.BLANC) {
                    Paint contour = new Paint(paintPiece);
                    contour.setStyle(Paint.Style.STROKE);
                    contour.setStrokeWidth(2f);
                    contour.setColor(Color.BLACK);
                    canvas.drawText(glyphePourPiece(piece), centreX, centreY, contour);
                }
            }
        }
    }

    private String glyphePourPiece(Piece piece) {
        boolean blanc = piece.getCouleur() == CouleurPiece.BLANC;

        if (piece instanceof Roi) return blanc ? "\u2654" : "\u265A";
        if (piece instanceof Reine) return blanc ? "\u2655" : "\u265B";
        if (piece instanceof Tour) return blanc ? "\u2656" : "\u265C";
        if (piece instanceof Fou) return blanc ? "\u2657" : "\u265D";
        if (piece instanceof Cavalier) return blanc ? "\u2658" : "\u265E";
        if (piece instanceof Pion) return blanc ? "\u2659" : "\u265F";
        return "?";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || gestionnaire == null) {
            return true;
        }

        int colonneTouchee = (int) (event.getX() / tailleCase);
        int ligneTouchee = 7 - (int) (event.getY() / tailleCase);
        Position positionTouchee = new Position(colonneTouchee, ligneTouchee);

        if (!positionTouchee.estValide()) {
            return true;
        }

        if (caseSelectionnee == null) {
            tenterSelection(positionTouchee);
        } else {
            tenterDeplacement(positionTouchee);
        }

        return true;
    }

    private void tenterSelection(Position position) {
        Plateau plateau = gestionnaire.getPartie().getPlateau();
        Piece piece = plateau.getPiece(position);

        if (piece == null || piece.getCouleur() != gestionnaire.getPartie().getJoueurActuel()) {
            return; // rien à sélectionner ici (case vide ou pièce adverse)
        }

        if (couleurLocale != null && piece.getCouleur() != couleurLocale) {
            return; // mode réseau : on ne peut pas déplacer les pièces de l'adversaire
        }

        caseSelectionnee = position;
        coupsLegauxDepuisSelection = new ArrayList<>();

        for (Coup coup : gestionnaire.genererTousLesCoupsLegaux()) {
            if (coup.getDepart().equals(position)) {
                coupsLegauxDepuisSelection.add(coup);
            }
        }

        invalidate();
    }

    private void tenterDeplacement(Position destination) {
        // Si l'utilisateur retape sur une de ses propres pièces, on change la sélection plutôt que d'annuler
        Plateau plateau = gestionnaire.getPartie().getPlateau();
        Piece pieceSurDestination = plateau.getPiece(destination);
        if (pieceSurDestination != null && pieceSurDestination.getCouleur() == gestionnaire.getPartie().getJoueurActuel()) {
            tenterSelection(destination);
            return;
        }

        for (Coup coup : coupsLegauxDepuisSelection) {
            if (coup.getArrivee().equals(destination)) {
                if (listener != null) {
                    listener.onCoupSelectionne(coup);
                }
                return;
            }
        }

        // Case tapée hors des coups légaux : on annule simplement la sélection
        reinitialiserSelection();
    }
}