package utils;

public class SesionUsuario {
    private static int id;
    private static String nombreCompleto;
    private static String nombreUsuario;
    private static String rol;

    // Se llama al iniciar sesión
    public static void iniciarSesion(int id, String nombreCompleto, String nombreUsuario, String rol) {
        SesionUsuario.id = id;
        SesionUsuario.nombreCompleto = nombreCompleto;
        SesionUsuario.nombreUsuario = nombreUsuario;
        SesionUsuario.rol = rol;
    }

    // Se llama al cerrar sesión
    public static void cerrarSesion() {
        SesionUsuario.id = 0;
        SesionUsuario.nombreCompleto = null;
        SesionUsuario.nombreUsuario = null;
        SesionUsuario.rol = null;
    }

    // Métodos para obtener los datos del usuario actual
    public static int getId() {
        return id;
    }

    public static String getNombreCompleto() {
        return nombreCompleto;
    }

    public static String getRol() {
        return rol;
    }
}