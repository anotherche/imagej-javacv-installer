package javacv_install;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

public final class SupplierRepositorySystemFactory {
	
	private SupplierRepositorySystemFactory() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
	
	public static RepositorySystem newRepositorySystem() {
		return new RepositorySystemSupplier().get();
	}
}
