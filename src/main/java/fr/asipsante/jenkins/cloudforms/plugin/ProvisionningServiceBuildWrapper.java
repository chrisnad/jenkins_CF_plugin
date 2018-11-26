/*******************************************************************************
 * (c) Copyright 1998-2017, ASIP. All rights reserved.
 ******************************************************************************/
package fr.asipsante.jenkins.cloudforms.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsBean;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsClient;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.OrderResponse;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.ScriptExecutor;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.VirtualMachine;
import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CfScriptLaunchException;
import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CloudformsException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

/**
 * Class Build wrapper, for execute action before and after build.
 * 
 * @author apierre
 *
 */
@Extension
public class ProvisionningServiceBuildWrapper extends BuildWrapper {

	/**
	 * Logger definition.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionningServiceBuildWrapper.class);
	/**
	 * time elapse before call next step.
	 */
	private static final long PAUSE_TIME_REQUEST = 10000;
	/**
	 * timeout before stop monitoring (30 minutes).
	 */
	private static final long REQUETE_TIMEOUT = (long) 1800000;
	/**
	 * list of services order in Jenkins.
	 */
	protected List<CloudformsBean> services;
	/**
	 * user added, custom environment variables for the build.
	 */
	private String customEnvVars;
	/**
	 * list of client associate to services.
	 */
	private List<CloudformsClient> clients = new ArrayList<>();

	/**
	 * name of the service order.
	 */

	/**
	 * Constructor.
	 * 
	 * @param services, list of services ordered in Jenkins.
	 * @param customEnvVars, custom environment variables for the build.
	 * 
	 */
	@DataBoundConstructor
	public ProvisionningServiceBuildWrapper(List<CloudformsBean> services, String customEnvVars) {
		this.customEnvVars = customEnvVars;
		this.services = services;
	}

	public ProvisionningServiceBuildWrapper() {

	}

	/**
	 * Method do before the build steps. Ordering and monitoring services.
	 */
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
			throws IOException, InterruptedException {

		EnvVars env = build.getEnvironment(listener);
		env.overrideAll(build.getBuildVariables());
		String[] customEnvVarsArray = customEnvVars.split("\\n");
		for (String var : customEnvVarsArray) {
			env.addLine(var);
		}

		ScriptExecutor script = new ScriptExecutor(launcher);

		for (CloudformsBean service : services) {
			try {
				final CloudformsClient client = new CloudformsClient.CloudformsClientBuilder(
						service.getCredentials().getUsername(), service.getCredentials().getPassword().getPlainText(),
						service.getBaseUrl()).certificate(service.getServerCertificate()).build();

				OrderResponse requestOrder = client.order(service.getServiceTemplateName(), service.getParameters(env),
						service.getServiceName());

				//JsonObject resources = client.getRequestParams().get("resource").getAsJsonObject();
				//Long timeoutRequest = REQUETE_TIMEOUT * resources.get("timeout").getAsLong();
				Long timeoutRequest = Long.parseLong(env.get("CF_TIMEOUT", String.valueOf(REQUETE_TIMEOUT)));

				Thread.sleep(PAUSE_TIME_REQUEST);
				
				if (client.monitorServiceProvisioning(requestOrder, timeoutRequest)) {
					service.setServiceName(client.getServiceName());
					LOGGER.info("Request successful, service provisioning complete");

					int serviceCount = 0;
					int vmCount = 0;
					
					//MyService_COUNT= [number of services with name "MyService"]
					env.addLine(service.getServiceUniqueId() + "_COUNT=" + client.getServiceIds(client.getServiceName()).size());
					
					for (String serviceId : client.getServiceIds(client.getServiceName())) {
						serviceCount++;
						
						//MyService_N_VM_COUNT= [number of VMs  in the Nth service with name "MyService"]
						//"N" is a number.
						env.addLine(service.getServiceUniqueId() + "_" + serviceCount + "_VM_COUNT=" + client.getVmIds(serviceId).size());
						
						for (String vmId : client.getVmIds(serviceId)) {
							vmCount++;
							VirtualMachine vm = null;
							vm = client.getVmAttributes(vmId);
							String vmCustomId = service.getServiceUniqueId() + "_" + serviceCount + "_VM" + vmCount;
							
							//MyService_N_VM1_NAME= [name of 1st VM in the Nth service with name "MyService"]
							env.addLine(vmCustomId + "_NAME=" + vm.getVmName());
							
							//MyService_N_VM1_IP_COUNT= [number of IPs in the Nth service with name "MyService"]
							env.addLine(vmCustomId + "_IP_COUNT=" + vm.getVmIps().size());
							
							for (int i=0; i < vm.getVmIps().size(); i++) {
								
								//MyService_N_VM_COUNT= [number of VMs  in the Nth service with name "MyService"]
								env.addLine(vmCustomId + "_IP" + i + "=" + vm.getVmIps().get(i));
							}
						}
					}
					LOGGER.info(env.toString());
					
					FilePath rootPath = build.getWorkspace();
					int resultCode;
					try {
						resultCode = script.executeScript(rootPath, service.getCommandLine());
					} catch (CfScriptLaunchException e) {
						throw new CloudformsException("FAILED, error during script execution : ", e);
					}
					if (resultCode != 0) {
						LOGGER.error("Script return code : {}", resultCode);
						build.setResult(Result.FAILURE);
					}

				} else {
					LOGGER.error("Request monitoring failure");
					throw new CloudformsException("FAILED, error during request monitoring");
				}
				
				clients.add(client);
				

			} catch (CloudformsException e) {
				LOGGER.error("CloudformsBuildStep see : ", e);
			}
		}

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) {
				LOGGER.info("wrapper methode teardown de l'environment");
				boolean result = false;
				try {
					result = doTearDown();
				} catch (CloudformsException e) {
					e.printStackTrace();
				}
				return result;
			}
		};
	}

	/**
	 * Method tearDown, do after build steps. Delete services.
	 * 
	 * @return true if all services are deleted.
	 * @throws CloudformsException exception
	 */
	protected boolean doTearDown() throws CloudformsException {
		boolean result = false;
		LOGGER.info(" *** tearDown du service ! ***");
		for (CloudformsBean service : services) {
			if (service.getDestroy()) {
				LOGGER.info("case retire cochée");
				result = doRetireService(service);
			} else {
				LOGGER.info("case retire non cochée");
				result = true;
			}
		}
		return result;
	}

	private boolean doRetireService(CloudformsBean service) throws CloudformsException {
		boolean result = false;
		for (CloudformsClient client : clients) {
			if (client.getServiceName().equals(service.getServiceName())) {
				if (client.retire(client.getServiceName())) {
					LOGGER.info("Service retire with SUCCESS");
					result = true;
				} else {
					LOGGER.info("Can not retire service FAILED");
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * need for decorate the wrapper.
	 */
	@Extension
	/**
	 * descriptor class for the wrapper in Jenkins.
	 * 
	 * @author apierre
	 *
	 */
	public static class DescriptorImpl extends BuildWrapperDescriptor {

		/**
		 * Name under which the wrapper appears in Jenkins.
		 */
		@Override
		public String getDisplayName() {
			return "Order Cloudforms services";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> ap) {
			return true;
		}

	}

	public List<CloudformsBean> getServices() {
		return services;
	}
}
