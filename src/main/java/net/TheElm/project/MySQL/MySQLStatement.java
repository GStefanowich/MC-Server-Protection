/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.MySQL;

import net.TheElm.project.CoreMod;

import java.sql.*;
import java.util.UUID;

public class MySQLStatement implements AutoCloseable {
    
    private final PreparedStatement stmt;
    
    private final String rawStatement;
    private boolean batched = false;
    private int batchCount = 0;
    private Integer slider = 0;
    
    MySQLStatement(MySQLHost host, String preparedStmt ) throws SQLException {
        this( host, preparedStmt, true );
    }
    MySQLStatement(MySQLHost host, String preparedStmt, boolean batchMode ) throws SQLException {
        this.rawStatement = preparedStmt;
        Connection conn = host.getConnection();
        
        CoreMod.logDebug( "Preparing new MySQL statement." );
        
        this.stmt = conn.prepareStatement( preparedStmt, Statement.RETURN_GENERATED_KEYS );
        this.batched = batchMode;
    }
    
    private MySQLStatement addPrepared( Object object ) {
        return this.addPrepared( ++this.slider, object );
    }
    private MySQLStatement addPrepared( int pos, Object object ) {
        if ( this.stmt != null ) {
            try {
                stmt.setObject( pos, object );
            } catch ( SQLException e ) {
                CoreMod.logError( e );
            }
        }
        return this;
    }
    public MySQLStatement addPrepared( UUID uuid ) {
        return this.addPrepared( (Object) uuid.toString() );
    }
    public MySQLStatement addPrepared( String string ) {
        return this.addPrepared( (Object) string );
    }
    public MySQLStatement addPrepared( Number i ) {
        return this.addPrepared( (Object) i );
    }
    public MySQLStatement addPrepared( Enum e ) {
        return this.addPrepared( e.name() );
    }
    
    public void addBatch() {
        if ( !this.batched )
            return;
        if ( this.stmt == null )
            return;
        try {
            this.stmt.addBatch();
            this.slider = 0;
            this.batchCount++;
        } catch ( SQLException e ) {
            CoreMod.logError( e );
        }
    }
    public int batchCount() {
        return this.batchCount;
    }
    
    public ResultSet executeStatement() throws SQLException {
        return this.executeStatement( false );
    }
    public ResultSet executeStatement( boolean close ) throws SQLException {
        try {
            
            this.slider = 0;
            return this.stmt.executeQuery();
            
        } finally {
            if (close)
                this.close();
        }
    }
    public int executeUpdate() throws SQLException {
        return this.executeUpdate( !this.batched );
    }
    public int executeUpdate( boolean close ) throws SQLException {
        try {

            this.slider = 0;
            return this.stmt.executeUpdate();

        } finally {
            if (close)
                this.close();
        }
    }
    
    public boolean isClosed() {
        if ( this.stmt == null )
            return true;
        try {
            // Check if connection is still open
            Connection connection = this.stmt.getConnection();
            
            if ( connection.isClosed() | (!connection.isValid( 5 )) )
                this.stmt.close();
            
            return this.stmt.isClosed();
            
        } catch ( SQLException e ) {
            return true;
        }
    }
    public void close() {
        try {
            if ( this.stmt != null ) {
                // Close the statement
                if ( !this.stmt.isClosed() )
                    this.stmt.close();
            }
        } catch ( SQLException e ) {
            CoreMod.logError( e );
        }
    }
}
