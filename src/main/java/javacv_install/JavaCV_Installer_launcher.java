package javacv_install;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.Prefs;
import ij.plugin.PlugIn;


public class JavaCV_Installer_launcher implements PlugIn{
	
	
	/** Base URL to the maven repository */
	private static final String MAVEN_BASE_URL = "https://repo1.maven.org/maven2/";
	private static final String APACHEMAVEN = "org/apache/maven/";
	private static final String installerClassName = "JavaCV_Installer";
	private static final String installerPkg = "javacv_install";
	private static final String msgTitle = "JavaCV Installer"; 
	
	
	private static ArrayList<Artifact> artifacts;
	private static ArrayList<Dependency> dependencies;
	private static String installerDirectory;
	private static String dependencyPath;
	private static boolean restartRequired;
	private static boolean updateLoader;
	
	static {
		artifacts = new ArrayList<Artifact>();
		
		artifacts.add(new Artifact("maven-artifact", "3.6.3", APACHEMAVEN + "maven-artifact/"));
		artifacts.add(new Artifact("maven-builder-support", "3.6.3", APACHEMAVEN + "maven-builder-support/"));
		artifacts.add(new Artifact("maven-model", "3.6.3", APACHEMAVEN + "maven-model/"));
		artifacts.add(new Artifact("maven-model-builder", "3.6.3", APACHEMAVEN + "maven-model-builder/"));
		artifacts.add(new Artifact("maven-repository-metadata", "3.6.3", APACHEMAVEN + "maven-repository-metadata/"));
		artifacts.add(new Artifact("maven-resolver-provider", "3.6.3", APACHEMAVEN + "maven-resolver-provider/"));
		artifacts.add(new Artifact("maven-resolver-api", "1.6.1", APACHEMAVEN + "resolver/maven-resolver-api/"));
		artifacts.add(new Artifact("maven-resolver-connector-basic", "1.6.1", APACHEMAVEN + "resolver/maven-resolver-connector-basic/"));
		artifacts.add(new Artifact("maven-resolver-impl", "1.4.1", APACHEMAVEN + "resolver/maven-resolver-impl/"));
		artifacts.add(new Artifact("maven-resolver-spi", "1.6.1", APACHEMAVEN + "resolver/maven-resolver-spi/"));
		artifacts.add(new Artifact("maven-resolver-transport-file", "1.1.0", APACHEMAVEN + "resolver/maven-resolver-transport-file/"));
		artifacts.add(new Artifact("maven-resolver-transport-http", "1.1.0", APACHEMAVEN + "resolver/maven-resolver-transport-http/"));
		artifacts.add(new Artifact("maven-resolver-util", "1.6.1", APACHEMAVEN + "resolver/maven-resolver-util/"));
		artifacts.add(new Artifact("org.eclipse.sisu.inject", "0.3.4", "org/eclipse/sisu/org.eclipse.sisu.inject/"));
		artifacts.add(new Artifact("plexus-interpolation", "1.25", "org/codehaus/plexus/plexus-interpolation/"));
		artifacts.add(new Artifact("plexus-utils", "3.2.1", "org/codehaus/plexus/plexus-utils/"));
		artifacts.add(new Artifact("slf4j-api", "1.7.30", "org/slf4j/slf4j-api/"));
		artifacts.add(new Artifact("jcl-over-slf4j", "1.7.30", "org/slf4j/jcl-over-slf4j/"));
		artifacts.add(new Artifact("javax.inject", "1", "javax/inject/javax.inject/"));
		artifacts.add(new Artifact("httpcore", "4.4.13", "org/apache/httpcomponents/httpcore/"));
		artifacts.add(new Artifact("httpclient", "4.5.12",  "org/apache/httpcomponents/httpclient/"));
		artifacts.add(new Artifact("commons-lang3", "3.10", "org/apache/commons/commons-lang3/"));
		artifacts.add(new Artifact("commons-codec", "1.14", "commons-codec/commons-codec/"));
		artifacts.add(new Artifact("commons-logging", "1.2", "commons-logging/commons-logging/"));
		
		/**where to deploy the Installer*/
		installerDirectory = IJ.getDirectory("plugins")+installerClassName+File.separatorChar;
		dependencies = new ArrayList<Dependency>();
		dependencyPath = getDependenciesPath();//installerDirectory;
		
		for (int i = 0; i<artifacts.size();i++) {
			dependencies.add(new Dependency (artifacts.get(i).getName(), artifacts.get(i).getVersion(), dependencyPath, artifacts.get(i).getURL()));
		}
		restartRequired = false;
		updateLoader = false;
		
	}

	@Override
	public void run(String arg) {
		if (arg.equals("about"))
		{showAbout(); return;}
		
		if(!IJ.isLinux() && !IJ.isWindows() && !IJ.isMacOSX()) {
			IJ.showMessage(msgTitle, "Unsupported operating system");
			return;
		}
		IJ.register(this.getClass());
		if(checkDependencies(false, false)){
//			if(restartRequired) {
//				Prefs.set("javacv.install_result_launcher", "restart required");
//				IJ.log("Please restart ImageJ to proceed with installation of necessary JavaCV libraries.");
//				IJ.showMessage(msgTitle, "Please restart ImageJ to proceed with installation of necessary JavaCV libraries.");
//				return;
//			}
			if(updateLoader || restartRequired) {
			
				
				restartRequired = true;
				Prefs.set("javacv.install_result_launcher", "restart required");
				IJ.log("Please restart ImageJ to proceed with installation of necessary JavaCV libraries.");
				IJ.showMessage(msgTitle, "Please restart ImageJ to proceed with installation of necessary JavaCV libraries.");
				
				return;
				
				
				
//				IJ.resetClassLoader();
//		        ClassLoader ijLoader = IJ.getClassLoader();
//		        String classDir;
//		        URLClassLoader loader = null;
//		        
//				//try {
//					//classDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
//					//IJ.log("UPDATE. classDir: " +classDir);
//					//if(classDir!=null && !classDir.isEmpty()) {
//			        	//URL classUrl;
//						try {
//							//classUrl = new File(classDir).toURI().toURL();
//							List<String> jars =jarsList(IJ.getDirectory("imagej") + "jars"+File.separator);
//							URL[] urls = new URL[jars.size()];
//							//urls[0] = classUrl;
//							for (int i = 0;i<urls.length;i++) {
//								urls[i] = new File(jars.get(i)).toURI().toURL();
//								IJ.log("NEW JAR: " +urls[i].toString());
//							}
//							loader = new URLClassLoader(urls, ijLoader);
//						} catch (MalformedURLException e) {
//							if(IJ.debugMode) { 
//								IJ.log(e.getLocalizedMessage());
//								IJ.log(e.toString());
//							}
//						}          
//			        //}
////				} catch (URISyntaxException e1) {
////					IJ.handleException(e1); //Marcel Boeglin 2013.09.01
////					return;
////				}
//		        
//		        if(loader==null) IJ.log("new loader IS NULL");
//		        
//		        Object installer = null;
//		        String className = installerPkg+"."+installerClassName;
//		        IJ.log("loader.getResources: "+loader.getResource("JavaCV_Installer").toString());
//		        try { 
//		            installer = loader.loadClass(className).newInstance(); 
//		            ((JavaCV_Installer)installer).run(arg);
//		        }
//		        catch (Exception e) {
//		            if (className.indexOf('_')!=-1)
//		                IJ.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
//		            IJ.handleException(e); 
//		        }

		        
			} else {
				restartRequired = false;
				Prefs.set("javacv.install_result_launcher", "success");
				JavaCV_Installer installer = new JavaCV_Installer();
				installer.run(arg);
			}
		}
		
	}
	
	public void showAbout() {
		IJ.showMessage("JavaCV_Installer", 
				"ImageJ plugin which helps to download and to install components of javacv\n"
				+ "package (java interface to OpenCV, FFmpeg and other) by Samuel Audet.\n"
				+ "     \n"
				+ "Other plugins which require javacv may use it to check if necessary libraries\n"
				+ "are installed and to install missing components of the required version.\n"
				+ "     \n"
				+ "The plugin creates a local repository in the directory \"local-maven-repo\"\n"
				+ "under ImageJ path to store files of various versions downloaded from the\n"
				+ "remote repository (a maven central). This allows to switch quickly between\n"
				+ "different javacv releases. This repository can be deleted at any time\n"
				+ "without consequences (next calls to the plugin will recreate the minimum\n"
				+ "required local repository).\n"
				+ "     \n"
				+ "The plugin also provides a helper function to check/install required \n"
				+ "parts of javacv from other user's plugins. \n"
				+ "     \n"
				+ "More information is available at \n"
				+ "https://forum.image.sc/t/new-javacv-installer-plugin/55392  "
			);
	}
	
	public static boolean isRestartRequired() {
		return restartRequired;
	}
	
    List<String> jarsList(String path) {
		ArrayList<String> result = new ArrayList<String>();
        String[] list = null;
        File f = new File(path);
        if (f.exists() && f.isDirectory())
            list = f.list();
        if (list==null)
            return result;
        boolean isJarsFolder = path.endsWith("jars")|| path.endsWith("lib");
        if (!path.endsWith(File.separator))
			path += File.separator;
        for (int i=0; i<list.length; i++) {
            File f2 = new File(path+list[i]);
            if (f2.isDirectory()) result.addAll(jarsList(path+list[i]));
            else if (list[i].endsWith(".jar")&&(!list[i].contains("_")||isJarsFolder)) {
            	result.add(path+list[i]);
            }
        }
        return result;
        
    }
	
	
	static class Artifact {
		private String name;
		private String version;
		private String urlRelPath;
		public Artifact (String name, String version, String urlRelPath){
			this.name = name;
			this.version = version;
			this.urlRelPath = urlRelPath;
		}
		public String getName(){
			return name;
		}
		public String getVersion(){
			return version;
		}
		public String getUrlRelPath(){
			return urlRelPath;
		}
		public String getJarName(){
			return name+"-"+version+".jar";
		}
		public String getURL(){
			return MAVEN_BASE_URL + urlRelPath + version+"/" + getJarName();
		}
	}
	
		
	
	static class Dependency {
		private String depFilename;
		private String depDirectory;
		private String depURL;
		private String depName;
		private String depVersion;
		
		public String getFileName(){
			return depFilename;
		}
		
		public String getDirectory(){
			return depDirectory;
		}
		
		public String getURL(){
			return depURL;
		}
		
		public String getDependencyName(){
			return depName;
		}
		
		public Dependency(String depName, String depVersion, String directory, String url) {
			this.depName = depName;
			this.depVersion = depVersion;
			this.depFilename = depName+"-"+depVersion+".jar";
			this.depDirectory = directory;
			this.depURL = url;
			
		}
		
		public boolean isInstalled() {
			if ((new File(depDirectory+depFilename)).exists()) return true; 
			DirectoryStream<Path> dirStream;
			try {
				dirStream = Files.newDirectoryStream(Paths.get(depDirectory), depName+"*.jar");
				for (Path path : dirStream){
					String name = path.getFileName().toString();
					String ver = getJarVersion(name);
					VerParser chkVer = new VerParser(ver);
					VerParser reqDepVer = new VerParser(depVersion);
					if(chkVer.compareTo(reqDepVer)>=0) return true;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
			
		}
		
				
		/**
		 * Download and install an artifact specified by the dependency 
		*/
		public boolean install() throws Exception {
			boolean success = false;
			if (!checkCreateDirectory(depDirectory)) return success;
		
			IJ.log("downloading " + depURL);
			InputStream is = null;
			URL url = null;
			try {
				url = new URL(depURL);
				URLConnection conn = url.openConnection();
				is = conn.getInputStream();
			} catch(MalformedURLException e1) {
				throw new Exception(depURL + " is not a valid URL");
			} catch(IOException e1) {
				throw new Exception("Can't open connection to " + depURL);
			}
			byte[] content = readFully(is);
			File out = new File(depDirectory, new File(url.getFile()).getName());
			IJ.log(" to " + out.getAbsolutePath());
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(out);
				fos.write(content);
				fos.close();
				success = true;
			} catch(FileNotFoundException e1) {
				throw new Exception("Could not open "
					+ out.getAbsolutePath() + " for writing. "
					+ "Maybe not enough permissions?");
			} catch(IOException e2) {
				throw new Exception("Error writing to "
					+ out.getAbsolutePath());
			}
			
			
			return success;
		}
	}
	
	static class VerParser implements Comparable<VerParser> {

	    private String version;

	    public final String get() {
	        return this.version;
	    }

	    public VerParser(String version) {
	        if(version == null)
	            throw new IllegalArgumentException("Version can not be null");
	        if(!version.matches("[0-9]+(\\.[0-9]+)*"))
	            throw new IllegalArgumentException("Invalid version format");
	        this.version = version;
	    }

	    @Override public int compareTo(VerParser that) {
	        if(that == null)
	            return 1;
	        String[] thisParts = this.get().split("\\.");
	        String[] thatParts = that.get().split("\\.");
	        int length = Math.max(thisParts.length, thatParts.length);
	        for(int i = 0; i < length; i++) {
	            int thisPart = i < thisParts.length ?
	                Integer.parseInt(thisParts[i]) : 0;
	            int thatPart = i < thatParts.length ?
	                Integer.parseInt(thatParts[i]) : 0;
	            if(thisPart < thatPart)
	                return -1;
	            if(thisPart > thatPart)
	                return 1;
	        }
	        return 0;
	    }

	    @Override public boolean equals(Object that) {
	        if(this == that)
	            return true;
	        if(that == null)
	            return false;
	        if(this.getClass() != that.getClass())
	            return false;
	        return this.compareTo((VerParser) that) == 0;
	    }

	}


	
	/** Checks if directory exists, creates it if not, and checks if we can write to it*/
	static boolean checkCreateDirectory(String path) {
		File directory = new File(path);
		if(!directory.exists() && !directory.mkdirs()) {
			IJ.log("Can't create folder "+path);
			IJ.showMessage(msgTitle, "Can't create folder\n"+path);
			return false;
		}
		if(!directory.canWrite()) {
			IJ.log("No permissions to write to folder "+path);
			IJ.showMessage(msgTitle, "No permissions to write to folder\n"+path);
			return false;
		}
		return true;
	}
	
	static String getJarVersion(String name)
	{
		String ver = name.replaceAll(".jar","").replaceAll("[a-zA-Z]","");
		while(ver.startsWith("-") ) ver = ver.substring(1);
		while(ver.endsWith("-") ) ver = ver.substring(0, ver.length()-1);
		int Dash = ver.indexOf("--");
		if (Dash>-1) ver = ver.substring(0,Dash);
		Dash = ver.indexOf("-");
		if (Dash>-1) ver = ver.substring(Dash+1);
		return ver;
	}
	
	/**
	 * Reads all bytes from the given InputStream and returns it as a
	 * byte array.
	 */
	private static byte[] readFully(InputStream is) throws Exception {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int c = 0;
		try {
			while((c = is.read()) != -1)
				buf.write(c);
			is.close();
		} catch(IOException e) {
			throw new Exception("Error reading from " + is);
		}
		return buf.toByteArray();
	}
	
	private static String getDependenciesPath(){
		
		char altSeparator = '/'== File.separatorChar?'\\':'/';
		String appPath = IJ.getDirectory("imagej").replace(altSeparator, File.separatorChar);
		String jarsPath = appPath+"jars"+ File.separatorChar;
		boolean fiji = false;
		
//		try {
//			Class<?> ijc = Class.forName("ij.ImageJ");
//			if(ijc==null) IJ.log("ij.ImageJ not found");
//			String classDir = new File(ijc.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
//			IJ.log("ij.ImageJ path: " +classDir);
//		} catch (ClassNotFoundException | URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		boolean jarstest = false;
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		URL[] urls = ((java.net.URLClassLoader) cl).getURLs();
//		for (URL url: urls) 
//			if (url.getFile().replace(altSeparator, File.separatorChar).contains(jarsPath)) {
//				jarstest = true;
//				break;
//			}
//		if (!jarstest) {
//		cl = IJ.getClassLoader();
//		urls = ((java.net.URLClassLoader) cl).getURLs();
//		for (URL url: urls) 
//			if (url.getFile().replace(altSeparator, File.separatorChar).contains(jarsPath)) {
//				jarstest = true;
//				break;
//			}
//		}
		
//		fiji = jarstest && (new File(appPath+"db.xml.gz").exists()) && IJ.getVersion().split("/").length>1;
		fiji = (new File(appPath+"db.xml.gz").exists()) && IJ.getVersion().split("/").length>1;
		
		if (fiji) return jarsPath;
		else return  IJ.getDirectory("plugins")+"jars"+File.separatorChar;

	}
	
	
	
	/**
	 * Returns true if all dependencies are found.
	 * Checks if all necessary dependencies are installed, 
	 * prompts to install if missing.
	 */
	private static boolean checkDependencies(boolean confirmRequired, boolean forceReinstall){
	
		
		
		
//		ArrayList<Dependency> dependencies = new ArrayList<Dependency>();
//		String installPath = GetDependenciesPath();//installerDirectory;
//		
//		for (int i = 0; i<artifacts.size();i++) {
//			dependencies.add(new Dependency (artifacts.get(i).getName(), artifacts.get(i).getVersion(), installPath, artifacts.get(i).getURL()));
//		}
				
		
		
		
		boolean installConfirmed = false, installed = true, installEvent = false;
		for(Dependency dep : dependencies) 
			if (forceReinstall || !dep.isInstalled()) {
				if (confirmRequired && !forceReinstall && !installConfirmed 
					&& !(installConfirmed = IJ.showMessageWithCancel(
											"Dependency check",
											"Not all required dependencies are installed.\n" +
											"Auto-install?"))) return false;
				
				try {
					if (!dep.install()) return false;
					else {
						updateLoader = installEvent = true;
						if (checkConflict(dependencyPath, dep)) {
							restartRequired = true;
						}
					}
				} catch (Exception e) {
					IJ.error(e.getMessage());
					IJ.log(e.getMessage());
					e.printStackTrace();
					installed = false;
				}
			}
			
			
		
			
		//if (installConfirmed || forceReinstall) {
//		if (installEvent) {
//			IJ.showMessage("JavaCV installer deployment", "Please restart ImageJ now");
//			IJ.log("ImageJ restart is required after javacv installation!");
//			restartRequired = true;
//		} else restartRequired = false;
		return installed;	
	}
		
	
	static boolean checkConflict(String path, Dependency dep) {
		boolean remove = false;
		try {
			String fileName = dep.getFileName();
			String depName = dep.getDependencyName();
			Set<String> jarFiles =filteredFileList(path, 
					new String[]{depName}, 
					new String[]{".jar"}, true);
			for (String jarFile : jarFiles) {
				if(Paths.get(jarFile).compareTo(Paths.get(path, fileName))!=0) {
					removeFile(jarFile);
					//IJ.log("Remove file "+jarFile+" CONFLICTING WITH "+Paths.get(path, fileName).toString());
					remove = true;
				}
			}
		} catch (Exception e) {
			if(IJ.debugMode) { 
				IJ.log(e.getLocalizedMessage());
				IJ.log(e.toString());
			}
		}
		return remove;
	}
	
	private static boolean removeFile(String fileToRemove) throws Exception {

		if(!(new File(fileToRemove)).exists()){
			return true;
		}
		Path path = Paths.get(fileToRemove);
		String imagejDirectory = IJ.getDirectory("imagej");
		String updateDirectory = imagejDirectory+"update"+File.separatorChar;
		String dstDirectory = updateDirectory+(path.getParent().toString()+File.separatorChar).substring(imagejDirectory.length());
		if (!checkCreateDirectory(dstDirectory)) return false;
		
		String dstPath = dstDirectory + path.getFileName();
		try {
			(new File(dstPath)).createNewFile();
			return true;
		} catch (IOException e) {
			IJ.log(e.toString());
			IJ.log("WARNING: Cannot write update folder for cleanup: "+dstPath);
			//Prefs.set("javacv.install_result", "cannot write update folder");
			return false;
		}
	}
	
	private static Set<String> filteredFileList(String dir, String[] names, String[] extensions, boolean recursive) throws IOException {
			
			if(!new File(dir).exists()) return new HashSet<String>();
			Stream<Path> stream = recursive?Files.walk(Paths.get(dir)).filter(Files::isRegularFile):
											Files.list(Paths.get(dir));
			return stream
					  .filter(file -> !Files.isDirectory(file))
					  .filter(file -> doesFileNameFit(file, names, true))
					  .filter(file -> doesFileNameFit(file, extensions, false))
					  .map(Path::toString)
					  .collect(Collectors.toSet());
		}
	
	private static boolean doesFileNameFit(Path file, String[] patterns, boolean starts) {
		if (patterns==null) return true;
		String name = file.getFileName().toString();
		boolean result = false;
		for(String patt : patterns) 
			result = result || patt == null || patt.isEmpty() || (starts?name.startsWith(patt):name.endsWith(patt));
		return result;
	}
			

}
