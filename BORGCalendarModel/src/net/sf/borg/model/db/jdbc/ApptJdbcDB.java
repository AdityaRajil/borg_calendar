/*
This file is part of BORG.
 
    BORG is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    BORG is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with BORG; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
Copyright 2003 by ==Quiet==
 */
/*
 * AppJdbcDB.java
 *
 * Created on February 1, 2004, 3:59 PM
 */

package net.sf.borg.model.db.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import net.sf.borg.common.util.Version;
import net.sf.borg.model.Appointment;
import net.sf.borg.model.AppointmentKeyFilter;
import net.sf.borg.model.db.DBException;
import net.sf.borg.model.db.KeyedBean;


/**
 *
 * this is the JDBC layer for access to the appointment table
 */
class ApptJdbcDB extends JdbcDB implements AppointmentKeyFilter
{
    static
    {
        Version.addVersion("$Id$");
    }
       
    /** Creates a new instance of AppJdbcDB */
    ApptJdbcDB(String url, String userid, String passwd) throws Exception
    {
        super( url, userid, passwd );
    }
    
    public void addObj(KeyedBean bean, boolean crypt) throws DBException, Exception
    {
        PreparedStatement stmt = connection_.prepareStatement( "INSERT INTO appointments ( appt_date, appt_num, userid, duration, text, skip_list," +
        " next_todo, vacation, holiday, private, times, frequency, todo, color, repeat ) VALUES " +
        "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        
        Appointment appt = (Appointment) bean;
        
        stmt.setTimestamp( 1, new java.sql.Timestamp( appt.getDate().getTime()), Calendar.getInstance() );
        stmt.setInt( 2, appt.getKey() );
        stmt.setInt( 3, 1 );
        
        stmt.setInt( 4, toInt( appt.getDuration() ) );
        stmt.setString( 5, appt.getText() );
        stmt.setString( 6, toStr( appt.getSkipList() ) );
        java.util.Date nt = appt.getNextTodo();
        if( nt != null )
            stmt.setDate( 7, new java.sql.Date( appt.getNextTodo().getTime()) );
        else
            stmt.setDate(7, null );
        stmt.setInt( 8, toInt( appt.getVacation() ));
        stmt.setInt( 9, toInt( appt.getHoliday() ));
        stmt.setInt( 10, toInt( appt.getPrivate()) );
        stmt.setInt( 11, toInt( appt.getTimes() ));
        stmt.setString( 12, appt.getFrequency());
        stmt.setInt( 13, toInt( appt.getTodo()) );
        stmt.setString( 14, appt.getColor());
        stmt.setInt( 15, toInt( appt.getRepeatFlag()) );
        
        stmt.executeUpdate();
        
        writeCache( appt );
    }
    
    public void delete(int key) throws DBException, Exception
    {
        PreparedStatement stmt = connection_.prepareStatement( "DELETE FROM appointments WHERE appt_num = ? AND userid = ?" );
        stmt.setInt( 1, key );
        stmt.setInt( 2, 1 );
        stmt.executeUpdate();
        
        delCache( key );
    }
    
    public Collection getKeys() throws Exception
    {
        ArrayList keys = new ArrayList();
        PreparedStatement stmt = connection_.prepareStatement("SELECT appt_num FROM appointments WHERE userid = ?" );
        stmt.setInt( 1, 1 );
        ResultSet rs = stmt.executeQuery();
        while( rs.next() )
        {
            keys.add( new Integer(rs.getInt("appt_num")) );
        }
        
        return( keys );
        
    }
    
    public Collection getTodoKeys() throws Exception
    {
        ArrayList keys = new ArrayList();
        PreparedStatement stmt = connection_.prepareStatement("SELECT appt_num FROM appointments WHERE userid = ? AND todo = '1'" );
        stmt.setInt( 1, 1 );
        ResultSet rs = stmt.executeQuery();
        while( rs.next() )
        {
            keys.add( new Integer(rs.getInt("appt_num")) );
        }
        
        return( keys );
        
    }
    
    public Collection getRepeatKeys() throws Exception
    {
        ArrayList keys = new ArrayList();
        PreparedStatement stmt = connection_.prepareStatement("SELECT appt_num FROM appointments WHERE userid = ? AND repeat = '1'" );
        stmt.setInt( 1, 1 );
        ResultSet rs = stmt.executeQuery();
        while( rs.next() )
        {
            keys.add( new Integer(rs.getInt("appt_num")) );
        }
        
        return( keys );
        
    }
    /*
    public boolean isRepeat(int key) throws Exception
    {
        PreparedStatement stmt = connection_.prepareStatement("SELECT repeat FROM appointments WHERE userid = ? AND appt_num = ?" );
        stmt.setInt( 1, 1 );
        stmt.setInt( 2, key );
        ResultSet rs = stmt.executeQuery();
        if( rs.next() && rs.getInt( "repeat" ) == 1 )
        {
            return(true);
        }
        return( false );
        
    }*/
    public int maxkey()
    {
        return(0);
    }
    
    public KeyedBean newObj()
    {
        return( new Appointment() );
    }
    
	PreparedStatement getPSOne(int key) throws SQLException
	{
		PreparedStatement stmt = connection_.prepareStatement("SELECT * FROM appointments WHERE appt_num = ? AND userid = ?" );
		stmt.setInt( 1, key );
		stmt.setInt( 2, 1 );
		return stmt;
	}
	
	PreparedStatement getPSAll() throws SQLException
	{
		PreparedStatement stmt = connection_.prepareStatement("SELECT * FROM appointments WHERE userid = ?" );
		stmt.setInt( 1, 1 );
		return stmt;
	}
	
	KeyedBean createFrom(ResultSet r) throws SQLException
	{
		Appointment appt = new Appointment();
		appt.setKey(r.getInt("appt_num"));
		appt.setDate( r.getTimestamp("appt_date"));
		appt.setDuration( new Integer(r.getInt("duration")));
		appt.setText( r.getString("text"));
		appt.setSkipList( toVect(r.getString("skip_list")));
		appt.setNextTodo( r.getDate("next_todo"));
		appt.setVacation( new Integer(r.getInt("vacation")));
		appt.setHoliday( new Integer(r.getInt("holiday")));
		appt.setPrivate( r.getInt("private") != 0 );
		appt.setTimes( new Integer(r.getInt("times")));
		appt.setFrequency( r.getString("frequency"));
		appt.setTodo( r.getInt("todo") != 0 );
		appt.setColor( r.getString("color"));
		appt.setRepeatFlag( r.getInt("repeat" ) != 0 );
		return appt;
	}
	
    public void updateObj(KeyedBean bean, boolean crypt) throws DBException, Exception
    {
        PreparedStatement stmt = connection_.prepareStatement( "UPDATE appointments SET  appt_date = ?, " +
        "duration = ?, text = ?, skip_list = ?," +
        " next_todo = ?, vacation = ?, holiday = ?, private = ?, times = ?, frequency = ?, todo = ?, color = ?, repeat = ?" +
        " WHERE appt_num = ? AND userid = ?");
        
        Appointment appt = (Appointment) bean;
        
        stmt.setTimestamp( 1, new java.sql.Timestamp( appt.getDate().getTime()), Calendar.getInstance() );
        
        stmt.setInt( 2, toInt( appt.getDuration() ) );
        stmt.setString( 3, appt.getText() );
        stmt.setString( 4, toStr( appt.getSkipList() ));
        java.util.Date nt = appt.getNextTodo();
        if( nt != null )
            stmt.setDate( 5, new java.sql.Date( appt.getNextTodo().getTime()) );
        else
            stmt.setDate( 5, null );
        stmt.setInt( 6, toInt( appt.getVacation() ));
        stmt.setInt( 7, toInt( appt.getHoliday() ));
        stmt.setInt( 8, toInt( appt.getPrivate()) );
        stmt.setInt( 9, toInt( appt.getTimes() ));
        stmt.setString( 10, appt.getFrequency());
        stmt.setInt( 11, toInt( appt.getTodo()) );
        stmt.setString( 12, appt.getColor());
        stmt.setInt( 13, toInt( appt.getRepeatFlag()) );
        stmt.setInt( 14, appt.getKey() );
        stmt.setInt( 15, 1 );
        
        stmt.executeUpdate();
        
        delCache( appt.getKey() );
        writeCache( appt );
    }
    
}
