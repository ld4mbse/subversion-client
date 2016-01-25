package clients;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
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
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevert;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

import handlers.UnversionedFileDeleterStatusHandler;

public class CheckoutAndUpdateClientFromLocalRepo {

	public static <V> void main(String[] args) {
		try {

//			String repositoryurl = "https://koneksys1:18080/svn/repository3/";
			String repositoryurl = "https://koneksys1:18080/svn/magicdrawrepository/";
			String workingCopyPath = "C:\\Users\\Axel\\Desktop\\mdwk";
			
			// http://svnkit.com/kb/javadoc/org/tmatesoft/svn/core/io/SVNRepository.html

			// Set up connection protocols support:
			// http:// and https://
			DAVRepositoryFactory.setup();
			// svn://, svn+xxx:// (svn+ssh:// in particular)
			SVNRepositoryFactoryImpl.setup();
			// file:///
			FSRepositoryFactory.setup();

			// creating a new SVNRepository instance
			
//			String repositoryurl = "file:///C:/csvn/data/repositories/myrepo";
			
			SVNURL repositorySVNurl = SVNURL
			.parseURIDecoded(repositoryurl);
			SVNRepository repository = DAVRepositoryFactory.create(repositorySVNurl);
//			SVNRepository repository = FSRepositoryFactory.create(repositorySVNurl);
			
//			JFrame frame = new JFrame();
//			JPanel pwpanel = new JPanel();
//			JPanel userpanel = new JPanel();
//			JLabel pwlabel = new JLabel("SVN password:");
//			JLabel userlabel = new JLabel("SVN username:");
//			JPasswordField pass = new JPasswordField(10);
//			JPasswordField user = new JPasswordField(10);
//			userpanel.add(userlabel);
//			userpanel.add(user);
//			pwpanel.add(pwlabel);
//			pwpanel.add(pass);
//			frame.add(userpanel);
//			frame.add(pwpanel);
//			frame.setVisible(true);
//			String[] options = new String[]{"OK", "Cancel"};
//			int option = JOptionPane.showOptionDialog(frame, null, "The title",
//			                         JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
//			                         null, options, options[1]);
//			if(option == 0) // pressing OK button
//			{
//				char[] userChar = user.getPassword();
//				System.out.println("username is: " + new String(userChar));
//				char[] password = pass.getPassword();
//			    System.out.println("password is: " + new String(password));
//			}
			
			ISVNAuthenticationManager authManager = SVNWCUtil
					.createDefaultAuthenticationManager("axel", "axel");
			repository.setAuthenticationManager(authManager);
			
			repository.testConnection();
			
			//need to identify latest revision
            long latestRevision = repository.getLatestRevision();
            System.out.println(  "Repository Latest Revision: " + latestRevision);
            
            
			DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);	    
		     SVNClientManager clientManager = SVNClientManager.newInstance(myOptions, "axel", "axel");
		     
		     
		     // do svn status
		     
		     // SVNStatus class is used to provide detailed status information for a Working Copy item
		     SVNStatusClient sVNStatusClient = clientManager.getStatusClient();
		     SVNStatus sVNStatus;
		     
		     File workingCopyDirectory = new File(workingCopyPath);
			try {
				sVNStatus = sVNStatusClient.doStatus(workingCopyDirectory, true);
				System.out.println(sVNStatus.getContentsStatus());
				
				if(!sVNStatus.getRemoteRevision().toString().equals(sVNStatus.getCommittedRevision().toString())){
					// get latest revision of repository
					// overwrite any local changes
					// perform checkout with new API
					final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
					try {
						
						// first delete all files in working copy which have been added and that are not under version control
						sVNStatusClient.doStatus(workingCopyDirectory, true, true, true, true, new UnversionedFileDeleterStatusHandler(true));
						
						// do a revert to undo all local changes
						// this operation does not affect the repository 
						System.out.println("revert " + repositoryurl + " to latest revision of " + workingCopyPath);
					    final SvnRevert revert = svnOperationFactory.createRevert();
					    revert.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
					    revert.setRevision(sVNStatus.getCommittedRevision());
					    revert.setRevertMissingDirectories(true);
					    revert.setDepth(SVNDepth.INFINITY);
					    revert.run();	
					    System.out.println("revert done");
						
					    // do an update to latest revision					    
						try {
						    final SvnUpdate update = svnOperationFactory.createUpdate();
						    update.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
//						    update.setSource(SvnTarget.fromURL(repositorySVNurl));
						   update.run();	
						    System.out.println("update " + workingCopyPath  + " based on " + repositoryurl);
						    System.out.println("update done");
						} finally {
						    svnOperationFactory.dispose();
						}	
						
						

					    
					    // do an update to get all the changes from the repo
					    
					    // do a checkout to latest revision					    
//						try {
//						    final SvnCheckout checkout = svnOperationFactory.createCheckout();
//						    checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
//						    checkout.setSource(SvnTarget.fromURL(repositorySVNurl));
//						    Long revisionNumberOfExportedDirectory = checkout.run();	
//						    System.out.println("checkout " + repositoryurl + " to " + workingCopyPath);
//						    System.out.println("revisionNumberOfExportedDirectory: " + revisionNumberOfExportedDirectory);
//						} finally {
//						    svnOperationFactory.dispose();
//						}	
					    
					} finally {
					    svnOperationFactory.dispose();
					}	
				}
				
				
				
//				if(sVNStatus.getContentsStatus().equals(SVNStatusType.STATUS_NORMAL)){
//					// denotes that the item in the Working Copy being currently processed has no local changes
//					// do nothing
//				}
				
                
			} catch (SVNException e) {
				
				// if not yet a working copy
				if(e.toString().contains("is not a working copy")){
					System.out.println(workingCopyPath + " is not a working copy");
					// perform checkout with old API
//					SVNUpdateClient updateClient = clientManager.getUpdateClient();
//					updateClient.setIgnoreExternals(false);
//					long revisionNumberOfExportedDirectory = updateClient.doCheckout(repositorySVNurl, workingCopyDirectory, SVNRevision.HEAD, SVNRevision.HEAD,
//					            true);
//					System.out.println("revisionNumberOfExportedDirectory: " + revisionNumberOfExportedDirectory);
					
					// perform checkout with new API
					final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
					try {
					    final SvnCheckout checkout = svnOperationFactory.createCheckout();
					    checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
					    checkout.setSource(SvnTarget.fromURL(repositorySVNurl));
					    Long revisionNumberOfExportedDirectory = checkout.run();	
					    System.out.println("checkout " + repositoryurl + " to " + workingCopyPath);
					    System.out.println("revisionNumberOfExportedDirectory: " + revisionNumberOfExportedDirectory);
					} finally {
					    svnOperationFactory.dispose();
					}	
					
				}
				else{
					e.printStackTrace();
				}
				
			}
		     
//		     if(sVNStatus.getContentsStatus() == SVNStatusType.){
//		    	 
//		     }
		     
		     
		     // if reply is that there is no change, just do nothing
		     
		     // if reply is "... not a working copy" 
		     // perform checkout
		     
		     // if reply is that repository has changed, perform update
		     
		     // if reply is that local working copy has changed, perform revert to HEAD (latest revision of repository)
		     // svn update –r HEAD x	??
		     // or svn switch HEAD-url ??
		     
		     // if reply is that local working copy and repository has changed, perform revert to HEAD (latest revision of repository)
		     
		    
			System.out.println("ok");

		} 
		catch (SVNAuthenticationException e) {
			// no repository at specified URL
			e.printStackTrace();	    
		    System.err.println("Authentication failed");
		    System.exit(1);
		}
		catch (SVNException e) {
			// no repository at specified URL
			e.printStackTrace();	    
		    System.err.println("no repository at specified URL");
		    System.exit(1);
		}

		
		
		
	     
	       
	     
	}

}
