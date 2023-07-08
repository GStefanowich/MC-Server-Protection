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

package net.theelm.sewingmachine.MySQL;

import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.utilities.mod.Sew;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLite implements MySQLHost {
    
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
        
        try {
            final File dir = Sew.getConfDir();
            final File jdbc = new File( dir.getAbsolutePath(), "sqlite.db" );
            
            System.out.println( "A.1" );
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + jdbc.getAbsolutePath());
            System.out.println( "A.2" );
            
            if (this.conn != null)
                System.out.println( this.conn.getMetaData().getDriverName() );
            
        } catch (RuntimeException e) {
            CoreMod.logError( e );
        }
        return this.conn;
    }
    
}
