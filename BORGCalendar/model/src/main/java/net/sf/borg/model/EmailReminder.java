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

 Copyright 2003 by Mike Berger
 */
/*
 * popups.java
 *
 * Created on January 16, 2004, 3:08 PM
 */

package net.sf.borg.model;

import net.sf.borg.common.PrefName;
import net.sf.borg.common.Prefs;
import net.sf.borg.common.Resource;
import net.sf.borg.common.SendJavaMail;
import net.sf.borg.model.entity.Appointment;
import net.sf.borg.model.entity.CalendarEntity;
import net.sf.borg.model.entity.Task;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * this class handles the daily email reminder
 */

public class EmailReminder {
	
	static private final Logger log = Logger.getLogger("net.sf.borg");

	/**
	 * Checks if entity should be shown as strike-through on a certain date.
	 * 
	 * @param appt the entity
	 * @param date the date
	 * 
	 * @return true, if is strike
	 */
	private static boolean isStrike(CalendarEntity appt, Date date) {
        return (appt.getColor() != null && appt.getColor().equals("strike"))
                || (appt.isTodo() && !(appt.getNextTodo() == null || !appt
                .getNextTodo().after(date)));
    }

	static public void sendDailyEmailReminder(Calendar emailday)
			throws Exception {
		sendDailyEmailReminder(emailday, false);
	}

	/**
	 * Send daily email reminder.
	 * 
	 * @param emailday
	 *            the emailday
	 *            
	 * @param forceResend
	 *    		resend the daily email, even if it was already sent
	 * 
	 * @throws Exception
	 *             the exception
	 */
	static public void sendDailyEmailReminder(Calendar emailday, boolean forceResend)
			throws Exception {

		// check if the email feature has been enabled
		String email = Prefs.getPref(PrefName.EMAILENABLED);
		if (email.equals("false"))
			return;

		// get the SMTP host and address
		String host = Prefs.getPref(PrefName.EMAILSERVER);
		String addr = Prefs.getPref(PrefName.EMAILADDR);
		String from = Prefs.getPref(PrefName.EMAILFROM);

		if (host.equals("") || addr.equals(""))
			return;

		Calendar cal = new GregorianCalendar();

		// if no date passed in, the timer has gone off and we need to check if
		// we
		// can send
		// email now
		int doy = -1;
		if (emailday == null) {
			// get the last day that email was sent
			int lastday = Prefs.getIntPref(PrefName.EMAILLAST);

			// if email was already sent today - don't send again
			doy = cal.get(Calendar.DAY_OF_YEAR);
			if (doy == lastday && forceResend == false)
				return;

			// create the calendar model key for tomorrow
			cal.add(Calendar.DATE, 1);
		} else {
			// just send email for the requested day
			cal = emailday;
		}

		// tx is the contents of the email
		String ap_tx = "Appointments for "
				+ DateFormat.getDateInstance().format(cal.getTime()) + ":\n";
		StringBuffer tx = new StringBuffer();

		// get the list of appts for the requested day
		Collection<Integer> l = AppointmentModel.getReference().getAppts(
				cal.getTime());
		if (l != null) {

			Appointment appt;

			// iterate through the day's appts
			for (Integer ik : l) {

				try {
					// read the appointment from the calendar model
					appt = AppointmentModel.getReference().getAppt(
							ik.intValue());

					// get the appt flags to see if the appointment is private
					// if so, don't include it in the email
					if (appt.isPrivate())
						continue;
					
					// skip strike through items
					if( isStrike(appt, cal.getTime()))
						continue;				

					if (!AppointmentModel.isNote(appt)) {
						// add the appointment time to the email if it is not a
						// note
						Date d = appt.getDate();
						SimpleDateFormat df = AppointmentModel.getTimeFormat();
						tx.append(df.format(d) + " ");
					}

					// add the appointment text
					if (appt.isEncrypted())
						tx.append(Resource.getResourceString("EncryptedItemShort"));
					else {
						// only show first line of appointment text
						String s = appt.getText();
						int ii = s.indexOf('\n');
						if (ii != -1) {
							tx.append(s, 0, ii);
						} else {
							tx.append(s);
						}
					}
					tx.append("\n");
				} catch (Exception e) {
					log.severe(e.toString());
					return;
				}
			}

		}

		// load any task tracker items for the email
		Collection<Task> tasks = TaskModel.getReference().get_tasks(
				cal.getTime());
		if (l != null && tasks != null) {

			for (Task task : tasks) {
				// add each task to the email - and remove newlines
				tx.append("Task[" + task.getKey() + "] ");
				tx.append(task.getSummary());
				tx.append("\n");
			}
		}
		
		// add any outstanding todos
		Collection<Appointment> todos = AppointmentModel.getReference().get_todos();
		StringBuffer tdbuf = new StringBuffer();
		for( Appointment todo : todos ) {
			Date nt = todo.getNextTodo();
			if (nt == null) {
				nt = todo.getDate();
			}
			
			Calendar tdcal = new GregorianCalendar();
			tdcal.setTime(nt);
			tdcal.set(Calendar.SECOND, 59);
			tdcal.set(Calendar.MINUTE, 59);
			tdcal.set(Calendar.HOUR_OF_DAY, 23);
			
			if( tdcal.before(cal))
			{
				if (!AppointmentModel.isNote(todo)) {
					// add the appointment time to the email if it is not a
					// note
					Date d = todo.getDate();
					SimpleDateFormat df = AppointmentModel.getTimeFormat();
					tdbuf.append(df.format(d) + " ");
				}

				// add the appointment text
				if (todo.isEncrypted())
					tdbuf.append(Resource.getResourceString("EncryptedItemShort"));
				else {
					// only show first line of appointment text
					String s = todo.getText();
					int ii = s.indexOf('\n');
					if (ii != -1) {
						tdbuf.append(s, 0, ii);
					} else {
						tdbuf.append(s);
					}
				}
				tdbuf.append("\n");
			}
			
		
		}
		
		if( !tdbuf.toString().equals(""))
		{
			tx.append("\n\n");
			tx.append(Resource.getResourceString("OverDue"));
			tx.append("\n" + tdbuf);
		}

		// send the email using SMTP
		if (!tx.toString().equals("")) {
			String stx = ap_tx + tx;
			StringTokenizer stk = new StringTokenizer(addr, ",;");
			while (stk.hasMoreTokens()) {
				String a = stk.nextToken();
				String f;
				if( from == null || from.isEmpty())
					f = a.trim();
				else 
					f= from;
				if (!a.equals("")) {
					
					SendJavaMail.sendMail(host, stx,
							Resource.getResourceString("Reminder_Notice"),
							f, a.trim(),
							Prefs.getPref(PrefName.EMAILUSER), gep());
				}
			}
		}
		else
		{
			log.info("Skipping email");
		}
		// record that we sent email today
		if (doy != -1)
			Prefs.putPref(PrefName.EMAILLAST, Integer.valueOf(doy));

		return;
	}

	// intentionally undocumented - not foolproof. unrelated to memo and appt
	// encryption, which is fully secure
	public static String gep() throws Exception {
		String p1 = Prefs.getPref(PrefName.EMAILPASS2);
		String p2 = Prefs.getPref(PrefName.EMAILPASS);
		if ("".equals(p2))
			return p2;

		if ("".equals(p1)) {
			sep(p2); // transition case
			return p2;
		}

		byte[] ba = Base64.getDecoder().decode(p1);
		SecretKey key = new SecretKeySpec(ba, "AES");
		Cipher dec = Cipher.getInstance("AES");
		dec.init(Cipher.DECRYPT_MODE, key);
		byte[] decba = Base64.getDecoder().decode(p2);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStream os = new CipherOutputStream(baos, dec);
		os.write(decba);
		os.close();

		return baos.toString();

	}

	// intentionally undocumented - not foolproof. unrelated to memo and appt
	// encryption, which is
	// fully secure
	public static void sep(String s) throws Exception {
		if ("".equals(s)) {
			Prefs.putPref(PrefName.EMAILPASS, s);
			return;
		}
		String p1 = Prefs.getPref(PrefName.EMAILPASS2);
		if ("".equals(p1)) {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			SecretKey key = keyGen.generateKey();
			p1 = new String(Base64.getEncoder().encode(key.getEncoded()));
			Prefs.putPref(PrefName.EMAILPASS2, p1);
		}

		byte[] ba = Base64.getDecoder().decode(p1);
		SecretKey key = new SecretKeySpec(ba, "AES");
		Cipher enc = Cipher.getInstance("AES");
		enc.init(Cipher.ENCRYPT_MODE, key);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStream os = new CipherOutputStream(baos, enc);
		os.write(s.getBytes());
		os.close();
		ba = baos.toByteArray();
		Prefs.putPref(PrefName.EMAILPASS, new String(Base64.getEncoder().encode(ba)));
	}

}
