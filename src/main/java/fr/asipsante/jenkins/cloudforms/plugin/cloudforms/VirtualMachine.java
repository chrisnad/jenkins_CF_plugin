package fr.asipsante.jenkins.cloudforms.plugin.cloudforms;

import java.util.ArrayList;

public class VirtualMachine {
	
	private String vmId;
	private String vmName;
	private ArrayList<String> vmIps;
	
	public VirtualMachine() {
	}
	
	public VirtualMachine(String vmId, String vmName, ArrayList<String> vmIps) {
		this.vmId = vmId;
		this.vmName = vmName;
		this.vmIps = vmIps;
	}
	
	public String getVmId() {
		return vmId;
	}
	
	public String getVmName() {
		return vmName;
	}
	
	public ArrayList<String> getVmIps() {
		return vmIps;
	}
	
	public void setVmId(String id) {
		this.vmId = id;
	}
	
	public void setVmName(String name) {
		this.vmName = name;
	}
	
	public void setVmIps(ArrayList<String> Ips) {
		this.vmIps = Ips;
	}

}
