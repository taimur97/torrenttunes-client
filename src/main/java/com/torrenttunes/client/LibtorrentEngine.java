package com.torrenttunes.client;

import static com.torrenttunes.client.db.Tables.LIBRARY;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SessionSettings;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.BlockDownloadingAlert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.StateChangedAlert;
import com.frostwire.jlibtorrent.alerts.StatsAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.swig.session_settings;
import com.torrenttunes.client.ScanDirectory.ScanInfo;
import com.torrenttunes.client.ScanDirectory.ScanStatus;
import com.torrenttunes.client.db.Tables.Library;


public enum LibtorrentEngine  {

	INSTANCE;



	private final Logger log = LoggerFactory.getLogger(LibtorrentEngine.class);


	private Session session;
	private Set<ScanInfo> scanInfos;

	private LibtorrentEngine() {
		log.info("Starting up libtorrent with version: " + LibTorrent.version());

		session = new Session();
		SessionSettings s = SessionSettings.newDefaults();
		s.setTorrentConnectBoost(5);
		s.setMinReconnectTime(1);
		s.setActiveDownloads(10);
//		s.setActiveSeeds(1);
		
	
//        s.setActiveSeeds(9999);
        
		log.info("active DL limit: " + s.getActiveLimit() +
				"\nactive seed limit: " + s.getActiveSeeds());
//		log.info("active seed limit: " + String.valueOf(session.getSettings().get));
//		sessionSettings.setActiveSeeds(0);
		
		session.setSettings(s);
		
		
		this.scanInfos = new LinkedHashSet<ScanInfo>();



	}


	public void startSeedingLibrary() {

		Tools.dbInit();
		List<Library> library = LIBRARY.findAll();
		library.isEmpty();
		Tools.dbClose();

		// start sharing them
		for (Library track : library) {
			String torrentPath = track.getString("torrent_path");
			String filePath = track.getString("file_path");

			File outputParent = new File(filePath).getParentFile();

			addTorrent(outputParent, new File(torrentPath));

			// Set up the scanInfo
			ScanInfo si = ScanInfo.create(new File(filePath));
			si.setStatus(ScanStatus.Seeding);
			si.setMbid(track.getString("mbid"));
			scanInfos.add(si);
		}

		log.info("Done seeding library, total of " + session.getTorrents().size() + " torrents shared");

	}

	public TorrentHandle addTorrent(File outputParent, File torrentFile) {
		TorrentHandle torrent = session.addTorrent(torrentFile, outputParent);
		log.info("added torrent " + torrent.getName());

		shareTorrent(torrent);

		return torrent;



	}


	private void shareTorrent(TorrentHandle torrent) {



		// Add the listeners
		session.addListener(new TorrentAlertAdapter(torrent) {

			//			@Override
			//			public void stats(StatsAlert alert) {
			//				TorrentStats ts = TorrentStats.create(torrent);
			//				log.info(ts.toString());
			//
			//				super.stats(alert);
			//			}
			
			@Override
			public void stateChanged(StateChangedAlert alert) {
				TorrentStats ts = TorrentStats.create(torrent);
				log.info(ts.toString());
				super.stateChanged(alert);
			}
			
			@Override
			public void blockFinished(BlockFinishedAlert alert) {
//				TorrentStats ts = TorrentStats.create(torrent);
//				log.info(ts.toString());
				super.blockFinished(alert);
			}
			@Override
			public void torrentFinished(TorrentFinishedAlert alert) {
				TorrentStats ts = TorrentStats.create(torrent);
				log.info(ts.toString());
				super.torrentFinished(alert);
			}

			

		});


		
//		torrent.readPiece(0);

		
		torrent.resume();
		
//		torrent.setPieceDeadline(0, 5);
//		torrent.forceReannounce();
//		torrent.forceDHTAnnounce();
//		torrent.queuePositionTop();
//		torrent.forceRecheck();
//		torrent.scrapeTracker();
		
		
		

		
	}




	public Set<ScanInfo> getScanInfos() {
		return scanInfos;
	}


	public Session getSession() {
		return session;
	}




}