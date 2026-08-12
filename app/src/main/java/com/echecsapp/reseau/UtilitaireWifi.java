package com.echecsapp.reseau;

import android.os.Build;

public class UtilitaireWifi {

    private UtilitaireWifi() {
        // classe utilitaire, pas d'instanciation
    }

    /**
     * Permissions nécessaires pour la découverte et la connexion Wifi Direct.
     * NEARBY_WIFI_DEVICES remplace en partie ACCESS_FINE_LOCATION depuis
     * Android 13 (API 33), mais on garde les deux par prudence : certains
     * fabricants exigent encore la localisation pour le scan Wifi Direct.
     */
    public static String[] permissionsRequises() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    android.Manifest.permission.NEARBY_WIFI_DEVICES,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            return new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
    }
}