package Interfaces;

import java.util.List;

import Excepciones.ConexionBDException;


public interface IGenericDAO<T> {

    int insertar(T entidad) throws ConexionBDException;

    boolean modificar(T entidad) throws ConexionBDException;

    boolean darDeBaja(int id) throws ConexionBDException;

    boolean darDeAlta(int id) throws ConexionBDException;

    T buscarPorId(int id) throws ConexionBDException;

    List<T> listarActivos() throws ConexionBDException;

    List<T> listarTodos() throws ConexionBDException;
}
