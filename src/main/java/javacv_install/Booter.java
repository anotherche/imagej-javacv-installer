package javacv_install;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import ij.IJ;


public final class Booter {
	
	/** Base URL to the maven repository */
	private static final String BASE_REPO = "https://repo1.maven.org/maven2/"; // "https://repo.maven.apache.org/maven2/"
	/** Local maven repository path */
	private static final String LOCAL_REPO = "local-maven-repo";
	
	
	private Booter() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
	
	public static RepositorySystem newRepositorySystem() {
		return SupplierRepositorySystemFactory.newRepositorySystem();
	}

	public static SessionBuilder newRepositorySystemSession(RepositorySystem system) {
		Path localRepoPath = FileSystems.getDefault().getPath(IJ.getDirectory("imagej") + LOCAL_REPO);
		return new SessionBuilderSupplier(system).get().setSystemProperties(System.getProperties())
				.withLocalRepositoryBaseDirectories(localRepoPath)
				.setRepositoryListener(new ConsoleRepositoryListener())
				.setTransferListener(new ConsoleTransferListener());
	}
	
	

	public static List<RemoteRepository> newRepositories() {
		return new ArrayList<>(Collections.singletonList(newCentralRepository()));
	}

	private static RemoteRepository newCentralRepository() {
		return new RemoteRepository.Builder("central", "default", BASE_REPO).build();
	}
}
