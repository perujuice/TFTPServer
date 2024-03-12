package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
						} else if (reqtype == OP_ERR) {
							HandleRQ(clientAddress, sendSocket, requestedFile.toString(), OP_ERR);
						} 
						// Write request
						else {
							requestedFile.insert(0, READDIR);
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
		System.out.println("old ParseRQ: " + requestedFile);
		int opcode = -1; // Initialize with an invalid opcode

		try {
			// Extract the opcode from the first 2 bytes
			opcode = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);

			// Check if it's RRQ or WRQ
			if (opcode == OP_RRQ || opcode == OP_WRQ) {
				// Find the end of the filename string (terminated by a 0 byte)
				int i = 2;
				while (buf[i] != 0)
					i++;
				// Extract the filename
				String filename = new String(buf, 2, i - 2, "UTF-8");
				// Update requestedFile
				requestedFile.setLength(0); // Clear any existing content
				requestedFile.append("/" + filename);
				System.out.println("after ParseRQ: " + requestedFile);


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
	 * @param sendSocket    (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opcode        (RRQ or WRQ)
	 */
	private void HandleRQ(InetSocketAddress clientAddress, DatagramSocket sendSocket, String requestedFile,
			int opcode) {
		String params = ""; // Placeholder for compilation
		if (opcode == OP_RRQ) {
			// Placeholder implementation
			boolean result = send_DATA_receive_ACK(sendSocket, clientAddress, requestedFile);
		} else if (opcode == OP_WRQ) {
			// Placeholder implementation
			boolean result = receive_DATA_send_ACK(sendSocket, clientAddress, requestedFile);
		} else {
			System.err.println("Invalid request. Sending an error packet.");
			send_ERR(sendSocket, clientAddress, 4);
		}
	}

	/**
	 * To be implemented
	 */
	private boolean send_DATA_receive_ACK(DatagramSocket sendSocket, InetSocketAddress clientAddress,
			String requestedFile) {
		try {
			System.out.println(requestedFile);
			// Construct the full path to the requested file
			String filePath = requestedFile; // Assuming requestedFile already contains the filename
			// Read the file into blocks
			byte[][] fileBlocks = readFileInBlocks(filePath);

			// Send each block and wait for ACK
			for (int i = 0; i < fileBlocks.length; i++) {
				// Send the current block and wait for an ACK
				boolean ackReceived = sendBlockAndWaitForAck(sendSocket, clientAddress, fileBlocks[i], i + 1, 3000);
				if (!ackReceived) {
					// If ACK wasn't received, log the error and return false
					System.err.println("Failed to receive ACK for block " + (i + 1));
					return false;
				}
			}

			// If all blocks were sent and acknowledged, return true
			return true;
		} catch (IOException e) {
			// Log any IO Exceptions and return false
			send_ERR(sendSocket, clientAddress, 1);
			e.printStackTrace();
			return false;
		}
	}

	private boolean receive_DATA_send_ACK(DatagramSocket socket, InetSocketAddress clientAddress, String filename) {
		File file = new File(WRITEDIR + filename);
		if (file.exists()) {
			send_ERR(socket, clientAddress, 6); // Error code 6 for File already exists
			return false;
		}
		List<byte[]> receivedData = new ArrayList<>();
		int expectedBlockNumber = 1;
		boolean transferComplete = false;

		// for first ack
		try {
			sendAck(socket, clientAddress, 0);
			System.out.println("ack" + 0 + "sent");
		} catch (IOException e) {
			System.err.println("Failed to send initial ACK: " + e.getMessage());
			return false; // Or appropriate error handling
		}

		while (!transferComplete) {
			try {
				byte[] dataBlock = receiveAndStoreBlock(socket, expectedBlockNumber);
				if (dataBlock != null) {
					receivedData.add(dataBlock);
					expectedBlockNumber++;
	
					// Send ACK for the received block
					try {
						sendAck(socket, clientAddress, expectedBlockNumber - 1);
					} catch (IOException e) {
						System.err.println("Failed to send ACK for block " + (expectedBlockNumber - 1) + ": " + e.getMessage());
						return false; // Or appropriate error handling
					}	
					if (dataBlock.length < 512) { // Check if this is the last block
						transferComplete = true;
					}
				} else {
					// send error and terminate
				}
			} catch (IOException e) {
				System.err.println("Failed to receive or store block: " + e.getMessage());
				return false; // Or appropriate error handling
			}
			
		}
		try {
			writeDataToFile(receivedData, filename);
		} catch (IOException e) {
			System.err.println("Failed to write data to file: " + e.getMessage());
			return false; // Or appropriate error handling
		}		return true;
	}

	private void send_ERR(DatagramSocket socket, InetSocketAddress clientAddress, int errorCode) {
		byte[] errPacket = new byte[BUFSIZE];
		String errMsg; // Error message based on errorCode
	
		switch (errorCode) {
			case 1: // File not found
				errMsg = "File not found";
				break;
			case 4: // Illegal TFTP operation
				errMsg = "Illegal TFTP operation";
				break;
			case 6: // File already exists
				errMsg = "File already exists";
				break;
			// Add cases for other error codes as needed
			default:
				errMsg = "Unknown error";
				break;
		}
	
		// Error opcode
		errPacket[0] = 0;
		errPacket[1] = OP_ERR;
		// Error code
		errPacket[2] = (byte) ((errorCode >> 8) & 0xFF);
		errPacket[3] = (byte) (errorCode & 0xFF);
		// Error message
		byte[] errMsgBytes = errMsg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		System.arraycopy(errMsgBytes, 0, errPacket, 4, errMsgBytes.length);
		errPacket[4 + errMsgBytes.length] = 0; // Null terminator for the string
	
		try {
			DatagramPacket packet = new DatagramPacket(errPacket, 4 + errMsgBytes.length + 1, clientAddress.getAddress(), clientAddress.getPort());
			socket.send(packet);
		} catch (IOException e) {
			System.err.println("Error sending the error packet: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Reads a file and divides it into blocks of byte arrays.
	 *
	 * @param filePath The path to the .bin file.
	 * @return An array of byte arrays, each representing a block of the file.
	 * @throws IOException If an I/O error occurs reading from the file.
	 */
	private byte[][] readFileInBlocks(String filePath) throws IOException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);

		int blockSize = 512; // Size of each block
		List<byte[]> blocks = new ArrayList<>();

		byte[] buffer = new byte[blockSize];
		int bytesRead;

		while ((bytesRead = fis.read(buffer)) != -1) {
			if (bytesRead < blockSize) {
				byte[] lastBlock = new byte[bytesRead];
				System.arraycopy(buffer, 0, lastBlock, 0, bytesRead);
				blocks.add(lastBlock);
			} else {
				blocks.add(buffer.clone());
			}
		}

		fis.close();

		byte[][] blocksArray = new byte[blocks.size()][];
		blocks.toArray(blocksArray);

		return blocksArray;
	}

	/**
	 * Sends a single block of data to the client and waits for an ACK.
	 * Timeout and retry logic should be implemented to handle transmission
	 * reliability.
	 *
	 * @param sendSocket    The socket used to send and receive packets.
	 * @param clientAddress The address and port of the client.
	 * @param dataBlock     The data block to be sent.
	 * @param blockNumber   The block number for the current data block.
	 * @return true if the block was sent and acknowledged, false otherwise.
	 */
	private boolean sendBlockAndWaitForAck(DatagramSocket sendSocket, InetSocketAddress clientAddress, byte[] dataBlock, int blockNumber, int timeout) throws IOException {
		byte[] sendData = new byte[dataBlock.length + 4]; // Plus 4 for the opcode and block number
		sendData[0] = 0;
		sendData[1] = OP_DAT; // DATA opcode
		sendData[2] = (byte) ((blockNumber >> 8) & 0xff); // High byte of block number
		sendData[3] = (byte) (blockNumber & 0xff); // Low byte of block number
		System.arraycopy(dataBlock, 0, sendData, 4, dataBlock.length);
	
		byte[] ackBuf = new byte[4]; // Buffer for receiving ACKs
		DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length);
	
		sendSocket.setSoTimeout(timeout);
	
		boolean ackReceived = false;
		while (!ackReceived) {
			try {
				// Sending packet
				System.out.println("Sending packet for block number " + blockNumber);
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress);
				sendSocket.send(sendPacket);
	
				// Attempting to receive ACK
				sendSocket.receive(ackPacket);
	
				// Check if the received packet is an ACK for the correct block
				if (ackBuf[1] == OP_ACK && ((ackBuf[2] & 0xff) << 8 | (ackBuf[3] & 0xff)) == blockNumber) {
					System.out.println("ACK received for block number " + blockNumber);
					ackReceived = true;
				}
			} catch (java.net.SocketTimeoutException e) {
				// Handle the timeout specifically
				System.out.println("Timeout waiting for ACK for block number " + blockNumber + ", resending packet.");
				// The packet will be resent on the next loop iteration
			}
		}
	
		return ackReceived;
	}

	private void sendAck(DatagramSocket socket, InetSocketAddress clientAddress, int blockNumber) throws IOException {
		byte[] ackPacket = new byte[4];
		ackPacket[0] = 0;
		ackPacket[1] = 4; // Opcode for ACK
		ackPacket[2] = (byte) (blockNumber >> 8);
		ackPacket[3] = (byte) (blockNumber);

		DatagramPacket packet = new DatagramPacket(ackPacket, ackPacket.length, clientAddress.getAddress(),
				clientAddress.getPort());
		socket.send(packet);
	}


	/**
	 * Attempts to receive a block of data from the client. If the block number
	 * matches the expected one,
	 * it returns the data portion of the block. Otherwise, it returns null.
	 * 
	 * @param socket              The DatagramSocket to receive the data.
	 * @param expectedBlockNumber The expected block number to receive.
	 * @return The data portion of the received block as a byte array, or null if
	 *         the block number does not match.
	 * @throws IOException If an I/O error occurs.
	 */
	public byte[] receiveAndStoreBlock(DatagramSocket socket, int expectedBlockNumber) throws IOException {
		socket.setSoTimeout(10000); // Timeout in milliseconds (10 seconds)

		byte[] buf = new byte[BUFSIZE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		try {
			socket.receive(packet); // Attempt to receive DATA packet, may throw SocketTimeoutException

			// Extract block number from received packet
			int blockNumber = ((buf[2] & 0xff) << 8) | (buf[3] & 0xff);

			if (blockNumber == expectedBlockNumber) {
				// Extract the data portion of the packet (excluding the 4-byte header)
				byte[] dataBlock = new byte[packet.getLength() - 4];
				System.arraycopy(buf, 4, dataBlock, 0, packet.getLength() - 4);

				return dataBlock; // Return the data portion of the block
			}
		} catch (IOException e) {

			throw e;
		}
		return null;
	}
	/**
     * Writes the contents of a list of byte arrays to a file.
     *
     * @param receivedData The list of byte arrays to write.
     * @param requestedFile The path and name of the file where the data should be written.
     * @throws IOException If an error occurs during file writing.
     */
    public void writeDataToFile(List<byte[]> receivedData, String requestedFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(requestedFile)) {
            for (byte[] dataBlock : receivedData) {
                fos.write(dataBlock);
				System.out.println("writing to: " + requestedFile);
            }
        }
		catch (IOException e) {
			e.printStackTrace();
		}
    }
}


