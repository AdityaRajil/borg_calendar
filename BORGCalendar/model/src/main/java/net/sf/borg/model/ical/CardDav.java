package net.sf.borg.model.ical;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.validate.ValidationException;
import net.sf.borg.common.*;
import net.sf.borg.model.AppointmentModel;
import net.sf.borg.model.entity.Appointment;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;

import net.fortuna.ical4j.connector.dav.CardDavCollection;
import net.fortuna.ical4j.connector.dav.CardDavStore;
import net.fortuna.ical4j.connector.dav.PathResolver;
import net.fortuna.ical4j.connector.dav.PathResolver.GenericPathResolver;
import net.fortuna.ical4j.util.CompatibilityHints;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.sf.borg.model.AddressModel;
import net.sf.borg.model.entity.Address;

public class CardDav {
	
	static private final Logger log = Logger.getLogger("net.sf.borg");

	static private void setHints() {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
	}
	
	private static PathResolver createPathResolver() {
		GenericPathResolver pathResolver = new GenericPathResolver();
		String basePath = Prefs.getPref(PrefName.CALDAV_PATH);
		if (!basePath.endsWith("/"))
			basePath += "/";
		pathResolver.setPrincipalPath(basePath + Prefs.getPref(PrefName.CALDAV_PRINCIPAL_PATH));
		pathResolver.setUserPath(basePath + Prefs.getPref(PrefName.CARDDAV_USER_PATH));
		return pathResolver;
	}
	
	public static void exportToFile(String filename) throws Exception {
		
		OutputStream oostr = IOHelper.createOutputStream(filename);
		OutputStreamWriter wr = new OutputStreamWriter(oostr);
		
		for( Address addr : AddressModel.getReference().getAddresses()) {
			wr.write(AddressVcardAdapter.toVcard(addr).toString());
		}
		
		oostr.close();
	}
	
	static public List<VCard> importVcardFromFile(String file) throws Exception {
		
		setHints();
		
		InputStream is = new FileInputStream(file);
		return importVcardFromInputStream(is);
	}

	static public List<VCard> importVcardFromInputStream(InputStream is) throws Exception {

		setHints();

		VCardBuilder builder = new VCardBuilder(is);
		List<VCard> l = builder.buildAll();
		is.close();

		return l;
	}

	static public String importVCard(List<VCard> vcards) throws Exception {

		int skipped = 0;
		StringBuffer dups = new StringBuffer();

		setHints();

		StringBuffer warning = new StringBuffer();

		ArrayList<Address> addrs = new ArrayList<Address>();

		AddressModel amodel = AddressModel.getReference();
		for( VCard vc : vcards){

			Address addr = AddressVcardAdapter.fromVcard(vc);
			if (addr != null)
				addrs.add(addr);

		}

		int imported = 0;
		int dup_count = 0;

		for (Address addr : addrs) {
			// check for dups - TODO

			imported++;
			try {
				amodel.saveAddress(addr);
			}
			catch(Warning w){
				Errmsg.getErrorHandler().notice(w.getMessage() + "\n\n" + addr.toString());
			}
		}

		warning.append("Imported: " + imported + "\n");
		warning.append("Skipped: " + (vcards.size() - imported) + "\n");
		//warning.append("Duplicates: " + dup_count + "\n");
		warning.append(dups.toString());

		if (warning.length() == 0)
			return (null);

		return (warning.toString());

	}

	@SuppressWarnings("deprecation")
	public static CardDavStore connect() throws Exception {

		if (!CalDav.isSyncing())
			return null;


		if (Prefs.getBoolPref(PrefName.CALDAV_ALLOW_SELF_SIGNED_CERT)) {
			// Allow access even though certificate is self signed
			Protocol lEasyHttps = new Protocol("https", new EasySslProtocolSocketFactory(), 443);
			Protocol.registerProtocol("https", lEasyHttps);
		} else {
			Protocol sslprot = new Protocol("https", new SSLProtocolSocketFactory(), 443);
			Protocol.registerProtocol("https", sslprot);
		}

		String protocol = Prefs.getBoolPref(PrefName.CALDAV_USE_SSL) ? "https" : "http";

		String server = Prefs.getPref(PrefName.CALDAV_SERVER);
		String serverPart[] = server.split(":");
		int port = -1;
		if (serverPart.length == 2) {
			try {
				port = Integer.parseInt(serverPart[1]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		URL url = new URL(protocol, serverPart[0], port, Prefs.getPref(PrefName.CALDAV_PATH));
		SocketClient.sendLogMessage("SYNC: connect to " + url.toString());
		log.info("SYNC: connect to " + url.toString());

		CardDavStore store = new CardDavStore("-", url, createPathResolver());

		if (store.connect(Prefs.getPref(PrefName.CALDAV_USER), CalDav.gep().toCharArray()))
			return store;

		return null;
	}

	public static CardDavCollection getCollection(CardDavStore store, String name) throws Exception {
		String id = createPathResolver().getUserPath(Prefs.getPref(PrefName.CALDAV_USER)) + "/" + name;
		return store.getCollection(id);
	}
}
