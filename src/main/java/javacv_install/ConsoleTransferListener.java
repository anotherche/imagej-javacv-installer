package javacv_install;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

/**
 * A simplistic transfer listener that logs uploads/downloads to the console.
 */
public final class ConsoleTransferListener extends AbstractTransferListener {
	
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
//		if(!trInit) {
//			log("Downloading information required to verify JavaCV dependencies...");
//			trInit = true;
//		}

		if (!event.getResource().getResourceName().endsWith(".xml"))
			IJLog.log(message, event.getResource().getResourceName().indexOf(".jar") == -1);

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
					IJLog.log(strBuff, true);
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

				IJLog.log(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput
						+ ")");

			}
		}
	}

	@Override
	public void transferFailed(TransferEvent event) {
		transferCompleted(event);

		if (!(event.getException() instanceof MetadataNotFoundException)) {
			IJLog.log(event.getException().toString());
		}
	}

	private void transferCompleted(TransferEvent event) {
		if (event.getResource().getResourceName().indexOf(".jar") != -1) {
			downloads.remove(event.getResource());

			StringBuilder buffer = new StringBuilder(64);
			pad(buffer, lastLength);
			String strBuff = buffer.toString().trim();
			if (!strBuff.isEmpty()) {
				IJLog.log(strBuff, true);
			}
		}
	}

	@Override
	public void transferCorrupted(TransferEvent event) {
		IJLog.log(event.getException().toString());
	}

	protected long toKB(long bytes) {
		return (bytes + 1023) / 1024;
	}

}
