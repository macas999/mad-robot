package com.madrobot.io;

/**
 * Tracks the progress of an I/O operation
 * @author elton.stephen.kent
 *
 */
public interface IOProgressCallback {
	/**
	 * Called when the I/O operation is completed.
	 */
	public void onComplete();

	/**
	 * Called when an I/O error has occurred.
	 * @param t
	 */
	public void onError(Throwable t);

	/**
	 * Called during the  I/O operation.
	 * @param bytesWritten number of bytes read/written
	 */
	public void onProgress(int bytes);
}