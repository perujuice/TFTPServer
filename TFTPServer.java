package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class TFTPServer {
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final String READDIR = "READDIR"; // custom address at your PC
	public static final String WRITEDIR = "WRITEDIR"; // custom address at your PC
	// OP codes
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;

	public static void main(String[] args) {
		if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		// Starting the server
		try {
			TFTPServer server = new TFTPServer();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void start() throws SocketException {
		byte[] buf = new byte[BUFSIZE];

		// Create socket
		DatagramSocket socket = new DatagramSocket(null);

		// Create local bind point
		SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		// Loop to handle client requests
		while (true) {

			final InetSocketAddress clientAddress = receiveFrom(socket, buf);

			// If clientAddress is null, an error occurred in receiveFrom()
			if (clientAddress == null)
				continue;

			final StringBuffer requestedFile = new StringBuffer();
			System.out.println("oldest: " + requestedFile);
			final int reqtype = ParseRQ(buf, requestedFile);

			new Thread() {
				public void run() {
					try {
						DatagramSocket sendSocket = new DatagramSocket(0);

						// Connect to client
						sendSocket.connect(clientAddress);

						System.out.printf("%s request for %s from %s using port %d\n",
								(reqtype == OP_RRQ) ? "Read" : "Write",
								requestedFile, // Assuming you want to print the file requested.
								clientAddress.getHostName(), clientAddress.getPort());

						// Read request
						if (reqtype == OP_RRQ) {
							requestedFile.insert(0, READDIR);
							HandleRQ(clientAddress, sendSocket, requestedFile.toString(), OP_RRQ);
						}
						// Write request
						else {
							requestedFile.insert(0, WRITEDIR);
							HandleRQ(clientAddress, sendSocket, requestedFile.toString(), OP_WRQ);
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	/**
	 * Reads the first block of data, i.e., the request for an action (read or
	 * write).
	 * 
	 * @param socket (socket to read from)
	 * @param buf    (where to store the read data)
	 * @return socketAddress (the socket address of the client)
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {

		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
		try {
			// Wait for an incoming packet
			socket.receive(receivePacket);
			SocketAddress socketAddress = receivePacket.getSocketAddress();
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
			System.out.println(socketAddress);
			return inetSocketAddress;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Parses the request in buf to retrieve the type of request and requestedFile
	 * 
	 * @param buf           (received request)
	 * @param requestedFile (name of file to read/write)
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) {
		int opcode = -1; // Initialize with an invalid opcode

		try {
			// Extract the opcode from the first 2 bytes
			opcode = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
	
			// Check if it's RRQ or WRQ
			if (opcode == OP_RRQ || opcode == OP_WRQ) {
				// Find the end of the filename string (terminated by a 0 byte)
				int i = 2;
				while (buf[i] != 0) i++;
				// Extract the filename
				String filename = new String(buf, 2, i - 2, "UTF-8");
				// Update requestedFile
				requestedFile.setLength(0); // Clear any existing content
				requestedFile.append("/" + filename);
			} else {
				// Invalid or unrecognized opcode
				opcode = OP_ERR;
			}
		} catch (Exception e) {
			System.err.println("Error parsing request: " + e.getMessage());
			opcode = OP_ERR; // Set to error opcode in case of exception
		}
	
		return opcode; // Return the opcode (RRQ, WRQ, or ERR in case of failure)
	}

	/**
	 * Handles RRQ and WRQ requests 
	 * 
	 * @param sendSocket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opcode (RRQ or WRQ)
	 */
	private void HandleRQ(InetSocketAddress clientAddress, DatagramSocket sendSocket, String requestedFile, int opcode) 
	{		
		String params = ""; // Placeholder for compilation
		if (opcode == OP_RRQ) {
			// Placeholder implementation
			boolean result = send_DATA_receive_ACK(sendSocket, clientAddress, requestedFile);
		} else if (opcode == OP_WRQ) {
			// Placeholder implementation
			boolean result = receive_DATA_send_ACK(params);
		} else {
			System.err.println("Invalid request. Sending an error packet.");
			send_ERR(params);
		}		
	}

	/**
	To be implemented
	*/
	private boolean send_DATA_receive_ACK(DatagramSocket sendSocket, InetSocketAddress clientAddress, String requestedFile) {

	}

	private boolean receive_DATA_send_ACK(String params)
	{return true;}

	private void send_ERR(String params)
	{}


}
