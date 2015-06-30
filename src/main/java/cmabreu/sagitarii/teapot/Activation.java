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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cmabreu.sagitarii.teapot.comm.FileUnity;


public class Activation implements Comparable<Activation> {
	private int order;
	private String fragment;
	private String experiment;
	private String workflow;
	private String activitySerial;
	private String command;
	private String instanceSerial;
	private String taskId;
	private List<String> sourceData = new ArrayList<String>();
	private Activation previousActivation;
	private String xmlOriginalData;
	private String type;
	private String executor;
	private String executorType;
	private String targetTable;
	private List<FileUnity> files;
	private TaskStatus status = TaskStatus.QUEUED;
	
	public TaskStatus getStatus() {
		return status;
	}
	
	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	
	public String getTaskId() {
		return taskId;
	}

	public Activation() {
		files = new ArrayList<FileUnity>();
		UUID uuid = UUID.randomUUID();
        taskId = uuid.toString().toUpperCase().substring(0,8);
	}

	public void addFile( FileUnity file ) {
		files.add( file );
	}
	
	public List<FileUnity> getFiles() {
		return files;
	}
	
	public String getExecutor() {
		return executor;
	}

	public void setExecutor(String executor) {
		this.executor = executor;
	}

	public String getExecutorType() {
		return executorType;
	}

	public void setExecutorType(String executorType) {
		this.executorType = executorType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExperimentRootFolder() throws Exception {
		File f = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath() );
		String teapotRoot =  f.getAbsolutePath();
		teapotRoot = teapotRoot.substring(0, teapotRoot.lastIndexOf( File.separator ) + 1);
		return teapotRoot + "namespaces" + "/" + workflow + "/" + experiment ;
	}
	
	public String getNamespace() {
		String result = "";
		try {
			result = getExperimentRootFolder() + "/" + fragment + "/" + instanceSerial + "/" + taskId;
		} catch ( Exception e ) {
			
		}
		return result; 
	}
	
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public String getFragment() {
		return fragment;
	}
	
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}
	
	public String getActivitySerial() {
		return activitySerial;
	}
	public void setActivitySerial(String serial) {
		this.activitySerial = serial;
	}
	
	public String getCommand() {
		return command;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	public String getInstanceSerial() {
		return instanceSerial;
	}
	
	public void setInstanceSerial(String instanceSerial) {
		this.instanceSerial = instanceSerial;
	}
	
	@Override
	public int compareTo(Activation pipe) {
		return ( (Integer)pipe.getOrder() ).compareTo( (Integer)order );
	}
	
	public List<String> getSourceData() {
		return sourceData;
	}

	public void setSourceData(List<String> sourceData) {
		this.sourceData = sourceData;
	}

	public Activation getPreviousActivation() {
		return previousActivation;
	}

	public void setPreviousActivation(Activation previousActivation) {
		this.previousActivation = previousActivation;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public String getWorkflow() {
		return workflow;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public String getXmlOriginalData() {
		return xmlOriginalData;
	}

	public void setXmlOriginalData(String xmlOriginalData) {
		this.xmlOriginalData = xmlOriginalData;
	}

	public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}
	
}
