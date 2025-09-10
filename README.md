## Instructions

TFTP server implemented using Java, allowing a standard tftp client to connect to the server and interact. The server can handle both read and write requests following the TFTP specification (RFC1350).

To test the server, launch the <code>TFTPServer.java</code> file, then connect a client.

### connecting a client to the server:

*connect [hostname or ip] [port]*

The tftp client is generally pre-installed in Linux and macOS, for windows you can use this GUI client: <link>https://github.com/PJO2/tftpd64/releases/</link>


### Download/Upload:

*get FILENAME*

*put FILENAME*

### Format

*mode ascii*
*mode octet*
*mode binary*

### Terminate the session:

*quit*

