package cmabreu.sagitarii.teapot;

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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


public class SystemProperties  {
    private int availableProcessors;
    private String soName;
    private String localIpAddress;
    private String machineName;
    private String macAddress;
    private OsType osType = OsType.UNIX_LIKE;
    private String javaVersion;
    private long freeMemory;
    private long totalMemory;
    private String teapotRootFolder;
    private String teapotJarPath;
    private String path;
	private String classPath;
	private String rHome;
	private String jriPath;

    public OsType getOsType() {
    	return this.osType;
    }
    
    private double getProcessCpuLoad() {
    	try {
	        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
	        AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
	        if (list.isEmpty())  return 0;
	        Attribute att = (Attribute)list.get(0);
	        Double value = (Double)att.getValue();
	        if (value == -1.0) return 0; 
	        return ((int)(value * 1000) / 10.0);
    	} catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException e) {
    		return 0;
    	}
    }    

    private InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = en.nextElement();
            for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }    
    
    public String getJavaVersion() {
    	return this.javaVersion;
    }

    public long getTotalMemory() {
    	totalMemory = Runtime.getRuntime().totalMemory();
		return totalMemory;
	}
    
    public long getFreeMemory() {
    	freeMemory = Runtime.getRuntime().freeMemory();
		return freeMemory;
	}
    
    public String getTeapotRootFolder() {
		return teapotRootFolder;
	}
    
    public String getTeapotJarPath() {
		return teapotJarPath;
	}
    
    public String getPath() {
		return path;
	}
    
    private void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
    
    public long getFreeDiskSpace() {
    	File myself = new File( teapotRootFolder );
    	return myself.getUsableSpace() /1024 /1024 ;
    }

    public long getTotalDiskSpace() {
    	File myself = new File( teapotRootFolder );
    	return myself.getTotalSpace() /1024 /1024 ;
    }

    
    public void setJriPath( String path ) {
    	jriPath = path;
    	try { addLibraryPath(path); } catch ( Exception e ) { } 
    }
    
    public SystemProperties() throws Exception {
    	
		File f = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath() );
		teapotJarPath =  f.getAbsolutePath();
		teapotRootFolder = teapotJarPath.substring(0, teapotJarPath.lastIndexOf( File.separator ) + 1).replace(File.separator, "/");
		
		try {	
			classPath = System.getenv("CLASSPATH");
			rHome = System.getenv("R_HOME");
			jriPath = System.getenv("LD_LIBRARY_PATH");
		} catch ( Exception e ) {
			
		}
		
		path = System.getProperty( "java.library.path");

		getProcessCpuLoad();
    	this.availableProcessors = Runtime.getRuntime().availableProcessors(); 
    	getFreeMemory(); 
    	getTotalMemory();  
    	this.soName = ManagementFactory.getOperatingSystemMXBean().getName();
    	this.localIpAddress = "***";
    	this.javaVersion = System.getProperty("java.version");
    	getProcessCpuLoad();
    	InetAddress ip;
		try {
			ip = getFirstNonLoopbackAddress(true, false);
			this.localIpAddress = ip.toString().replace("/", "");
			
			InetAddress iplocal = InetAddress.getLocalHost();
			this.machineName = iplocal.getCanonicalHostName();

			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			
			byte[] mac = network.getHardwareAddress();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			this.macAddress =  sb.toString();    	
		} catch ( SocketException | UnknownHostException e) {
		}
    	getProcessCpuLoad();
    	discoverOsType();
	}

    
    private void discoverOsType() {
    	String os = this.soName.toLowerCase();
    	if ( (os.indexOf("win") >= 0) ) {
    		this.osType = OsType.WINDOWS;
    	}
    	if ( (os.indexOf("mac") >= 0) ) {
    		this.osType = OsType.MAC;
    	}
    }
    
    public String getMacAddress() {
    	return this.macAddress;
    }
    
    public String getMachineName() {
    	return this.machineName;
    }
    
    public Double getCpuLoad() {
    	return getProcessCpuLoad();
    }
    
    public String getLocalIpAddress() {
    	return this.localIpAddress;
    }
	
    public int getAvailableProcessors() {
    	return this.availableProcessors;
    }

    public String getSoName() {
    	return this.soName;
    }
    
    public String getrHome() {
		return rHome;
	}
    
    public String getClassPath() {
		return classPath;
	}
    
    public String getJriPath() {
		return jriPath;
	}
	
}
