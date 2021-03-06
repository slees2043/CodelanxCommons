/*
 * Copyright (C) 2016 Codelanx, All Rights Reserved
 *
 * This work is licensed under a Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 *
 * This program is protected software: You are free to distrubute your
 * own use of this software under the terms of the Creative Commons BY-NC-ND
 * license as published by Creative Commons in the year 2015 or as published
 * by a later date. You may not provide the source files or provide a means
 * of running the software outside of those licensed to use it.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the Creative Commons BY-NC-ND license
 * long with this program. If not, see <https://creativecommons.org/licenses/>.
 */
package com.codelanx.commons.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides façade methods for making SQL interactions less verbose
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public class Databases {

    /**
     * Formats an {@link SQLException} to be output with more information about
     * the error
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param ex The relevant {@link SQLException}
     * @return A formatted string with the error code and message
     */
    public static String simpleErrorOutput(SQLException ex) {
        return String.format("(%d) %s", ex.getErrorCode(), ex.getMessage());
    }

    /**
     * Closes a passed {@link Statement} object and swallows any
     * {@link SQLException} that occurs. Can handle null parameters
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param closable The {@link Statement} to close
     */
    public static void close(Statement closable) {
        if (closable == null) {
            return;
        }
        try {
            closable.close();
        } catch (SQLException ex) {}
    }

    /**
     * Closes a passed {@link Connection} object and swallows any
     * {@link SQLException} that occurs. Can handle null parameters
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param closable The {@link Connection} to close
     */
    public static void close(Connection closable) {
        if (closable == null) {
            return;
        }
        try {
            closable.close();
        } catch (SQLException ex) {}
    }

    /**
     * Closes a passed {@link ResultSet} object and swallows any
     * {@link SQLException} that occurs. Can handle null parameters
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param closable The {@link ResultSet} to close
     */
    public static void close(ResultSet closable) {
        if (closable == null) {
            return;
        }
        try {
            closable.close();
        } catch (SQLException ex) {}
    }

}
