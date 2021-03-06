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
package com.codelanx.commons.data;

import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.Databases;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Represents an object that connects to an SQL database and allows operations
 * to be made upon it
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public interface SQLDataType extends DataType, AutoCloseable {

    /**
     * Checks if a table exists within the set database
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param tableName Name of the table to check for
     * @return {@code true} if the table exists
     */
    public boolean checkTable(String tableName);

    /**
     * Checks if a table has the following column
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param tableName The table to look in
     * @param columnName The column to search for
     * @return {@code true} if the column exists
     */
    public boolean checkColumn(String tableName, String columnName);

    /**
     * Executes a query, and applies the resulting {@link ResultSet} to the
     * passed {@link SQLFunction}. This method will return anything returned
     * from the lambda body
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <R> The return type from the lambda body
     * @param oper The operation to apply to the {@link ResultSet}
     * @param sql The SQL statement to execute
     * @param params Any {@link PreparedStatement} parameters
     * @return The return value of the lambda
     */
    default public <R> SQLResponse<R> query(SQLFunction<? super ResultSet, R> oper, String sql, Object... params) {
        PreparedStatement stmt = null;
        R back = null;
        try {
            stmt = this.prepare(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            back = oper.apply(rs);
            Databases.close(rs);
        } catch (SQLException ex) {
            if (this.isSendingErrorOutput()) {
                Debugger.error(ex, "Error in SQL operation: %s", Databases.simpleErrorOutput(ex));
            }
            return new SQLResponse<>(ex);
        } finally {
            Databases.close(stmt);
        }
        return new SQLResponse<>(back);
    }

    /**
     * Works similarly to #query, but will return only the first row. Thus,
     * {@link ResultSet#next} is not necessary to call. If the returned result is empty,
     * then
     *
     * @since 0.3.2
     * @version 0.3.2
     *
     * @param oper The operation to perform on a row
     * @param sql The sql string
     * @param params The sql parameters to be bound to the statement
     * @param <R> The return type
     * @see SQLDataType#query(SQLFunction, String, Object...)
     * @return The result of this query, or {@code null} if nothing was able to be selected
     */
    default public <R> SQLResponse<R> select(SQLFunction<? super ResultRow, R> oper, String sql, Object... params) {
        return this.query(rs -> {
            if (rs.next()) {
                return oper.apply(new ResultRow(rs));
            }
            return null;
        }, sql, params);
    }

    /**
     * Strictly speaking, this is a facade method for #select which allows you
     * to retrieve not only the first row but the first column as a result, and use
     * the first parameter to map the value from a ResultSet to an object. For example:
     * <br><br>
     *     {@code select(ResultSet::getInt, "SELECT 5")}
     *
     * @since 0.3.2
     * @version 0.3.2
     *
     * @param oper An {@link SQLBiFunction} which consumes the given {@link ResultSet},
     *             with the second argument defining the column index to select from
     *             the results
     * @param sql The sql string
     * @param params The sql parameters to be bound to the statement
     * @param <R> The return type
     * @see SQLDataType#selectByColumn(SQLBiFunction, int, String, Object...)
     * @return The result of this query
     */
    default public <R> SQLResponse<R> selectFirst(SQLBiFunction<? super ResultRow, Integer, R> oper, String sql, Object... params) {
        return this.selectByColumn(oper, 1, sql, params);
    }

    /**
     * Strictly speaking, this is a facade method for #select which allows you
     * to retrieve not only the first row but the selected column as a result, and use
     * the first parameter to map the value from a ResultSet to an object. For example:
     * <br><br>
     *     {@code select(ResultSet::getInt, 3, "SELECT 10, 8, 6, 4, 2") //returns 3rd column, 6}
     *
     * @since 0.3.2
     * @version 0.3.2
     *
     * @param oper An {@link SQLBiFunction} which consumes the given {@link ResultSet},
     *             with the second argument defining the column index to select from
     *             the result
     *
     * @param column the index of the column to select
     * @param sql The sql string
     * @param params The sql parameters to be bound to the statement
     * @param <R> The return type
     * @see SQLDataType#query(SQLFunction, String, Object...)
     * @return The result of this query
     */
    default public <R> SQLResponse<R> selectByColumn(SQLBiFunction<? super ResultRow, Integer, R> oper, int column, String sql, Object... params) {
        return this.select(rs -> oper.apply(rs, column), sql, params);
    }

    /**
     * Strictly speaking, this is a facade method for #select which allows you
     * to retrieve not only the first row but the selected column as a result, and use
     * the first parameter to map the value from a ResultSet to an object. For example:
     * <br><br>
     *     {@code selectByName(ResultSet::getInt, "bob", "SELECT 1 AS joe, 2 AS bob") //returns bob column, 6}
     *
     * @since 0.3.2
     * @version 0.3.2
     *
     * @param oper An {@link SQLBiFunction} which consumes the given {@link ResultSet},
     *             with the second argument defining the column index to select from
     *             the result
     *
     * @param columnName the name of the column to select
     * @param sql The sql string
     * @param params The sql parameters to be bound to the statement
     * @param <R> The return type
     * @see SQLDataType#query(SQLFunction, String, Object...)
     * @return The result of this query
     */
    default public <R> SQLResponse<R> selectByColumn(SQLBiFunction<? super ResultRow, String, R> oper, String columnName, String sql, Object... params) {
        return this.select(rs -> oper.apply(rs, columnName), sql, params);
    }

    /**
     * Executes a query, and applies the resulting {@link ResultSet} to the
     * passed {@link SQLConsumer}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param oper The operation to apply to the {@link ResultSet}
     * @param sql The SQL statement to execute
     * @param params Any {@link PreparedStatement} parameters
     * @return An {@link SQLResponse} containing any required information
     */
    @SuppressWarnings("rawtypes")
    default public SQLResponse<?> query(SQLConsumer<? super ResultSet> oper, String sql, Object... params) {
        SQLResponse resp = SQLResponse.EMPTY.clone();
        PreparedStatement stmt = null;
        try {
            stmt = this.prepare(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            oper.accept(rs);
            Databases.close(rs);
        } catch (SQLException ex) {
            if (this.isSendingErrorOutput()) {
                Debugger.error(ex, "Error in SQL operation: %s", Databases.simpleErrorOutput(ex));
            }
            resp.setException(ex);
        } finally {
            Databases.close(stmt);
        }
        return resp;
    }

    /**
     * Executes a query that can change values
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param query The string query to execute
     * @param params Any {@link PreparedStatement} parameters
     * @return 0 for no returned results, or the number of returned rows
     */
    @SuppressWarnings("rawtypes")
    default public SQLResponse update(String query, Object... params) {
        SQLResponse resp = new SQLResponse();
        PreparedStatement stmt = null;
        int back = 0;
        try {
            stmt = this.prepare(query);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            back = stmt.executeUpdate();
        } catch (SQLException ex) {
            if (this.isSendingErrorOutput()) {
                Debugger.error(ex, "Error in SQL operation: %s", Databases.simpleErrorOutput(ex));
            }
        } finally {
            Databases.close(stmt);
        }
        resp.setUpdatedRows(back);
        return resp;
    }

    /**
     * Executes a batch update on a database, and maps passed parameters based
     * upon 0-indexed {@code paramMappers} for each parameter. An example of
     * a usage would be:
     * <br><br>{@code
     * //A collection of objects to apply to each batch
     * Collection<SomeObject> yourCollection;
     * SQLDataType#batchUpdate("<SQL Query>", 500, yourCollection,
     *    (s) -> s.getName(),
     *    (s) -> s.getID(),
     *    (s) -> s.getLastName());
     * );
     * }
     * <br><br>
     * In the above example, the {@link PreparedStatement} parameters would
     * map each object so that "Parameter 1" would be the result of
     * {@code SomeObject#getName}, and parameter 2 would be the result of
     * {@code SomeObject#getID}, and so on
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <T> The type of the objects being batch committed
     * @param query The SQL query to execute
     * @param batchSize The size of each batch
     * @param params The objects to use in each batch
     * @param paramMappers A series of functions for mapping objects to params
     * @return An int of the total rows affected for all operations
     */
    default public <T> int batchUpdate(String query, int batchSize, Collection<T> params, Function<T, ?>... paramMappers) {
        PreparedStatement stmt = null;
        int back = 0;
        try {
            stmt = this.prepare(query);
            this.setAutoCommit(false);
            Iterator<T> itr = params.iterator();
            for (int i = 1; itr.hasNext(); i++) {
                T val = itr.next();
                for (int w = 0; w < paramMappers.length; w++) {
                    stmt.setObject(w + 1, paramMappers[w].apply(val));
                }
                stmt.addBatch();
                if (i >= batchSize) {
                    back += IntStream.of(stmt.executeBatch()).reduce(0, Integer::sum);
                    this.commit();
                    i = 1;
                }
            }
            back += IntStream.of(stmt.executeBatch()).reduce(0, Integer::sum);
            this.commit();
            this.setAutoCommit(true);
        } catch (SQLException ex) {
            if (this.isSendingErrorOutput()) {
                Debugger.error(ex, "Error in SQL operation: %s", Databases.simpleErrorOutput(ex));
            }
        } finally {
            Databases.close(stmt);
        }
        return back;
    }

    /**
     * Returns a {@link PreparedStatement} in which you can easily protect
     * against SQL injection attacks.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param stmt The string to prepare
     * @return A {@link PreparedStatement} from the passed string
     * @throws SQLException The connection cannot be established
     */
    default public PreparedStatement prepare(String stmt) throws SQLException {
        return this.getConnection().prepareStatement(stmt);
    }

    /**
     * Returns whether or not this connection automatically commits changes
     * to the database.
     *
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return the current state of this connection's auto-commit mode 
     * @throws SQLException database access error or closed connection
     */
    default public boolean isAutoCommit() throws SQLException {
        return this.getConnection().getAutoCommit();
    }

    /**
     * Sets whether or not to automatically commit changes to the database. If
     * disabled, transactions will be grouped and not executed except from a
     * manual call to {@link SQLDataType#commit()} or
     * {@link SQLDataType#rollback()}.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param set {@code true} to enable, {@code false} to disable
     */
    default public void setAutoCommit(boolean set) {
        if (this.getConnection() != null) {
            try {
                this.getConnection().setAutoCommit(set);
            } catch (SQLException ex) {
                Debugger.error(ex, "Error setting " + this.getClass().getSimpleName() + "#setAutoCommit");
            }
        }
    }

    /**
     * Pushes any queued transactions to the database
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @throws SQLException The connection cannot be established, an access
     *                      error occurred, or auto-commit is enabled
     */
    default public void commit() throws SQLException {
        if (this.getConnection() != null) {
            this.getConnection().commit();
        }
    }

    /**
     * Cancel any queued transactions
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @throws SQLException The connection cannot be established, an access
     *                      error occurred, or auto-commit is enabled
     */
    default public void rollback() throws SQLException {
        if (this.getConnection() != null) {
            this.getConnection().rollback();
        }
    }

    /**
     * Runs a {@link PreparedStatement} using the provided {@code sql} parameter.
     * The following {@link SQLFunction} will then be run using this constructed
     * statement. This is typically more-so for use in one-time executed
     * statements
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <R> The type of the return value
     * @param oper The {@link SQLFunction} operation to use
     * @param sql The SQL statement to execute
     * @param params Parameters to pass to the {@link PreparedStatement}
     * @return The returned result of the {@link SQLFunction}
     */
    default public <R> SQLResponse<R> operate(SQLFunction<? super PreparedStatement, R> oper, String sql, Object... params) {
        SQLResponse<R> back = new SQLResponse<>();
        PreparedStatement stmt = null;
        try {
            stmt = this.prepare(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            back.setResponse(oper.apply(stmt));
        } catch (SQLException ex) {
            if (this.isSendingErrorOutput()) {
                Debugger.error(ex, "Error in SQL operation: %s", Databases.simpleErrorOutput(ex));
            }
            back.setException(ex);
        } finally {
            if (stmt != null) {
                Databases.close(stmt);
            }
        }
        return back;
    }

    /**
     * Returns the {@link Connection} object for ease of use in exposing more
     * internal API
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The {@link Connection} object in use by this {@link SQLDataType}
     */
    public Connection getConnection();

    /**
     * {@inheritDoc}
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    @Override
    default public void close() {
        try {
            this.getConnection().close();
        } catch (SQLException ex) {
            Debugger.error(ex, "Error closing SQL connection: %s", Databases.simpleErrorOutput(ex));
        }
    }

    /**
     * Sets whether or not to print errors to the console automatically. This
     * is defaulted to {@code true}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param errors {@code false} to disable error output
     */
    public abstract void setErrorOutput(boolean errors);

    /**
     * Determines whether or not to automatically print errors to the console
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return {@code true} if content is printed to the console.
     */
    public boolean isSendingErrorOutput();

}
