/*
 * Copyright (C) 2018-2021 Stanislav Chizhik
 * ImageJ/Fiji plugin which helps to download and to install components of javacv package 
 * (java interface to OpenCV, FFmpeg and other) by Samuel Audet.
 * Other plugins which require javacv may use it to check if necessary libraries are 
 * installed and to install missing components.
 */

package javacv_install;

import ij.IJ;
import ij.Macro;
import ij.Prefs;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.gui.*;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.eclipse.aether.*;
//import org.eclipse.aether.collection.CollectRequest;
//import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
//import org.eclipse.aether.artifact.*;
//import org.eclipse.aether.graph.*;
//import org.eclipse.aether.resolution.*;
//import org.eclipse.aether.transfer.*;
//import org.eclipse.aether.impl.DefaultServiceLocator;
//import org.eclipse.aether.repository.LocalRepository;
//import org.eclipse.aether.repository.RemoteRepository;
//import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
//import org.eclipse.aether.spi.connector.transport.TransporterFactory;
//import org.eclipse.aether.transport.file.FileTransporterFactory;
//import org.eclipse.aether.transport.http.HttpTransporterFactory;
//import org.eclipse.aether.util.artifact.JavaScopes;
//import org.eclipse.aether.util.filter.DependencyFilterUtils;
//import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
//import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
//import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
//import org.eclipse.aether.util.graph.transformer.ConflictResolver;
//
//import org.eclipse.aether.version.Version;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javacv_install.JavaCV_Installer_launcher.VerParser;

public class JavaCV_Installer implements PlugIn {

	private static RepositorySystem repSystem;
	private static CloseableSession repSession;
	private static List<RemoteRepository> repList;
	private static List<String> versions;
	private static String newestVersion;
	private static Map<String, List<JavaCVComponent>> compsByVer;
	private static String[] optionalCompNames;
	private static boolean[] compSelection;
	private static int compsPannelInd;
	private static boolean updateLine;
	private static boolean showInfoMsg;
	private static String installedJavaCVVersion;
	private static Set<String> installedComponents;
	private static Set<String> installedArtifacts;
	private static List<JavaCVDependency> dependencies;
	private static String installerDirectory;
	private static String imagejDirectory;
	private static String updateDirectory;
	private static String depsPath;
	private static String natLibsPath;
	private static String platformSpecifier;
	private static boolean restartRequired;
	private boolean beQuiet;

	// Installation constants

	private static final String INSTALLER_VERSION = "0.6.0";
	private static final String IMAGEJ_NAME = "imagej";
	private static final String PLATFORM_SUFFIX = "-platform";
	private static final String DIALOG_TITLE = "JavaCV installation";

	/** Base URL to the maven repository */
	private static final String BASE_REPO = "https://repo1.maven.org/maven2/"; // "https://repo.maven.apache.org/maven2/"

	/** Local maven repository path */
	private static final String LOCAL_REPO = "local-maven-repo";

	/** Platform specifier for the 32-bit windows */
	private static final String WIN_32 = "windows-x86";

	/** Platform specifier for the 64-bit windows */
	private static final String WIN_64 = "windows-x86_64";

	/** Platform specifier for the 32-bit linux */
	private static final String LIN_32 = "linux-x86";
	private static final String LIN_ARM_32 = "linux-armhf";

	/** Platform specifier for the 64-bit linux */
	private static final String LIN_64 = "linux-x86_64";
	private static final String LIN_ARM_64 = "linux-arm64";

	/** Platform specifier for the mac osx */
	private static final String MAC = "macosx-x86_64";
	private static final String MAC_ARM = "macosx-arm64";

	static {

		imagejDirectory = IJ.getDirectory(IMAGEJ_NAME);
		updateDirectory = imagejDirectory + "update" + File.separatorChar;
		boolean isarm = isARM();
		boolean is64 = IJ.is64Bit();
		if (IJ.isLinux())
			platformSpecifier = is64 ? (isarm ? LIN_ARM_64 : LIN_64) : (isarm ? LIN_ARM_32 : LIN_32);
		else if (IJ.isWindows())
			platformSpecifier = is64 ? WIN_64 : WIN_32;
		else if (IJ.isMacOSX())
			platformSpecifier = isarm ? MAC_ARM : MAC;
		repSystem = Booter.newRepositorySystem();
		repSession = Booter.newRepositorySystemSession(repSystem).build();
		repList = Booter.newRepositories();
		compsByVer = new HashMap<>();
		installedComponents = new HashSet<>();
		installedArtifacts = new HashSet<>();
		updateLine = false;
		showInfoMsg = false;
		restartRequired = false;
		compsPannelInd = -1;

		installerDirectory = IJ.getDirectory("plugins") + "JavaCV_Installer" + File.separator;

		// Where dependencies are looked for in Fiji or ImageJ
		getDependenciesPath();

		try {
			getAvailableVersions();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			readInstallCfg();
		} catch (SAXException | IOException | ParserConfigurationException e1) {
			log("Installation configuration is missing or incorrect");
			if (IJ.debugMode) {
				log(e1.getLocalizedMessage());
				log(e1.toString());
			}
		}

	}

	public static String getInstallerVersion() {
		return INSTALLER_VERSION;
	}

	public static boolean isRestartRequired() {
		return restartRequired;
	}

	static class JavaCVComponent {
		private String name;
		private String artifactName;
		private String version;

		public JavaCVComponent(String artifactName, String version) {
			this.artifactName = artifactName;
			this.name = artifactName.replace(PLATFORM_SUFFIX, "");
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public String getArtifactName() {
			return artifactName;
		}

		public String getVersion() {
			return version;
		}
	}

//	public static void main(String[] args) {
//		if(CheckJavaCV(null, null, true, false)){
//			log("javacv is installed");
//		}
//
//		else
//			log("javacv install failed or canceled");
//
//	}

	@Override
	public void run(String arg) {
		IJ.register(this.getClass());

		boolean canWriteToImageJ = new File(IJ.getDirectory(IMAGEJ_NAME)).canWrite();

		if (!canWriteToImageJ) {
			String path = IJ.getDirectory(IMAGEJ_NAME);
			String msg = "No write access: " + path;
			if (IJ.isMacOSX() && path != null && path.startsWith("/private/var/folders/")) {
				msg = "ImageJ is in a read-only folder due to Path Randomization.\n"
						+ "To work around this problem, drag ImageJ.app to another\n"
						+ "folder and then (optionally) drag it back.";
			}
			IJ.error("JavaCV Installer", msg);
			return;
		}

		String[] split = null;
		if (arg == null)
			arg = "";
		arg = arg.trim();
		if (!arg.isEmpty())
			split = arg.split(" ");

		if (split == null) {
			if (checkJavaCV(null, null, true, false)) {
				if (Macro.getOptions() == null) {
					log("javacv is installed");
					IJ.log("---------------------------------------------");
				}

			} else
				log("javacv install failed or canceled");
		} else {
			String version = "";
			String components = "";
			beQuiet = arg.indexOf("quiet") != -1;
			boolean force = arg.indexOf("force") != -1;
			for (String opt : split)
				if (opt.indexOf("components:") == 0 && opt.length() > 11) {
					components = opt.substring(11).replace(',', ' ');
					break;
				}
			for (String opt : split)
				if (opt.indexOf("version:") == 0 && opt.length() > 8) {
					version = opt.substring(8);
					break;
				}
			if (checkJavaCV(components, version, !beQuiet, force)) {
				if (Macro.getOptions() == null) {
					log("javacv is installed");
					IJ.log("---------------------------------------------");
				}

			} else
				log("javacv install failed or canceled");
		}
	}

	private static void readInstallCfg() throws SAXException, IOException, ParserConfigurationException {

		File xmlFile = new File(installerDirectory + "installcfg.xml");
//		IJ.log("xmlFile: "+xmlFile.getPath());
		if (!xmlFile.exists())
			return;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);

		doc.getDocumentElement().normalize();

		Node nVer = doc.getElementsByTagName("version").item(0);
		if (nVer == null || nVer.getTextContent().isEmpty()
				|| (versions != null && !versions.contains(nVer.getTextContent()))) {
			log("Incorrect install config file. Ignoring.");
			return;
		}

		installedJavaCVVersion = nVer.getTextContent();

		installedComponents.clear();
		NodeList nCompList = doc.getElementsByTagName("component");
		if (nCompList != null)
			for (int i = 0; i < nCompList.getLength(); i++) {

				Node nComp = nCompList.item(i);
				if (nComp.getNodeType() == Node.ELEMENT_NODE) {

					Element elem = (Element) nComp;
					installedComponents.add(elem.getAttribute("name"));
				}
			}

		installedArtifacts.clear();
		NodeList nArtList = doc.getElementsByTagName("file");
		if (nArtList != null && nArtList.getLength() > 0)
			for (int i = 0; i < nArtList.getLength(); i++) {

				Node nArt = nArtList.item(i);
				if (nArt.getNodeType() == Node.ELEMENT_NODE) {

					Element elem = (Element) nArt;
					installedArtifacts.add(elem.getAttribute("path"));
				}
			}
	}

	private static void writeInstallCfg() throws ParserConfigurationException, TransformerException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();

		Element root = doc.createElement("JavaCV-install-info");
		doc.appendChild(root);

		Element nVer = doc.createElement("version");
		nVer.appendChild(doc.createTextNode(installedJavaCVVersion));
		root.appendChild(nVer);

		Element nComps = doc.createElement("components");
		root.appendChild(nComps);

		for (String comp : installedComponents) {
			Element nComp = doc.createElement("component");
			nComp.setAttribute("name", comp);
			nComps.appendChild(nComp);
		}

		Element nArts = doc.createElement("files");
		root.appendChild(nArts);

		for (String art : installedArtifacts) {
			Element nArt = doc.createElement("file");
			nArt.setAttribute("path", art);
			nArts.appendChild(nArt);
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();

		transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transf.setOutputProperty(OutputKeys.INDENT, "yes");
		transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		DOMSource source = new DOMSource(doc);

		JavaCV_Installer_launcher.checkCreateDirectory(installerDirectory);
		File xmlFile = new File(installerDirectory + "installcfg.xml");
//		IJ.log("xmlFile: "+xmlFile.getPath());

		StreamResult file = new StreamResult(xmlFile.toURI().getPath());

		transf.transform(source, file);

	}

	private static void log(String message, boolean setUpdate) {
		IJ.log((updateLine ? "\\Update:" : "") + message);
		updateLine = setUpdate;
	}

	private static void log(String message) {
		log(message, false);
	}

	static class DuplicateFilter implements DependencyFilter {

		private Set<String> included;

		public DuplicateFilter() {
			this.included = new HashSet<>();
		}

		@Override
		public boolean accept(final DependencyNode node, final List<DependencyNode> parents) {
			String artifactName = node.getArtifact().toString();
			boolean newArtifact = !included.contains(artifactName);
			if (newArtifact)
				included.add(artifactName);
			return newArtifact;
		}

		@Override
		public String toString() {
			return "duplicateFilter{not included previously}";
		}

	}
//	static class InfoFilter implements DependencyFilter {
//
//		public InfoFilter() {
//		}
//
//		@Override
//		public boolean accept(final DependencyNode node, final List<DependencyNode> parents) {
//			String artifactName = node.getArtifact().toString();
//			
//			log("INFO FILTER|||"+artifactName+"|||"+parents.stream().map(DependencyNode::toString).collect(Collectors.toList()));
//			return true;
//		}
//
//		@Override
//		public String toString() {
//			return "infoFilter{artifact info}";
//		}
//	}

	/**
	 * A simplistic repository listener that logs events to the console.
	 */
	static class ConsoleRepositoryListener extends AbstractRepositoryListener {

		@Override
		public void artifactDescriptorInvalid(RepositoryEvent event) {
			log("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
		}

		@Override
		public void artifactDescriptorMissing(RepositoryEvent event) {
			log("Missing artifact descriptor for " + event.getArtifact());
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

	/**
	 * A simplistic transfer listener that logs uploads/downloads to the console.
	 */
	static class ConsoleTransferListener extends AbstractTransferListener {

		// private PrintStream out;

		private Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();

		private int lastLength;
		private long lastProcentage = -1L;
		private long lastTotal;

		@Override
		public void transferInitiated(TransferEvent event) {
			// String message = updateLine?"\\Update:":"";

			String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading"
					: "Downloading" + ": " + event.getResource().getRepositoryUrl()
							+ event.getResource().getResourceName();
//			if(!trInit) {
//				log("Downloading information required to verify JavaCV dependencies...");
//				trInit = true;
//			}

			if (!event.getResource().getResourceName().endsWith(".xml"))
				log(message, event.getResource().getResourceName().indexOf(".jar") == -1);

			// updateLine = event.getResource().getResourceName().indexOf(".jar")==-1;
			// if (event.getResource().getResourceName().indexOf(".jar")!=-1) {
			// updateLine = false;
			// } else {
			// updateLine = true;
			// }
		}

		@Override
		public void transferProgressed(TransferEvent event) {
			if (event.getResource().getResourceName().indexOf(".jar") != -1) {
				TransferResource resource = event.getResource();
				// if (resource.getContentLength() > 1024*1024)
				downloads.put(resource, event.getTransferredBytes());

				StringBuilder buffer = new StringBuilder(64);
				long maxtotal = 0;
				long maxtotcompl = 0;
				for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
					long total = entry.getKey().getContentLength();
					long complete = entry.getValue();
					if (total > maxtotal) {
						maxtotal = total;
						maxtotcompl = complete;
					}
					buffer.append(getStatus(complete, total)).append("  ");
				}

				int pad = lastLength - buffer.length();
				lastLength = buffer.length();
				pad(buffer, pad);
				// buffer.append( '\r' );
				long procentage = Math.round(Math.floor(maxtotal > 0 ? 100.0 * maxtotcompl / maxtotal : 0));
				if ((lastProcentage != procentage || lastTotal != maxtotal) && procentage % 10L == 0L) {
					String strBuff = buffer.toString().trim();
					if (!strBuff.isEmpty()) {
						log(strBuff, true);
						// updateLine = true;
					}

					// String strBuff = buffer.toString().trim();
					// if (!strBuff.isEmpty()) IJ.log( buffer.toString() );//(
					// "\\Update:"+buffer.toString() );
				}
				lastProcentage = procentage;
				lastTotal = maxtotal;

			}
		}

		private String getStatus(long complete, long total) {
			if (total >= 1024) {
				return toKB(complete) + "/" + toKB(total) + " KB ";
			} else if (total >= 0) {
				return complete + "/" + total + " B ";
			} else if (complete >= 1024) {
				return toKB(complete) + " KB ";
			} else {
				return complete + " B ";
			}
		}

		private void pad(StringBuilder buffer, int spaces) {
			String block = "                                        ";
			while (spaces > 0) {
				int n = Math.min(spaces, block.length());
				buffer.append(block, 0, n);
				spaces -= n;
			}
		}

		@Override
		public void transferSucceeded(TransferEvent event) {
			if (event.getResource().getResourceName().indexOf(".jar") != -1) {
				transferCompleted(event);

				TransferResource resource = event.getResource();
				long contentLength = event.getTransferredBytes();
				if (contentLength >= 0) {
					String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
					String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

					String throughput = "";
					long duration = Duration.between(resource.getStartTime(), Instant.now()).toMillis();
					if (duration > 0) {
						long bytes = contentLength - resource.getResumeOffset();
						DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
						double kbPerSec = (bytes / 1024.0) / (duration / 1000.0);
						throughput = " at " + format.format(kbPerSec) + " KB/sec";
					}

					log(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput
							+ ")");

				}
			}
		}

		@Override
		public void transferFailed(TransferEvent event) {
			transferCompleted(event);

			if (!(event.getException() instanceof MetadataNotFoundException)) {
				log(event.getException().toString());
			}
		}

		private void transferCompleted(TransferEvent event) {
			if (event.getResource().getResourceName().indexOf(".jar") != -1) {
				downloads.remove(event.getResource());

				StringBuilder buffer = new StringBuilder(64);
				pad(buffer, lastLength);
				String strBuff = buffer.toString().trim();
				if (!strBuff.isEmpty()) {
					log(strBuff, true);
				}
			}
		}

		@Override
		public void transferCorrupted(TransferEvent event) {
			log(event.getException().toString());
		}

		protected long toKB(long bytes) {
			return (bytes + 1023) / 1024;
		}

	}

	/**
	 * A factory for repository system instances that employs Maven Artifact
	 * Resolver's built-in service locator infrastructure to wire up the system's
	 * components.
	 */
//	static class ManualRepositorySystemFactory
//	{
//		private static final Logger LOGGER = LoggerFactory.getLogger( ManualRepositorySystemFactory.class );
//
//		public static RepositorySystem newRepositorySystem()
//		{
//			/*
//			 * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
//			 * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
//			 * factories.
//			 */
//			
//			@SuppressWarnings("deprecation")
//			DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
//			locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
//			locator.addService( TransporterFactory.class, FileTransporterFactory.class );
//			locator.addService( TransporterFactory.class, HttpTransporterFactory.class );
//
//			locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
//			{
//				@Override
//				public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
//				{
//					LOGGER.error( "Service creation failed for {} with implementation {}",
//							type, impl, exception );
//				}
//			} );
//
//			return locator.getService( RepositorySystem.class );
//		}
//
//	}

	/**
	 * A helper to boot the repository system and a repository system session.
	 */
//	static class Booter
//	{
//
//		public static RepositorySystem newRepositorySystem()
//		{
//			return ManualRepositorySystemFactory.newRepositorySystem();
//		}
//
//		public static DefaultRepositorySystemSession newRepositorySystemSession( RepositorySystem system )
//		{
//			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
//
//			LocalRepository localRepo = new LocalRepository( IJ.getDirectory("imagej")+LOCAL_REPO );
//			session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );
//
//			session.setTransferListener( new ConsoleTransferListener() );
//			session.setRepositoryListener( new ConsoleRepositoryListener() );
//
//			// uncomment to generate dirty trees
//			// session.setDependencyGraphTransformer( null );
//
//			return session;
//		}
//
//		public static List<RemoteRepository> newRepositories( RepositorySystem system, RepositorySystemSession session )
//		{
//			return new ArrayList<>( Collections.singletonList( newCentralRepository() ) );
//		}
//
//		private static RemoteRepository newCentralRepository()
//		{
//			return new RemoteRepository.Builder( "central", "default", BASE_REPO ).build();
//		}
//
//	}

	private static class Booter {
		public static RepositorySystem newRepositorySystem() {
			return SupplierRepositorySystemFactory.newRepositorySystem();
		}

		public static SessionBuilder newRepositorySystemSession(RepositorySystem system) {
			Path localRepoPath = FileSystems.getDefault().getPath(IJ.getDirectory(IMAGEJ_NAME) + LOCAL_REPO);
			return new SessionBuilderSupplier(system).get().setSystemProperties(System.getProperties())
					.withLocalRepositoryBaseDirectories(localRepoPath)
					.setRepositoryListener(new ConsoleRepositoryListener())
					.setTransferListener(new ConsoleTransferListener())
					.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true)
					.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true)
					.setSystemProperty("javacpp.platform", platformSpecifier);
		}

		public static List<RemoteRepository> newRepositories() {
			return new ArrayList<>(Collections.singletonList(newCentralRepository()));
		}

		private static RemoteRepository newCentralRepository() {
			return new RemoteRepository.Builder("central", "default", BASE_REPO).build();
		}
	}

	private static class SupplierRepositorySystemFactory {
		public static RepositorySystem newRepositorySystem() {
			return new RepositorySystemSupplier().get();
		}
	}

	/**
	 * Determines and return all available versions of an artifact.
	 */

	public static List<String> getAvailableVersions() throws Exception {

		if (versions != null && versions.size() != 0)
			return versions;
		/// version request
		Artifact artifact = new DefaultArtifact("org.bytedeco:javacv-platform:[0,)");

		VersionRangeRequest rangeRequest = new VersionRangeRequest();
		rangeRequest.setArtifact(artifact);
		rangeRequest.setRepositories(repList);

		VersionRangeResult rangeResult = repSystem.resolveVersionRange(repSession, rangeRequest);
		if (rangeResult != null) {
			versions = rangeResult.getVersions().stream().map(Version::toString).collect(Collectors.toList());
			newestVersion = rangeResult.getHighestVersion().toString();
			return versions;
		} else {
			return null;
		}

	}

	/**
	 * Determines and return all available versions of an artifact.
	 */

	public static String getNewestJavaCVVersion() throws Exception {

		if (newestVersion == null || newestVersion.isEmpty())
			getAvailableVersions();
		return newestVersion;
	}

	public static String getInstalledJavaCVVersion() {
		return installedJavaCVVersion;
	}

	public static List<String> getInstalledJavaCVComponents() {
		if (installedComponents == null)
			return null;
		return new ArrayList<>(installedComponents);
	}

	private static List<ArtifactResult> resolveDependencies(String reqVersion, List<String> reqComps) throws Exception {

		if (reqComps == null)
			reqComps = new ArrayList<>();

		List<JavaCVComponent> allJavaCVComps = getComponentsByVer(reqVersion);
		List<String> allComps = allJavaCVComps.stream().map(JavaCVComponent::getName).sorted()
				.collect(Collectors.toList());

		List<JavaCVComponent> reqJavaCVComps = new ArrayList<>();

		// collect set of all requested components with their versions
		for (JavaCVComponent comp : allJavaCVComps)
			if (reqComps.contains(comp.getName()))
				reqJavaCVComps.add(comp);

		// append all interdependencies
		if (showInfoMsg)
			log("Checking interdependencies...");
		if (new VerParser(reqVersion).compareTo(new VerParser("1.4.4")) > 0) {
			ArtifactDescriptorRequest dRequest = new ArtifactDescriptorRequest();
			dRequest.setRepositories(Booter.newRepositories());

			for (int i = 0; i < reqJavaCVComps.size(); i++) {
				JavaCVComponent reqComp = reqJavaCVComps.get(i);
				Artifact art = new DefaultArtifact(
						"org.bytedeco:" + reqComp.getArtifactName() + ":" + reqComp.getVersion());
				dRequest.setArtifact(art);
				if (showInfoMsg)
					log(art.toString() + "...", true);
				ArtifactDescriptorResult descriptorResult = repSystem.readArtifactDescriptor(repSession, dRequest);
				for (Dependency dependency : descriptorResult.getDependencies()) {
					String artId = dependency.getArtifact().getArtifactId();
					int suff = artId.indexOf(PLATFORM_SUFFIX);
					if (suff != -1) {
						String compname = artId.replace(PLATFORM_SUFFIX, "");
						if (allComps.contains(compname) && !reqComps.contains(compname)) {
							reqComps.add(compname);
							reqJavaCVComps.add(new JavaCVComponent(artId, dependency.getArtifact().getVersion()));
							if (showInfoMsg)
								log(reqComp.getName() + " depends on " + compname);
						}
					}
				}
			}
		}
		if (showInfoMsg)
			log("Final list of required components: " + reqComps);

		// set resolve filters
		allComps.removeAll(reqComps);
		Set<String> exclusions = new HashSet<>();
		for (String comp : allComps)
			exclusions.add("*:" + comp + "*:");
		PatternExclusionsDependencyFilter exclusionFilter = new PatternExclusionsDependencyFilter(exclusions);
		PatternInclusionsDependencyFilter inclusionFilter = new PatternInclusionsDependencyFilter("*bytedeco*:*:*:*");
		// DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(
		// JavaScopes.COMPILE );
		// DependencyFilter filter = DependencyFilterUtils.andFilter(classpathFlter,
		// inclusionFilter, exclusionFilter, new DuplicateFilter());
		DependencyFilter filter = DependencyFilterUtils.andFilter(inclusionFilter, exclusionFilter,
				new DuplicateFilter());
		// set collect request
		CollectRequest collectRequest = new CollectRequest();
		Artifact artifact = new DefaultArtifact("org.bytedeco:javacv-platform:" + reqVersion);
		collectRequest.setRoot(new Dependency(artifact, "compile"));
		collectRequest.setRepositories(repList);
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
		// resolve!
		if (showInfoMsg)
			log("Resolving dependencies...");
		DependencyResult depRes = repSystem.resolveDependencies(repSession, dependencyRequest);
		if (showInfoMsg && depRes != null)
			log("Done");
		List<ArtifactResult> gplList;
		List<ArtifactResult> depList;
		depList = new ArrayList<>(new HashSet<>(depRes.getArtifactResults()));

		// check if ffmpeg-gpl can be used instead ffmpeg
		DependencyResult depRes_gpl = null;
		if (reqComps.contains("ffmpeg") && new VerParser(reqVersion).compareTo(new VerParser("1.5.4")) > 0) {
			if (showInfoMsg)
				log("Resolving optional ffmpeg-gpl dependencies...");
			String ffmpegVersion = "";
			for (ArtifactResult artifactResult : depRes.getArtifactResults()) {
				if (artifactResult.getArtifact().getArtifactId().indexOf("ffmpeg-platform") != -1) {
					ffmpegVersion = artifactResult.getArtifact().getVersion();
					break;
				}
			}
			if (!ffmpegVersion.isEmpty()) {
				artifact = new DefaultArtifact("org.bytedeco:ffmpeg-platform-gpl:" + ffmpegVersion);
				collectRequest.setRoot(new Dependency(artifact, null));
				collectRequest.setRepositories(repList);
				// DependencyFilter gplFilter = DependencyFilterUtils.andFilter(classpathFlter,
				// inclusionFilter, new DuplicateFilter());
				DependencyFilter gplFilter = DependencyFilterUtils.andFilter(inclusionFilter, new DuplicateFilter());
				dependencyRequest = new DependencyRequest(collectRequest, gplFilter);
				depRes_gpl = repSystem.resolveDependencies(repSession, dependencyRequest);
				if (!depRes_gpl.getArtifactResults().isEmpty()) {
					if (showInfoMsg)
						log("Optional ffmpeg-gpl dependencies are resolved");
					Predicate<ArtifactResult> ffmpegPlatformGpl = x -> x.getArtifact().getArtifactId()
							.equals("ffmpeg-platform-gpl");
					Predicate<ArtifactResult> natLibGpl = x -> x.getArtifact().getClassifier()
							.equals(platformSpecifier + "-gpl");
					if (depRes_gpl.getArtifactResults().stream().anyMatch(ffmpegPlatformGpl)
							&& depRes_gpl.getArtifactResults().stream().anyMatch(natLibGpl)) {
						List<ArtifactResult> removesList = new ArrayList<>();
						for (ArtifactResult artifactResult : depList) {
							if (artifactResult.getArtifact().getArtifactId().equals("ffmpeg-platform")
									|| (artifactResult.getArtifact().getArtifactId().equals("ffmpeg") && artifactResult
											.getArtifact().getClassifier().equals(platformSpecifier))) {
								removesList.add(artifactResult);
							}
						}
						depList.removeAll(removesList);
					}
				} else if (showInfoMsg)
					log("No optional gpl dependencies resolved");
			} else if (showInfoMsg)
				log("ffmpeg version is not found");
		}

		if (depRes_gpl != null) {
			gplList = new ArrayList<>(new HashSet<>(depRes_gpl.getArtifactResults()));
			depList.addAll(gplList);
		}
		// check and remove duplicates and version conflicts
		Map<String, ArtifactResult> mArtResults = new HashMap<>();
		for (ArtifactResult artifactResult : depList) {
			Artifact art = artifactResult.getArtifact();
			String artName = art.getArtifactId() + (!art.getClassifier().isEmpty() ? "-" + art.getClassifier() : "");
			if (!mArtResults.containsKey(artName))
				mArtResults.put(artName, artifactResult);
			else {
				String artVer = art.getVersion();
				String artJavacvVer = "";
				String artCompVer = "";
				int dash = artVer.indexOf('-');
				if (dash > -1) {
					artJavacvVer = artVer.substring(dash + 1);
					artCompVer = artVer.substring(0, dash);
				} else
					artJavacvVer = artVer;
				Artifact selArt = mArtResults.get(artName).getArtifact();
				String selArtVer = selArt.getVersion();
				String selArtJavacvVer = "";
				String selArtCompVer = "";
				dash = selArtVer.indexOf('-');
				if (dash > -1) {
					selArtJavacvVer = selArtVer.substring(dash + 1);
					selArtCompVer = selArtVer.substring(0, dash);
				} else
					selArtJavacvVer = selArtVer;
				int verCompare = new VerParser(artJavacvVer).compareTo(new VerParser(selArtJavacvVer));
				if (verCompare > 0 || (verCompare == 0 && !selArtCompVer.isEmpty() && !artCompVer.isEmpty()
						&& new VerParser(artCompVer).compareTo(new VerParser(selArtCompVer)) > 0))
					mArtResults.put(artName, artifactResult);

			}
		}
		return new ArrayList<>(mArtResults.values());
	}

	static class JavaCVDependency {
		private String depFilename;
		private String depDirectory;
		private String srcPath;

		public JavaCVDependency(String filename, String directory, String srcPath) {
			this.depFilename = filename;
			this.depDirectory = directory;
			this.srcPath = srcPath;
		}

		public String getName() {
			return depFilename;
		}

		public String getDirectory() {
			return depDirectory;
		}

		public boolean isInstalled() {
			return (new File(depDirectory + depFilename)).exists();
		}

		/**
		 * Install a JavaCV component specified by the dependency
		 */
		public boolean install() throws Exception {

			if (!(new File(srcPath)).exists()) {
				log("Source file not found " + srcPath);
				if (showInfoMsg)
					IJ.showMessage(DIALOG_TITLE, "Source file not found\n" + srcPath);
				Prefs.set("javacv.install_result", "source file not found");
				return false;
			}

			String dstDirectory = updateDirectory + depDirectory.substring(imagejDirectory.length());
			File directory = new File(dstDirectory);

			if (!directory.exists() && !directory.mkdirs()) {
				log("Can't create folder " + dstDirectory);
				if (showInfoMsg)
					IJ.showMessage(DIALOG_TITLE, "Can't create folder\n" + dstDirectory);
				Prefs.set("javacv.install_result", "cannot create update folder");
				return false;
			}
			if (!directory.canWrite()) {
				log("No permissions to write to folder " + dstDirectory);
				if (showInfoMsg)
					IJ.showMessage(DIALOG_TITLE, "No permissions to write to folder\n" + depDirectory);
				Prefs.set("javacv.install_result", "cannot write update folder");
				return false;
			}
			String dstPath = dstDirectory + depFilename;
			Files.copy(Paths.get(srcPath), Paths.get(dstPath), StandardCopyOption.REPLACE_EXISTING);

			return true;
		}

		/**
		 * Remove JavaCV component specified by the dependency
		 */
		public boolean remove() throws Exception {

			if (!(new File(depDirectory + depFilename)).exists()) {
				return true;
			}

			String dstDirectory = updateDirectory + depDirectory.substring(imagejDirectory.length());
			File directory = new File(dstDirectory);

			if (!directory.exists() && !directory.mkdirs()) {
				log("Can't create folder " + dstDirectory);
				if (showInfoMsg)
					IJ.showMessage(DIALOG_TITLE, "Can't create folder\n" + dstDirectory);
				Prefs.set("javacv.install_result", "cannot create update folder");
				return false;
			}
			if (!directory.canWrite()) {
				log("No permissions to write to folder " + dstDirectory);
				if (showInfoMsg)
					IJ.showMessage(DIALOG_TITLE, "No permissions to write to folder\n" + dstDirectory);
				Prefs.set("javacv.install_result", "cannot write update folder");
				return false;
			}
			String dstPath = dstDirectory + depFilename;
			try {
				(new File(dstPath)).createNewFile();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				Prefs.set("javacv.install_result", "cannot write update folder");
				return false;
			}
		}
	}

	@SuppressWarnings("serial")
	static class ConfirmDialog extends Dialog implements ActionListener, KeyListener, WindowListener {

		private Button ok;
		private Button cancel;
		private MultiLineLabel label;
		private boolean wasCanceled;
		private boolean wasOKed;

		public ConfirmDialog(String title, String message) {
			super((Frame) null, title, true);
			setLayout(new BorderLayout());
			if (message == null)
				message = "";
			Font font = null;
			double scale = Prefs.getGuiScale();
			if (scale > 1.0) {
				font = getFont();
				if (font != null)
					font = font.deriveFont((float) (font.getSize() * scale));
				else
					font = new Font("SansSerif", Font.PLAIN, (int) (12 * scale));
				setFont(font);
			}
			label = new MultiLineLabel(message);
			if (font != null)
				label.setFont(font);
			else if (!IJ.isLinux())
				label.setFont(new Font("SansSerif", Font.PLAIN, 14));
			Panel panel = new Panel();
			panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
			panel.add(label);
			add("Center", panel);
			ok = new Button("  OK  ");
			ok.addActionListener(this);
			ok.addKeyListener(this);
			cancel = new Button("  CANCEL  ");
			cancel.addActionListener(this);
			cancel.addKeyListener(this);
			panel = new Panel();
			panel.setLayout(new FlowLayout());
			panel.add(ok);
			panel.add(cancel);
			add("South", panel);
			if (ij.IJ.isMacintosh())
				setResizable(false);
			pack();
			ok.requestFocusInWindow();
			GUI.centerOnImageJScreen(this);
			addWindowListener(this);
			setVisible(true);
		}

		/** Returns true if the user clicked on "Cancel". */
		public boolean wasCanceled() {
			return wasCanceled;
		}

		/** Returns true if the user has clicked on "OK" or a macro is running. */
		public boolean wasOKed() {
			return wasOKed;
		}

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == ok || source == cancel) {
				wasCanceled = source == cancel;
				wasOKed = source == ok;
				dispose();
			}
		}

		public void windowClosing(WindowEvent e) {
			wasCanceled = true;
			dispose();
		}

		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			IJ.setKeyDown(keyCode);

			if (keyCode == KeyEvent.VK_ENTER) {
				wasOKed = true;
				dispose();
			} else if (keyCode == KeyEvent.VK_ESCAPE) {
				wasCanceled = true;
				dispose();
				IJ.resetEscape();
			}
		}

		public void keyReleased(KeyEvent e) {
			int keyCode = e.getKeyCode();
			IJ.setKeyUp(keyCode);
		}

		public void keyTyped(KeyEvent e) {
		}

		public void windowActivated(WindowEvent e) {
		}

		public void windowOpened(WindowEvent e) {
		}

		public void windowClosed(WindowEvent e) {
		}

		public void windowIconified(WindowEvent e) {
		}

		public void windowDeiconified(WindowEvent e) {
		}

		public void windowDeactivated(WindowEvent e) {
		}

	}

	private static void getDependenciesPath() {
		char altSeparator = '/' == File.separatorChar ? '\\' : '/';
		String appPath = IJ.getDirectory(IMAGEJ_NAME).replace(altSeparator, File.separatorChar);
		String fijiJarsPath = appPath + "jars" + File.separatorChar;
		String ijJarsPath = IJ.getDirectory("plugins") + "jars" + File.separatorChar;
		boolean fiji = false;

//		boolean jarstest = false;
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		URL[] urls = ((java.net.URLClassLoader) cl).getURLs();
//		for (URL url: urls) 
//			if (url.getFile().replace(altSeparator, File.separatorChar).contains(fijiJarsPath)) {
//				jarstest = true;
//				break;
//			}
//		
//		if (!jarstest) {
//		cl = IJ.getClassLoader();
//		urls = ((java.net.URLClassLoader) cl).getURLs();
//		for (URL url: urls) 
//			if (url.getFile().replace(altSeparator, File.separatorChar).contains(fijiJarsPath)) {
//				jarstest = true;
//				break;
//			}
//		}

//		fiji = jarstest && (new File(appPath+"db.xml.gz").exists())  && IJ.getVersion().split("/").length>1;
		fiji = (new File(appPath + "db.xml.gz").exists()) && IJ.getVersion().split("/").length > 1;

		if (fiji) {
			depsPath = fijiJarsPath;
			natLibsPath = depsPath + (IJ.isLinux() ? (IJ.is64Bit() ? "linux64" : "linux32")
					: (IJ.isWindows() ? (IJ.is64Bit() ? "win64" : "win32") : "macosx")) + File.separator;
		} else {
			depsPath = ijJarsPath;
			natLibsPath = depsPath;
		}
	}

	public static List<JavaCVComponent> getComponentsByVer(String version) throws Exception {

		List<JavaCVComponent> result = compsByVer.get(version);
		if (result == null) {
			if (versions == null || versions.isEmpty()) {
				log("Information about JavaCV versions is not available");
				return null;
			} else if (!versions.contains(version)) {
				log("Requested JavaCV version (" + version + ") is unknown");
				return null;
			}
			result = new ArrayList<>();
			Artifact artifact = new DefaultArtifact("org.bytedeco:javacv-platform:" + version);
			ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
			descriptorRequest.setArtifact(artifact);
			descriptorRequest.setRepositories(repList);

			ArtifactDescriptorResult descriptorResult = repSystem.readArtifactDescriptor(repSession, descriptorRequest);
			for (Dependency dependency : descriptorResult.getDependencies()) {
				String scope = dependency.getScope();
				String aId = dependency.getArtifact().getArtifactId();
				if (!(scope.isEmpty() || scope.equalsIgnoreCase("compile")) || aId.equals("javacpp-platform"))
					continue;
				int suff = aId.indexOf(PLATFORM_SUFFIX);
				if (suff != -1) {
					result.add(new JavaCVComponent(aId, dependency.getArtifact().getVersion()));
				}
			}
			compsByVer.put(version, result);

		}
		return result;
	}

	private static boolean isInstalledVersionValid() {
		return installedJavaCVVersion != null && !installedJavaCVVersion.isEmpty() && versions != null
				&& !versions.isEmpty() && versions.contains(installedJavaCVVersion);
	}

	private static boolean doesInstalledVersionMeet(String version, boolean treatAsMinVer) {
		if (isInstalledVersionValid()) {
			if (treatAsMinVer)
				return new VerParser(version).compareTo(new VerParser(installedJavaCVVersion)) <= 0;
			else
				return new VerParser(version).compareTo(new VerParser(installedJavaCVVersion)) == 0;
		}
		return false;
	}

	static boolean isFileConflicting(Path path, String reqVersion) {
		String name = path.getFileName().toString();
		String platformName = platformSpecifier.substring(0, platformSpecifier.indexOf("-"));
		// GenericVersionScheme gvs = new GenericVersionScheme();
		int platformIndex = name.indexOf(platformName);
		if (platformIndex > 0) {
			String check64bit = name.substring(platformIndex + platformName.length() + 4);
			boolean is64bit = check64bit.startsWith("_64");
			if ((is64bit && !IJ.is64Bit()) || (!is64bit && IJ.is64Bit())) {
				return true;
			}
		}
		String ver = JavaCV_Installer_launcher.getJarVersion(name);
		VerParser chkVer = new VerParser(ver);
		return ((!isInstalledVersionValid() || chkVer.compareTo(new VerParser(installedJavaCVVersion)) != 0)
				&& chkVer.compareTo(new VerParser(reqVersion)) != 0);
	}

	/**
	 * Returns true if video import plugin can run. Checks if all necessary
	 * dependencies are installed, prompts to install if missing. It assumes that
	 * the currently installed version is acceptable (or the newest if none is
	 * installed).
	 */
	public static boolean checkJavaCV(String reqCompNames, boolean showOptDlg, boolean forceReinstall) {
		return checkJavaCV(reqCompNames, null, showOptDlg, forceReinstall);
	}

	/**
	 * Returns true if video import plugin can run. Checks if all necessary
	 * dependencies are installed, prompts to install if missing. Options dialog is
	 * not displayed. Only missing files are installed.
	 */
	public static boolean checkJavaCV(String reqCompNames, String reqVersion) {
		return checkJavaCV(reqCompNames, reqVersion, false, false);
	}

	/**
	 * Returns true if video import plugin can run. Checks if all necessary
	 * dependencies are installed, prompts to install if missing. It assumes that
	 * the currently installed version is acceptable (or the newest if none is
	 * installed). Options dialog is not displayed. Only missing files are
	 * installed.
	 */
	public static boolean checkJavaCV(String reqCompNames) {
		return checkJavaCV(reqCompNames, null, false, false);
	}

	/**
	 * Returns true if video import plugin can run. Checks if all necessary
	 * dependencies are installed, prompts to install if missing. Minimal required
	 * version is specified. Options dialog is not displayed. Only missing files are
	 * installed.
	 */
	public static boolean checkMinJavaCV(String reqCompNames, String minVersion) {
		if (isInstalledVersionValid()
				&& new VerParser(minVersion).compareTo(new VerParser(installedJavaCVVersion)) <= 0) {
			if (showInfoMsg)
				log("Installed JavaCV version is acceptable");
			return checkJavaCV(reqCompNames);
		}
		return checkJavaCV(reqCompNames, minVersion);
	}

	/**
	 * Returns true if video import plugin can run. Checks if all necessary
	 * dependencies are installed, prompts to install if missing.
	 */
	public static boolean checkJavaCV(String reqCompNames, String reqVersion, boolean showOptDlg,
			boolean forceReinstall) {

		String messageTitle = "JavaCV dependency check";
		String autoInstallMsg = "Not all required JavaCV dependencies are installed.\nAuto-install?";
		String minVerLabel = "Treat_selected_version_as_minimal_required";
		String versionChoiceLabel = "Version";
		boolean macroConfirmed = false;

		if (restartRequired) {
			IJ.showMessage(messageTitle, "ImageJ must be restarted after previuos install operation!");
			Prefs.set("javacv.install_result", "restart required");
			return false;
		}

		if (!IJ.isLinux() && !IJ.isWindows() && !IJ.isMacOSX()) {
			IJ.showMessage(messageTitle, "Unsupported operating system");
			Prefs.set("javacv.install_result", "unsupported operating system");
			return false;
		}

		if (versions == null || versions.isEmpty()) {
			IJ.showMessage(messageTitle,
					"Information about avalable javacv vesions cannot be obtained for some reason.");
			Prefs.set("javacv.install_result", "no information about available versions");
			return false;
		}

		String macroOptions = Macro.getOptions();
		if (macroOptions != null)
			reqVersion = Macro.getValue(macroOptions, versionChoiceLabel, "");

		showInfoMsg = showOptDlg && macroOptions == null;

		if (showInfoMsg) {
			log("JavaCV installation config:");
			log("installed version - " + installedJavaCVVersion);
			log("installed components - " + installedComponents);
			log("Available javacv versions - " + versions);
		}

		if (reqVersion == null || reqVersion.isEmpty())
			reqVersion = isInstalledVersionValid() ? installedJavaCVVersion : newestVersion;
		if (!versions.contains(reqVersion)) {
			String supposedVer = isInstalledVersionValid() ? installedJavaCVVersion : newestVersion;
			String msg = "The requested JavaCV version (" + reqVersion + ") is unknown. ";
			String msg1 = "Proceed with the "
					+ (supposedVer.equalsIgnoreCase(newestVersion) ? "newest version (" + newestVersion
							: "current version (" + installedJavaCVVersion)
					+ ")?";
			ConfirmDialog cd = new ConfirmDialog(messageTitle, msg + msg1);

			if (cd.wasOKed()) {
				reqVersion = supposedVer;
				if (macroOptions != null) {
					String optVersion = Macro.getValue(macroOptions, "version", "");
					if (!optVersion.isEmpty()) {
						int verind = macroOptions.indexOf(optVersion);
						macroOptions = macroOptions.substring(0, verind) + reqVersion
								+ macroOptions.substring(verind + optVersion.length());
					}
					Macro.setOptions(macroOptions);
				}

			} else {
				Prefs.set("javacv.install_result", "version is not available");
				return false;
			}
		}

		List<String> optionalCompList = new ArrayList<>();
		try {
			optionalCompList = getComponentsByVer(reqVersion).stream().map(JavaCVComponent::getName).sorted()
					.collect(Collectors.toList());
		} catch (Exception e3) {
			if (IJ.debugMode) {
				log(e3.getLocalizedMessage());
				log(e3.toString());
			}
		}

		// If macro is running check if the installed version meets the requested
		// version
		if (macroOptions != null) {
			boolean treatAsMinVer = macroOptions.indexOf(minVerLabel.toLowerCase(Locale.US)) > -1;
			if (!doesInstalledVersionMeet(reqVersion, treatAsMinVer)) {
				if (isInstalledVersionValid()) {
					ConfirmDialog cd = new ConfirmDialog(messageTitle, "JavaCV version (" + reqVersion
							+ ") is requested, which is different from the installed (" + installedJavaCVVersion
							+ ").\n"
							+ "Continue with the installation of the requested version (the current version will be uninstalled)?");
					if (!cd.wasOKed()) {
						Prefs.set("javacv.install_result", "canceled");
						return false;
					}
					macroConfirmed = true;
				} else {
//					if (showInfoMsg) {
//						ConfirmDialog cd = new ConfirmDialog( messageTitle, autoInstallMsg);
//						if(!cd.wasOKed()) {
//							Prefs.set("javacv.install_result", "canceled");
//							return false;
//						}
//					}
					macroConfirmed = true;
				}
				if (!macroConfirmed && installedComponents != null && !installedComponents.isEmpty()) {
					for (String comp : optionalCompList) {
						if (macroOptions.indexOf(comp) > -1 && !installedComponents.contains(comp)) {
//							if (showInfoMsg) {
//								ConfirmDialog cd = new ConfirmDialog( messageTitle, autoInstallMsg);
//								if(!cd.wasOKed()) {
//									Prefs.set("javacv.install_result", "canceled");
//									return false;
//								}
//							}
							macroConfirmed = true;
							break;
						}
					}
				}

			}
		}

		optionalCompNames = optionalCompList.toArray(new String[0]);
		compSelection = new boolean[optionalCompNames.length];

		if (reqCompNames != null) {
			String[] deps = reqCompNames.split("[ ]+");
			if (deps.length > 0) {
				for (String dep : deps) {
					int compIndex = optionalCompList.indexOf(dep);
					if (compIndex > -1)
						compSelection[compIndex] = true;
					else
						log("Component (" + dep + ") is not available in version " + reqVersion);
				}
			}
		}

		boolean treatAsMinVer = false;
		if (showOptDlg) {
			GenericDialog gd = new GenericDialog("JavaCV installation options");

			String[] versionsArr = new String[versions.size()];
			versionsArr = versions.toArray(versionsArr);
			gd.addMessage("Currently installed: " + (isInstalledVersionValid() ? installedJavaCVVersion : "none")
					+ "\nSelect required version");
			gd.addChoice(versionChoiceLabel, versionsArr, reqVersion);
			final Choice versionChoice = ((Choice) gd.getChoices().elementAt(0));
			Panel optPanelParent = new Panel();
			versionChoice.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					for (Component cb : ((Panel) optPanelParent.getComponent(0)).getComponents())
						cb.setEnabled(false);

					String selectedVersion = versionChoice.getSelectedItem();
					log("Requesting components avalable in version " + selectedVersion + "...", true);
					try {
						optionalCompNames = getComponentsByVer(selectedVersion).stream().map(JavaCVComponent::getName)
								.sorted().collect(Collectors.toList()).toArray(new String[0]);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					if (showInfoMsg)
						log("Components available in " + selectedVersion + ": " + Arrays.asList(optionalCompNames));
					compSelection = new boolean[optionalCompNames.length];
					for (int i = 0; i < compSelection.length; i++)
						compSelection[i] = false;
					optPanelParent.removeAll();
					compsPannelInd = gd.getComponentCount();
					gd.addCheckboxGroup(5, 5, optionalCompNames, compSelection);
					Panel optPanel = (Panel) gd.getComponent(compsPannelInd);
					optPanelParent.add(optPanel);
					gd.pack();
				}
			});

			String[] installOptions = new String[] { "Install missing", "Reinstall selected" };
			gd.addRadioButtonGroup("Select_installation_option", installOptions, 2, 1,
					forceReinstall ? installOptions[1] : installOptions[0]);
			gd.addCheckbox(minVerLabel, false);
			gd.addMessage("Select necessary packages");

			try {
				optionalCompNames = getComponentsByVer(versionChoice.getSelectedItem()).stream()
						.map(JavaCVComponent::getName).sorted().collect(Collectors.toList()).toArray(new String[0]);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (showInfoMsg)
				log("Components available in " + versionChoice.getSelectedItem() + ": "
						+ Arrays.asList(optionalCompNames));
			compSelection = new boolean[optionalCompNames.length];
			for (int i = 0; i < compSelection.length; i++)
				compSelection[i] = false;

			gd.addPanel(optPanelParent);

			compsPannelInd = gd.getComponentCount();
			gd.addCheckboxGroup(5, 5, optionalCompNames, compSelection);
			Panel optPanel = (Panel) gd.getComponent(compsPannelInd);
			optPanelParent.add(optPanel);
			gd.pack();
			gd.showDialog();

			if (gd.wasCanceled()) {
				Prefs.set("javacv.install_result", "canceled");
				return false;
			}

			reqVersion = gd.getNextChoice();
			if (gd.getNextRadioButton().equals(installOptions[1]))
				forceReinstall = true;
			treatAsMinVer = gd.getNextBoolean();
			Panel optpan = (Panel) optPanelParent.getComponent(0);
			for (int i = 0; i < compSelection.length; i++) {
				if (macroOptions == null) {
					Component cb = optpan.getComponent(i);
					if (IJ.isLinux())
						cb = ((Panel) cb).getComponent(0);
					compSelection[i] = ((Checkbox) cb).getState();
				} else {

					compSelection[i] = macroOptions.indexOf(optionalCompNames[i]) > -1;
				}
				if (Recorder.record && compSelection[i]) {
					Recorder.recordOption(optionalCompNames[i]);
				}
			}

		}

		if (doesInstalledVersionMeet(reqVersion, treatAsMinVer)) {
			if (showInfoMsg)
				log("The installed JavaCV version meets the minimum requirements");
			reqVersion = installedJavaCVVersion;
		}

		List<String> reqComps = new ArrayList<>();
		for (int i = 0; i < compSelection.length; i++)
			if (compSelection[i])
				reqComps.add(optionalCompNames[i]);
		if (showInfoMsg) {
			log("Requested JavaCV version: " + reqVersion);
			log("Requested components: " + reqComps);
		}

		List<ArtifactResult> artifactResults = new ArrayList<>();
		try {
			artifactResults = resolveDependencies(reqVersion, reqComps);
			if (artifactResults == null) {
				log("Dependencies are not resolved for some reason. The presence of the required files could not be verified.");
				Prefs.set("javacv.install_result", "dependencies are not resolved");
				return false;
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		if (showInfoMsg)
			log("Resolved dependencies:");
		dependencies = new ArrayList<>();
		for (ArtifactResult artifactResult : artifactResults) {
			Artifact artifact = artifactResult.getArtifact();
			String srcPath = artifact.getPath().toString();
			String fileName = artifact.getPath().getFileName().toString();
			String dstDir = artifact.getClassifier().isEmpty() ? depsPath : natLibsPath;
			dependencies.add(new JavaCVDependency(fileName, dstDir, srcPath));
			if (showInfoMsg)
				log(artifact.toString() + " resolved to " + srcPath);
		}
		if (showInfoMsg)
			log(" ");

		boolean installConfirmed = false;
		boolean installed = true;

		if (isInstalledVersionValid() && !reqVersion.equalsIgnoreCase(installedJavaCVVersion)) {
			String msg = "The current installed JavaCV version (" + installedJavaCVVersion + ") will be changed to "
					+ reqVersion;
			if (!macroConfirmed) {
				ConfirmDialog cd = new ConfirmDialog(messageTitle, msg + ".\nContinue?");
				if (!(installConfirmed = cd.wasOKed())) {
					Prefs.set("javacv.install_result", "canceled");
					return false;
				}
			}

			log(msg);
			log("Marking files for deletion on ImageJ restart...");

			for (String art : installedArtifacts) {

				log(art);
				Path artPath = Paths.get(art);
				try {
					if (!new JavaCVDependency(artPath.getFileName().toString(),
							artPath.getParent().toString() + File.separator, null).remove()) {
						return false;
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					log("Cannot mark file " + artPath.getFileName().toString() + " for removal");
					log("Install operations are rejected");
					try {
						Files.walk(Paths.get(updateDirectory)).sorted(Comparator.reverseOrder()).map(Path::toFile)
								.forEach(File::delete);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Prefs.set("javacv.install_result", "cannot remove previous installation");
					return false;
				}

			}
			installedArtifacts.clear();
			installedComponents.clear();
			log(" ");
			log("Installing selected version...");
			IJ.log("=======================================");
		}

		Set<String> newInstalled = new HashSet<>(
				dependencies.stream().map(x -> x.getDirectory() + x.getName()).collect(Collectors.toList()));
		boolean installEvent = false;
		for (JavaCVDependency dep : dependencies)
			if (forceReinstall || !dep.isInstalled()) {
				if (!forceReinstall && !installConfirmed) {
					if (!(installConfirmed = showInfoMsg || macroConfirmed)) {

//						if (showInfoMsg) {
//							ConfirmDialog cd = new ConfirmDialog( messageTitle, autoInstallMsg);
//							if(!cd.wasOKed()) {
//								Prefs.set("javacv.install_result", "canceled");
//								return false;
//							}
//						}
					}
				}

				try {
					if (dep.install()) {
						installEvent = true;
						log(dep.getName() + " will be installed to " + dep.getDirectory());
					} else {

						return false;
					}
				} catch (Exception e) {
					log("Install error: " + e.getMessage());
					e.printStackTrace();
					installed = false;
					Prefs.set("javacv.install_result", "cannot install");
				}
			}

		//// Try to cleanup conflicts and previous incorrect installations
		boolean conflictsFound = false;
		try {
			if (showInfoMsg) {
				log(" ");
				log("Searching for possible conflicts...");
			}
			List<String> allComps = getComponentsByVer(reqVersion).stream().map(x -> x.name).sorted()
					.collect(Collectors.toList());
			allComps.add("javacv");
			allComps.add("javacpp");
			Set<String> checkDirs = new HashSet<>();
			checkDirs.add(depsPath);
			checkDirs.add(natLibsPath);
			for (String checkDir : checkDirs) {
				if (new File(checkDir).exists())
					for (String checkComp : allComps) {
						DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(checkDir),
								checkComp + "*.jar");
						for (Path path : dirStream) {
							if (isFileConflicting(path, reqVersion)) {
								conflictsFound = true;
								new JavaCVDependency(path.getFileName().toString(),
										path.getParent().toString() + File.separator, null).remove();
								log("Conflicting file will be removed: " + path);
							}

						}
					}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		installConfirmed = installConfirmed || conflictsFound || installEvent;

		if (installConfirmed || forceReinstall) {
			IJ.showMessage("JavaCV installation", "Please restart ImageJ now");
			log("ImageJ restart is required after javacv installation!");
			IJ.log("---------------------------------------------");
			restartRequired = true;
		} else
			restartRequired = false;

		if (installed) {
			installedJavaCVVersion = reqVersion;
			installedComponents.addAll(reqComps);
			installedArtifacts.addAll(newInstalled);// (artifactResults.stream().map(x->x.getArtifact().getFile().getPath()).collect(Collectors.toList()));
			try {
				writeInstallCfg();
				Prefs.set("javacv.install_result", restartRequired ? "restart required" : "success");
			} catch (ParserConfigurationException | TransformerException e) {
				e.printStackTrace();
			}

		}

		return installed;
	}

	public static boolean isARM() {

		String osarch = System.getProperty("os.arch");
		return osarch != null
				&& (osarch.indexOf("aarch") != -1 || (osarch.indexOf("ARM") != -1) || (osarch.indexOf("arm") != -1));
	}

}
