package cmabreu.taskmanager.core;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;


public class SystemProperties  {
    private int availableProcessors;
    private String soName;
    private String localIpAddress;
    private String machineName;
    private String macAddress;
    private OsType osType = OsType.UNIX_LIKE;
    private String javaVersion;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 

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

    
    public SystemProperties() throws Exception {
    	getProcessCpuLoad();
    	logger.debug("processors...");
    	this.availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    	logger.debug("SO name...");
    	this.soName = ManagementFactory.getOperatingSystemMXBean().getName();
    	this.localIpAddress = "***";
    	logger.debug("java version...");
    	this.javaVersion = System.getProperty("java.version");
    	getProcessCpuLoad();
    	InetAddress ip;
    	logger.debug("MAC address...");
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
			//this.macAddress =  sb.toString() + "-" + generateSerial();    	
			this.macAddress =  sb.toString();    	
		} catch ( SocketException | UnknownHostException e) {
		}
    	getProcessCpuLoad();
    	logger.debug("OS type...");
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
	
}
