# Proyecto: Restaurante SIFood - Sistema de Gestión de Restaurante 
Descripción
SIFOOD es una aplicación de escritorio completa para la gestión integral de un restaurante, desarrollada como proyecto académico. El sistema ofrece una solución robusta para la administración de personal, menús, inventario y el flujo completo de atención al cliente, desde la asignación de mesas hasta la facturación.

La aplicación está construida en Java Swing y se conecta a una base de datos PostgreSQL para la persistencia de datos. El proyecto demuestra conceptos clave de desarrollo de software, diseño de bases de datos relacionales, seguridad informática y arquitectura de aplicaciones.

Características Principales y Vistas
Login Seguro por Roles: Autenticación de usuarios contra la base de datos, con contraseñas protegidas mediante hashing (pgcrypto). El sistema distingue entre los roles de Administrador y Mesero.

Panel de Administrador: Un centro de control con múltiples pestañas para:

Gestión de Usuarios: Crear nuevos empleados (meseros, cocineros).

Gestión de Inventario: Actualizar el stock de ingredientes.

Dashboard de Métricas: Visualizar reportes clave como los platos más vendidos y el empleado del mes.

Panel de Mesero (Cajero): Una interfaz visual e interactiva para la gestión del salón.

Mapa de Mesas: Muestra el estado de todas las mesas en tiempo real (Disponible/Ocupada) mediante un código de colores.

Lógica de Estados: Las acciones disponibles cambian dependiendo del estado de la mesa seleccionada.

Ventana de Toma de Pedido: Una interfaz rica para tomar pedidos.

Menú Visual: Muestra los platos con imágenes, descripción y precio en una tabla.

Carrito de Compras: Un resumen del pedido actual que se actualiza en tiempo real.

Registro de Pedidos: Guarda la orden en la base de datos de forma transaccional y segura.

# Tecnologías Utilizadas
Lenguaje: Java (OpenJDK 21)

Interfaz Gráfica (Frontend): Java Swing

Base de Datos (Backend): PostgreSQL

Conexión a BD: JDBC (Driver de PostgreSQL)

IDE: IntelliJ IDEA Community Edition

Herramientas de BD: pgAdmin 4, dbdiagram.io

Empaquetado: Launch4j (para la creación del .exe)

Instalación y Puesta en Marcha
# Para ejecutar este proyecto en un entorno de desarrollo, sigue estos pasos:

* Prerrequisitos:

* Tener instalado un JDK 21 o superior.

* Tener un servidor de PostgreSQL accesible (puede ser local o en la nube).

* Tener Git instalado.

* Clonar el Repositorio:

* git clone https://github.com/JuanL525/Restaurant_SIFood.git
cd Restaurante_SIFood

* Configurar la Base de Datos:

* Crea una nueva base de datos en tu servidor PostgreSQL.

* Abre el archivo SIFood_DB.sql que se encuentra en el repositorio.

* Ejecuta el script completo en tu nueva base de datos usando pgAdmin. Esto creará todas las tablas, vistas, procedimientos y datos de ejemplo.

* Configurar el Proyecto en IntelliJ IDEA:

* Abre IntelliJ y selecciona File -> Open... y elige la carpeta Restaurante_SIFood que clonaste.

* Ve a File -> Project Structure... -> Modules -> Dependencies.

* Haz clic en + -> JARs or Directories... y añade la librería de PostgreSQL que se encuentra en la carpeta /lib.

* Navega al archivo src/utils/DatabaseConnection.java y actualiza las constantes HOST, DATABASE, USER y PASSWORD con las credenciales de tu base de datos.

# Ejecutar la Aplicación:

Encuentra la clase Main.java y haz clic derecho -> Run 'Main.main()'.

Uso de la Aplicación
Una vez ejecutada la aplicación, puedes usar las siguientes credenciales de ejemplo (basadas en el script SQL) para probar los diferentes roles:

# Credenciales
Usuario Administrador:

Usuario: admin
Contraseña: 123

Usuario Mesero:

Usuario: juan
Contraseña: 123

Autor
Juan Lucero

juan.lucero@epn.edu.ec
