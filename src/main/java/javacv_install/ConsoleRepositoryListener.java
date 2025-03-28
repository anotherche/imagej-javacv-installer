package javacv_install;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

/**
 * A simplistic repository listener that logs events to the console.
 */
public final class ConsoleRepositoryListener extends AbstractRepositoryListener {
	
	@Override
	public void artifactDescriptorInvalid(RepositoryEvent event) {
		IJLog.log("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
	}

	@Override
	public void artifactDescriptorMissing(RepositoryEvent event) {
		IJLog.log("Missing artifact descriptor for " + event.getArtifact());
	}

	// public void artifactInstalled( RepositoryEvent event )
	// {
	// out.println( "Installed " + event.getArtifact() + " to " + event.getFile() );
	// }
	//
	// public void artifactInstalling( RepositoryEvent event )
	// {
	// out.println( "Installing " + event.getArtifact() + " to " + event.getFile()
	// );
	// }

	// public void artifactResolved( RepositoryEvent event )
	// {
	// IJ.log( "Resolved artifact " + event.getArtifact() + " from " +
	// event.getRepository() );
	// }
	//
	// public void artifactDownloading( RepositoryEvent event )
	// {
	// IJ.log( "Downloading artifact " + event.getArtifact() + " from " +
	// event.getRepository() );
	// }

	// public void artifactDownloaded( RepositoryEvent event )
	// {
	// if (event.getArtifact().getExtension()=="jar")
	// IJ.log( "Downloaded artifact " + event.getArtifact() + " from " +
	// event.getRepository() );
	// }

	// public void artifactResolving( RepositoryEvent event )
	// {
	// IJ.log( "Resolving artifact " + event.getArtifact() );
	// }

	// public void metadataDeployed( RepositoryEvent event )
	// {
	// out.println( "Deployed " + event.getMetadata() + " to " +
	// event.getRepository() );
	// }
	//
	// public void metadataDeploying( RepositoryEvent event )
	// {
	// out.println( "Deploying " + event.getMetadata() + " to " +
	// event.getRepository() );
	// }
	//
	// public void metadataInstalled( RepositoryEvent event )
	// {
	// out.println( "Installed " + event.getMetadata() + " to " + event.getFile() );
	// }
	//
	// public void metadataInstalling( RepositoryEvent event )
	// {
	// out.println( "Installing " + event.getMetadata() + " to " + event.getFile()
	// );
	// }
	//
	// public void metadataInvalid( RepositoryEvent event )
	// {
	// out.println( "Invalid metadata " + event.getMetadata() );
	// }
	//
	// public void metadataResolved( RepositoryEvent event )
	// {
	// out.println( "Resolved metadata " + event.getMetadata() + " from " +
	// event.getRepository() );
	// }
	//
	// public void metadataResolving( RepositoryEvent event )
	// {
	// out.println( "Resolving metadata " + event.getMetadata() + " from " +
	// event.getRepository() );
	// }

}
