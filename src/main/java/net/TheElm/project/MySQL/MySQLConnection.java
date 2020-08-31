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

import com.mysql.cj.jdbc.MysqlDataSource;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnection implements MySQLHost {
    
    private Connection conn = null;
    
    @Override
    public Connection getConnection() throws SQLException {
        /* 
         * Test if MySQL is still connected
         */
        if ( this.conn != null ) {
            if ( (!this.conn.isClosed()) && this.conn.isValid(5) )
                return this.conn;
            if ( !this.conn.isClosed() )
                this.conn.close();
        }
        
        /*
         * Create a new connection
         */
        
        MysqlDataSource dataSource = new MysqlDataSource();
        
        // Enable STRICT mode
        dataSource.setStrictUpdates( true );
        
        // Login information
        dataSource.setUser(SewConfig.get(SewConfig.DB_USER));
        dataSource.setPassword(SewConfig.get(SewConfig.DB_PASS));
        
        dataSource.setServerTimezone( "UTC" );
        
        // Database information
        dataSource.setServerName(SewConfig.get(SewConfig.DB_HOST));
        dataSource.setPortNumber(SewConfig.get(SewConfig.DB_PORT));
        
        dataSource.setDatabaseName(SewConfig.get(SewConfig.DB_NAME));
        
        CoreMod.logInfo( "Opening our SQL connection" );
        
        // Allow multiple queries
        dataSource.setAllowMultiQueries( true );
        
        // Auto reconnect if closed
        dataSource.setAutoReconnect( true );
        
        return ( this.conn = dataSource.getConnection() );
    }
    
}
