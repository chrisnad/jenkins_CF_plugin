/*******************************************************************************
 * (c) Copyright 1998-2017, ASIP. All rights reserved.
 ******************************************************************************/
package fr.asipsante.jenkins.cloudforms.plugin.cloudforms;

import com.google.gson.JsonObject;



public class OrderParameters {

    private String action;
    private String name;
    private JsonObject resource;
    
    public OrderParameters() {
    }

    public OrderParameters(String action, String name, JsonObject resource) {
        this.action = action;
        this.resource = resource;
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public JsonObject getResource() {
        return resource;
    }

    public void setResource(JsonObject resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OrderParameters [action=" + action + ", name=" + name + ", resource=" + resource + "]";
	}

    
    
    
}
