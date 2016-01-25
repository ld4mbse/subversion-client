package clients;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

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
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevert;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

import handlers.FileStatusHandler;
import handlers.InfoHandler;
import util.FileMetadata;
import util.UserLoginInfo;

public class SubversionFileClientNonStatic {

	private SVNRepository repository;
//	private String repositoryurl = "https://koneksys1:18080/svn/myrepo";
	private String fileurl;
	private String fileName;
	// static String repositoryurl = "https://Axel-PC/svn/simulinkrepository";
	private String repositoryurl;
	// "https://svn.svnkit.com/repos/svnkit/trunk/";

	private String workingCopyPath = "C:\\Users\\Axel\\Desktop\\myhttpswk\\myrepo4";

	private SVNClientManager clientManager;
	private ISVNAuthenticationManager authManager;
	private SvnOperationFactory svnOperationFactory;

	private boolean wasLoginSuccesful = false;

	public static void main(String[] args) {
		String repositoryurl = "https://koneksys1:18080/svn/myrepo";
		SubversionFileClientNonStatic subversionFileClient = new SubversionFileClientNonStatic();
		subversionFileClient.connectWithRepo();
		System.exit(0);
	}

	public FileMetadata connectWithRepo() {
		FileMetadata fileMetadata = null;
		try {
			 UserLoginInfo userLoginInfo = new UserLoginInfo();
			 userLoginInfo.setUsername("axel");
			 userLoginInfo.setPassword("axel");
			 setUpSVNClient(userLoginInfo);
			 setUpConnectionToRepo(userLoginInfo, repositoryurl);
			 repository.testConnection();

//			if (!wasLoginSuccesful) {
//				createLoginDialog();
//			}

			// need to identify latest revision
			long latestRevision = repository.getLatestRevision();
			System.out.println("Checking Repository " + repositoryurl + " at latest revision " + latestRevision);
			System.out.println("Checking Working Copy " + workingCopyPath + "\r\n");

			// do svn status

			// SVNStatus class is used to provide detailed status information
			// for a Working Copy item
			SVNStatusClient sVNStatusClient = clientManager.getStatusClient();
			SVNStatus sVNStatus;
			File workingCopyDirectory = new File(workingCopyPath);
			svnOperationFactory = new SvnOperationFactory();
			try {

				sVNStatus = sVNStatusClient.doStatus(workingCopyDirectory, true);
				if (sVNStatus == null) {
					fileMetadata = deleteFileAndPerformCheckout();
				} else {

					// existing working copy may be for a different repository
					if (repositoryurl.endsWith("/")) {
						repositoryurl = repositoryurl.substring(0, repositoryurl.length() - 1);
					}
					if (sVNStatus.getRemoteURL().toString().toLowerCase().equals(repositoryurl.toLowerCase())) {

						try {
							// for each single file

							// perform revert and update
							// do a revert to undo all local changes
							// this operation does not affect the repository
							System.out.println("revert " + repositoryurl + " to latest revision of " + workingCopyPath);
							final SvnRevert revert = svnOperationFactory.createRevert();
							revert.setSingleTarget(SvnTarget.fromFile(new File(workingCopyPath + "/" + fileName)));
							revert.run();
							System.out.println("revert done");

							// do an update to latest revision
							fileMetadata = performUpdate();

						} finally {
							svnOperationFactory.dispose();
						}

					} else {
						System.out.println("working copy " + workingCopyPath + " previously linked to other repository"
								+ sVNStatus.getRemoteURL().toString());
						// remove all local files in working copy and do a fresh
						// checkout
						fileMetadata = deleteFileAndPerformCheckout();

					}
					
				}

			} catch (SVNException e) {
				// if not yet a working copy
				if (e.toString().contains("is not a working copy")) {
					System.out.println("working copy " + workingCopyPath + " is new");
					fileMetadata = deleteFileAndPerformCheckout();
				}
				// working copy linked to another repository, which is not
				// accessible
				else if (e.toString().contains("E175002")) {
					System.out.println("working copy " + workingCopyPath + " previously linked to other repository");
					// non-accessible repository - host unknown or no access
					// authorization
					fileMetadata = deleteFileAndPerformCheckout();
				} else if (e.toString().contains("E170001")) {
					System.out.println("working copy " + workingCopyPath + " previously linked to other repository");
					// non-accessible repository - other required authentication
					// credentials
					fileMetadata = deleteFileAndPerformCheckout();
				} else {
					e.printStackTrace();
				}
			}

		} catch (SVNException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// add a listener to close application when adapter is closing
		// System.exit(0);
		System.out.println("\r\nconnection with repository done");
		return fileMetadata;

	}

	private FileMetadata deleteFileAndPerformCheckout() {
		new File(workingCopyPath + "/" + fileName).delete();
		return performCheckout();
	}

	private FileMetadata performUpdate() {
		// update entire working copy (in order to get new files from repo)
		// do an update to latest revision
		FileMetadata fileMetadata = null;
		try {
			final SvnUpdate update = svnOperationFactory.createUpdate();
			// update.setSingleTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(fileurl)));
			// update.setSingleTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(fileName)));
			update.setSingleTarget(SvnTarget.fromFile(new File(workingCopyPath + "/" + fileName)));

			// update.setSource(SvnTarget.fromURL(repositorySVNurl));
			update.run();
			System.out.println("update " + workingCopyPath + "/" + fileName + " based on " + repositoryurl);
			System.out.println("update done");
			fileMetadata = retrieveFileMetadata();
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			svnOperationFactory.dispose();
		}
		return fileMetadata;
	}

	private void setUpConnectionToRepo(UserLoginInfo userLoginInfo, String repositoryurl) {
		// http://svnkit.com/kb/javadoc/org/tmatesoft/svn/core/io/SVNRepository.html

		// Set up connection protocols support:
		// http:// and https://
		DAVRepositoryFactory.setup();
		// svn://, svn+xxx:// (svn+ssh:// in particular)
		SVNRepositoryFactoryImpl.setup();
		// file:///
		FSRepositoryFactory.setup();

		// creating a new SVNRepository instance
		// String repositoryurl = "https://koneksys1:18080/svn/myrepo";
		SVNURL repositorySVNurl;
		try {
			repositorySVNurl = SVNURL.parseURIDecoded(repositoryurl);

			repository = DAVRepositoryFactory.create(repositorySVNurl);

			authManager = SVNWCUtil.createDefaultAuthenticationManager(userLoginInfo.getUsername(),
					userLoginInfo.getPassword());
			repository.setAuthenticationManager(authManager);

		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setUpSVNClient(UserLoginInfo userLoginInfo) {
		DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);
		clientManager = SVNClientManager.newInstance(myOptions, userLoginInfo.getUsername(),
				userLoginInfo.getPassword());
	}

	public static void setUpSVNWCClient(UserLoginInfo userLoginInfo) {
		DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);

	}

	public String isLoginSuccess(UserLoginInfo userLoginInfo) {
		try {
			if (repositoryurl.startsWith("file")) {
				// connection to local repo (url starting with file:/// scheme)
				// will not perform user authentication test
				// it is necessary to perform an svn status to perform user
				// authentication test
				setUpConnectionToRepo(userLoginInfo, repositoryurl);
				setUpSVNClient(userLoginInfo);
				SVNStatusClient sVNStatusClient = clientManager.getStatusClient();
				File workingCopyDirectory = new File(workingCopyPath);

				try {
					SVNStatus sVNStatus = sVNStatusClient.doStatus(workingCopyDirectory, true);
					return "ok";
				} catch (SVNException e) {
					// if not yet a working copy
					if (e.toString().contains("is not a working copy")) {
						System.out.println(workingCopyPath + " is not a working copy");

						// if the repository is using the file scheme
						// and if the working directory does not yet exist
						// svn can perform a checkout even if the
						// username/password is false!

						return "ok";
					} else {
						return "authentication";
					}
				}

			} else {
				setUpConnectionToRepo(userLoginInfo, repositoryurl);
				repository.testConnection();
				setUpSVNClient(userLoginInfo);
				return "ok";
			}

		} catch (SVNAuthenticationException e) {
			// no repository at specified URL
			return "authentication";
		} catch (SVNException e) {
			// if not yet a working copy
			if (e.toString().contains("is not a working copy")) {
				System.out.println(workingCopyPath + " is not a working copy");
				return "ok";
			} else {
				return "connection";
			}

		}

	}

	public FileMetadata syncFile(String repositoryurl2, String workingCopyPath2) {
		// get repository directory
		String[] repParts = repositoryurl2.split("/");
		fileName = repParts[repParts.length - 1];
		repositoryurl = repositoryurl2.replace(fileName, "");
		
//		String[] repParts2 = repositoryurl2.split("/");
//		String subversionFileName = repParts2[repParts.length - 1];		
//		String subversionFileDirURL = repositoryurl2.replace(subversionFileName, "");																	
//		String localSubversionFileDir = subversionFileDirURL.replace(":", "");
//		localSubversionFileDir = localSubversionFileDir.replace("/", "");
//		fileName = localSubversionFileDir + "---" + subversionFileName;
//		repositoryurl = subversionFileDirURL;
		
		fileurl = repositoryurl2;		
		workingCopyPath = workingCopyPath2;
		return connectWithRepo();

	}

	public static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	public FileMetadata performCheckout() {
		// perform checkout with new API
		FileMetadata fileMetadata = null;
		try {
			final SvnCheckout checkout = svnOperationFactory.createCheckout();
			checkout.setSingleTarget(SvnTarget.fromFile(new File(workingCopyPath)));
			checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(repositoryurl)));
			checkout.setDepth(SVNDepth.EMPTY);
			Long revisionNumberOfExportedDirectory = checkout.run();
			System.out.println("checkout empty repository " + repositoryurl + " to " + workingCopyPath);
			System.out.println("revisionNumberOfExportedDirectory: " + revisionNumberOfExportedDirectory);

			// perform update
			fileMetadata = performUpdate();

			// fileMetadatas = retrieveFileMetadata();
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			svnOperationFactory.dispose();
		}
		return fileMetadata;
	}

	private FileMetadata retrieveFileMetadata() {
		// ArrayList<FileMetadata> fileMetadatas = new
		// ArrayList<FileMetadata>();
		FileMetadata fileMetadata = null;
		System.out.println(" ");
		System.out.println("retrieving file metadata");
		DefaultSVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);
		SVNWCClient sVNWCClient = new SVNWCClient(authManager, myOptions);
		SVNInfo info;
		// InfoHandler infoHandler = new InfoHandler(fileMetadatas);
		try {
			info = sVNWCClient.doInfo(new File(workingCopyPath + "/" + fileName), SVNRevision.WORKING);
			
			String[] workingCopyDirs = workingCopyPath.split("/");
			String workingCopyDir = workingCopyDirs[workingCopyDirs.length - 1];
					
//					String[] repParts2 = repositoryurl2.split("/");
//					String subversionFileName = repParts2[repParts.length - 1];		
//					String subversionFileDirURL = repositoryurl2.replace(subversionFileName, "");																	
//					String localSubversionFileDir = subversionFileDirURL.replace(":", "");
//					localSubversionFileDir = localSubversionFileDir.replace("/", "");
//					fileName = localSubversionFileDir + "---" + subversionFileName;
//					repositoryurl = subversionFileDirURL;
			
			fileMetadata = new FileMetadata(workingCopyDir + "---" + fileName, info.getAuthor(), info.getCommittedDate().toString(),
					info.getRepositoryRootURL().toString(), info.getRevision().toString(), info.getURL().toString());
			// sVNWCClient.doInfo(new File(workingCopyPath), SVNRevision.HEAD,
			// SVNRevision.HEAD, SVNDepth.INFINITY, null,
			// new InfoHandler(fileMetadatas));
			// for (FileMetadata fileMetadata2 : fileMetadatas) {
			// if(fileMetadata2.getPath().equals(fileName)){
			// fileMetadata = fileMetadata2;
			// break;
			// }
			// }

		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileMetadata;
	}

	/**
	 * List all files from a directory and its subdirectories
	 * 
	 * @param directoryName
	 *            to be listed
	 */
	public static ArrayList<File> listFilesAndFilesSubDirectories(String directoryName) {

		File directory = new File(directoryName);

		// get all the files from a directory
		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				// System.out.println(file.getAbsolutePath());
			} else if (file.isDirectory() & !(file.getPath().contains(".svn"))) { // remove
																					// files
																					// of
																					// subversion
																					// (.svn)
				listFilesAndFilesSubDirectories(file.getAbsolutePath());
			}
		}

		ArrayList<File> files = new ArrayList<File>();
		for (File file : fList) {
			if (!(file.getPath().contains(".svn"))) {
				files.add(file);
			}

		}

		return files;
	}

	public void createLoginDialog() {
		final JFrame frame = new JFrame("Subversion Authentication");
		LoginDialog loginDlg = new LoginDialog(frame);
		loginDlg.setVisible(true);
		wasLoginSuccesful = loginDlg.succeeded;
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 100);
		frame.setLayout(new FlowLayout());

	}

	public class LoginDialog extends JDialog {

		private JTextField tfUsername;
		private JPasswordField pfPassword;
		private JLabel lbUsername;
		private JLabel lbPassword;
		private JButton btnLogin;
		private JButton btnCancel;
		boolean succeeded;

		public LoginDialog(JFrame parent) {
			super(parent, "Subversion Login", true);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);

			//
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints cs = new GridBagConstraints();

			cs.fill = GridBagConstraints.HORIZONTAL;

			lbUsername = new JLabel("Username: ");
			cs.gridx = 0;
			cs.gridy = 0;
			cs.gridwidth = 1;
			panel.add(lbUsername, cs);

			tfUsername = new JTextField(20);
			cs.gridx = 1;
			cs.gridy = 0;
			cs.gridwidth = 2;
			panel.add(tfUsername, cs);

			lbPassword = new JLabel("Password: ");
			cs.gridx = 0;
			cs.gridy = 1;
			cs.gridwidth = 1;
			panel.add(lbPassword, cs);

			pfPassword = new JPasswordField(20);
			cs.gridx = 1;
			cs.gridy = 1;
			cs.gridwidth = 2;
			panel.add(pfPassword, cs);
			panel.setBorder(new LineBorder(Color.GRAY));

			btnLogin = new JButton("Login");

			btnLogin.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					UserLoginInfo userLoginInfo = new UserLoginInfo();
					userLoginInfo.setUsername(getUsername());
					userLoginInfo.setPassword(getPassword());

					// if (Login.authenticate(getUsername(), getPassword())) {
					String connectionStatus = isLoginSuccess(userLoginInfo);
					if (connectionStatus.equals("ok")) {
						JOptionPane.showMessageDialog(LoginDialog.this, "You have successfully logged in.",
								"Subversion Login", JOptionPane.INFORMATION_MESSAGE);
						succeeded = true;
						parent.dispose();
						// System.exit(0);
					} else if (connectionStatus.equals("authentication")) {
						JOptionPane.showMessageDialog(LoginDialog.this, "Invalid username or password",
								"Authentication Error", JOptionPane.ERROR_MESSAGE);
						// reset username and password
						tfUsername.setText("");
						pfPassword.setText("");
						succeeded = false;

					} else {
						JOptionPane.showMessageDialog(LoginDialog.this,
								"username and password are ok but other connection problem", "Subversion Login",
								JOptionPane.INFORMATION_MESSAGE);
						// reset username and password
						tfUsername.setText("");
						pfPassword.setText("");
						succeeded = false;
					}
				}
			});

			btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					dispose();
					System.exit(0);
				}
			});
			JPanel bp = new JPanel();
			bp.add(btnLogin);
			bp.add(btnCancel);

			pfPassword.addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(KeyEvent keyEvent) {

					if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
						UserLoginInfo userLoginInfo = new UserLoginInfo();
						userLoginInfo.setUsername(getUsername());
						userLoginInfo.setPassword(getPassword());

						String connectionStatus = isLoginSuccess(userLoginInfo);
						if (connectionStatus.equals("ok")) {
							JOptionPane.showMessageDialog(LoginDialog.this, "You have successfully logged in.",
									"Subversion Login", JOptionPane.INFORMATION_MESSAGE);
							succeeded = true;
							dispose();
							// System.exit(0);
						} else if (connectionStatus.equals("authentication")) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Invalid username or password",
									"Subversion Login", JOptionPane.ERROR_MESSAGE);
							// reset username and password
							tfUsername.setText("");
							pfPassword.setText("");
							succeeded = false;
						} else {
							JOptionPane.showMessageDialog(LoginDialog.this,
									"username and password are ok but other connection problem", "Subversion Login",
									JOptionPane.INFORMATION_MESSAGE);
							// succeeded = true;
							dispose();
						}
					}

				}

				@Override
				public void keyReleased(KeyEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void keyTyped(KeyEvent arg0) {
					// TODO Auto-generated method stub

				}

			});

			getContentPane().add(panel, BorderLayout.CENTER);
			getContentPane().add(bp, BorderLayout.PAGE_END);

			pack();
			setResizable(false);
			setLocationRelativeTo(parent);
		}

		public String getUsername() {
			return tfUsername.getText().trim();
		}

		public String getPassword() {
			return new String(pfPassword.getPassword());
		}

		public boolean isSucceeded() {
			return succeeded;
		}
	}

}
