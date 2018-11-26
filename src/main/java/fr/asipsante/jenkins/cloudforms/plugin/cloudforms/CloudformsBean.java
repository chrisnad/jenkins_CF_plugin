/*******************************************************************************
 * (c) Copyright 1998-2017, ASIP. All rights reserved.
 ******************************************************************************/
package fr.asipsante.jenkins.cloudforms.plugin.cloudforms;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import okio.Buffer;
/**
 * Cloudforms configuration for connect to Cloudforms and define the service to order.
 * @author apierre
 *
 */
public class CloudformsBean extends AbstractDescribableImpl<CloudformsBean> {
	
	/**
	 * logger definition.
	 */
    private static final Logger LOGGER = Logger.getLogger(CloudformsBean.class.getName());
    /**
     * name of the template service to order.
     */
    private final String serviceTemplateName;
    /**
     * credentials.
     */
    private final String credentialsId;
    /**
     * parameters to order.
     */
    private final String parameters;
    /**
     * Command line.
     */
    private final String commandLine;
    /**
     * true for retire the service at the end.
     */
    private final boolean destroy;
    /**
     * Service unique id
     */
    private final String serviceUniqueId;
    /**
     * cloudforms certificate.
     */
    private Certificate serverCertificate;
    /**
     * base url for cloudforms api.
     */
    private URL baseUrl;
    /**
     * credentials for cloudforms.
     */
    private StandardUsernamePasswordCredentials credentials;
    /**
     * name of the service order.
     */
    private String serviceName;
    
    /**
     * Constructor with the parameters entered in Jenkins.
     * @param commandLine, command line
     * @param serviceTemplateName, Service template name.
     * @param serviceUniqueId, Service unique id
     * @param parameters, parameters
     * @param serverCertificate, server certificate
     * @param credentialsId, credentials
     * @param baseUrl, url
     * @param destroy, retire
     */
    @DataBoundConstructor
    public CloudformsBean(String serviceTemplateName, String parameters, Certificate serverCertificate, String credentialsId, URL baseUrl, boolean destroy, String commandLine, String serviceUniqueId) {
        this.serviceTemplateName = serviceTemplateName;
        this.parameters = parameters;
        this.serverCertificate = serverCertificate;
        this.credentialsId = credentialsId;
        this.commandLine = Util.fixEmptyAndTrim(commandLine);
        this.baseUrl = baseUrl;
        this.destroy = destroy;
        this.serviceUniqueId = serviceUniqueId;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * service template name getter.
     * @return the service template name.
     */
    public String getServiceTemplateName() {
        return serviceTemplateName;
    }
    /**
     * service id getter.
     * @return the service id.
     */
    public String getServiceUniqueId() {
        return serviceUniqueId;
    }
    /**
     * parameters getter.
     * @param env, jenkins environment variables
     * @return modified parameters.
     */
    public String getParameters(EnvVars env) {
    	
    	String params = parameters;
		for (Entry<String, String> entry : env.entrySet()) {
			params = params.replace("$"+entry.getKey(), entry.getValue());
			params = params.replace("${"+entry.getKey()+"}", entry.getValue());
		}
        return params;
    }
    /**
     * initial parameters getter.
     * @return modified parameters.
     */
    public String getParameters() {
    	return parameters;
    }
    /**
     * command line getter.
     * @return a command line.
     */
    public String getCommandLine() {
        return commandLine;
    }
    /**
     * serverCertificate getter.
     * @return a certificate.
     */
    public Certificate getServerCertificate() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            serverCertificate = factory.generateCertificate(new Buffer().writeUtf8(getDescriptor().getServerCertificate()).inputStream());
        } catch (CertificateException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return serverCertificate;
    }
    
    /**
     * credentials id getter.
     * @return id.
     */
    public String getCredentialsId() {
        return credentialsId;
    }
    /**
     * base url getter.
     * @return baseurl.
     */
    public URL getBaseUrl() {
        try {
            baseUrl = new URL(getDescriptor().getBaseUrl());
        } catch (MalformedURLException ex) {
            Logger.getLogger(CloudformsBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        return baseUrl;
    }

    /**
     * credentials getter.
     * @return a credential.
     */
    public StandardUsernamePasswordCredentials getCredentials() {
        Jenkins jenkins = Jenkins.getInstance();
        List<StandardUsernamePasswordCredentials> matches = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialsId);
        credentials = CredentialsMatchers.firstOrNull(matches, matcher);
        return credentials;
    }
    
    /**
     * destroy getter
     * @return boolean.
     */
    public boolean getDestroy() {
    	return destroy;
    }
    /**
     * service name getter.
     * @return name.
     */
    public String getServiceName() {
    	serviceName = UUID.randomUUID().toString() + "-" + serviceUniqueId;
		return serviceName;
	}
    /**
     * name setter.
     * @param name, name
     */
	public void setServiceName(String name) {
		this.serviceName = name;
	}


	/**
	 * Inner Class for decorate the section in Jenkins.
	 * @author apierre
	 *
	 */
	@Extension
    public static final class DescriptorImpl extends Descriptor<CloudformsBean> {

        private String baseUrl;
        private String serverCertificate;

        /**
         * default constructor.
         */
        public DescriptorImpl() {
        	load();
        }
		
        @Override
        public String getDisplayName() {
            return "Cloudforms";
        }

        /**
         * create a listBox for the credentials items.
         * @return a listBox.
         */
        public ListBoxModel doFillCredentialsIdItems() {
            return new StandardListBoxModel().withEmptySelection().withMatching(CredentialsMatchers.instanceOf(UsernamePasswordCredentials.class), CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class));
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.baseUrl = json.getString("baseUrl");
            this.serverCertificate = json.getString("serverCertificate");
            save();
            return super.configure(req, json);
        }

        /**
         * getter for baseUrl.
         * @return the base Url.
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * getter for serverCertificate.
         * @return the serverCertificate.
         */
        public String getServerCertificate() {
            return serverCertificate;
        }
        
        /**
         * check that 'serviceName' is not empty.
         * @param project, project
         * @param value, value
         * @return validation.
         */
        public FormValidation doCheckServiceName(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter String value) {
            if (0 == value.length()) {
                return FormValidation.error("Empty service name");
            }
            return FormValidation.ok();
        }
        
        /**
         * check that 'parameters' is not empty.
         * @param project, project
         * @param value, value
         * @return validation.
         */
        public FormValidation doCheckParameters(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter String value) {
            if (0 == value.length()) {
                return FormValidation.error("Empty parameters");
            }
            return FormValidation.ok();
        }

        /**
         * check that 'credentials' is not empty.
         * @param project, project
         * @param value, value
         * @return validation.
         */
        public FormValidation doCheckCredentialsId(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter String value) {
            if (0 == value.length()) {
                return FormValidation.error("Choose credentials");
            }
            return FormValidation.ok();
        }
    }

}
