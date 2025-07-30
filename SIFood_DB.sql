-- #############################################################################
-- #                                                                           #
-- #                     SCRIPT DE BASE DE DATOS PARA SIFOOD                   #
-- #                        PostgreSQL                                         #
-- #                                                                           #
-- #############################################################################


DROP SCHEMA IF EXISTS sifood_schema CASCADE;

-- #############################################################################
-- SECCIÓN 1: CREACIÓN DE ESQUEMA Y ROLES DE BASE DE DATOS
-- #############################################################################
-- Creamos un esquema para organizar todos nuestros objetos de base de datos.
CREATE SCHEMA sifood_schema;
SET search_path TO sifood_schema;

COMMENT ON SCHEMA sifood_schema IS 'Esquema principal para el sistema de gestión de restaurante SIFood.';

-- Creación de roles personalizados para la gestión de la BD
-- ROL DE ADMINISTRADOR DE BD: Control total sobre la base de datos.
CREATE ROLE administrador_db WITH LOGIN PASSWORD 'admin_pass_secure' CREATEDB CREATEROLE;
COMMENT ON ROLE administrador_db IS 'Rol con máximos privilegios para administrar la estructura de la BD.';

-- ROL DE OPERADOR: Rol que usará la aplicación Java Swing para conectarse.
CREATE ROLE operador_app WITH LOGIN PASSWORD 'operador_pass_secure';
COMMENT ON ROLE operador_app IS 'Rol para la aplicación SIFood. Tiene permisos para operar el día a día (INSERT, SELECT, UPDATE).';

-- ROL DE AUDITOR: Solo puede leer datos para auditoría y reportes.
CREATE ROLE auditor_db WITH LOGIN PASSWORD 'auditor_pass_secure';
COMMENT ON ROLE auditor_db IS 'Rol de solo lectura para fines de auditoría y análisis de datos.';

-- Roles adicionales para demostración de privilegios
CREATE ROLE proveedor_externo;
CREATE ROLE cliente_fidelizado;


-- #############################################################################
-- SECCIÓN 2: CREACIÓN DE TABLAS (CATÁLOGOS Y TABLAS FUNCIONALES)
-- #############################################################################

-- Tablas de Catálogos y Parámetros
CREATE TABLE categorias_platos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT
);
COMMENT ON TABLE categorias_platos IS 'Categorías para los platos del menú (Ej: Entradas, Platos Fuertes, Bebidas).';

CREATE TABLE estados_pedido (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT
);
COMMENT ON TABLE estados_pedido IS 'Posibles estados de un pedido (Ej: Solicitado, En Cocina, Servido, Pagado).';

CREATE TABLE turnos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL
);
COMMENT ON TABLE turnos IS 'Turnos de trabajo del restaurante (Ej: Almuerzo, Cena).';

-- Tablas Funcionales 
CREATE TABLE ingredientes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    stock_disponible NUMERIC(10, 2) NOT NULL CHECK (stock_disponible >= 0),
    unidad_medida VARCHAR(20) NOT NULL -- Ej: 'gramos', 'unidades', 'ml'
);
COMMENT ON TABLE ingredientes IS 'Control de inventario de ingredientes para los platos.';

CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    clave_hash TEXT NOT NULL, -- Almacenar siempre el hash de la clave
    rol_app VARCHAR(20) NOT NULL CHECK (rol_app IN ('Administrador', 'Mesero', 'Cocinero')),
    activo BOOLEAN DEFAULT TRUE,
    -- Validación a nivel de BD para el formato del nombre de usuario
    CONSTRAINT chk_nombre_usuario_formato CHECK (nombre_usuario ~ '^[a-zA-Z0-9_]{4,20}$')
);
COMMENT ON TABLE usuarios IS 'Usuarios que pueden iniciar sesión en la aplicación SIFood';

CREATE TABLE mesas (
    id SERIAL PRIMARY KEY,
    numero_mesa INT NOT NULL UNIQUE CHECK (numero_mesa > 0),
    capacidad INT NOT NULL CHECK (capacidad > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'disponible' CHECK (estado IN ('disponible', 'ocupada', 'reservada'))
);
COMMENT ON TABLE mesas IS 'Mesas físicas del restaurante y su estado actual.';

CREATE TABLE platos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    precio NUMERIC(10, 2) NOT NULL CHECK (precio > 0),
    categoria_id INT NOT NULL REFERENCES categorias_platos(id),
    disponible BOOLEAN DEFAULT TRUE
);
COMMENT ON TABLE platos IS 'Catálogo de platos que ofrece el restaurante.';

CREATE TABLE plato_ingredientes (
    plato_id INT NOT NULL REFERENCES platos(id) ON DELETE CASCADE,
    ingrediente_id INT NOT NULL REFERENCES ingredientes(id) ON DELETE RESTRICT,
    cantidad_necesaria NUMERIC(10, 2) NOT NULL CHECK (cantidad_necesaria > 0),
    PRIMARY KEY (plato_id, ingrediente_id)
);
COMMENT ON TABLE plato_ingredientes IS 'Tabla de enlace para definir la receta de cada plato.';

CREATE TABLE pedidos (
    id SERIAL PRIMARY KEY,
    mesa_id INT NOT NULL REFERENCES mesas(id),
    usuario_id_mesero INT NOT NULL REFERENCES usuarios(id),
    turno_id INT NOT NULL REFERENCES turnos(id),
    estado_id INT NOT NULL REFERENCES estados_pedido(id),
    numero_comensales INT NOT NULL DEFAULT 1 CHECK (numero_comensales > 0),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMPTZ,
    subtotal NUMERIC(10, 2) DEFAULT 0.00,
    propina NUMERIC(10, 2) DEFAULT 0.00,
    total NUMERIC(10, 2) DEFAULT 0.00
);
COMMENT ON TABLE pedidos IS 'Cabecera de cada pedido realizado en el restaurante.';

CREATE TABLE detalle_pedidos (
    id SERIAL PRIMARY KEY,
    pedido_id INT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    plato_id INT NOT NULL REFERENCES platos(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario_congelado NUMERIC(10, 2) NOT NULL 
);
COMMENT ON TABLE detalle_pedidos IS 'Líneas de detalle para cada plato dentro de un pedido.';


-- #############################################################################
-- SECCIÓN 3: TABLAS DE AUDITORÍA Y CONTROL
-- #############################################################################

CREATE TABLE log_acciones (
    id BIGSERIAL PRIMARY KEY,
    fecha_accion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_bd VARCHAR(100),
    ip_cliente INET,
    rol_activo VARCHAR(100),
    operacion VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
    nombre_tabla VARCHAR(100) NOT NULL,
    id_afectado VARCHAR(100),
    transaccion_ejecutada TEXT,
    datos_antiguos_jsonb JSONB -- Para control de versiones de registros
);
COMMENT ON TABLE log_acciones IS 'Bitácora centralizada que registra todas las operaciones críticas en la BD.';


-- #############################################################################
-- SECCIÓN 4: GESTIÓN DE PRIVILEGIOS (GRANT / REVOKE)
-- #############################################################################

-- Dar privilegios básicos de conexión y uso del esquema
GRANT CONNECT ON DATABASE neondb TO operador_app, auditor_db; 
GRANT USAGE ON SCHEMA sifood_schema TO operador_app, auditor_db, proveedor_externo;

-- Privilegios para el ROL OPERADOR
GRANT SELECT, INSERT, UPDATE, DELETE ON
    pedidos, detalle_pedidos, mesas
TO operador_app;
GRANT SELECT, UPDATE ON ingredientes, platos TO operador_app;
GRANT SELECT ON categorias_platos, estados_pedido, turnos, usuarios TO operador_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sifood_schema TO operador_app;

-- Privilegios para el ROL AUDITOR
GRANT SELECT ON ALL TABLES IN SCHEMA sifood_schema TO auditor_db;

-- Privilegios para el ROL PROVEEDOR
GRANT SELECT, UPDATE (stock_disponible) ON ingredientes TO proveedor_externo;
REVOKE INSERT, DELETE ON ingredientes FROM proveedor_externo;


-- #############################################################################
-- SECCIÓN 5: FUNCIONES Y TRIGGERS PARA AUDITORÍA 
-- #############################################################################

-- Función para Auditoría
CREATE OR REPLACE FUNCTION fn_log_acciones()
RETURNS TRIGGER AS $$
DECLARE
    v_id_afectado TEXT;
BEGIN
    -- Determinar el ID del registro afectado
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        v_id_afectado := NEW.id;
    ELSE
        v_id_afectado := OLD.id;
    END IF;

    -- Insertar en la tabla de log
    INSERT INTO log_acciones (
        usuario_bd, ip_cliente, rol_activo, operacion,
        nombre_tabla, id_afectado, transaccion_ejecutada, datos_antiguos_jsonb
    )
    VALUES (
        current_user,           -- Usuario de la BD
        inet_client_addr(),     -- IP del cliente
        current_role,           -- Rol activo en la sesión
        TG_OP,                  -- Operación (INSERT, UPDATE, DELETE)
        TG_TABLE_NAME,          -- Tabla afectada
        v_id_afectado,          -- ID del registro
        current_query(),        -- Sentencia SQL ejecutada
        CASE                    -- Guardar datos antiguos en UPDATE/DELETE
            WHEN TG_OP = 'UPDATE' THEN to_jsonb(OLD)
            WHEN TG_OP = 'DELETE' THEN to_jsonb(OLD)
            ELSE NULL
        END
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Creación de Triggers para tablas críticas
CREATE TRIGGER trg_audit_pedidos
AFTER INSERT OR UPDATE OR DELETE ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_log_acciones();

CREATE TRIGGER trg_audit_detalle_pedidos
AFTER INSERT OR UPDATE OR DELETE ON detalle_pedidos
FOR EACH ROW EXECUTE FUNCTION fn_log_acciones();

CREATE TRIGGER trg_audit_platos
AFTER INSERT OR UPDATE OR DELETE ON platos
FOR EACH ROW EXECUTE FUNCTION fn_log_acciones();


-- Trigger para actualizar stock de ingredientes al registrar un pedido
CREATE OR REPLACE FUNCTION fn_actualizar_stock()
RETURNS TRIGGER AS $$
BEGIN
    -- Reducir el stock por cada ingrediente del plato añadido
    UPDATE ingredientes i
    SET stock_disponible = stock_disponible - (pi.cantidad_necesaria * NEW.cantidad)
    FROM plato_ingredientes pi
    WHERE i.id = pi.ingrediente_id AND pi.plato_id = NEW.plato_id;

    -- Verificar si algún ingrediente quedó en negativo 
    IF EXISTS (
        SELECT 1 FROM ingredientes i
        JOIN plato_ingredientes pi ON i.id = pi.ingrediente_id
        WHERE pi.plato_id = NEW.plato_id AND i.stock_disponible < 0
    ) THEN
        RAISE EXCEPTION 'Stock insuficiente para preparar el plato.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_descontar_stock_al_pedir
AFTER INSERT ON detalle_pedidos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_stock();


-- #############################################################################
-- SECCIÓN 6: VISTAS PARA REPORTES Y OPERACIÓN (MESERO, COCINA, GERENTE)
-- #############################################################################

-- VISTA PARA LA COCINA: Muestra pedidos activos y sus platos.
CREATE OR REPLACE VIEW vista_cocina AS
SELECT
    p.id AS pedido_id,
    m.numero_mesa,
    pl.nombre AS nombre_plato,
    dp.cantidad,
    ep.nombre AS estado_pedido,
    p.fecha_creacion
FROM pedidos p
JOIN detalle_pedidos dp ON p.id = dp.pedido_id
JOIN platos pl ON dp.plato_id = pl.id
JOIN mesas m ON p.mesa_id = m.id
JOIN estados_pedido ep ON p.estado_id = ep.id
WHERE ep.nombre IN ('Solicitado', 'En Cocina')
ORDER BY p.fecha_creacion ASC;

-- VISTA PARA EL MESERO: Estado de las mesas y total de la cuenta.
CREATE OR REPLACE VIEW vista_mesero_mesas AS
SELECT
    m.id AS mesa_id,
    m.numero_mesa,
    m.estado,
    p.id AS pedido_activo_id,
    COALESCE(SUM(dp.cantidad * dp.precio_unitario_congelado), 0) AS total_actual
FROM mesas m
LEFT JOIN pedidos p ON m.id = p.mesa_id AND p.fecha_cierre IS NULL
LEFT JOIN detalle_pedidos dp ON p.id = dp.pedido_id
GROUP BY m.id, m.numero_mesa, m.estado, p.id
ORDER BY m.numero_mesa;

-- VISTA PARA EL GERENTE: Reporte de consumo diario.
CREATE OR REPLACE VIEW vista_gerente_consumo_diario AS
SELECT
    DATE(fecha_cierre) AS dia,
    SUM(total) AS ventas_totales,
    COUNT(id) AS numero_pedidos
FROM pedidos
WHERE fecha_cierre IS NOT NULL
GROUP BY DATE(fecha_cierre)
ORDER BY dia DESC;

-- VISTA PARA EL GERENTE: Platos más pedidos.
CREATE OR REPLACE VIEW vista_gerente_platos_mas_pedidos AS
SELECT
    pl.nombre,
    c.nombre AS categoria,
    SUM(dp.cantidad) AS total_vendido
FROM detalle_pedidos dp
JOIN platos pl ON dp.plato_id = pl.id
JOIN categorias_platos c ON pl.categoria_id = c.id
GROUP BY pl.nombre, c.nombre
ORDER BY total_vendido DESC;

-- VISTA PARA EL GERENTE: Ventas por turno.
CREATE OR REPLACE VIEW vista_gerente_ventas_por_turno AS
SELECT
    t.nombre AS turno,
    DATE(p.fecha_cierre) AS dia,
    SUM(p.total) AS ventas_en_turno
FROM pedidos p
JOIN turnos t ON p.turno_id = t.id
WHERE p.fecha_cierre IS NOT NULL
GROUP BY t.nombre, DATE(p.fecha_cierre)
ORDER BY dia DESC, turno;


-- #############################################################################
-- SECCIÓN 7: FUNCIÓN PARA CÁLCULO DE CUENTA Y PROPINA
-- #############################################################################

CREATE OR REPLACE FUNCTION calcular_cuenta_con_propina(
    p_pedido_id INT,
    p_porcentaje_propina NUMERIC
)
RETURNS TABLE(subtotal NUMERIC, propina_calculada NUMERIC, total_final NUMERIC) AS $$
DECLARE
    v_subtotal NUMERIC;
BEGIN
    -- Calcular subtotal
    SELECT SUM(cantidad * precio_unitario_congelado)
    INTO v_subtotal
    FROM detalle_pedidos
    WHERE pedido_id = p_pedido_id;

    -- Actualizar el pedido con los totales
    UPDATE pedidos
    SET
        subtotal = v_subtotal,
        propina = v_subtotal * (p_porcentaje_propina / 100.0),
        total = v_subtotal * (1 + p_porcentaje_propina / 100.0),
        fecha_cierre = CURRENT_TIMESTAMP,
        estado_id = (SELECT id FROM estados_pedido WHERE nombre = 'Pagado')
    WHERE id = p_pedido_id;

    -- Devolver los resultados
    RETURN QUERY
    SELECT
        pedidos.subtotal,
        pedidos.propina,
        pedidos.total
    FROM pedidos
    WHERE id = p_pedido_id;
END;
$$ LANGUAGE plpgsql;


-- #############################################################################
-- SECCIÓN 8: INSERCIÓN DE DATOS DE EJEMPLO
-- #############################################################################
SET search_path TO sifood_schema;

-- Catálogos
INSERT INTO categorias_platos (nombre, descripcion) VALUES
('Entradas', 'Aperitivos para abrir el apetito.'),
('Platos Fuertes', 'Platos principales de la casa.'),
('Bebidas', 'Refrescos, jugos y otras bebidas.'),
('Postres', 'Dulces para finalizar la comida.');

INSERT INTO estados_pedido (nombre, descripcion) VALUES
('Solicitado', 'El mesero ha tomado la orden.'),
('En Cocina', 'La orden se está preparando.'),
('Servido', 'Los platos están en la mesa.'),
('Pagado', 'La cuenta ha sido saldada.');

INSERT INTO turnos (nombre, hora_inicio, hora_fin) VALUES
('Almuerzo', '12:00:00', '16:00:00'),
('Cena', '18:00:00', '23:00:00');

-- Datos
INSERT INTO ingredientes (nombre, stock_disponible, unidad_medida) VALUES
('Pechuga de Pollo', 5000, 'gramos'),
('Lechuga Romana', 20, 'unidades'),
('Tomate Riñón', 10000, 'gramos'),
('Queso Parmesano', 2000, 'gramos'),
('Carne de Res (Lomo)', 3000, 'gramos'),
('Papas', 20000, 'gramos'),
('Limón', 50, 'unidades');

INSERT INTO usuarios (nombre_completo, nombre_usuario, clave_hash, rol_app, activo) VALUES
('Juan Pérez', 'jperez_admin', 'hash_muy_seguro_123', 'Administrador', true),
('Ana Gómez', 'agomez_mesera', 'hash_muy_seguro_456', 'Mesero', true),
('Carlos Chef', 'cchef_cocinero', 'hash_muy_seguro_789', 'Cocinero', true);

INSERT INTO mesas (numero_mesa, capacidad, estado) VALUES
(1, 4, 'disponible'),
(2, 2, 'disponible'),
(3, 6, 'ocupada');

INSERT INTO platos (nombre, descripcion, precio, categoria_id, disponible) VALUES
('Ensalada César', 'Clásica ensalada con pollo, crutones y aderezo césar.', 8.50, 1, true),
('Lomo a la Pimienta', 'Medallón de lomo fino en salsa de pimienta, acompañado de papas.', 15.00, 2, true),
('Limonada', 'Jugo de limón fresco endulzado.', 2.50, 3, true);

INSERT INTO plato_ingredientes (plato_id, ingrediente_id, cantidad_necesaria) VALUES
(1, 1, 150), -- Ensalada César -> 150g de Pollo
(1, 2, 0.5), -- Ensalada César -> 0.5 unidades de Lechuga
(2, 5, 250), -- Lomo -> 250g de Carne
(2, 6, 200); -- Lomo -> 200g de Papas

-- Pedido de ejemplo
INSERT INTO pedidos (mesa_id, usuario_id_mesero, turno_id, estado_id, numero_comensales) VALUES
(3, 2, 2, 1, 4); -- Pedido en la mesa 3, atendido por Ana, en el turno de cena, estado Solicitado

INSERT INTO detalle_pedidos (pedido_id, plato_id, cantidad, precio_unitario_congelado) VALUES
(1, 2, 2, 15.00), -- 2 Lomos
(1, 1, 1, 8.50),  -- 1 Ensalada
(1, 3, 4, 2.50);  -- 4 Limonadas


-- #############################################################################
-- SECCIÓN 9: PROCEDIMIENTOS ALMACENADOS
-- #############################################################################

-- 1. INSERCIÓN CON VALIDACIONES Y TRANSACCIONES
CREATE OR REPLACE PROCEDURE sp_registrar_pedido_completo(
    p_mesa_id INT,
    p_mesero_id INT,
    p_platos_ids INT[], 
    p_cantidades INT[],
    INOUT p_pedido_id INT DEFAULT NULL
) LANGUAGE plpgsql AS $$
DECLARE
    v_estado_mesa VARCHAR(20);
    v_plato_id INT;
    v_cantidad INT;
    v_precio NUMERIC;
    v_index INT := 1;
BEGIN
    -- Validación cruzada: La mesa debe existir y estar disponible
    SELECT estado INTO v_estado_mesa FROM mesas WHERE id = p_mesa_id;
    IF NOT FOUND OR v_estado_mesa <> 'disponible' THEN
        RAISE EXCEPTION 'Mesa no disponible o no existe.';
    END IF;
    
    -- Validación cruzada: El mesero debe existir y estar activo
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = p_mesero_id AND activo = TRUE AND rol_app = 'Mesero') THEN
        RAISE EXCEPTION 'Mesero no válido o inactivo.';
    END IF;

    -- Paso 1: Actualizar estado de la mesa
    UPDATE mesas SET estado = 'ocupada' WHERE id = p_mesa_id;

    -- Paso 2: Crear el pedido en la tabla 'pedidos'
    INSERT INTO pedidos (mesa_id, usuario_id_mesero, turno_id, estado_id)
    VALUES (p_mesa_id, p_mesero_id, (SELECT id FROM turnos WHERE CURRENT_TIME BETWEEN hora_inicio AND hora_fin), (SELECT id FROM estados_pedido WHERE nombre = 'Solicitado'))
    RETURNING id INTO p_pedido_id;

    -- Paso 3: Insertar cada plato en 'detalle_pedidos'
    FOREACH v_plato_id IN ARRAY p_platos_ids
    LOOP
        v_cantidad := p_cantidades[v_index];
        SELECT precio INTO v_precio FROM platos WHERE id = v_plato_id;
        
        INSERT INTO detalle_pedidos (pedido_id, plato_id, cantidad, precio_unitario_congelado)
        VALUES (p_pedido_id, v_plato_id, v_cantidad, v_precio);
        
        v_index := v_index + 1;
    END LOOP;

EXCEPTION
    WHEN OTHERS THEN
        -- Si ocurre cualquier error, se hace rollback automáticamente
        RAISE NOTICE 'Ocurrió un error: %. Se revertirán los cambios.', SQLERRM;
        RAISE; -- Re-lanza la excepción para que el cliente la reciba
END;
$$;
COMMENT ON PROCEDURE sp_registrar_pedido_completo IS 'Registra un pedido completo con sus detalles, validando mesa y mesero, dentro de una transacción.';

-- 2. ACTUALIZACIÓN MASIVA POR CONDICIÓN
CREATE OR REPLACE PROCEDURE sp_aplicar_descuento_por_categoria(
    p_categoria_id INT,
    p_porcentaje_descuento NUMERIC
) LANGUAGE plpgsql AS $$
BEGIN
    IF p_porcentaje_descuento < 0 OR p_porcentaje_descuento > 100 THEN
        RAISE EXCEPTION 'El porcentaje de descuento debe estar entre 0 y 100.';
    END IF;

    UPDATE platos
    SET precio = precio * (1 - (p_porcentaje_descuento / 100.0))
    WHERE categoria_id = p_categoria_id;

    RAISE NOTICE 'Descuento del %%% aplicado a la categoría %.', p_porcentaje_descuento, p_categoria_id;
END;
$$;
COMMENT ON PROCEDURE sp_aplicar_descuento_por_categoria IS 'Aplica un descuento porcentual a todos los platos de una categoría específica.';

-- 3. ELIMINACIÓN SEGURA
CREATE OR REPLACE PROCEDURE sp_desactivar_usuario(p_usuario_id INT)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE usuarios
    SET activo = FALSE
    WHERE id = p_usuario_id AND rol_app <> 'Administrador'; -- Medida de seguridad para que no se pueda desactivar a un admin así

    IF NOT FOUND THEN
        RAISE NOTICE 'Usuario no encontrado o es un Administrador.';
    END IF;
END;
$$;
COMMENT ON PROCEDURE sp_desactivar_usuario IS 'Realiza una eliminación lógica de un usuario (lo marca como inactivo).';

-- 4. GENERACIÓN DE REPORTES POR PERÍODO
CREATE OR REPLACE FUNCTION fn_generar_reporte_ventas(
    p_fecha_inicio DATE,
    p_fecha_fin DATE
)
RETURNS TABLE(total_ventas NUMERIC, total_pedidos BIGINT, ticket_promedio NUMERIC) 
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        COALESCE(SUM(p.total), 0) AS total_ventas,
        COALESCE(COUNT(p.id), 0) AS total_pedidos,
        COALESCE(AVG(p.total), 0) AS ticket_promedio
    FROM pedidos p
    WHERE p.fecha_cierre::DATE BETWEEN p_fecha_inicio AND p_fecha_fin;
END;
$$;
COMMENT ON FUNCTION fn_generar_reporte_ventas IS 'Genera un reporte de ventas totales, número de pedidos y ticket promedio en un rango de fechas.';

-- 5. FACTURACIÓN AUTOMÁTICA
CREATE OR REPLACE PROCEDURE sp_facturar_mesa(
    p_mesa_id INT,
    p_porcentaje_propina NUMERIC
) LANGUAGE plpgsql AS $$
DECLARE
    v_pedido_id INT;
    v_subtotal NUMERIC;
BEGIN
    -- Encontrar el pedido activo para la mesa
    SELECT id INTO v_pedido_id FROM pedidos WHERE mesa_id = p_mesa_id AND fecha_cierre IS NULL;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La mesa no tiene un pedido activo para facturar.';
    END IF;

    -- Calcular subtotal
    SELECT SUM(cantidad * precio_unitario_congelado) INTO v_subtotal
    FROM detalle_pedidos WHERE pedido_id = v_pedido_id;

    -- Actualizar el pedido con los totales
    UPDATE pedidos
    SET
        subtotal = v_subtotal,
        propina = v_subtotal * (p_porcentaje_propina / 100.0),
        total = v_subtotal * (1 + p_porcentaje_propina / 100.0),
        fecha_cierre = CURRENT_TIMESTAMP,
        estado_id = (SELECT id FROM estados_pedido WHERE nombre = 'Pagado')
    WHERE id = v_pedido_id;
    
    -- Liberar la mesa
    UPDATE mesas SET estado = 'disponible' WHERE id = p_mesa_id;

    RAISE NOTICE 'Mesa % facturada correctamente. Pedido ID: %.', p_mesa_id, v_pedido_id;
END;
$$;
COMMENT ON PROCEDURE sp_facturar_mesa IS 'Calcula el total de un pedido, lo cierra, y libera la mesa.';



ALTER TABLE sifood_schema.ingredientes
ADD COLUMN costo_unitario NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (costo_unitario >= 0);

COMMENT ON COLUMN sifood_schema.ingredientes.costo_unitario IS 'Costo del ingrediente por unidad de medida (ej: costo por gramo, por unidad).';

-- Actualizacion del costo de un ingrediente de ejemplo
UPDATE sifood_schema.ingredientes SET costo_unitario = 0.02 WHERE nombre = 'Carne de Res (Lomo)'; -- ej: 2 centavos por gramo



-- #############################################################################
-- SECCIÓN 10: FUNCIONES ADICIONALES DEFINIDAS POR EL USUARIO
-- #############################################################################

-- 1. FUNCIÓN DE ESTADO CONDICIONAL 
-- Devuelve un indicador del nivel de stock de un ingrediente.
CREATE OR REPLACE FUNCTION fn_verificar_stock_ingrediente(
    p_ingrediente_id INT
)
RETURNS TEXT 
LANGUAGE plpgsql AS $$
DECLARE
    v_stock_actual NUMERIC;
    v_umbral_bajo NUMERIC := 50.0; 
BEGIN
    SELECT stock_disponible INTO v_stock_actual
    FROM ingredientes WHERE id = p_ingrediente_id;

    IF v_stock_actual > v_umbral_bajo THEN
        RETURN 'OK';
    ELSIF v_stock_actual > 0 THEN
        RETURN 'BAJO STOCK';
    ELSE
        RETURN 'SIN STOCK';
    END IF;
END;
$$;
COMMENT ON FUNCTION fn_verificar_stock_ingrediente IS 'Devuelve un estado condicional (OK, BAJO STOCK, SIN STOCK) basado en el inventario de un ingrediente.';

-- 2. FUNCIÓN DE CÁLCULO AGREGADO PERSONALIZADO
-- Calcula el costo total de los ingredientes para preparar un solo plato.
CREATE OR REPLACE FUNCTION fn_calcular_costo_plato(
    p_plato_id INT
)
RETURNS NUMERIC
LANGUAGE sql AS $$
    SELECT COALESCE(SUM(pi.cantidad_necesaria * i.costo_unitario), 0)
    FROM plato_ingredientes pi
    JOIN ingredientes i ON pi.ingrediente_id = i.id
    WHERE pi.plato_id = p_plato_id;
$$;
COMMENT ON FUNCTION fn_calcular_costo_plato IS 'Realiza un cálculo agregado para determinar el costo de materia prima de un plato.';

-- 3. FUNCIÓN DE DURACIÓN
-- Calcula el tiempo que transcurrió desde que se creó un pedido hasta que se cerró.
CREATE OR REPLACE FUNCTION fn_calcular_duracion_pedido(
    p_pedido_id INT
)
RETURNS INTERVAL 
LANGUAGE plpgsql AS $$
DECLARE
    v_duracion INTERVAL;
BEGIN
    SELECT fecha_cierre - fecha_creacion INTO v_duracion
    FROM pedidos
    WHERE id = p_pedido_id;

    RETURN v_duracion; -- Devolverá NULL si el pedido no ha sido cerrado
END;
$$;
COMMENT ON FUNCTION fn_calcular_duracion_pedido IS 'Calcula y devuelve la duración (un intervalo de tiempo) de un pedido completado.';




-- Ejemplo 1: Ver el estado del stock de todos los ingredientes
SELECT nombre, stock_disponible, fn_verificar_stock_ingrediente(id) AS estado_stock
FROM sifood_schema.ingredientes;

-- Ejemplo 2: Ver el costo y la ganancia de cada plato
SELECT 
    nombre, 
    precio AS precio_venta, 
    fn_calcular_costo_plato(id) AS costo_plato,
    (precio - fn_calcular_costo_plato(id)) AS ganancia_bruta
FROM sifood_schema.platos;

-- Ejemplo 3: Ver cuánto tiempo estuvieron los clientes en las mesas para los pedidos ya pagados
SELECT 
    id AS pedido_id, 
    mesa_id, 
    fn_calcular_duracion_pedido(id) AS tiempo_en_mesa
FROM sifood_schema.pedidos 
WHERE estado_id = (SELECT id FROM sifood_schema.estados_pedido WHERE nombre = 'Pagado');



-- #########################################################################
-- Ver todos los INDICES de mi database     
-- #########################################################################
SELECT
    schemaname AS schema,
    tablename AS tabla,
    indexname AS nombre_del_indice,
    indexdef AS definicion
FROM
    pg_indexes
WHERE
    schemaname = 'sifood_schema'
ORDER BY
    tablename,
    indexname;

-- ######################################################################
-- Implementación del Hash en la base de datos
-- ######################################################################

-- Instalación el conjunto de herramientas de criptografía 
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA sifood_schema;



SET search_path TO sifood_schema;

-- Esta consulta tomará la contraseña '123', la hasheará, y guardará
-- el resultado en la columna clave_hash para todos los usuarios.
UPDATE usuarios
SET clave_hash = crypt('123', gen_salt('bf'));



-- Rol para usuario final
CREATE ROLE cajero_limitado WITH LOGIN PASSWORD 'Cajero_SIFood$2025';
GRANT CONNECT ON DATABASE neondb TO cajero_limitado;
GRANT USAGE ON SCHEMA sifood_schema TO cajero_limitado;

-- Este usuario solo puede ver el estado de las mesas, nada más.
GRANT SELECT ON sifood_schema.vista_mesero_mesas TO cajero_limitado;


-- 1. Añade la nueva columna a la tabla 'platos'
ALTER TABLE sifood_schema.platos
ADD COLUMN imagen_nombre VARCHAR(100);

COMMENT ON COLUMN sifood_schema.platos.imagen_nombre IS 'Nombre del archivo de imagen para el plato (ej: ensalada.png)';

-- 2. Asignar nombres de imagen a los platos existentes 
UPDATE sifood_schema.platos SET imagen_nombre = 'ensalada_cesar.png' WHERE nombre = 'Ensalada César';
UPDATE sifood_schema.platos SET imagen_nombre = 'lomo_pimienta.png' WHERE nombre = 'Lomo a la Pimienta';
UPDATE sifood_schema.platos SET imagen_nombre = 'limonada.png' WHERE nombre = 'Limonada';


-- Procedimiento para actualizar stock
SET search_path TO sifood_schema;

CREATE OR REPLACE PROCEDURE sp_actualizar_stock_ingrediente(
    p_ingrediente_id INT,
    p_nuevo_stock DOUBLE PRECISION 
)
LANGUAGE plpgsql AS $$
BEGIN
    -- Validación para no permitir stock negativo
    IF p_nuevo_stock < 0 THEN
        RAISE EXCEPTION 'El stock no puede ser un número negativo.';
    END IF;

    UPDATE ingredientes
    SET stock_disponible = p_nuevo_stock
    WHERE id = p_ingrediente_id;

    -- Si no se actualiza ninguna fila, es que el ID no existía
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No se encontró un ingrediente con el ID %', p_ingrediente_id;
    END IF;
END;
$$;


-- Vista para ver al empleado del mes
SET search_path TO sifood_schema;

CREATE OR REPLACE VIEW vista_gerente_ventas_por_mesero AS
SELECT
    u.nombre_completo,
    SUM(p.total) AS ventas_totales,
    COUNT(p.id) AS pedidos_atendidos
FROM pedidos p
JOIN usuarios u ON p.usuario_id_mesero = u.id
WHERE p.fecha_cierre IS NOT NULL AND u.rol_app = 'Mesero'
GROUP BY u.nombre_completo
ORDER BY ventas_totales DESC;

COMMENT ON VIEW vista_gerente_ventas_por_mesero IS 'Reporte de ventas totales agrupadas por mesero.';



-- Facturar Mesa
SET search_path TO sifood_schema;

CREATE OR REPLACE PROCEDURE sp_facturar_mesa(
    p_mesa_id INT,
    p_porcentaje_propina DOUBLE PRECISION
) LANGUAGE plpgsql AS $$
DECLARE
    v_pedido_id INT;
    v_subtotal NUMERIC;
BEGIN
    -- Encontrar el pedido activo para la mesa
    SELECT id INTO v_pedido_id FROM pedidos WHERE mesa_id = p_mesa_id AND fecha_cierre IS NULL;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La mesa no tiene un pedido activo para facturar.';
    END IF;

    -- Calcular subtotal
    SELECT SUM(cantidad * precio_unitario_congelado) INTO v_subtotal
    FROM detalle_pedidos WHERE pedido_id = v_pedido_id;

    -- Actualizar el pedido con los totales
    UPDATE pedidos
    SET
        subtotal = v_subtotal,
        propina = v_subtotal * (p_porcentaje_propina / 100.0),
        total = v_subtotal * (1 + p_porcentaje_propina / 100.0),
        fecha_cierre = CURRENT_TIMESTAMP,
        estado_id = (SELECT id FROM estados_pedido WHERE nombre = 'Pagado')
    WHERE id = v_pedido_id;
    
    -- Liberar la mesa
    UPDATE mesas SET estado = 'disponible' WHERE id = p_mesa_id;

    RAISE NOTICE 'Mesa % facturada correctamente. Pedido ID: %.', p_mesa_id, v_pedido_id;
END;
$$;



-- Asegúrate de estar en el schema correcto
SET search_path TO sifood_schema;

CREATE OR REPLACE PROCEDURE sp_facturar_mesa(
    p_mesa_id INT,
    p_porcentaje_propina DOUBLE PRECISION,
    OUT p_pedido_id_salida INT -- PARÁMETRO DE SALIDA
) LANGUAGE plpgsql AS $$
DECLARE
    v_subtotal NUMERIC;
BEGIN
    -- Encontrar el pedido activo para la mesa
    SELECT id INTO p_pedido_id_salida FROM pedidos WHERE mesa_id = p_mesa_id AND fecha_cierre IS NULL;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La mesa no tiene un pedido activo para facturar.';
    END IF;

    -- Calcular subtotal
    SELECT SUM(cantidad * precio_unitario_congelado) INTO v_subtotal
    FROM detalle_pedidos WHERE pedido_id = p_pedido_id_salida;

    -- Actualizar el pedido con los totales
    UPDATE pedidos SET
        subtotal = v_subtotal,
        propina = v_subtotal * (p_porcentaje_propina / 100.0),
        total = v_subtotal * (1 + p_porcentaje_propina / 100.0),
        fecha_cierre = CURRENT_TIMESTAMP,
        estado_id = (SELECT id FROM estados_pedido WHERE nombre = 'Pagado')
    WHERE id = p_pedido_id_salida;
    
    -- Liberar la mesa
    UPDATE mesas SET estado = 'disponible' WHERE id = p_mesa_id;

    RAISE NOTICE 'Mesa % facturada correctamente. Pedido ID: %.', p_mesa_id, p_pedido_id_salida;
END;
$$;