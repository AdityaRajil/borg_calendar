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

package net.sf.borg.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.borg.common.app.AppHelper;
import net.sf.borg.common.io.IOHelper;
import net.sf.borg.common.io.OSServicesHome;
import net.sf.borg.common.util.Errmsg;
import net.sf.borg.common.util.Prefs;
import net.sf.borg.common.util.Resource;
import net.sf.borg.common.util.Version;
import net.sf.borg.common.util.XTree;
import net.sf.borg.model.AddressModel;
import net.sf.borg.model.Appointment;
import net.sf.borg.model.AppointmentModel;
import net.sf.borg.model.Day;
import net.sf.borg.model.Task;
import net.sf.borg.model.TaskModel;
import net.sf.borg.ui.OptionsView.RestartListener;

import com.jeans.trayicon.WindowsTrayIcon;



// This is the month view GUI
// it is the main borg window
// like most of the other borg window, you really need to check the netbeans form
// editor to get the graphical picture of the whole window
public class CalendarView extends View {
    
    // current year/month being viewed
    private int year_;
    private int month_;
    private boolean trayIcon_;
    private RestartListener rl_;
    
    // the button we used to dismiss any child dialog
    private MemDialog dlgMemFiles;
    
    // the file we chose in our memory file chooser dialog
    private String memFile;
    
    static {
        Version.addVersion("$Id$");
    }
    
    private static CalendarView singleton = null;
    public static CalendarView getReference(RestartListener rl, boolean trayIcon) {
        if( singleton == null || !singleton.isShowing())
            singleton = new CalendarView(rl, trayIcon);
        return( singleton );
    }
    
    private CalendarView(RestartListener rl, boolean trayIcon) {
        super();
        trayIcon_ = trayIcon;
        rl_ = rl;
        addModel(AppointmentModel.getReference());
        addModel(TaskModel.getReference());
        addModel(AddressModel.getReference());
        init();
    }
    
    private void init() {
        
        initComponents();
        
        
        GridBagConstraints cons;
        
        // the day boxes - which will contain a date button and day text
        days = new JPanel[37];
        
        // the day text areas
        daytext = new JTextPane[37];
        
        // the date buttons
        daynum = new JButton[37];
        
        
        // initialize the days
        for( int i = 0; i < 37; i++ ) {
            
            // allocate a panel for each day
            // and add a date button and non wrapping text pane
            // in each
            days[i] = new JPanel();
            days[i].setLayout(new GridBagLayout());
            // as per the experts, this subclass of JTextPane is the only way to
            // stop word-wrap
            JTextPane jep = null;
            String wrap = Prefs.getPref("wrap", "false" );
            if( wrap.equals("true") ) {
                jep = new JTextPane();
            }
            else {
                jep =  new JTextPane() {
                    public boolean getScrollableTracksViewportWidth() {
                        return false;
                    }
                    public void setSize(Dimension d) {
                        if(d.width < getParent().getSize().width) {
                            d.width = getParent().getSize().width;
                        }
                        super.setSize(d);
                    }
                };
            }
            daytext[i] = jep;
            daytext[i].setEditable(false);
            daynum[i] = new JButton("N");
            
            // when the date button is pressed, call the borg controlling class
            // to request popup of an appointment editor for the day
            daynum[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    JButton but = (JButton) evt.getSource();
                    int day = Integer.parseInt(but.getText());
                    // start the appt editor view
                    //AppointmentListView ag = AppointmentListView.getReference(year_, month_, day);
                    AppointmentListView ag = new AppointmentListView(year_, month_, day);
                    ag.show();
                }
            });
            
            // continue laying out the day panel. want the date button in upper right
            // and want the text pane top to be lower than the bottom of the
            // button.
            Insets is = new Insets(1,4,1,4);
            daynum[i].setMargin(is);
            days[i].setBorder(new BevelBorder(BevelBorder.RAISED));
            days[i].add(daynum[i]);
            cons = new GridBagConstraints();
            cons.gridx = 0;
            cons.gridy = 0;
            cons.gridwidth = 1;
            cons.fill = GridBagConstraints.NONE;
            cons.anchor = GridBagConstraints.NORTHEAST;
            days[i].add(daynum[i], cons);
            
            cons.gridx = 0;
            cons.gridy = 1;
            cons.gridwidth = 1;
            cons.weightx = 1.0;
            cons.weighty = 1.0;
            cons.fill = GridBagConstraints.BOTH;
            cons.anchor = GridBagConstraints.NORTHWEST;
            
            // put the appt text in an invisible scroll pane
            // scrollbars will only appear if needed due to amount of appt text
            JScrollPane sp = new JScrollPane();
            sp.setViewportView(daytext[i]);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setBorder( new EmptyBorder(0,0,0,0) );
            sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0,10));
            sp.getVerticalScrollBar().setPreferredSize(new Dimension(10,0));
            days[i].add(sp,cons);
            
            
            jPanel1.add(days[i]);
        }
        
        // add filler to the Grid
        jPanel1.add( new JPanel() );
        jPanel1.add( new JPanel() );
        jPanel1.add( new JPanel() );
        
        
        setDayLabels();
        //
        // ToDo PREVIEW BOX
        //
        todoPreview = new JTextPane() {
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
            public void setSize(Dimension d) {
                if(d.width < getParent().getSize().width) {
                    d.width = getParent().getSize().width;
                }
                super.setSize(d);
            }
        };
        todoPreview.setBackground( new Color( 204,204,204 ));
        todoPreview.setEditable(false);
        JScrollPane sp = new JScrollPane();
        sp.setViewportView(todoPreview);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //sp.setBorder( new javax.swing.border.EmptyBorder(0,0,0,0) );
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0,5));
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(5,0));
        jPanel1.add( sp );
        
        //
        // TASK PREVIEW BOX
        //
        taskPreview = new JTextPane() {
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
            public void setSize(Dimension d) {
                if(d.width < getParent().getSize().width) {
                    d.width = getParent().getSize().width;
                }
                super.setSize(d);
            }
        };
        taskPreview.setBackground( new Color( 204,204,204 ));
        taskPreview.setEditable(false);
        sp = new JScrollPane();
        sp.setViewportView(taskPreview);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //sp.setBorder( new javax.swing.border.EmptyBorder(0,0,0,0) );
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0,5));
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(5,0));
        jPanel1.add( sp );
        
        // update the styles used in the appointment text panes for the various appt text
        // colors, based on the current font size set by the user
        updStyles();
        
        // init view to current month
        try {
            today();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
        
        String shared = Prefs.getPref("shared", "false");
        if( !shared.equals("true")) {
            syncMI.setEnabled(false);
        }
        else {
            syncMI.setEnabled(true);
        }
        
		dlgMemFiles = new MemDialog(this);
		dlgMemoryFilesChooser.setSize(200,350);
		listFiles.setModel(new DefaultListModel());
		importMI.setEnabled(!AppHelper.isApplet());
		exportMI.setEnabled(AppHelper.isApplication());

        // show the window
        pack();
        setVisible(true);
        
        String version = Resource.getVersion();
        if( version.indexOf("beta") != -1 )
            Errmsg.notice(Resource.getResourceString("betawarning"));
        
    }
    
    public void destroy() {
        this.dispose();
    }
    
	// create an OutputStream from a URL string, special-casing "mem:"
	private OutputStream createOutputStreamFromURL(String urlstr)
		throws Exception
	{
		if (urlstr.startsWith("mem:"))
			return IOHelper.createOutputStream(urlstr);
		else
			return IOHelper.createOutputStream(new URL(urlstr));
	}

    /* set borg to current month and refresh the screen */
    private void today() throws Exception {
        GregorianCalendar cal = new GregorianCalendar();
        month_ = cal.get(Calendar.MONTH);
        year_ = cal.get(Calendar.YEAR);
        refresh();
    }
    
    // initialize the various text styles used for appointment
    // text for a single text pane
    private void initStyles(JTextPane textPane, Style def, Font font) {
        //Initialize some styles.
        Style bl = textPane.addStyle("black", def);
        int fontsize = font.getSize();
        String family = font.getFamily();
        boolean bold = font.isBold();
        boolean italic = font.isItalic();
        
        StyleConstants.setFontFamily(bl, family);
        StyleConstants.setBold(bl, bold);
        StyleConstants.setItalic( bl, italic);
        StyleConstants.setFontSize(bl, fontsize);
        
        try {
            Style s = textPane.addStyle("blue", bl);
            StyleConstants.setForeground(s, Color.BLUE);
            
            
            s = textPane.addStyle("red", bl);
            StyleConstants.setForeground(s, Color.RED);
            
            
            s = textPane.addStyle("green", bl);
            StyleConstants.setForeground(s, Color.GREEN);
            
            
            s = textPane.addStyle("white", bl);
            StyleConstants.setForeground(s, Color.WHITE);
            
            s = textPane.addStyle("sm", bl );
            StyleConstants.setBold(s,false);
            StyleConstants.setFontSize(s,fontsize);
            
            s = textPane.addStyle("smul", s );
            StyleConstants.setUnderline( s, true );
        }
        catch( NoSuchFieldError e ) {
            // java 1.3 - just use black
        }
        
        
    }
    
    // update the text styles for all appt text panes
    // this is called when the user changes the font size
    void updStyles() {
        
        
        int fontsize = Prefs.getPref("apptfontsize", 10 );
        
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        // update all of the text panes
        String s = Prefs.getPref("apptfont", "SansSerif-10");
        Font f = Font.decode(s);
        for( int i = 0; i < 37; i++ ) {
            initStyles(daytext[i], def, f);
        }
        
        s = Prefs.getPref("previewfont", "SansSerif-10");
        f = Font.decode(s);
        
        initStyles( todoPreview, def, f );
        initStyles( taskPreview, def, f );
        
    }
    
    // adds a string to an appt text pane using a given style
    private void addString(JTextPane tp, String s, String style) throws Exception {
        
        StyledDocument doc = tp.getStyledDocument();
        
        if( style == null ) style = "black";
        
        // get the right style based on the color
        Style st = tp.getStyle(style);
        
        // static can be null for old BORG DBs that have
        // colors no longer supported. Only 2-3 people would encounter this.
        // default to black
        if( st == null )
            st = tp.getStyle("black");
        
        // add string to text pane
        doc.insertString(doc.getLength(), s, st);
        
    }
    
    void setDayLabels() {
        // determine first day and last day of the month
        GregorianCalendar cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Prefs.getPref("first_dow", Calendar.SUNDAY ));
        cal.set(Calendar.DATE, 1 );
        cal.add( Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK) );
        SimpleDateFormat dwf = new SimpleDateFormat("EEEE");
        jLabel1.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel3.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel4.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel5.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel6.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel7.setText( dwf.format(cal.getTime() ));
        cal.add( Calendar.DAY_OF_WEEK, 1 );
        jLabel8.setText( dwf.format(cal.getTime() ));
    }
    
    private void exit()
    {
    	if (IOHelper.isMemFilesDirty() && !AppHelper.isApplet())
    	{
    		setVisible(false);
			JOptionPane.showMessageDialog(
				this,
				Resource.getResourceString("Memory_Files_Changed"),
				Resource.getResourceString("Memory_Files_Changed_Title"),
				JOptionPane.INFORMATION_MESSAGE);
			dlgMemFiles.setMemento(IOHelper.getMemFilesMemento());
			dlgMemFiles.setVisible(true);
    	}
		System.exit(0);
    }
    
    /** refresh displays a month on the main gui window and updates the todo and task previews*/
    public void refresh() {
        try {
            
            // determine first day and last day of the month
            GregorianCalendar cal = new GregorianCalendar();
            int today = -1;
            if( month_ == cal.get(Calendar.MONTH) && year_ == cal.get(Calendar.YEAR)) {
                today = cal.get(Calendar.DAY_OF_MONTH);
                Today.setEnabled(false);
            }
            else {
                Today.setEnabled(true);
            }
            
            cal.setFirstDayOfWeek(Prefs.getPref("first_dow", Calendar.SUNDAY ));
            
            // set cal to day 1 of month
            cal.set( year_, month_, 1 );
            
            // set month title
            SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy");
            MonthLabel.setText( df.format(cal.getTime()) );
            
            // get day of week of day 1
            int fd = cal.get( Calendar.DAY_OF_WEEK ) - cal.getFirstDayOfWeek();
            if( fd == -1 ) fd = 6;
            
            // get last day of month
            int ld = cal.getActualMaximum( Calendar.DAY_OF_MONTH );
            
            // set show public/private flags
            boolean showpub = false;
            boolean showpriv = false;
            String sp = Prefs.getPref("showpublic", "true" );
            if( sp.equals("true") )
                showpub = true;
            sp = Prefs.getPref("showprivate", "false" );
            if( sp.equals("true") )
                showpriv = true;
            
            // fill in the day boxes that correspond to days in this month
            for( int i = 0; i < 37; i++ ) {
                
                int daynumber = i - fd + 1;
                
                
                // clear any text in the box from the last displayed month
                StyledDocument doc = daytext[i].getStyledDocument();
                if( doc.getLength() > 0 )
                    doc.remove(0,doc.getLength());
                
                // if the day box is not needed for the current month, make it invisible
                if( daynumber <= 0 || daynumber > ld ) {
                    // the following line fixes a bug (in Java) where any daytext not
                    // visible in the first month would show its first line of text
                    // in the wrong Style when it became visible in a different month -
                    // using a Style not even set by this program.
                    // once this happened, resizing the window would fix the Style
                    // so it probably is a swing bug
                    addString( daytext[i], "bug fix", "black" );
                    
                    days[i].setVisible(false);
                }
                else {
                    
                    // set value of date button
                    daynum[i].setText(Integer.toString(daynumber));
                    
                    // get appointment info for the day's appointments from the data model
                    Day di = Day.getDay( year_, month_, daynumber, showpub,showpriv);
                    Collection appts = di.getAppts();
                    
                    if( appts != null ) {
                        Iterator it = appts.iterator();
                        
                        // iterate through the day's appts
                        while( it.hasNext() ) {
                            
                            Appointment info = (Appointment) it.next();
                            
                            // add the day's text in the right color. If the appt is the last
                            // one - don't add a trailing newline - it will make the text pane
                            // have one extra line - forcing an unecessary scrollbar at times
                            if( it.hasNext() ) {
                                addString( daytext[i], info.getText() + "\n", info.getColor() );
                            }
                            else
                                addString( daytext[i], info.getText(), info.getColor() );
                            
                        }
                    }
                    
                    // reset the text pane to show the top left of the appt text if the text
                    // scrolls up or right
                    daytext[i].setCaretPosition(0);
                    
                    int xcoord = i%7;
                    int dow = cal.getFirstDayOfWeek() + xcoord;
                    if( dow == 8 ) dow = 1;
                    
                    // set the day color based on if the day is today, or if any of the
                    // appts for the day are holidays, vacation days, half-days, or weekends
                    if( today == daynumber ) {
                        // today color is pink
                        daytext[i].setBackground( new Color(225,150,150));
                        days[i].setBackground( new Color(225,150,150));
                    }
                    else if( di.getHoliday() == 1 ) {
                        // holiday color
                        daytext[i].setBackground( new Color(245,203,162));
                        days[i].setBackground( new Color(245,203,162));
                    }
                    else if( di.getVacation() == 1 ) {
                        // vacation color
                        daytext[i].setBackground( new Color(155,255,153));
                        days[i].setBackground( new Color(155,255,153));
                    }
                    else if( di.getVacation() == 2 ) {
                        // half day color
                        daytext[i].setBackground( new Color(200,255,200));
                        days[i].setBackground( new Color(200,255,200));
                    }
                    else if( dow != Calendar.SUNDAY && dow != Calendar.SATURDAY ) {
                        // weekday color
                        days[i].setBackground( new Color(255,233,192));
                        daytext[i].setBackground( new Color(255,233,192));
                    }
                    else {
                        // weekend color
                        daytext[i].setBackground( new Color(245,203,162));
                        days[i].setBackground( new Color(245,203,162));
                    }
                    
                    days[i].setVisible(true);
                    
                }
                
                
                
            }
            
            // label the week buttons
            // the buttons have bad names due to my lazy use of NetBeans
            cal.setMinimalDaysInFirstWeek(4);
            cal.set( year_, month_, 1 );
            int wk = cal.get( Calendar.WEEK_OF_YEAR );
            jButton1.setText( Integer.toString(wk));
            cal.set( year_, month_, 8 );
            wk = cal.get( Calendar.WEEK_OF_YEAR );
            jButton2.setText( Integer.toString(wk));
            cal.set( year_, month_, 15 );
            wk = cal.get( Calendar.WEEK_OF_YEAR );
            jButton3.setText( Integer.toString(wk));
            cal.set( year_, month_, 22 );
            wk = cal.get( Calendar.WEEK_OF_YEAR );
            jButton4.setText( Integer.toString(wk));
            cal.set( year_, month_, 29 );
            wk = cal.get( Calendar.WEEK_OF_YEAR );
            jButton5.setText( Integer.toString(wk));
            
            
            // update todoPreview Box
            StyledDocument tdoc = todoPreview.getStyledDocument();
            tdoc.remove(0,tdoc.getLength());
            
            // sort and add the todos
            Vector tds = AppointmentModel.getReference().get_todos();
            if( tds.size() > 0 ) {
                addString( todoPreview, Resource.getResourceString("Todo_Preview") + "\n", "smul" );
                
                
                // the treeset will sort by date
                TreeSet ts = new TreeSet(new Comparator() {
                    public int compare(java.lang.Object obj, java.lang.Object obj1) {
                        try {
                            Appointment r1 = (Appointment)obj;
                            Appointment r2 = (Appointment)obj1;
                            Date dt1 = r1.getNextTodo();
                            if( dt1 == null ) {
                                dt1 = r1.getDate();
                            }
                            Date dt2 = r2.getNextTodo();
                            if( dt2 == null ) {
                                dt2 = r2.getDate();
                            }
                            
                            if( dt1.after( dt2 ))
                                return(1);
                            return(-1);
                        }
                        catch( Exception e ) {
                            return(0);
                        }
                    }
                });
                
                // sort the todos by adding to the TreeSet
                for( int i = 0; i < tds.size(); i++ ) {
                    ts.add( tds.elementAt(i) );
                }
                
                Iterator it = ts.iterator();
                while( it.hasNext() ) {
                    try {
                        Appointment r = (Appointment) it.next();
                        
                        // !!!!! only show first line of appointment text !!!!!!
                        String tx = "";
                        String xx = r.getText();
                        int ii = xx.indexOf('\n');
                        if( ii != -1 ) {
                            tx = xx.substring(0,ii);
                        }
                        else {
                            tx = xx;
                        }
                        addString( todoPreview, tx + "\n", "sm" );
                        
                    }
                    catch( Exception e)
                    { Errmsg.errmsg(e); }
                    
                }
                todoPreview.setCaretPosition(0);
            }
            else {
                addString( todoPreview, Resource.getResourceString("Todo_Preview") + "\n", "smul" );
                addString( todoPreview, Resource.getResourceString("none_pending"), "sm" );
            }
            
            
            // update taskPreview Box
            StyledDocument tkdoc = taskPreview.getStyledDocument();
            tkdoc.remove(0,tkdoc.getLength());
            
            // sort and add the tasks
            Vector tks = TaskModel.getReference().get_tasks();
            if( tks.size() > 0 ) {
                addString( taskPreview, Resource.getResourceString("Task_Preview") + "\n", "smul" );
                
                // the treeset will sort by date
                TreeSet ts = new TreeSet(new Comparator() {
                    public int compare(java.lang.Object obj, java.lang.Object obj1) {
                        try {
                            Task r1 = (Task)obj;
                            Task r2 = (Task)obj1;
                            Date dt1 = r1.getDueDate();
                            Date dt2 = r2.getDueDate();
                            if( dt1.after( dt2 ))
                                return(1);
                            return(-1);
                        }
                        catch( Exception e ) {
                            return(0);
                        }
                    }
                });
                
                // sort the tasks by adding to the treeset
                for( int i = 0; i < tks.size(); i++ ) {
                    ts.add( tks.elementAt(i) );
                }
                
                Iterator it = ts.iterator();
                while( it.hasNext() ) {
                    try {
                        Task r = (Task) it.next();
                        
                        // !!!!! only show first line of task text !!!!!!
                        String tx = "";
                        String xx = r.getDescription();
                        int ii = xx.indexOf('\n');
                        if( ii != -1 ) {
                            tx = xx.substring(0,ii);
                        }
                        else {
                            tx = xx;
                        }
                        addString( taskPreview, "BT" + r.getTaskNumber() + ":" + tx + "\n", "sm" );
                        
                    }
                    catch( Exception e)
                    { Errmsg.errmsg(e); }
                    
                }
                taskPreview.setCaretPosition(0);
            }
            else {
                addString( taskPreview, Resource.getResourceString("Task_Preview") + "\n", "smul" );
                addString( taskPreview, Resource.getResourceString("none_pending"), "sm" );
            }
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
        
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        dlgMemoryFilesChooser = new javax.swing.JDialog();
        pnlMainMemFilesChooser = new javax.swing.JPanel();
        lblMemChooser = new javax.swing.JLabel();
        listFiles = new javax.swing.JList();
        pnlButtonsMemFilesChooser = new javax.swing.JPanel();
        bnMemFilesChooserOK = new javax.swing.JButton();
        bnMemFilesChooserCancel = new javax.swing.JButton();
        MonthLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        Next = new javax.swing.JButton();
        Prev = new javax.swing.JButton();
        Today = new javax.swing.JButton();
        Goto = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        ActionMenu = new javax.swing.JMenu();
        TaskTrackMI = new javax.swing.JMenuItem();
        ToDoMenu = new javax.swing.JMenuItem();
        AddressMI = new javax.swing.JMenuItem();
        SearchMI = new javax.swing.JMenuItem();
        PrintMonthMI = new javax.swing.JMenuItem();
        printprev = new javax.swing.JMenuItem();
        syncMI = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        OptionMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        navmenu = new javax.swing.JMenu();
        nextmi = new javax.swing.JMenuItem();
        prevmi = new javax.swing.JMenuItem();
        todaymi = new javax.swing.JMenuItem();
        gotomi = new javax.swing.JMenuItem();
        catmenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        impexpMenu = new javax.swing.JMenu();
        impXML = new javax.swing.JMenu();
        importMI = new javax.swing.JMenuItem();
        impurl = new javax.swing.JMenuItem();
        impXMLMem = new javax.swing.JMenuItem();
        expXML = new javax.swing.JMenu();
        exportMI = new javax.swing.JMenuItem();
        expurl = new javax.swing.JMenuItem();
        expXMLMem = new javax.swing.JMenuItem();
        viewMem = new javax.swing.JMenuItem();
        helpmenu = new javax.swing.JMenu();
        helpMI = new javax.swing.JMenuItem();
        licsend = new javax.swing.JMenuItem();
        readme = new javax.swing.JMenuItem();
        chglog = new javax.swing.JMenuItem();
        AboutMI = new javax.swing.JMenuItem();

        dlgMemoryFilesChooser.setTitle(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("dlgMemoryFilesChooser"));
        dlgMemoryFilesChooser.setLocationRelativeTo(this);
        dlgMemoryFilesChooser.setModal(true);
        dlgMemoryFilesChooser.setName("dlgMemoryFilesChooser");
        pnlMainMemFilesChooser.setLayout(new java.awt.BorderLayout());

        pnlMainMemFilesChooser.setMinimumSize(new java.awt.Dimension(200, 315));
        pnlMainMemFilesChooser.setPreferredSize(new java.awt.Dimension(200, 315));
        lblMemChooser.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("lblMemChooser"));
        pnlMainMemFilesChooser.add(lblMemChooser, java.awt.BorderLayout.NORTH);

        listFiles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listFiles.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listFilesValueChanged(evt);
            }
        });

        pnlMainMemFilesChooser.add(listFiles, java.awt.BorderLayout.CENTER);

        dlgMemoryFilesChooser.getContentPane().add(pnlMainMemFilesChooser, java.awt.BorderLayout.CENTER);

        bnMemFilesChooserOK.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("OK"));
        bnMemFilesChooserOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnMemFilesChooserOKActionPerformed(evt);
            }
        });

        pnlButtonsMemFilesChooser.add(bnMemFilesChooserOK);

        bnMemFilesChooserCancel.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Cancel"));
        bnMemFilesChooserCancel.setDefaultCapable(false);
        bnMemFilesChooserCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnMemFilesChooserCancelActionPerformed(evt);
            }
        });

        pnlButtonsMemFilesChooser.add(bnMemFilesChooserCancel);

        dlgMemoryFilesChooser.getContentPane().add(pnlButtonsMemFilesChooser, java.awt.BorderLayout.SOUTH);

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Borg");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        MonthLabel.setBackground(new java.awt.Color(137, 137, 137));
        MonthLabel.setFont(new java.awt.Font("Dialog", 0, 24));
        MonthLabel.setForeground(new java.awt.Color(51, 0, 51));
        MonthLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        MonthLabel.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Month"));
        MonthLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(MonthLabel, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridLayout(1, 7));

        jPanel2.setBorder(new javax.swing.border.EtchedBorder());
        jLabel1.setForeground(MonthLabel.getForeground());
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel1);

        jLabel3.setForeground(MonthLabel.getForeground());
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel3);

        jLabel4.setForeground(MonthLabel.getForeground());
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel4);

        jLabel5.setForeground(MonthLabel.getForeground());
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel5);

        jLabel6.setForeground(MonthLabel.getForeground());
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel6);

        jLabel7.setForeground(MonthLabel.getForeground());
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel7);

        jLabel8.setForeground(MonthLabel.getForeground());
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel2.add(jLabel8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridLayout(0, 7));

        jPanel1.setPreferredSize(new java.awt.Dimension(800, 600));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel1, gridBagConstraints);

        Next.setForeground(MonthLabel.getForeground());
        Next.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Next__>>"));
        Next.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NextActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(Next, gridBagConstraints);

        Prev.setForeground(MonthLabel.getForeground());
        Prev.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("<<__Prev"));
        Prev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrevActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(Prev, gridBagConstraints);

        Today.setForeground(MonthLabel.getForeground());
        Today.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("curmonth"));
        Today.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                today(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(Today, gridBagConstraints);

        Goto.setForeground(MonthLabel.getForeground());
        Goto.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Go_To"));
        Goto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GotoActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(Goto, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridLayout(0, 1));

        jPanel3.setMaximumSize(new java.awt.Dimension(20, 32767));
        jPanel3.setMinimumSize(new java.awt.Dimension(30, 60));
        jPanel3.setPreferredSize(new java.awt.Dimension(30, 60));
        jButton1.setText("00");
        jButton1.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton1.setMaximumSize(new java.awt.Dimension(20, 10));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jPanel3.add(jButton1);

        jButton2.setText("00");
        jButton2.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton2.setMaximumSize(new java.awt.Dimension(20, 10));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jPanel3.add(jButton2);

        jButton3.setText("00");
        jButton3.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton3.setMaximumSize(new java.awt.Dimension(20, 10));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jPanel3.add(jButton3);

        jButton4.setText("00");
        jButton4.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton4.setMaximumSize(new java.awt.Dimension(20, 10));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jPanel3.add(jButton4);

        jButton5.setText("00");
        jButton5.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jButton5.setMaximumSize(new java.awt.Dimension(20, 10));
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jPanel3.add(jButton5);

        jPanel3.add(jPanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(jPanel3, gridBagConstraints);

        menuBar.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
        ActionMenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Action"));
        TaskTrackMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Task_Tracking"));
        TaskTrackMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TaskTrackMIActionPerformed(evt);
            }
        });

        ActionMenu.add(TaskTrackMI);

        ToDoMenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("To_Do"));
        ToDoMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToDoMenuActionPerformed(evt);
            }
        });

        ActionMenu.add(ToDoMenu);

        AddressMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Address_Book"));
        AddressMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddressMIActionPerformed(evt);
            }
        });

        ActionMenu.add(AddressMI);

        SearchMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("srch"));
        SearchMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchMIActionPerformed(evt);
            }
        });

        ActionMenu.add(SearchMI);

        PrintMonthMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("pmonth"));
        PrintMonthMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrintMonthMIActionPerformed(evt);
            }
        });

        ActionMenu.add(PrintMonthMI);

        printprev.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("pprev"));
        printprev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printprevActionPerformed(evt);
            }
        });

        ActionMenu.add(printprev);

        syncMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Synchronize"));
        syncMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                syncMIActionPerformed(evt);
            }
        });

        ActionMenu.add(syncMI);

        exitMenuItem.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Exit"));
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });

        ActionMenu.add(exitMenuItem);

        menuBar.add(ActionMenu);

        OptionMenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Options"));
        jMenuItem1.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("ep"));
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });

        OptionMenu.add(jMenuItem1);

        menuBar.add(OptionMenu);

        navmenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("navmenu"));
        nextmi.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Next_Month"));
        nextmi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NextActionPerformed(evt);
            }
        });

        navmenu.add(nextmi);

        prevmi.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Previous_Month"));
        prevmi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrevActionPerformed(evt);
            }
        });

        navmenu.add(prevmi);

        todaymi.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Today"));
        todaymi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                today(evt);
            }
        });

        navmenu.add(todaymi);

        gotomi.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Goto"));
        gotomi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GotoActionPerformed(evt);
            }
        });

        navmenu.add(gotomi);

        menuBar.add(navmenu);

        catmenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Categories"));
        jMenuItem2.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("choosecat"));
        jMenuItem2.setActionCommand("Choose Displayed Categories");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });

        catmenu.add(jMenuItem2);

        jMenuItem3.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("addcat"));
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });

        catmenu.add(jMenuItem3);

        jMenuItem4.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("remcat"));
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });

        catmenu.add(jMenuItem4);

        menuBar.add(catmenu);

        impexpMenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("impexpMenu"));
        impXML.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("impXML"));
        importMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("impmenu"));
        importMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMIActionPerformed(evt);
            }
        });

        impXML.add(importMI);

        impurl.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("impurl"));
        impurl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                impurlActionPerformed(evt);
            }
        });

        impXML.add(impurl);

        impXMLMem.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("impXMLMem"));
        impXMLMem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                impXMLMemActionPerformed(evt);
            }
        });

        impXML.add(impXMLMem);

        impexpMenu.add(impXML);

        expXML.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("expXML"));
        exportMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("expmenu"));
        exportMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMIActionPerformed(evt);
            }
        });

        expXML.add(exportMI);

        expurl.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("expurl"));
        expurl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expurlActionPerformed(evt);
            }
        });

        expXML.add(expurl);

        expXMLMem.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("expXMLMem"));
        expXMLMem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expXMLMemActionPerformed(evt);
            }
        });

        expXML.add(expXMLMem);

        impexpMenu.add(expXML);

        viewMem.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("viewMem"));
        viewMem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewMemActionPerformed(evt);
            }
        });

        impexpMenu.add(viewMem);

        menuBar.add(impexpMenu);

        helpmenu.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Help"));
        helpMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("Help"));
        helpMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMIActionPerformed(evt);
            }
        });

        helpmenu.add(helpMI);

        licsend.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("License"));
        licsend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                licsendActionPerformed(evt);
            }
        });

        helpmenu.add(licsend);

        readme.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("viewreadme"));
        readme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readmeActionPerformed(evt);
            }
        });

        helpmenu.add(readme);

        chglog.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("viewchglog"));
        chglog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chglogActionPerformed(evt);
            }
        });

        helpmenu.add(chglog);

        AboutMI.setText(java.util.ResourceBundle.getBundle("resource/borg_resource").getString("About"));
        AboutMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutMIActionPerformed(evt);
            }
        });

        helpmenu.add(AboutMI);

        menuBar.add(helpmenu);

        setJMenuBar(menuBar);

    }//GEN-END:initComponents

    private void listFilesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listFilesValueChanged
        bnMemFilesChooserOK.setEnabled(listFiles.getSelectedIndex() != -1);
    }//GEN-LAST:event_listFilesValueChanged

    private void bnMemFilesChooserCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bnMemFilesChooserCancelActionPerformed
    	memFile = null;
        dlgMemoryFilesChooser.setVisible(false);
    }//GEN-LAST:event_bnMemFilesChooserCancelActionPerformed

    private void bnMemFilesChooserOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bnMemFilesChooserOKActionPerformed
		memFile = (String) listFiles.getSelectedValue();
		dlgMemoryFilesChooser.setVisible(false);
    }//GEN-LAST:event_bnMemFilesChooserOKActionPerformed

    private void viewMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewMemActionPerformed
		dlgMemFiles.setMemento(IOHelper.getMemFilesMemento());
		dlgMemFiles.setVisible(true);
    }//GEN-LAST:event_viewMemActionPerformed

    private void impXMLMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_impXMLMemActionPerformed
		try{
			// Argh! This ugliness is because the dialog is not
			// a subclass. Either you can't do this with NetBeans
			// or I haven't figured it out....
			bnMemFilesChooserOK.setEnabled(false);
			String[] files = IOHelper.getMemFilesList();
			DefaultListModel model = (DefaultListModel) listFiles.getModel();
			model.removeAllElements();
			for (int i=0; i<files.length; ++i)
			{
				model.addElement(files[i]);
			}
			dlgMemoryFilesChooser.setVisible(true);
			if (memFile != null)
			{
				impURLCommon(memFile, IOHelper.openStream(memFile));
			}
		}
		catch( Exception e) {
			Errmsg.errmsg(e);
		}
    }//GEN-LAST:event_impXMLMemActionPerformed

    private void expXMLMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expXMLMemActionPerformed
		try {
			expURLCommon("mem:");
			JOptionPane.showMessageDialog(
				this,
				ResourceBundle.getBundle("resource/borg_resource").getString(
					"expXMLMemConfirmation"),
				ResourceBundle.getBundle("resource/borg_resource").getString(
					"expXMLMemTitle"),
				JOptionPane.INFORMATION_MESSAGE);
		 }
		 catch( Exception e) {
			 Errmsg.errmsg(e);
		 }
    }//GEN-LAST:event_expXMLMemActionPerformed

    private void expurlActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_expurlActionPerformed
    {//GEN-HEADEREND:event_expurlActionPerformed
       try {
            String prevurl = Prefs.getPref("lastExpUrl", "");
            String urlst = JOptionPane.showInputDialog(ResourceBundle.getBundle("resource/borg_resource").getString("enturl"), prevurl);
            if( urlst == null || urlst.equals("") ) return;
            Prefs.putPref("lastExpUrl", urlst);
            expURLCommon(urlst);
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_expurlActionPerformed
    
	private void impurlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_impurlActionPerformed
        try{
            String prevurl = Prefs.getPref("lastImpUrl", "");
            String urlst = JOptionPane.showInputDialog(ResourceBundle.getBundle("resource/borg_resource").getString("enturl"), prevurl);
            if( urlst == null || urlst.equals("") ) return;
            
            Prefs.putPref("lastImpUrl", urlst);
            URL url = new URL(urlst);
            impURLCommon(urlst, IOHelper.openStream(url));
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_impurlActionPerformed
    
    private void syncMIActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_syncMIActionPerformed
    {//GEN-HEADEREND:event_syncMIActionPerformed
        try {
            AppointmentModel.getReference().sync();
            AddressModel.getReference().sync();
            TaskModel.getReference().sync();
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_syncMIActionPerformed
    
    private void readmeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_readmeActionPerformed
    {//GEN-HEADEREND:event_readmeActionPerformed
        new HelpScreen("/resource/README.txt").show();
    }//GEN-LAST:event_readmeActionPerformed
    
    private void chglogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_chglogActionPerformed
    {//GEN-HEADEREND:event_chglogActionPerformed
        new HelpScreen("/resource/CHANGES.txt").show();
    }//GEN-LAST:event_chglogActionPerformed
    
    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // add new category
        
        String inputValue = JOptionPane.showInputDialog(Resource.getResourceString("AddCat"));
        if( inputValue == null || inputValue.equals("") ) return;
        try{
            AppointmentModel.getReference().addCategory(inputValue);
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_jMenuItem3ActionPerformed
    
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        try{
            AppointmentModel.getReference().syncCategories();
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_jMenuItem4ActionPerformed
    
    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        CategoryChooser.getReference().show();
    }//GEN-LAST:event_jMenuItem2ActionPerformed
    
    private void AddressMIActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_AddressMIActionPerformed
    {//GEN-HEADEREND:event_AddressMIActionPerformed
        AddrListView ab = AddrListView.getReference();
        ab.refresh();
        ab.show();
    }//GEN-LAST:event_AddressMIActionPerformed
    
    private void impCommon( XTree xt ) throws Exception {
        String type = xt.name();
        if( !type.equals("TASKS" ) && !type.equals("APPTS") && !type.equals("ADDRESSES"))
            throw new Exception(Resource.getResourceString("Could_not_determine_if_the_import_file_was_for_TASKS,_APPTS,_or_ADDRESSES,_check_the_XML") );
        
        int ret = JOptionPane.showConfirmDialog(null, Resource.getResourceString("Importing_") + type + ", OK?", Resource.getResourceString("Import_WARNING"), JOptionPane.OK_CANCEL_OPTION);
        
        if( ret != JOptionPane.OK_OPTION )
            return;
        
        if( type.equals("TASKS" ) ) {
            TaskModel taskmod = TaskModel.getReference();
            taskmod.importXml(xt);
        }
        else if( type.equals("APPTS") ) {
            AppointmentModel calmod = AppointmentModel.getReference();
            calmod.importXml(xt);
        }
        else {
            AddressModel addrmod = AddressModel.getReference();
            addrmod.importXml(xt);
        }
    }
    
	private void impURLCommon(String url, InputStream istr) throws Exception
	{
		XTree xt = XTree.readFromStream(istr);
		if( xt == null )
			throw new Exception( Resource.getResourceString("Could_not_parse_") + url );
		//System.out.println(xt.toString());
		impCommon(xt);
	}
    
    private void expURLCommon(String url) throws Exception
    {
		OutputStream fos = createOutputStreamFromURL(url + "/borg.xml");
		Writer fw = new OutputStreamWriter(fos, "UTF8");            
		AppointmentModel.getReference().export(fw);
		fw.close();
            
		fos = createOutputStreamFromURL(url + "/mrdb.xml");
		fw = new OutputStreamWriter(fos, "UTF8");            
		TaskModel.getReference().export(fw);
		fw.close();
                        
		fos = createOutputStreamFromURL(url + "/addr.xml");
		fw = new OutputStreamWriter(fos, "UTF8");            
		AddressModel.getReference().export(fw);
		fw.close();
    }
    
    private void importMIActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_importMIActionPerformed
    {//GEN-HEADEREND:event_importMIActionPerformed
        try {
            //String msg = Resource.getResourceString("This_will_import_tasks,_addresses_or_appointments_into_an_**EMPTY**_database...continue?");
            //int ret = JOptionPane.showConfirmDialog(null, msg, Resource.getResourceString("Import_WARNING"), JOptionPane.OK_CANCEL_OPTION);
            
            //if( ret != JOptionPane.OK_OPTION )
            //return;
			InputStream istr =
				OSServicesHome
					.getInstance()
					.getServices()
					.fileOpen
					(
						".",
						Resource
							.getResourceString("Please_choose_File_to_Import_From")
					);
					
			if (istr == null)
				return;
            
            // parse xml file
            XTree xt = XTree.readFromStream( istr );
            istr.close();
            if( xt == null )
            	throw new Exception( Resource.getResourceString("Could_not_parse_") + "XML");
            
            impCommon(xt);
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_importMIActionPerformed
    
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton5ActionPerformed
    {//GEN-HEADEREND:event_jButton5ActionPerformed
        new WeekView( month_, year_, 29);
    }//GEN-LAST:event_jButton5ActionPerformed
    
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
    {//GEN-HEADEREND:event_jButton4ActionPerformed
        new WeekView( month_, year_, 22);
    }//GEN-LAST:event_jButton4ActionPerformed
    
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
    {//GEN-HEADEREND:event_jButton3ActionPerformed
        new WeekView( month_, year_, 15);
    }//GEN-LAST:event_jButton3ActionPerformed
    
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        new WeekView( month_, year_, 8);
    }//GEN-LAST:event_jButton2ActionPerformed
    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        new WeekView( month_, year_, 1);
    }//GEN-LAST:event_jButton1ActionPerformed
    
    private void printprevActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_printprevActionPerformed
    {//GEN-HEADEREND:event_printprevActionPerformed
        new MonthPreView( month_, year_);
    }//GEN-LAST:event_printprevActionPerformed
    
    private void exportMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMIActionPerformed
        
        // user wants to export the task and calendar DBs to an XML file
        File dir;
        while( true ) {
            // prompt for a directory to store the files
            JFileChooser chooser = new JFileChooser();
            
            chooser.setCurrentDirectory( new File(".") );
            chooser.setDialogTitle(Resource.getResourceString("Please_choose_directory_to_place_XML_files"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal != JFileChooser.APPROVE_OPTION)
                return;
            
            String s = chooser.getSelectedFile().getAbsolutePath();
            dir = new File(s);
            String err = null;
            if( !dir.exists() ) {
                err = Resource.getResourceString("Directory_[") + s + Resource.getResourceString("]_does_not_exist");
            }
            else if( !dir.isDirectory() ) {
                err = "[" + s + Resource.getResourceString("]_is_not_a_directory");
            }
            else if( !dir.canWrite() ) {
                err = Resource.getResourceString("Directory_[") + s + Resource.getResourceString("]_is_not_writable");
            }
            
            if( err == null )
                break;
            
            Errmsg.notice( err );
        }
        
        try {
            String fname = dir.getAbsolutePath() + "/borg.xml";
            OutputStream ostr = IOHelper.createOutputStream(fname);
            Writer fw = new OutputStreamWriter(ostr, "UTF8");            
            AppointmentModel.getReference().export(fw);
            fw.close();
            
            fname = dir.getAbsolutePath() + "/mrdb.xml";
			ostr = IOHelper.createOutputStream(fname);
			fw = new OutputStreamWriter(ostr, "UTF8");            
            TaskModel.getReference().export(fw);
            fw.close();
            
            fname = dir.getAbsolutePath() + "/addr.xml";
			ostr = IOHelper.createOutputStream(fname);
			fw = new OutputStreamWriter(ostr, "UTF8");            
            AddressModel.getReference().export(fw);
            fw.close();
            
        }
        catch( Exception e) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_exportMIActionPerformed
    
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // bring up the options window
        new OptionsView(this, rl_).show();
    }//GEN-LAST:event_jMenuItem1ActionPerformed
    
    private void licsendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_licsendActionPerformed
        // show the open source license
        new HelpScreen("/resource/license.htm").show();
    }//GEN-LAST:event_licsendActionPerformed
    
    private void helpMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMIActionPerformed
        // show the help page
        new HelpScreen("/resource/help.htm").show();
    }//GEN-LAST:event_helpMIActionPerformed
    
    
    private void TaskTrackMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TaskTrackMIActionPerformed
        TaskListView bt_ = TaskListView.getReference( );
        bt_.refresh();
        bt_.show();
    }//GEN-LAST:event_TaskTrackMIActionPerformed
    
    private void AboutMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutMIActionPerformed
        
        // show the About data
        String build_info = "";
        String version = "";
        try {
            // get the version and build info from a properties file in the jar file
            InputStream is = getClass().getResource("/properties").openStream();
            Properties props = new Properties();
            props.load(is);
            is.close();
            version = props.getProperty("borg.version");
            build_info = Resource.getResourceString("Build_Number:_") + props.getProperty("build.number") + Resource.getResourceString("Build_Time:_") + props.getProperty("build.time") + "\n";
            
            
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
        
        // build and show the version info.
        String info = Resource.getResourceString("Berger-Organizer_v") + version + Resource.getResourceString("contrib") +
        build_info;
        Object opts[] =
        {Resource.getResourceString("Dismiss"), Resource.getResourceString("Show_Detailed_Source_Version_Info") };
        int n = JOptionPane.showOptionDialog(null, info, Resource.getResourceString("About_BORG"), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("/resource/borg.jpg")), opts, opts[0]);
        if( n == JOptionPane.NO_OPTION ) {
            info = Resource.getResourceString("Versions_of_--Loaded--_Classes") + Version.getVersion();
            JOptionPane.showMessageDialog(null, info, Resource.getResourceString("BORG_Source_File_Details"), JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass().getResource("/resource/borg.jpg")));
        }
        
    }//GEN-LAST:event_AboutMIActionPerformed
    
    private void SearchMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchMIActionPerformed
        // user wants to do a search, so prompt for search string and request search results
        String inputValue = JOptionPane.showInputDialog(Resource.getResourceString("Enter_search_string:"));
        if( inputValue == null || inputValue.equals("") ) return;
        // bring up srch window
        SearchView sg = new SearchView(inputValue );
        sg.show();
        
    }//GEN-LAST:event_SearchMIActionPerformed
    
    private void PrintMonthMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrintMonthMIActionPerformed
        
        // print the current month
        try {
            MonthPreView.printMonth( month_, year_);
        }
        catch( Exception e )
        { Errmsg.errmsg(e); }
        
    }//GEN-LAST:event_PrintMonthMIActionPerformed
    
    
    
    private void ToDoMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToDoMenuActionPerformed
        // ask borg class to bring up the todo window
        try {
            TodoView.getReference().show();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_ToDoMenuActionPerformed
    
    private void GotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GotoActionPerformed
        
        // GOTO a particular month
        
        // prompt for month/year
        String in = JOptionPane.showInputDialog(Resource.getResourceString("Enter_a_Date_(mm/yyyy)"));
        if( in == null ) return;
        
        int index = in.indexOf('/');
        if( index == -1 ) return;
        
        // parse out MM/YYYY
        String mo = in.substring(0,index);
        String yr = in.substring(index+1);
        try {
            // just set the member month_ and year_ vars and call refresh to update the view
            month_ = Integer.parseInt(mo)-1;
            year_ = Integer.parseInt(yr);
            refresh();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
        
        
    }//GEN-LAST:event_GotoActionPerformed
    
    
    private void today(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_today
        try {
            // set view back to month containing today
            today();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_today
    
    private void PrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrevActionPerformed
        // go to previous month - decrement month/year and call refresh of view
        
        if( month_ == 0 ) {
            month_ = 11;
            year_--;
        }
        else {
            month_--;
        }
        try {
            refresh();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }// Add your handling code here:
    }//GEN-LAST:event_PrevActionPerformed
    
    private void NextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NextActionPerformed
        // go to next month - increment month/year and call refresh of view
        if( month_ == 11 ) {
            month_ = 0;
            year_++;
        }
        else {
            month_++;
        }
        try {
            refresh();
        }
        catch( Exception e ) {
            Errmsg.errmsg(e);
        }
    }//GEN-LAST:event_NextActionPerformed
    
    private void exitMenuItemActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        if( trayIcon_ ) {
            WindowsTrayIcon.cleanUp();
        }
        if(  AppHelper.isApplet() ) {
            this.dispose();
        }
        else {
			exit();
        }
        
    }//GEN-LAST:event_exitMenuItemActionPerformed
    
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        if( trayIcon_ || AppHelper.isApplet() ) {
            this.dispose();
        }
        else {
			exit();
        }
        
    }//GEN-LAST:event_exitForm
    
    
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutMI;
    private javax.swing.JMenu ActionMenu;
    private javax.swing.JMenuItem AddressMI;
    private javax.swing.JButton Goto;
    private javax.swing.JLabel MonthLabel;
    private javax.swing.JButton Next;
    private javax.swing.JMenu OptionMenu;
    private javax.swing.JButton Prev;
    private javax.swing.JMenuItem PrintMonthMI;
    private javax.swing.JMenuItem SearchMI;
    private javax.swing.JMenuItem TaskTrackMI;
    private javax.swing.JMenuItem ToDoMenu;
    private javax.swing.JButton Today;
    private javax.swing.JButton bnMemFilesChooserCancel;
    private javax.swing.JButton bnMemFilesChooserOK;
    private javax.swing.JMenu catmenu;
    private javax.swing.JMenuItem chglog;
    private javax.swing.JDialog dlgMemoryFilesChooser;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu expXML;
    private javax.swing.JMenuItem expXMLMem;
    private javax.swing.JMenuItem exportMI;
    private javax.swing.JMenuItem expurl;
    private javax.swing.JMenuItem gotomi;
    private javax.swing.JMenuItem helpMI;
    private javax.swing.JMenu helpmenu;
    private javax.swing.JMenu impXML;
    private javax.swing.JMenuItem impXMLMem;
    private javax.swing.JMenu impexpMenu;
    private javax.swing.JMenuItem importMI;
    private javax.swing.JMenuItem impurl;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel lblMemChooser;
    private javax.swing.JMenuItem licsend;
    private javax.swing.JList listFiles;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu navmenu;
    private javax.swing.JMenuItem nextmi;
    private javax.swing.JPanel pnlButtonsMemFilesChooser;
    private javax.swing.JPanel pnlMainMemFilesChooser;
    private javax.swing.JMenuItem prevmi;
    private javax.swing.JMenuItem printprev;
    private javax.swing.JMenuItem readme;
    private javax.swing.JMenuItem syncMI;
    private javax.swing.JMenuItem todaymi;
    private javax.swing.JMenuItem viewMem;
    // End of variables declaration//GEN-END:variables
    
    
    /** array of day panels
     */
    private JPanel days[];
    
    
    private JTextPane daytext[];
    /** date buttons
     */
    private JButton daynum[];
    
    private JTextPane todoPreview;
    private JTextPane taskPreview;
    
}
