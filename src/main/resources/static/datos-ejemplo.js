/*
 * Datos ficticios de scripts/02_initial_data.sql, con los campos del modelo Java.
 * Se cargan opcionalmente en memoria para la vista previa; no consultan ni
 * modifican la base de datos y no simulan operaciones de la API.
 * Los identificadores locales siguen el orden de los INSERT de cada tabla.
 * El stock incluye los dos UPDATE finales del script: 96, 78, 0 y 0.
 */
window.DATOS_EJEMPLO = {
  catalogos: {
    sedes: [
      { id: 1, nombre: "SEDE CENTRAL DEMO", direccion: "Direccion ficticia 1", departamento: "Guatemala" },
      { id: 2, nombre: "SEDE ALTIPLANO DEMO", direccion: "Direccion ficticia 2", departamento: "Chimaltenango" }
    ],
    estatus: [
      { id: 1, nombre: "ACTIVO", descripcion: "Registro habilitado" },
      { id: 2, nombre: "INACTIVO", descripcion: "Registro deshabilitado" },
      { id: 3, nombre: "PAGADO", descripcion: "Prestamo pagado" },
      { id: 4, nombre: "VENCIDO", descripcion: "Prestamo vencido" }
    ],
    tiposPersona: [
      { id: 1, nombre: "EMPLEADO" },
      { id: 2, nombre: "SOCIO" },
      { id: 3, nombre: "OTRO" }
    ],
    tiposCuenta: [
      { id: 1, nombre: "AHORRO" },
      { id: 2, nombre: "MONETARIA" }
    ],
    tiposPrestamo: [
      { id: 1, nombre: "EFECTIVO" },
      { id: 2, nombre: "HIPOTECARIO" },
      { id: 3, nombre: "CONSUMO_PRODUCTOS" }
    ],
    categoriasProducto: [
      { id: 1, nombre: "INSUMO_SOCIOS" },
      { id: 2, nombre: "PRODUCTO_EXPORTACION" }
    ]
  },
  personas: [
    {
      id: 1, nombre: "Ana", apellido: "Ejemplo", dni: "DEMO-SOCIO-001",
      direccion: "Direccion ficticia de prueba", telefono: null,
      tipoPersonaId: 2, estatusId: 1, sedeId: 1, fechaRegistro: "2026-01-10"
    },
    {
      id: 2, nombre: "Luis", apellido: "Prueba", dni: "DEMO-SOCIO-002",
      direccion: "Direccion ficticia de prueba", telefono: null,
      tipoPersonaId: 2, estatusId: 1, sedeId: 2, fechaRegistro: "2026-01-10"
    },
    {
      id: 3, nombre: "Marta", apellido: "Ficticia", dni: "DEMO-EMPL-001",
      direccion: "Direccion ficticia de prueba", telefono: null,
      tipoPersonaId: 1, estatusId: 1, sedeId: 1, fechaRegistro: "2026-01-10"
    }
  ],
  cuentas: [
    { id: 1, numero: "CTA-DEMO-001", tipoCuentaId: 1, estatusId: 1, sedeId: 1, fechaApertura: "2026-01-10" },
    { id: 2, numero: "CTA-DEMO-002", tipoCuentaId: 2, estatusId: 1, sedeId: 2, fechaApertura: "2026-01-10" }
  ],
  cuentasPersonas: [
    { cuentaId: 1, personaId: 1, titular: true },
    { cuentaId: 1, personaId: 2, titular: false },
    { cuentaId: 2, personaId: 2, titular: true }
  ],
  saldos: [
    { id: 1, cuentaId: 1, monto: "1000.00", fecha: "2026-01-10" },
    { id: 2, cuentaId: 1, monto: "1250.00", fecha: "2026-01-11" },
    { id: 3, cuentaId: 2, monto: "500.00", fecha: "2026-01-10" }
  ],
  prestamos: [
    {
      id: 1, personaId: 1, tipoPrestamoId: 3, estatusId: 1,
      monto: "192.00", tasaInteres: "10.00", fecha: "2026-01-10", fechaVencimiento: "2027-01-10",
      detalles: [
        { id: 1, prestamoId: 1, productoId: 1, cantidad: 4, precioUnitario: "25.50" },
        { id: 2, prestamoId: 1, productoId: 2, cantidad: 2, precioUnitario: "45.00" }
      ]
    },
    {
      id: 2, personaId: 2, tipoPrestamoId: 1, estatusId: 1,
      monto: "1500.00", tasaInteres: "12.00", fecha: "2026-01-10", fechaVencimiento: "2027-01-10",
      detalles: []
    }
  ],
  productos: [
    {
      id: 1, nombre: "Semilla maiz DEMO", descripcion: "Producto ficticio para pruebas academicas",
      categoriaId: 1, precio: "25.50", stock: 96
    },
    {
      id: 2, nombre: "Abono DEMO", descripcion: "Producto ficticio para pruebas academicas",
      categoriaId: 1, precio: "45.00", stock: 78
    },
    {
      id: 3, nombre: "Tomate DEMO", descripcion: "Producto ficticio para pruebas academicas",
      categoriaId: 2, precio: "7.50", stock: 0
    },
    {
      id: 4, nombre: "Cebolla DEMO", descripcion: "Producto ficticio para pruebas academicas",
      categoriaId: 2, precio: "5.00", stock: 0
    }
  ],
  albaranes: [
    {
      id: 1, numeroTalonario: "TAL-DEMO-001", personaId: 1, sedeId: 1, fecha: "2026-01-12",
      detalles: [
        { id: 1, albaranId: 1, productoId: 3, cantidadAceptada: "125.50", cantidadRechazada: "4.50", precioUnitario: "7.50" },
        { id: 2, albaranId: 1, productoId: 4, cantidadAceptada: "80.00", cantidadRechazada: "0.00", precioUnitario: "5.00" }
      ]
    }
  ]
};
