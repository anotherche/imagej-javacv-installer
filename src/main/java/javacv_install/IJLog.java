package javacv_install;

import ij.IJ;

public final class IJLog {
	
	private static boolean updateLine;
	
	private IJLog() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
	
	public static void log(String message, boolean setUpdate) {
		IJ.log((updateLine ? "\\Update:" : "") + message);
		updateLine = setUpdate;
	}

	public static void log(String message) {
		log(message, false);
	}

}
