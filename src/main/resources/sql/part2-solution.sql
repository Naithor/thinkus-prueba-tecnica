-- ============================================================
-- BTG Pactual - Parte 2: Consulta SQL
-- ============================================================
-- Pregunta: Obtener los nombres de los clientes que tienen
-- inscrito algun producto disponible SOLO en las sucursales
-- que visitan.
-- ============================================================

SELECT DISTINCT c.nombre
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
      AND d.idSucursal NOT IN (
          SELECT v.idSucursal
          FROM Visitan v
          WHERE v.idCliente = c.id
      )
);

-- ============================================================
-- EXPLICACION:
-- ============================================================
-- 1. JOIN Inscripcion: Obtenemos los productos que cada cliente
--    tiene inscritos.
--
-- 2. NOT EXISTS + NOT IN: Buscamos clientes DONDE NO EXISTA ninguna sucursal donde
--    el producto este disponible que el cliente NO visite.
--
-- 3. DISTINCT: Evita duplicados si un cliente tiene multiples
--    productos que cumplen la condicion.
--
-- ============================================================
-- EJEMPLO:
-- ============================================================
-- Producto P1 disponible en sucursales S1, S2
-- Cliente C1 visita S1, S2 -> CUMPLE (visita todas donde P1 esta disponible)
-- Cliente C2 visita S1     -> NO CUMPLE (P1 tambien esta en S2 que no visita)
-- ============================================================
