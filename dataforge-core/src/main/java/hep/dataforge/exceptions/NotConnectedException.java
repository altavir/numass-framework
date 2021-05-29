/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.exceptions;

import hep.dataforge.connections.Connection;

/**
 * An exception thrown when the request is sent to the closed connection
 *
 * @author Alexander Nozik
 */
public class NotConnectedException extends Exception {

    Connection connection;

    public NotConnectedException(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getMessage() {
        return "The connection is not open";
    }

}
