
import java.lang.System;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The program takes no arguments.
//

public class CSdict {
	static final int MAX_LEN = 255;
	static final int PERMITTED_ARGUMENT_COUNT = 1;
	static Boolean debugOn = false;

	// Here are the command table
	static final String openCmd = "open";
	static final String setCmd = "set";
	static final String dictCmd = "dict";
	static final String currdictCmd = "currdict";
	static final String defineCmd = "define";
	static final String matchCmd = "match";
	static final String prefixMatchComd = "prefixmatch";
	static final String closeCmd = "close";
	static final String quitCmd = "quit";

	// second arguments
	static Socket socket;
	static PrintWriter outputToServer;
	static BufferedReader inputFromServer;
	static String curDict = "*";
	static boolean connectionAvailable = false;

	public static void main(String[] args) {
		byte cmdString[] = new byte[MAX_LEN];

		if (args.length == PERMITTED_ARGUMENT_COUNT) {
			debugOn = args[0].equals("-d");
			if (debugOn) {
				System.out.println("Debugging output enabled");
			} else {
				System.out.println("997 Invalid command line option - Only -d is allowed");
				return;
			}
		} else if (args.length > PERMITTED_ARGUMENT_COUNT) {
			System.out.println("996 Too many command line options - Only -d is allowed");
			return;
		}

		try {
			for (int len = 1; len > 0;) {
				System.out.print("csdict> ");
				len = System.in.read(cmdString);

				if (len <= 0){
					terminate(quitCmd);
					break;
				}
				String decoded = new String(cmdString, "UTF-8");
				// needed to track multiple inputs properly, but test it out:
				String[] firstLine = decoded.split(System.getProperty("line.separator"));

				String[] userCommand = firstLine[0].split("\\s+");
				String cmd = userCommand[0].toLowerCase();

				// Note: we only need to add "-->" to requests sent to the
				// server
				// if (debugOn) System.out.println("--> " + decoded);
				if (cmd.equals("#") || cmd.equals("")) {
					continue;
				}

				if (cmd.equals(openCmd)) {
					if (connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length > 3 || userCommand.length == 1) {
						printArgumentsError();
					} else {
						String server = userCommand[1];
						// if the user command only has open <servername>
						if (userCommand.length == 2) {
							int port = 2628;
							establishConnection(server, port);
						} else {
							try {
								int port = Integer.parseInt(userCommand[2]);
								establishConnection(server, port);
							} catch (NumberFormatException e) {
								printInvalidArgument();
							}
						}
					}
				}

				else if (cmd.equals(dictCmd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 1) {
						printArgumentsError();
					} else {
						listDictionary();
					}
				}

				else if (cmd.equals(setCmd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 2) {
						printArgumentsError();
					} else {
						String dictionary = userCommand[1];
						setDictionary(dictionary);
					}
				}

				else if (cmd.equals(currdictCmd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 1) {
						printArgumentsError();
					} else {
						System.out.println(curDict);
					}
				}

				else if (cmd.equals(defineCmd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 2) {
						printArgumentsError();
					} else {
						String word = userCommand[1];
						define(word);
					}
				}

				else if (cmd.equals(matchCmd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 2) {
						printArgumentsError();
					} else {
						String word = userCommand[1];
						match(word);
					}
				}

				else if (cmd.equals(prefixMatchComd)) {
					if (!connectionAvailable) {
						printConnectionError();
					} else if (userCommand.length != 2) {
						printArgumentsError();
					} else {
						String word = userCommand[1];
						prefixMatch(word);
					}
				}

				else if (cmd.equals(closeCmd)) {
					if (userCommand.length != 1) {
						printArgumentsError();
					} else {
						terminate(cmd);
					}
				}

				else if (cmd.equals(quitCmd)) {
					if (userCommand.length != 1) {
						printArgumentsError();
					} else {
						terminate(cmd);
					}
				}

				else {
					printInvalidCommand();
				}

			}

		} catch (IOException exception) {
			System.err.println("998 Input error while reading commands, terminating");
		}

	}

	private static void printInvalidCommand() {
		System.out.println("900 Invalid command");
	}

	private static void printArgumentsError() {
		System.out.println("901 Incorrect number of arguments");
	}

	private static void printInvalidArgument() {
		System.out.println("902 Invalid argument");
	}

	private static void printConnectionError() {
		System.out.println("903 Supplied command not expected at this time");
	}

	private static void printDictionaryError() {
		System.out.println("930 Dictionary does not exist");
	}

	private static void match(String word) throws IOException {
		// TODO: must be an easier way to check w/o 2 server requests...
		outputToServer.println("MATCH " + curDict + " exact " + word);

		if (debugOn) {
			while (true) {
				String line = "<-- " + inputFromServer.readLine();
				System.out.println(line);
				if (line.startsWith("<-- 550")) {
					printDictionaryError();
				} else if (line.startsWith("<-- 552")) {
					System.out.println("****No matching word(s) found****");
				}
				if (line.startsWith("<-- 250")) {
					break;
				}
			}
		} else {
			while (true) {
				String line = (inputFromServer.readLine());
				if (line.startsWith("550")) {
					printDictionaryError();
					return;
				} else if (line.startsWith("552")) {
					System.out.println("****No matching word(s) found****");
					return;
				}
				if (line.startsWith("250")) {
					break;
				}
			}
			outputToServer.println("MATCH " + curDict + " exact " + word);
			while (true) {
				String line = (inputFromServer.readLine());
				System.out.println(line);
				if (line.startsWith("250")) {
					break;
				}
			}
		}
	}

	private static void prefixMatch(String word) throws IOException {
		outputToServer.println("MATCH " + curDict + " prefix " + word);
		if (debugOn) {
			while (true) {
				String line = "<-- " + inputFromServer.readLine();
				System.out.println(line);
				if (line.startsWith("<-- 550")) {
					printDictionaryError();
				} else if (line.startsWith("<-- 552")) {
					System.out.println("*****No prefix matches found*****");
				}
				if (line.startsWith("<-- 250")) {
					break;
				}
			}
		} else {
			while (true) {
				String line = (inputFromServer.readLine());
				if (line.startsWith("550")) {
					printDictionaryError();
					return;
				} else if (line.startsWith("552")) {
					System.out.println("*****No prefix matches found*****");
					return;
				}
				if (line.startsWith("250")) {
					break;
				}
			}
			outputToServer.println("MATCH " + curDict + " prefix " + word);
			while (true) {
				String line = (inputFromServer.readLine());
				System.out.println(line);
				if (line.startsWith("250")) {
					break;
				}
			}
		}
	}

	private static void define(String word) throws IOException {
		outputToServer.println("DEFINE " + curDict + " " + word);

		if (debugOn) {
			System.out.println("--> DEFINE " + curDict + " " + word);
			while (true) {
				String line = (inputFromServer.readLine());
				System.out.println(line);
				if (line.startsWith("."))
					break;
			}
		} else {
			while (true) {
				// check if the input from server is null
				String line = (inputFromServer.readLine());

				if (line.startsWith("552")) {
					System.out.println("**No definition found**");
					outputToServer.println("MATCH * . " + word);
					while (true) {
						String list = (inputFromServer.readLine());

						if (line.startsWith("552")) {
							System.out.println("***No dictionaries have a definition for this word***");
							break;
						}
						if (line.startsWith("250"))
							break;
						System.out.println(list);
					}

				}
			}
		}
	}

	private static void setDictionary(String dictionary) {
		curDict = dictionary;
	}

	private static void establishConnection(String server, int port) throws IOException {

		try {
			socket = new Socket(server, port);
		} catch (UnknownHostException e) {
			// e.printStackTrace();
			// This error message is only used here so no need to abstract into
			// function
			String msg = "920 Control connection to " + server + " on port " + port + " failed to open";
			System.out.println(msg);
		}

		if (socket != null) {
			outputToServer = new PrintWriter(socket.getOutputStream(), true);
			inputFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String line = inputFromServer.readLine();
			if (debugOn) {
				System.out.println("<-- " + line);
			} else {
				// We do not need to print this in non debug mode
				// System.out.println(line);
			}
		}
		connectionAvailable = true;
	}

	private static void listDictionary() throws IOException {
		outputToServer.println("SHOW DB");
		if (debugOn) {
			System.out.println("--> SHOW DB");
			System.out.print("<-- ");

			// we only need to add "<-- " to the first line
			while (true) {
				String line = (inputFromServer.readLine());
				System.out.println(line);
				if (line == null) {
					String msg = "925 Control connection I/O error, closing control connection";
					System.out.println(msg);
				} else if (line.startsWith(".")) {
					String finalline = "<-- " + (inputFromServer.readLine());
					System.out.println(finalline);
					break;
				}
			}
		} else {
			while (true) {
				String line = (inputFromServer.readLine());
				System.out.println(line);
				if (line == null) {
					String msg = "925 Control connection I/O error, closing control connection";
					System.out.println(msg);
				} else if (line.startsWith(".")) {
					break;
				}
			}
		}
	}

	private static void terminate(String termType) throws IOException {
		if (connectionAvailable) {
			if (!socket.isClosed()) {
				outputToServer.println("QUIT");
				if (debugOn) {
					System.out.println("<-- " + inputFromServer.readLine());
				}
				connectionAvailable = false;
			}
			if (termType.equals(quitCmd)) {
				System.exit(0);
			}
		} else {
			if (termType.equals(closeCmd)) {
				printConnectionError();
			} else {
				System.exit(0);
			}
		}
	}
}