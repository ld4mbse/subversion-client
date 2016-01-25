package clients;
import java.io.File;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepository;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import handlers.UnversionedFileDeleterStatusHandler;

public class StatusClient {

	public static <V> void main(String[] args) {
		try {

			// http://svnkit.com/kb/javadoc/org/tmatesoft/svn/core/io/SVNRepository.html

			// Set up connection protocols support:
			// http:// and https://
			DAVRepositoryFactory.setup();
			// svn://, svn+xxx:// (svn+ssh:// in particular)
			SVNRepositoryFactoryImpl.setup();
			// file:///
			FSRepositoryFactory.setup();

			// creating a new SVNRepository instance
			String repositoryurl = "https://koneksys1:18080/svn/myrepo";
			SVNURL repositorySVNurl = SVNURL.parseURIDecoded(repositoryurl);
			SVNRepository repository = DAVRepositoryFactory
					.create(repositorySVNurl);

			ISVNAuthenticationManager authManager = SVNWCUtil
					.createDefaultAuthenticationManager("admin", "admin");
			repository.setAuthenticationManager(authManager);

			repository.testConnection();

			// need to identify latest revision
			long latestRevision = repository.getLatestRevision();
			System.out.println("Repository Latest Revision: " + latestRevision);

			DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);
			SVNClientManager clientManager = SVNClientManager.newInstance(
					myOptions, "name", "passw");

			
			

			// SVNStatus class is used to provide detailed status information
			// for a Working Copy item
			SVNStatusClient sVNStatusClient = clientManager.getStatusClient();
			SVNStatus sVNStatus;
			String workingCopyPath = "C:\\Users\\Axel\\Desktop\\myhttpswk\\myrepo";
			File workingCopyDirectory = new File(workingCopyPath);
			
			// do svn status
			sVNStatusClient.doStatus(workingCopyDirectory, true, true, true, true, new UnversionedFileDeleterStatusHandler(true));
			
			
			

		} catch (SVNException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("ok");

	}

}
