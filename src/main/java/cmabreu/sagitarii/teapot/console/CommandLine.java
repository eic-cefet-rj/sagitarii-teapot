package cmabreu.sagitarii.teapot.console;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

import java.io.IOException;
import java.io.StreamTokenizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/** This class provides a generic command-line interpreter.
 * See CommandLineTest.java for a usage example.
 * 
 * @author medined
 * Created on May 20, 2003
 *
 */
public class CommandLine {

	/**
	 * A simple interface to enforce how the
	 * command classes are executed.
	 */
	public interface ICommand {
		public boolean doIt(List<String> v);
	}

	/**
	 * Holds the coorespondence of command to
	 * java class. The key is the command and
	 * the entry is the java class that gets 
	 * invoked. The java class must implement
	 * the CommandLine.ICommand interface.
	 */
	private HashMap<String,String> cmdToClass = new HashMap<String,String>();

	/**
	 * Displayed when the program first starts.
	 * Provides for customization option.
	 */
	private String commandLineHeader = null;

	/**
	 * Displayed when the HELP command is executed.
	 * Provides for customization option.
	 */
	private String commandLineHelp = null;

	/**
	 * Provides an unchanging prompt for each
	 * line of input.
	 * Provides for customization option.
	 */
	private String commandLinePrompt = "> ";

	/**
	 * Displayed when the program first starts.
	 * Provides for customization option.
	 */
	private String commandLineVersion = null;

	/**
	 * This variable allows runtime detection of
	 * input type: from a file (false) or from
	 * a command line (true).
	 */
	private boolean isInteractive = true;

	/**
	 * A simple constructor.
	 */
	public CommandLine() {
		super();
	}

	/**
	 * Makes the correspondence between the command
	 * and the class to invoke. Notice that the command
	 * is trimmed and converted to upper-case.
	 * 
	 */
	public void assignClassToCommnd(String cmd, String clazz) {
		cmdToClass.put(cmd.trim().toUpperCase(), clazz);	
	}

	/**
	 * Invoked by the HELP command. It displays a list
	 * of active commands. Note that the list may change
	 * if the assignClassToCommand method is called after
	 * the HELP command is executed.
	 */
	public void cmdHelp() {
		System.out.println("");
		System.out.println(" Parameters w/spaces can be quoted.");
		System.out.println(" quit leaves program.");
		if (getCommandLineHelp() != null) {
			System.out.println( getCommandLineHelp() );
		}
		
		System.out.println(" Available commands: ");
		System.out.println("");
		
		if (cmdToClass.isEmpty() == false) {
			Iterator i = cmdToClass.keySet().iterator();
			while (i.hasNext()) {
				System.out.println(" > " +  i.next() );
			}
		}
		
		System.out.println("");
	}

	/**
	 * Getter for commandLineHeader variable.
	 */
	public String getCommandLineHeader() {
		return commandLineHeader;
	}

	/**
	 * Getter for commandLineHelp variable.
	 */
	public String getCommandLineHelp() {
		return commandLineHelp;
	}

	/**
	 * Getter for commandLinePrompt variable.
	 */
	public String getCommandLinePrompt() {
		return commandLinePrompt;
	}

	/**
	 * Getter for commandLineVersion variable.
	 */
	public String getCommandLineVersion() {
		return commandLineVersion;
	}

	/**
	 * Displays the version, basic command list, and optional
	 * help message.
	 */
	public void init() {
		if (this.isInteractive == true) {
			if (getCommandLineVersion() != null) {
				System.out.println(getCommandLineVersion());
			}
			//System.out.println("EXIT leaves program.");
			//System.out.println("HELP lists commands.");
			if (getCommandLineHelp() != null) {
				System.out.println(getCommandLineHelp());
			}
		}
	}

	/**
	 * Parses the input stream into tokens. Note that
	 * quotes are needed when command-line parameters 
	 * have spaces.
	 */
	public void parseStream(StreamTokenizer st) {
		List v = new ArrayList<String>();
	    int token = 0;

	    try { 
	        // Prepare the tokenizer for Java-style 
	        // tokenizing rules
    	    st.parseNumbers();
        	st.wordChars('_', '_');
        	st.eolIsSignificant(true);
    
	        // If whitespace is not to be discarded, 
	        // make this call
    	    //st.ordinaryChars(0, ' ');
    
	        // These calls caused comments to be discarded
    	    st.slashSlashComments(true);
        	st.slashStarComments(true);
    
    		if (true == isInteractive) {
				System.out.print(getCommandLinePrompt());
    		}

	        // Parse the file
	        do {
	            token = st.nextToken();
    	        switch (token) {
	        	    case StreamTokenizer.TT_NUMBER:
    	        	    // A number was found; the value is in nval
        	        	double num = st.nval;
	        	    	v.add( num );
            	    	break;
	            	case StreamTokenizer.TT_WORD:
    	            	// A word was found; the value is in sval
        	    	    v.add(st.sval);
    	    	        break;
	    	        case '"':
    	    	        // A double-quoted string was found; sval 
    	    	        // contains the contents
        	    	    v.add(st.sval);
            	    	break;
		            case '\'':
    		            // A single-quoted string was found; sval 
    		            // contains the contents
        	    	    v.add(st.sval);
            		    break;
	            	case StreamTokenizer.TT_EOL:
	            		if ( processCommands(v) == false ) {
	            			token = StreamTokenizer.TT_EOF;
	            		}
						v.clear();
			    		if ( isInteractive == true ) {
							System.out.print(getCommandLinePrompt());
			    		}
	    	            // End of line character found
    	    	        break;
	    	        case StreamTokenizer.TT_EOF:
    	    	        // End of file has been reached
        	    	    break;
		            default:
		                // A regular character was found; the value 
		                // is the token itself
    		            char ch = (char)st.ttype;
        		        break;
				}
	        } while (token != StreamTokenizer.TT_EOF);

	    } catch (IOException e) {
	    	e.printStackTrace();
    	} catch (Exception e) {
	    	e.printStackTrace();
	    }

	}

	/**
	 * This method executes the commands. There are
	 * four standard commands (exit, help, # and rem).
	 * Otherwise, the command is looked up in the cmdToClass
	 * hashmap so that its equivalent java class is found. 
	 * Then the class is executed using reflection.
	 */
	private boolean processCommands(List<String> v) {
		long tickStart = System.currentTimeMillis();
		boolean retValue = true;
		
   		if ( isInteractive == false ) {
			System.out.println(v);
		}
		
		for ( String cmd : v ) {
			
			if (cmd.equalsIgnoreCase("exit")) {
				retValue = false;
			} else if (cmd.equalsIgnoreCase("help")) {
				cmdHelp();
			} else if (cmd.equalsIgnoreCase("rem")) {
				break;
			} else if (cmd.equalsIgnoreCase("# ")) {
				break;
			} else {
				String finalCmd = cmd.trim().toUpperCase();
				String classToInvoke = (String)cmdToClass.get(finalCmd);
				try {
					retValue = runMethod(classToInvoke, v);
				} catch (NullPointerException e) {
					System.out.println(	"Unknown command: ["+ cmd.trim().toUpperCase()+ "]"	);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Error Running: ["+ classToInvoke+ "] with [" + v + "]");
				}
				break;
			}
		}

		long tickEnd = System.currentTimeMillis();
		long tickTotal = tickEnd - tickStart;
		DecimalFormat df = new DecimalFormat();
		df.setDecimalSeparatorAlwaysShown(true);
		//System.out.println("  completed: "	+ df.format(tickTotal)+ " msecs");

		return retValue;
	}

	/**
	 * This method hides the ugly details of reflection. In
	 * a nutshell it invokes the doIt() method of the
	 * java class associated with the command being executed.
	 */
	public boolean runMethod( String theName, List v ) {
		// get the class object.
		Class theClass = null;
		try {
			theClass = Class.forName( theName );
			if (theClass == null) {
				throw new RuntimeException("theClass is null.");
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Class Not Found: " + theName);
		}

		// get the constructor object.
		Constructor theConstructor = null;
		try {
			theConstructor = theClass.getConstructor(new Class[0]);
			if (theConstructor == null) {
				throw new RuntimeException("theConstructor is null.");
			}
		}
		catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new RuntimeException(	"Unable to find constructor for "+ theName	);
		}

		// get the instance object.
		ICommand theInstance = null;
		try {
			theInstance = 
				(ICommand)theConstructor.newInstance(new Class[0]);
			if (null == theInstance) { 
				throw new RuntimeException("theInstance is null."); 
			}
		} 
		catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
			throw new RuntimeException("InvocationTargetException for "	+ theName); 
		}
		catch (java.lang.NoClassDefFoundError e) {
			throw new RuntimeException(	"NoClassDefFoundError for "	+ theName); 
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(	"Unable to load constructor for "+ theName); 
		}

		Class[] parameterTypes = new Class[1];
		parameterTypes[0] = java.util.List.class;

		// gets the method object.
		Method theMethod = null;
		try {
			theMethod = theClass.getMethod("doIt", parameterTypes);
			if (null == theMethod) { 
				throw new RuntimeException("theMethod is null."); 
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to find test method for "+ theName); 
		}
		
		Object[] params = new Object[1];
		params[0] = v;

		Boolean theReturnValue;

		try {
			theReturnValue = (Boolean)theMethod.invoke(theInstance, params);
		} 
		catch (IllegalAccessException e) {
			throw new RuntimeException(	"IllegalAccessException for " + theName);
		}
		catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
			throw new RuntimeException(	"InvocationTargetException for " + theName); 
		}
		catch (NoClassDefFoundError e) {
			throw new RuntimeException(	"NoClassDefFoundError for " + theName); 
		}
		
		return( theReturnValue.booleanValue() );
	}

	/** Setter for the commandLineHeader variable
	 */
	public void setCommandLineHeader(String string) {
		commandLineHeader = string;
	}

	/** Setter for the commandLineHelp variable
	 */
	public void setCommandLineHelp(String string) {
		commandLineHelp = string;
	}

	/** Setter for the commandLinePrompt variable
	 */
	public void setCommandLinePrompt(String string) {
		commandLinePrompt = string; 
	}

	/** Setter for the commandLineVersion variable
	 */
	public void setCommandLineVersion(String string) {
		commandLineVersion = string;
	}

	/**
	 * Sets the isInteractive status.
	 */
	public void setIsInteractive(boolean isInteractive) {
		this.isInteractive = isInteractive;
	}

}
