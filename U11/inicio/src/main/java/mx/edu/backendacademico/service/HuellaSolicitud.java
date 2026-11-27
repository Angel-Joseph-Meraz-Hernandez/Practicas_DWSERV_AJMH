package mx.edu.backendacademico.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HuellaSolicitud {
    private HuellaSolicitud() {}

    public static String calcular(String matricula, String nombre, String correo) {
        String contenido = String.join("|", matricula.trim().toUpperCase(), nombre.trim(),
                correo.trim().toLowerCase()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular la huella SHA-256", e);
        }
    }
    /*
    public static void main(String[] args) {
        String huella1 = calcular("a001", "Juan Perez", "JUAN@CORREO.COM");
        String huella2 = calcular("A001", "Juan Perez", "juan@correo.com");
        System.out.println("Huella 1: " + huella1);
        System.out.println("Huella 2: " + huella2);
        System.out.println("¿Son iguales?: " + huella1.equals(huella2));
        System.out.println("Longitud: " + huella1.length());
    }
    */
}
