package handlers;

import java.util.ArrayList;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;

import util.FileMetadata;

public class InfoHandler implements ISVNInfoHandler {

	ArrayList<FileMetadata> filesmetadata;
	
	public InfoHandler(ArrayList<FileMetadata> filesmetadata) {
		super();
		this.filesmetadata = filesmetadata;
	}

	@Override
	public void handleInfo(SVNInfo info) throws SVNException {
		if(info.getKind() == SVNNodeKind.FILE){
			filesmetadata.add(new FileMetadata(info.getPath(), info.getAuthor(), info.getCommittedDate().toString(), info.getRepositoryRootURL().toString(), info.getRevision().toString(), info.getURL().toString()));
			System.out.println(" ");
			System.out.println("path: " + info.getPath());
			System.out.println("author: " + info.getAuthor());
			System.out.println("committedDate: " + info.getCommittedDate());
			System.out.println("repositoryRootURL: " + info.getRepositoryRootURL());
			System.out.println("revision: " + info.getRevision());			
			System.out.println("svnURL: " + info.getURL());
		}
		
		
		
	}

	public ArrayList<FileMetadata> getFilesmetadata() {
		return filesmetadata;
	}

	public void setFilesmetadata(ArrayList<FileMetadata> filesmetadata) {
		this.filesmetadata = filesmetadata;
	}

}
