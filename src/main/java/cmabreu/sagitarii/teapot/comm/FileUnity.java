package cmabreu.sagitarii.teapot.comm;

public class FileUnity {
	public static int NOT_UPLOADED = -1;  
	public static int NOT_EXISTS   = -999;  
	
	private	String name;
	private String attribute;
	private int id;
	
	public FileUnity( String fileName ) {
		name = fileName;
		id = NOT_UPLOADED;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
}
