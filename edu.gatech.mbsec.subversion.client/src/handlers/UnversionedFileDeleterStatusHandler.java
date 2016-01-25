package handlers;
/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 * 
 * 
 * The TMate Open Source License.

License at http://svnkit.com/license.html

This license applies to all portions of TMate SVNKit library, which 
are not externally-maintained libraries (e.g. Ganymed SSH library).

All the source code and compiled classes in package org.tigris.subversion.javahl
except SvnClient class are covered by the license in JAVAHL-LICENSE file

Copyright (c) 2004-2012 TMate Software. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
      
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
      
    * Redistributions in any form must be accompanied by information on how to 
      obtain complete source code for the software that uses SVNKit and any 
      accompanying software that uses the software that uses SVNKit. The source 
      code must either be included in the distribution or be available for no 
      more than the cost of distribution plus a nominal fee, and must be freely 
      redistributable under reasonable conditions. For an executable file, complete 
      source code means the source code for all modules it contains. It does not 
      include source code for modules or files that typically accompany the major 
      components of the operating system on which the executable file runs.
      
    * Redistribution in any form without redistributing source code for software 
      that uses SVNKit is possible only when such redistribution is explictly permitted 
      by TMate Software. Please, contact TMate Software at support@svnkit.com to 
      get such permission.

THIS SOFTWARE IS PROVIDED BY TMATE SOFTWARE ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT, ARE 
DISCLAIMED. 

IN NO EVENT SHALL TMATE SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */


import java.io.File;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevert;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

import com.sun.jna.platform.FileUtils;

/*
 * This is  an implementation of ISVNStatusHandler & ISVNEventHandler  that  is 
 * used in WorkingCopy.java to display status information. This  implementation  
 * is passed to 
 * 
 * SVNStatusClient.doStatus(File path, boolean recursive, boolean remote, 
 * boolean reportAll, boolean includeIgnored, boolean collectParentExternals, 
 * ISVNStatusHandler handler)
 * 
 * For each item to be processed doStatus(..) collects status  information  and 
 * creates an SVNStatus object which holds that information. Then  doStatus(..) 
 * calls an implementor's handler.handleStatus(SVNStatus) passing it the status 
 * info collected.
 * 
 * StatusHandler  will  be  also  provided  to  an  SVNStatusClient object as a 
 * handler of events generated by a doStatus(..) method. For  example,  if  the 
 * status is invoked with the flag remote=true (like 'svn status -u'  command), 
 * so then the status operation will be finished with dispatching  an  SVNEvent 
 * to ISVNEventHandler that will 'say' that the status is performed against the
 * youngest revision (the event holds that revision number). 
 */
public class UnversionedFileDeleterStatusHandler implements ISVNStatusHandler, ISVNEventHandler {
    private boolean myIsRemote;
    String repositoryurl;
    String workingCopyPath;
    SvnOperationFactory svnOperationFactory;
//    SVNStatus sVNStatus;
    public UnversionedFileDeleterStatusHandler(boolean isRemote) {
        myIsRemote = isRemote;
        this.repositoryurl = repositoryurl;
        this.workingCopyPath = workingCopyPath; 
        this.svnOperationFactory = svnOperationFactory; 
//        this.sVNStatus = sVNStatus;
        
    }
    /*
     * This is  an  implementation  of ISVNStatusHandler.handleStatus(SVNStatus 
     * status)
     */
    public void handleStatus(SVNStatus status) {
        /*
         * Gets  the  status  of  file/directory/symbolic link  text  contents. 
         * It is  SVNStatusType  who  contains  information on the state of  an 
         * item. 
         */
        SVNStatusType contentsStatus = status.getContentsStatus();

        String pathChangeType = " ";
        
        boolean isAddedWithHistory = status.isCopied();
        if (contentsStatus == SVNStatusType.STATUS_MODIFIED) {
            /*
             * The contents of the file have been Modified.
             */
            pathChangeType = "M";
        } else if (contentsStatus == SVNStatusType.STATUS_CONFLICTED) {
            /*
             * The  file  item  is  in a state of  Conflict. That  is,  changes 
             * received from the server during an  update  overlap  with  local 
             * changes the user has in his working copy. 
             */
            pathChangeType = "C";
        } else if (contentsStatus == SVNStatusType.STATUS_DELETED) {
            /*
             * The file, directory or symbolic link item has been scheduled for 
             * Deletion from the repository.
             */
            pathChangeType = "D";
        } else if (contentsStatus == SVNStatusType.STATUS_ADDED) {
            /*
             * The file, directory or symbolic link item has been scheduled for 
             * Addition to the repository.
             */
            pathChangeType = "A";
        } else if (contentsStatus == SVNStatusType.STATUS_UNVERSIONED) {
            /*
             * The file, directory or symbolic link item is not  under  version 
             * control.
             */
            pathChangeType = "?";
        } else if (contentsStatus == SVNStatusType.STATUS_EXTERNAL) {
            /*
             * The item is unversioned, but is used by an eXternals definition.
             */
            pathChangeType = "X";
        } else if (contentsStatus == SVNStatusType.STATUS_IGNORED) {
            /*
             * The file, directory or symbolic link item is not  under  version 
             * control, and is configured to be Ignored during 'add',  'import' 
             * and 'status' operations. 
             */
            pathChangeType = "I";
        } else if (contentsStatus == SVNStatusType.STATUS_MISSING
                || contentsStatus == SVNStatusType.STATUS_INCOMPLETE) {
            /*
             * The file, directory or  symbolic  link  item  is  under  version 
             * control but is missing or somehow incomplete. The  item  can  be 
             * missing if it is removed using a command incompatible  with  the 
             * native Subversion command line client (for example, just removed 
             * from the filesystem). In the case the item is  a  directory,  it 
             * can  be  incomplete if the user happened to interrupt a checkout 
             * or update.
             */
            pathChangeType = "!";
        } else if (contentsStatus == SVNStatusType.STATUS_OBSTRUCTED) {
            /*
             * The file, directory or symbolic link item is in  the  repository 
             * as one kind of object, but what's actually in the user's working 
             * copy is some other kind. For example, Subversion  might  have  a 
             * file in the repository,  but  the  user  removed  the  file  and 
             * created a directory in its place, without using the 'svn delete' 
             * or 'svn add' command (or SVNKit analogues for them).
             */
            pathChangeType = "~";
        } else if (contentsStatus == SVNStatusType.STATUS_REPLACED) {
            /*
             * The file, directory or symbolic link item was  Replaced  in  the 
             * user's working copy; that is, the item was deleted,  and  a  new 
             * item with the same name was added (within  a  single  revision). 
             * While they may have the same name, the repository considers them 
             * to be distinct objects with distinct histories.
             */
            pathChangeType = "R";
        } else if (contentsStatus == SVNStatusType.STATUS_NONE
                || contentsStatus == SVNStatusType.STATUS_NORMAL) {
            /*
             * The item was not modified (normal).
             */
            pathChangeType = " ";
        }
        
        // if file in working copy is different than in repository
        // do a revert and an update
        if((pathChangeType.equals("M") | pathChangeType.equals("C"))) {
        	// if modified, in conflict, 
        	// revert and update
        	System.out.println(status.getFile().getAbsolutePath()
					+ ": modified or in conflict with repository " + repositoryurl);	
        	performRevertOnFile(svnOperationFactory, status);
			performUpdateOnFile(svnOperationFactory, status);

        }
        else if(pathChangeType.equals("D")) {
        	System.out.println(status.getFile().getAbsolutePath()
					+ ": deleted in repository " + repositoryurl);
        	// if deleted on repository
        	deleteDir(status.getFile(), status);
        }
        else if(pathChangeType.equals("A")) {
        	System.out.println(status.getFile().getAbsolutePath()
					+ ": added in repository " + repositoryurl);
        	// if added on repository
        	performUpdateOnFile(svnOperationFactory, status);
        }
        else if(pathChangeType.equals("?") | pathChangeType.equals("I") | pathChangeType.equals("X")) {
        	System.out.println(status.getFile().getAbsolutePath()
					+ ": not under version control in repository " + repositoryurl);
        	// if file is not under version control in repository (?)
        	// if file is to be ignored (I)
        	// if file not under version control in repository but used by external app (X)
        	deleteDir(status.getFile(), status);
        }
        else if(pathChangeType.equals("~")) {
        	System.out.println(status.getFile().getAbsolutePath()
					+ ": different kind of file under same name in repository " + repositoryurl);
        	// The file, directory or symbolic link item is in  the  repository 
            // * as one kind of object, but what's actually in the user's working 
            // * copy is some other kind
        	deleteDir(status.getFile(), status);
        	performUpdateOnFile(svnOperationFactory, status);
        }
        
        
        else if(pathChangeType.equals(" ") ) {
        	// do nothing
        	System.out.println(status.getFile().getAbsolutePath()
					+ ":\t no change with repository " + repositoryurl);        	
        }
        
        /*
         * If SVNStatusClient.doStatus(..) was invoked with  remote = true  the 
         * following code finds out whether the current item had  been  changed 
         * in the repository   
         */
        String remoteChangeType = " ";

        if(status.getRemotePropertiesStatus() != SVNStatusType.STATUS_NONE || 
           status.getRemoteContentsStatus() != SVNStatusType.STATUS_NONE) {
            /*
             * the local item is out of date
             */
            remoteChangeType = "*";
        }
        /*
         * Now getting the status of properties of an item. SVNStatusType  also 
         * contains information on the properties state.
         */
        SVNStatusType propertiesStatus = status.getPropertiesStatus();
        /*
         * Default - properties are normal (unmodified).
         */
        String propertiesChangeType = " ";
        if (propertiesStatus == SVNStatusType.STATUS_MODIFIED) {
            /*
             * Properties were modified.
             */
            propertiesChangeType = "M";
        } else if (propertiesStatus == SVNStatusType.STATUS_CONFLICTED) {
            /*
             * Properties are in conflict with the repository.
             */
            propertiesChangeType = "C";
        }

        /*
         * Whether the item was locked in the .svn working area  (for  example, 
         * during a commit or maybe the previous operation was interrupted, in 
         * this case the lock needs to be cleaned up). 
         */
        boolean isLocked = status.isLocked();
        /*
         * Whether the item is switched to a different URL (branch).
         */
        boolean isSwitched = status.isSwitched();
        /*
         * If the item is a file it may be locked.
         */
        SVNLock localLock = status.getLocalLock();
        /*
         * If  doStatus()  was  run  with  remote=true  and the item is a file, 
         * checks whether a remote lock presents.
         */
        SVNLock remoteLock = status.getRemoteLock();
        String lockLabel = " ";

        if (localLock != null) {
            /*
             * at first suppose the file is locKed
             */
            lockLabel = "K";
            if (remoteLock != null) {
                /*
                 * if the lock-token of the local lock differs from  the  lock-
                 * token of the remote lock - the lock was sTolen!
                 */
                if (!remoteLock.getID().equals(localLock.getID())) {
                    lockLabel = "T";
                }
            } else {
                if(myIsRemote){
	                /*
	                 * the  local  lock presents but there's  no  lock  in  the
	                 * repository - the lock was Broken. This  is  true only if 
                     * doStatus() was invoked with remote=true.
	                 */
	                lockLabel = "B";
                }
            }
        } else if (remoteLock != null) {
            /*
             * the file is not locally locked but locked  in  the  repository -
             * the lock token is in some Other working copy.
             */
            lockLabel = "O";
        }

        /*
         * Obtains the working revision number of the item.
         */
        long workingRevision = status.getRevision().getNumber();
        
        if(workingRevision == -1){
        	// non empty directories do not get deleted
        	deleteDir(status.getFile(), status);
        	System.out.println("deleted file not under version control: " + status.getFile());
        	
//        	// delete directory
//        	if(status.getFile().isDirectory()){
//        		FileUtils.deleteDirectory();
//        	}
//        	else{
//        		// delete file
//            	status.getFile().delete();   
//            	System.out.println("deleted file not under version control: " + status.getFile());
//        	}
        	
        	
        	
        	 
        }
        
        
        /*
         * Obtains the number of the revision when the item was last changed. 
         */
        long lastChangedRevision = status.getCommittedRevision().getNumber();
        String offset = "                                ";
        String[] offsets = new String[3];
        offsets[0] = offset.substring(0, 6 - String.valueOf(workingRevision)
                .length());
        offsets[1] = offset.substring(0, 6 - String
                .valueOf(lastChangedRevision).length());
        //status
        offsets[2] = offset.substring(0,
                offset.length()
                        - (status.getAuthor() != null ? status.getAuthor()
                                .length() : 1));
        /*
         * status is shown in the manner of the native Subversion command  line
         * client's command "svn status"
         */
        System.out.println(pathChangeType
                + propertiesChangeType
                + (isLocked ? "L" : " ")
                + (isAddedWithHistory ? "+" : " ")
                + (isSwitched ? "S" : " ")
                + lockLabel
                + "  "
                + remoteChangeType
                + "  "
                + workingRevision
                + offsets[0]
                + (lastChangedRevision >= 0 ? String
                        .valueOf(lastChangedRevision) : "?") + offsets[1]
                + (status.getAuthor() != null ? status.getAuthor() : "?")
                + offsets[2] + status.getFile().getPath());
    }
    
    /*
     * This is an implementation for 
     * ISVNEventHandler.handleEvent(SVNEvent event, double progress)
     */
    public void handleEvent(SVNEvent event, double progress) {
        /*
         * Gets the current action. An action is represented by SVNEventAction.
         * In case of a status operation a current action can be determined via 
         * SVNEvent.getAction() and SVNEventAction.STATUS_-like constants. 
         */
        SVNEventAction action = event.getAction();
        /*
         * Print out the revision against which the status was performed.  This 
         * event is dispatched when the SVNStatusClient.doStatus() was  invoked 
         * with the flag remote set to true - that is for  a  local  status  it 
         * won't be dispatched.
         */
        if(action == SVNEventAction.STATUS_COMPLETED){
            System.out.println("Status against revision:  "+ event.getRevision());
        }
    
    }

    /*
     * Should be implemented to check if the current operation is cancelled. If 
     * it is, this method should throw an SVNCancelException. 
     */
    public void checkCancelled() throws SVNCancelException {
    
    }
    
    void deleteDir(File file, SVNStatus status){
        File[] contents = status.getFile().listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f, status);
            }
        }
        status.getFile().delete();
        System.out.println(status.getFile().getAbsolutePath() + ": deleted ");
    }
    
    void performUpdateOnFile(SvnOperationFactory svnOperationFactory, SVNStatus sVNStatus){
    	// update
		try {
			final SvnUpdate update = svnOperationFactory
					.createUpdate();
			update.setSingleTarget(SvnTarget
					.fromFile(sVNStatus.getFile()));
			// update.setSource(SvnTarget.fromURL(repositorySVNurl));
			update.run();
			System.out.println(sVNStatus.getFile().getAbsolutePath()
					+ ": updated");				
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			svnOperationFactory.dispose();
		}
    }
    
    void performRevertOnFile(SvnOperationFactory svnOperationFactory, SVNStatus sVNStatus){
    	final SvnRevert revert = svnOperationFactory
				.createRevert();
		revert.setSingleTarget(SvnTarget
				.fromFile(new File(sVNStatus.getFile().getAbsolutePath())));
//		revert.setRevision(sVNStatus.getCommittedRevision());
		revert.setRevertMissingDirectories(true);
		revert.setDepth(SVNDepth.INFINITY);
		try {
			revert.run();
			System.out.println(sVNStatus.getFile().getAbsolutePath()
					+ ": local changes reverted");	
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}