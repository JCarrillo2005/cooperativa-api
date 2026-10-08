package com.cooperativa.model;

import java.math.BigDecimal;

public class DetalleAlbaran {
    private int id;
    private int albaranId;
    private int productoId;
    private BigDecimal cantidadAceptada;    // pasó control de calidad
    private BigDecimal cantidadRechazada;   // no pasó control de calidad
    private BigDecimal precioUnitario;      // precio pagado (mejor que el de mercado)

    public DetalleAlbaran() {}

    public DetalleAlbaran(int productoId, BigDecimal cantidadAceptada,
                          BigDecimal cantidadRechazada, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.cantidadAceptada = cantidadAceptada;
        this.cantidadRechazada = cantidadRechazada;
        this.precioUnitario = precioUnitario;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAlbaranId() { return albaranId; }
    public void setAlbaranId(int albaranId) { this.albaranId = albaranId; }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

    public BigDecimal getCantidadAceptada() { return cantidadAceptada; }
    public void setCantidadAceptada(BigDecimal cantidadAceptada) { this.cantidadAceptada = cantidadAceptada; }

    public BigDecimal getCantidadRechazada() { return cantidadRechazada; }
    public void setCantidadRechazada(BigDecimal cantidadRechazada) { this.cantidadRechazada = cantidadRechazada; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getSubtotal() {
        return cantidadAceptada.multiply(precioUnitario);
    }
}