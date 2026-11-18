package Seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public class Seguridad {

    private static final String ALGORITMO = "SHA-256";

    // un salt distinto por usuario evita hashes iguales si repiten la contrasenia
    public static String generarSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return aHexadecimal(bytes);
    }

    public static String hashear(String contrasenia, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITMO);
            byte[] hash = md.digest((salt + contrasenia).getBytes(StandardCharsets.UTF_8));
            return aHexadecimal(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo no disponible: " + ALGORITMO);
        }
    }

    private static String aHexadecimal(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

        /*
        public static void() {
            String salt = Seguridad.generarSalt();
            System.out.println("salt: " + salt);
            System.out.println("hash: " + Seguridad.hashear("1234", salt));
        }
        */


}