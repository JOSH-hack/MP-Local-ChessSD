package com.echecsapp.reseau;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;

import java.util.Collections;
import java.util.Set;

public class UtilitaireBluetooth {

    private UtilitaireBluetooth() {
        // classe utilitaire, pas d'instanciation
    }

    /**
     * Permissions à demander à l'exécution (runtime) avant toute utilisation
     * du Bluetooth. Diffèrent selon la version d'Android : les permissions
     * granulaires BLUETOOTH_CONNECT/SCAN/ADVERTISE remplacent l'ancienne
     * exigence de localisation à partir d'Android 12 (API 31).
     */
    public static String[] permissionsRequises() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE
            };
        } else {
            return new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
    }

    /**
     * Retourne les appareils déjà appairés (via les réglages Bluetooth du
     * téléphone, en dehors de l'app). C'est dans cette liste que
     * l'utilisateur choisira l'appareil à qui se connecter.
     */
    public static Set<BluetoothDevice> obtenirAppareilsApparies(BluetoothAdapter adaptateur) {
        try {
            return adaptateur.getBondedDevices();
        } catch (SecurityException e) {
            return Collections.emptySet();
        }
    }

    public static boolean bluetoothDisponibleEtActive(BluetoothAdapter adaptateur) {
        try {
            return adaptateur != null && adaptateur.isEnabled();
        } catch (SecurityException e) {
            return false;
        }
    }
}