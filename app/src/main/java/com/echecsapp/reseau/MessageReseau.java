package com.echecsapp.reseau;

import com.echecsapp.modele.Coup;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Format des messages échangés entre les deux appareils. Sérialisé en une
 * ligne de JSON (pas de retour à la ligne interne, donc compatible avec un
 * protocole simple "une ligne = un message" sur le flux Bluetooth/Wifi).
 */
public class MessageReseau {

    public enum Type {
        PROFIL,
        COUP,
        ABANDON,
        PROPOSITION_NULLE,
        ACCEPTATION_NULLE,
        REFUS_NULLE,
        PROPOSITION_REVANCHE,
        ACCEPTATION_REVANCHE
    }

    private final Type type;
    private final JSONObject donnees;

    public MessageReseau(Type type, JSONObject donnees) {
        this.type = type;
        this.donnees = donnees;
    }

    public Type getType() {
        return type;
    }

    public JSONObject getDonnees() {
        return donnees;
    }

    public String serialiser() {
        try {
            JSONObject racine = new JSONObject();
            racine.put("type", type.name());
            racine.put("donnees", donnees);
            return racine.toString();
        } catch (JSONException e) {
            throw new RuntimeException("Erreur de sérialisation du message réseau", e);
        }
    }

    public static MessageReseau depuis(String json) throws JSONException {
        JSONObject racine = new JSONObject(json);
        Type type = Type.valueOf(racine.getString("type"));
        JSONObject donnees = racine.getJSONObject("donnees");
        return new MessageReseau(type, donnees);
    }

    // ---- Fabriques pratiques ----

    public static MessageReseau pourProfil(String nom, String avatar) throws JSONException {
        JSONObject donnees = new JSONObject();
        donnees.put("nom", nom);
        donnees.put("avatar", avatar);
        return new MessageReseau(Type.PROFIL, donnees);
    }

    public static MessageReseau pourCoup(Coup coup) throws JSONException {
        JSONObject donnees = new JSONObject();
        donnees.put("departColonne", coup.getDepart().getColonne());
        donnees.put("departLigne", coup.getDepart().getLigne());
        donnees.put("arriveeColonne", coup.getArrivee().getColonne());
        donnees.put("arriveeLigne", coup.getArrivee().getLigne());
        if (coup.isEstPromotion()) {
            donnees.put("promotion", String.valueOf(coup.getTypePiecePromotion()));
        }
        return new MessageReseau(Type.COUP, donnees);
    }

    public static MessageReseau simple(Type type) {
        return new MessageReseau(type, new JSONObject());
    }
}