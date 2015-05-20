package cmabreu.taskmanager.core;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import cmabreu.sagitarii.teapot.Activation;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;


public class TaskManager implements ITaskManager {
	private List<ITask> tasks = new ArrayList<ITask>();
	private List<ITask> recordedTasks = new ArrayList<ITask>();
	private String recordTaskSerial = "";
    private int availableProcessors;
    private String soName;
    private String localIpAddress;
    private String machineName;
    private String macAddress;
    private OsType osType = OsType.UNIX_LIKE;
    private String javaVersion;
    private ITaskObserver taskObserver;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private int runningTaskCount = 0;

	@Override
	public List<ITask> getTasks() {
		return new ArrayList<ITask>( tasks );
	}

	public void startRecord( String recordTaskSerial ) {
		this.recordTaskSerial = recordTaskSerial;
	}
	
	public void stopRecord() {
		this.recordTaskSerial = "";
	}
	
	public void flushRecordMemory() {
		recordedTasks.clear();
	}
	
	public List<ITask> getRecordedTasks() {
		return new ArrayList<ITask>( recordedTasks );
	}

	
    @Override
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
    
    @Override
    public String getJavaVersion() {
    	return this.javaVersion;
    }
    
    @Override
    public void setObserver( ITaskObserver taskObserver ) {
    	this.taskObserver = taskObserver;
    }
    
    public TaskManager() throws Exception {
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
    
    @Override
    public String getMacAddress() {
    	return this.macAddress;
    }
    
    @Override
    public String getMachineName() {
    	return this.machineName;
    }
    
    @Override 
    public Double getCpuLoad() {
    	return getProcessCpuLoad();
    }
    
    @Override
    public String getLocalIpAddress() {
    	return this.localIpAddress;
    }
	
    @Override
    public int getAvailableProcessors() {
    	return this.availableProcessors;
    }

    @Override
    public String getSoName() {
    	return this.soName;
    }
    
	@Override
	public int getRunningTaskCount() {
		if ( runningTaskCount < 0 ) {
			runningTaskCount = 0;
		}
		return runningTaskCount;
	}

	@Override
	public String startTask( Activation activation , String applicationName ) {
		String pipelineId = activation.getPipelineSerial();
		int order = activation.getOrder();
		
		DateLibrary dl = DateLibrary.getInstance();
		dl.setTo( new Date() );
 
		logger.debug("start task " + activation.getType() + " " + activation.getActivitySerial() + " ("+ pipelineId + "-" + order + "):");
		logger.debug( applicationName );
        
		ITask task = new ExternalTask( this, activation, applicationName );
		task.setSourceData( activation.getSourceData() );
		
		Thread executorThread = new Thread(task);
        executorThread.setName("Teapot task " + activation.getActivitySerial() + pipelineId + order );
        executorThread.start();
        this.tasks.add(task);
        runningTaskCount++;
        
        if ( recordTaskSerial.length() > 30000 ) {
        	flushRecordMemory();
        }
        
        System.out.println("tasks running " + runningTaskCount );
        
        if ( activation.getActivitySerial().equals( recordTaskSerial ) ) {
        	recordedTasks.add( task );
        }
        
        return activation.getActivitySerial() + pipelineId + order;
	}
	
	
	@Override
	public void notify( ITask task ) {
		this.tasks.remove(task);
		runningTaskCount--;
		if( taskObserver != null ) {
			taskObserver.notify( task );
		}
	}

}
