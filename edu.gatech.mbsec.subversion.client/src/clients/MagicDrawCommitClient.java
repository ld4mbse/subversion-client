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
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnScheduleForRemoval;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class MagicDrawCommitClient {

	public static <V> void main(String[] args) {
		try {

//			String workingCopyPath = "C:\\Users\\Axel\\Desktop\\myhttpswk\\myrepo3";
			String workingCopyPath = "C:\\Users\\Axel\\Desktop\\mdwk";
//			String repositoryurl = "https://Axel-PC/svn/magicdrawrepository";
			String repositoryurl = "https://koneksys1:18080/svn/magicdrawrepository/";
			
			
			// http://svnkit.com/kb/javadoc/org/tmatesoft/svn/core/io/SVNRepository.html

			// Set up connection protocols support:
			// http:// and https://
			DAVRepositoryFactory.setup();
			// svn://, svn+xxx:// (svn+ssh:// in particular)
			SVNRepositoryFactoryImpl.setup();
			// file:///
			FSRepositoryFactory.setup();

			// creating a new SVNRepository instance
			
			SVNURL repositorySVNurl = SVNURL.parseURIDecoded(repositoryurl);
			SVNRepository repository = DAVRepositoryFactory
					.create(repositorySVNurl);

			ISVNAuthenticationManager authManager = SVNWCUtil
					.createDefaultAuthenticationManager("axel", "axel");
			repository.setAuthenticationManager(authManager);

			repository.testConnection();

			// need to identify latest revision
			long latestRevision = repository.getLatestRevision();
			System.out.println("Repository Latest Revision: " + latestRevision);

			DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);
			SVNClientManager clientManager = SVNClientManager.newInstance(
					myOptions, "axel", "axel");

			// do svn status

			// SVNStatus class is used to provide detailed status information
			// for a Working Copy item
			SVNStatusClient sVNStatusClient = clientManager.getStatusClient();
			SVNStatus sVNStatus;
			
			
			File workingCopyDirectory = new File(workingCopyPath);
			
			// commit entire working copy
			System.out.println("commit " + workingCopyPath + " to " + repositoryurl );
			
			final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
			
//			SvnScheduleForAddition svnScheduleForAddition = svnOperationFactory.createScheduleForAddition();
//			svnScheduleForAddition.setSingleTarget(SvnTarget.fromFile(new File(workingCopyDirectory + "/Water_Supply_Example_2.mdzip")));
//			svnScheduleForAddition.run();
//			
//			SvnScheduleForRemoval svnScheduleForRemoval = svnOperationFactory.createScheduleForRemoval();
//			svnScheduleForRemoval.setSingleTarget(SvnTarget.fromFile(new File(workingCopyDirectory + "/TestProject2.mdzip")));
//			svnScheduleForRemoval.run();
			
//			SvnScheduleForAddition svnScheduleForAddition = svnOperationFactory.createScheduleForAddition();
//			svnScheduleForAddition.setSingleTarget(SvnTarget.fromFile(new File(workingCopyDirectory + "/TestProject2.mdzip")));
//			svnScheduleForAddition.run();
//			
//			SvnScheduleForRemoval svnScheduleForRemoval = svnOperationFactory.createScheduleForRemoval();
//			svnScheduleForRemoval.setSingleTarget(SvnTarget.fromFile(new File(workingCopyDirectory + "/Water_Supply_Example_2.mdzip")));
//			svnScheduleForRemoval.run();
			
			final SvnCommit commit = svnOperationFactory
					.createCommit();
			commit.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
		    commit.setCommitMessage("Commit message");
		    
		    final SVNCommitInfo commitInfo = commit.run();
		    
		    
			System.out.println("new revision " + commitInfo.getNewRevision() );
			
			
			
			
			

			// if(sVNStatus.getContentsStatus() == SVNStatusType.){
			//
			// }

			// if reply is that there is no change, just do nothing

			// if reply is "... not a working copy"
			// perform checkout

			// if reply is that repository has changed, perform update

			// if reply is that local working copy has changed, perform revert
			// to HEAD (latest revision of repository)
			// svn update –r HEAD x ??
			// or svn switch HEAD-url ??

			// if reply is that local working copy and repository has changed,
			// perform revert to HEAD (latest revision of repository)

		} catch (SVNException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("ok");

	}

}
