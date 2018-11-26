/*******************************************************************************
 * (c) Copyright 1998-2017, ASIP. All rights reserved.
 ******************************************************************************/
package fr.asipsante.jenkins.cloudforms.plugin.cloudforms;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CloudformsException;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Class used to order or retire a service and to monitor an order request on
 * Cloudforms's api.
 *
 * @author aboittiaux
 */
public class CloudformsClient {

	/**
	 * Logger definition.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CloudformsClient.class);
	/**
	 * pause during the monitoring.
	 */
	private static final long REFRESH_RATE = 10000;
	/**
	 * client okHttp.
	 */
	private final OkHttpClient okHttpClient;
	/**
	 * base url for the cloudforms api.
	 */
	private final URL baseUrl;
	/**
	 * gson builder.
	 */
	private final Gson gson = new Gson();
	/**
	 * parameters for the request.
	 */
	private final JsonObject requestParams = new JsonObject();
	/**
	 * name of the service order.
	 */
	private String serviceName = "";
	/**
	 * parameters for order a service.
	 */
	private OrderParameters orderParameters;
	// private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

	/**
	 * constructor using the inner class to provide the client.
	 * 
	 * @param builder, builder
	 */
	public CloudformsClient(CloudformsClientBuilder builder) {
		this.baseUrl = builder.baseUrl;
		this.okHttpClient = builder.httpClient;
	}

	/**
	 * Method order used to Bundle or VM provisioning
	 * 
	 * @param serviceTemplateName template name of the service.
	 * @param properties          parameters for the service.
	 * @param serviceName         unique name of the service.
	 * @return orderResponse      response object.
	 * @throws IOException          exception.
	 * @throws InterruptedException exception.
	 */
	public OrderResponse order(String serviceTemplateName, String properties, String serviceName)
			throws IOException, InterruptedException {

		ServiceParameters parameters = getServiceTemplateParameters(serviceTemplateName);
		String href = null;
		String idServiceCatalogs = null;
		String id = null;
		Response response;
		OrderResponse orderResponse = new OrderResponse();

		if (parameters != null && !parameters.getResources().isEmpty()) {
			for (Map<String, String> resource : parameters.getResources()) {
				idServiceCatalogs = resource.get("service_template_catalog_id");
				href = resource.get("href");
				id = resource.get("id");
			}

			JsonObject jsonParameters = gson.fromJson(properties, JsonObject.class);
			jsonParameters.add("href", gson.toJsonTree(href));
			if (jsonParameters.get("option_0_service_name") != null) {
				serviceName = jsonParameters.get("option_0_service_name").getAsString();
			}
			this.serviceName = serviceName;

			orderParameters = new OrderParameters("order", serviceName, jsonParameters);

			requestParams.add("action", gson.toJsonTree(orderParameters.getAction()));
			requestParams.add("name", gson.toJsonTree(orderParameters.getName()));
			requestParams.add("resource", gson.toJsonTree(jsonParameters));

			LOGGER.info("Paramètres de requete order => {} ", requestParams);
			URL urlOrder = new URL(baseUrl + "/api/service_catalogs/" + idServiceCatalogs + "/service_templates/" + id);
			LOGGER.info("URL => {} ", urlOrder);
			LOGGER.debug(urlOrder.toString());
			LOGGER.debug(requestParams.toString());
			RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestParams));
			Request request = new Request.Builder().post(body).url(urlOrder).build();

			response = okHttpClient.newCall(request).execute();
			String responseBody = response.body().string();
			LOGGER.info("Response => {} ", response.toString());

			if (!response.isSuccessful()) {
				try {
					throw new CloudformsException(responseBody);
				} catch (CloudformsException e) {
					LOGGER.error("Can not order the service, see :", e);
				}
			}
			
			orderResponse = new Gson().fromJson(responseBody, OrderResponse.class);
		}
		return orderResponse;
	}

	/**
	 * Method to retire a service
	 * 
	 * @param serviceName name of the service to retire.
	 * @return true if the service is retire.
	 * @throws CloudformsException exception.
	 */
	public boolean retire(String serviceName) throws CloudformsException {
		try {
			ServiceParameters parameters = getServiceParametersByName(serviceName);
			String id = "";
			JsonObject requestContent = new JsonObject();
			Response response;
			if (parameters != null && !parameters.getResources().isEmpty()) {
				for (Map<String, String> resource : parameters.getResources()) {
					id = resource.get("id");
				}
				requestContent.add("action", gson.toJsonTree("retire"));
				URL urlRetire = new URL(baseUrl + "/api/services/" + id);
				LOGGER.info("url retire --> {} ", urlRetire);
				RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestContent));
				Request request = new Request.Builder().post(body).url(urlRetire).build();
				response = okHttpClient.newCall(request).execute();
				return response.isSuccessful();
			}
		} catch (MalformedURLException e) {
			throw new CloudformsException("Bad URL", e);
		} catch (IOException e) {
			throw new CloudformsException("Can not execute request", e);
		}

		return false;
	}

	/**
	 * Method to monitor the service provisioning.
	 * 
	 * @param orderResponse request to execute.
	 * @param timeout      duration to execute the request.
	 * @return true if service finished, false else.
	 * @throws InterruptedException request interrupted.
	 */
	public boolean monitorServiceProvisioning(OrderResponse orderResponse, Long timeout) throws InterruptedException {
		boolean provisioning = false;
		long maxDuration = System.currentTimeMillis() + timeout;
		try {
			if (orderResponse != null) {
				String idRequest = "0";
				String status = "";

				BigDecimal idRequestDecimal = new BigDecimal(orderResponse.getServiceId());
				idRequest = idRequestDecimal.toPlainString();
				status = orderResponse.getServiceStatus();

				provisioning = monitorRequeteProvisioning(status, idRequest, maxDuration);
			}
		} catch (MalformedURLException urlEx) {
			LOGGER.error("problem with the URL, see :", urlEx);
		} catch (IOException ioEx) {
			LOGGER.error("problem during monitoring, see :", ioEx);
		}
		return provisioning;
	}

	/**
	 * Private method that returns a ServiceParameters object, powered by Cloudforms
	 * api call with the service name.
	 * 
	 * @param serviceName.
	 * @return a serviceParameters.
	 * @throws IOException
	 */
	private ServiceParameters getServiceParameters(String serviceName) throws IOException {

		URL url = new URL(baseUrl + "/api/services?filter[]=name=" + serviceName + "&expand=resources&attributes=id");
		ServiceParameters serviceParameters = null;

		// Appel à l'api Cloudforms pour récupérer les infos nécessaires au
		// provisionnement d'un service
		try {
			Request request = new Request.Builder().url(url).build();

			Response response = okHttpClient.newCall(request).execute();

			String jsonService = response.body().string();
			LOGGER.info("json service response ==> {}", jsonService);

			if (response.isSuccessful()) {
				serviceParameters = gson.fromJson(jsonService, ServiceParameters.class);
				if (serviceParameters.getResources().isEmpty()) {
					throw new CloudformsException(jsonService);
				}
			}
		} catch (IOException iOException) {
			LOGGER.error(iOException.getMessage(), iOException);
		} catch (JsonSyntaxException jsonSyntaxException) {
			LOGGER.error(jsonSyntaxException.getMessage(), jsonSyntaxException);
		} catch (CloudformsException cloudformsException) {
			LOGGER.error(cloudformsException.getMessage(), cloudformsException);
		}
		return serviceParameters;
	}

	public ArrayList<String> getServiceIds(String serviceName) throws IOException {

		ServiceParameters parameters = getServiceParameters(serviceName);
		ArrayList<String> id = new ArrayList<String>();

		if (parameters != null && !parameters.getResources().isEmpty()) {
			for (Map<String, String> resource : parameters.getResources()) {
				id.add(resource.get("id"));
			}
		}
		return id;
	}

	/**
	 * Private method that returns a ServiceParameters object, powered by Cloudforms
	 * api call with the service Id.
	 * 
	 * @param serviceId.
	 * @return a serviceParameters.
	 * @throws IOException
	 */
	private ServiceParameters getServiceVms(String serviceId) throws IOException {

		URL url = new URL(baseUrl + "/api/services/" + serviceId + "/vms?expand=resources&attributes=id");
		ServiceParameters serviceParameters = null;

		// Appel à l'api Cloudforms pour récupérer les infos nécessaires au
		// VMs d'un service
		try {
			Request request = new Request.Builder().url(url).build();

			Response response = okHttpClient.newCall(request).execute();

			String jsonService = response.body().string();
			LOGGER.info("json service response ==> {}", jsonService);

			if (response.isSuccessful()) {
				serviceParameters = gson.fromJson(jsonService, ServiceParameters.class);
				if (serviceParameters.getResources().isEmpty()) {
					throw new CloudformsException(jsonService);
				}
			}
		} catch (IOException iOException) {
			LOGGER.error(iOException.getMessage(), iOException);
		} catch (JsonSyntaxException jsonSyntaxException) {
			LOGGER.error(jsonSyntaxException.getMessage(), jsonSyntaxException);
		} catch (CloudformsException cloudformsException) {
			LOGGER.error(cloudformsException.getMessage(), cloudformsException);
		}
		return serviceParameters;
	}

	public ArrayList<String> getVmIds(String serviceId) throws IOException {

		ServiceParameters parameters = getServiceVms(serviceId);
		ArrayList<String> id = new ArrayList<String>();

		if (parameters != null && !parameters.getResources().isEmpty()) {
			for (Map<String, String> resource : parameters.getResources()) {
				id.add(resource.get("id"));
			}
		}
		return id;
	}

	/**
	 * Public method that returns a VirtualMachine object, powered by Cloudforms
	 * api call with the VM id.
	 * 
	 * @param vmId.
	 * @return a virtualMachine.
	 * @throws IOException
	 */
	public VirtualMachine getVmAttributes(String vmId) throws IOException {

		URL url = new URL(baseUrl + "/api/vms/" + vmId + "?expand=resources&attributes=name,ipaddresses");

		VirtualMachine virtualMachine = new VirtualMachine();

		// Appel à l'api Cloudforms pour récupérer les infos nécessaires à
		// une VM en particulier
		try {
			Request request = new Request.Builder().url(url).build();

			Response response = okHttpClient.newCall(request).execute();

			String jsonService = response.body().string();
			JsonObject jo = new JsonObject();
			jo.getAsJsonObject(jsonService);
			LOGGER.info("json service response ==> {}", jsonService);

			if (response.isSuccessful()) {
				virtualMachine.setVmId(jo.get("id").getAsString());
				virtualMachine.setVmName(jo.get("name").getAsString());

				ArrayList<String> ipAdresses = new ArrayList<String>(
						Arrays.asList(jo.getAsJsonArray("ipaddresses").getAsString().split(",")));

				virtualMachine.setVmIps(ipAdresses);

				if (virtualMachine.getVmId().isEmpty()) {
					throw new CloudformsException(jsonService);
				}
			}

		} catch (IOException iOException) {
			LOGGER.error(iOException.getMessage(), iOException);
		} catch (JsonSyntaxException jsonSyntaxException) {
			LOGGER.error(jsonSyntaxException.getMessage(), jsonSyntaxException);
		} catch (CloudformsException cloudformsException) {
			LOGGER.error(cloudformsException.getMessage(), cloudformsException);
		}
		return virtualMachine;
	}

	public URL getBaseUrl() {
		return baseUrl;
	}

	public String getServiceName() {
		return serviceName;
	}

	public JsonObject getRequestParams() {
		return requestParams;
	}

	public OrderParameters getOrderParameters() {
		return orderParameters;
	}

	/**
	 * Private method, who return an ServiceParameters object, powered by the api
	 * Cloudforms call with the serviceTemplate name.
	 * 
	 * @param serviceTemplateName.
	 * @return a serviceParameters.
	 * @throws IOException
	 */
	private ServiceParameters getServiceTemplateParameters(String serviceTemplateName) throws IOException {
		URL url = new URL(baseUrl + "/api/service_templates?filter[]=name=" + serviceTemplateName
				+ "&expand=resources&attributes=href,service_template_catalog_id");
		ServiceParameters serviceParameters = null;

		// Appel à l'api Cloudforms pour récupérer les infos nécessaires au
		// provisionnement d'un serviceTemplate
		try {
			Request request = new Request.Builder().url(url).build();

			Response response = okHttpClient.newCall(request).execute();

			String jsonService = response.body().string();
			LOGGER.info("json service response ==> {}", jsonService);

			if (response.isSuccessful()) {
				serviceParameters = gson.fromJson(jsonService, ServiceParameters.class);
				if (serviceParameters.getResources().isEmpty()) {
					throw new CloudformsException(jsonService);
				}
			}
		} catch (IOException iOException) {
			LOGGER.error(iOException.getMessage(), iOException);
		} catch (JsonSyntaxException jsonSyntaxException) {
			LOGGER.error(jsonSyntaxException.getMessage(), jsonSyntaxException);
		} catch (CloudformsException cloudformsException) {
			LOGGER.error(cloudformsException.getMessage(), cloudformsException);
		}
		return serviceParameters;
	}

	/**
	 * Private method for monitoring the service request.
	 * 
	 * @param status      of the request.
	 * @param             idRequest.
	 * @param maxDuration timeout for the monitoring.
	 * @return true if the service provisioning is finished.
	 * @throws IOException
	 */
	private boolean monitorRequeteProvisioning(String status, String idRequest, Long maxDuration) throws IOException {
		boolean provisioning = false;
		String requestState = "";
		try {
			if ("Ok".equals(status)) {
				URL url = new URL(baseUrl + "/api/service_requests/" + idRequest);
				Request request = new Request.Builder().url(url).build();
				LOGGER.info("requete monitoring service ===> {}", request);
				do {
					Response response = okHttpClient.newCall(request).execute();
					String result = response.body().string();
					LOGGER.info("result => {}", result);
					@SuppressWarnings("unchecked")
					Map<String, Object> requestMonitoring = (Map<String, Object>) gson.fromJson(result, Map.class);
					if (!requestMonitoring.isEmpty()) {
						LOGGER.info("Taille de la réponse => {}", requestMonitoring.size());
						requestState = (String) requestMonitoring.get("request_state");
					}
					Thread.sleep(REFRESH_RATE);
				} while (!"finished".equals(requestState) && (System.currentTimeMillis() < maxDuration));
				if ("finished".equals(requestState)) {
					provisioning = true;
				} else {
					LOGGER.info("monitoring not ending, status not finished, timeout");
				}
			}
		} catch (MalformedURLException urlEx) {
			LOGGER.error("problem with URL, see : ", urlEx);
		} catch (InterruptedException e) {
			LOGGER.error("problem during pause, see :", e);
		}
		return provisioning;
	}

	/**
	 * Private method, who return an ServiceParameters object, powered by the api
	 * Cloudforms call with the service name.
	 * 
	 * @param serviceName name of the service.
	 * @return a serviceParameters.
	 * @throws MalformedURLException
	 */
	private ServiceParameters getServiceParametersByName(String serviceName) throws MalformedURLException {
		LOGGER.info("serviceName --> : {}", serviceName);
		URL url = new URL(
				baseUrl + "/api/services?filter[]=name=" + serviceName + "&expand=resources&attributes=href,id");
		ServiceParameters serviceParameters = null;
		try {
			Request request = new Request.Builder().url(url).build();
			Response response = okHttpClient.newCall(request).execute();
			String jsonService = response.body().string();
			LOGGER.info("reponse de la requete service by name -->> {}", jsonService);
			if (response.isSuccessful()) {
				serviceParameters = gson.fromJson(jsonService, ServiceParameters.class);
				if (serviceParameters.getResources().isEmpty()) {
					throw new CloudformsException("No parameters found : " + jsonService);
				}
			}
		} catch (IOException iOException) {
			LOGGER.error(iOException.getMessage(), iOException);
		} catch (CloudformsException cloudformsException) {
			LOGGER.error(cloudformsException.getMessage(), cloudformsException);
		}
		return serviceParameters;
	}

	/**
	 * Builder for configure the CloudformsClient with the given parameters.
	 * 
	 * @author apierre
	 *
	 */
	public static class CloudformsClientBuilder {

		public static final int TIME_OUT_CONNECTION = 60;
		public static final int READ_OUT_CONNECTION = 60;
		public static final int WRITE_OUT_CONNECTION = 60;
		private final URL baseUrl;
		private Authenticator auth;
		private Certificate serverCertificate;
		private OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
		private OkHttpClient httpClient;
		private X509TrustManager trustManager;
		private SSLSocketFactory sslSocketFactory;
		private String userName;
		private String password;

		/**
		 * constructor for client Builder with obligatory parameters.
		 * 
		 * @param userName.
		 * @param password.
		 * @param baseUrl.
		 */
		public CloudformsClientBuilder(final String userName, final String password, URL baseUrl) {
			this.baseUrl = baseUrl;
			this.userName = userName;
			this.password = password;
		}

		/**
		 * generate a cloudforms client with a certificate.
		 * 
		 * @param serverCertificate for client.
		 * @return a cloudforms client builder.
		 */
		public CloudformsClientBuilder certificate(Certificate serverCertificate) {
			
			this.serverCertificate = serverCertificate;
			
			try {
				trustManager = trustManagerForCertificates();
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, new TrustManager[] { trustManager }, null);
				sslSocketFactory = sslContext.getSocketFactory();
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		/**
		 * build the client.
		 * 
		 * @return a cloudforms client.
		 */
		public CloudformsClient build() {

			auth = new Authenticator() {
				@Override
				public Request authenticate(Route route, Response response) throws IOException {
					String credentials = Credentials.basic(userName, password);
					return response.request().newBuilder().header("Authorization", credentials).build();
				}
			};

			httpBuilder.authenticator(auth).connectTimeout(TIME_OUT_CONNECTION, TimeUnit.SECONDS)
					.readTimeout(READ_OUT_CONNECTION, TimeUnit.SECONDS)
					.writeTimeout(WRITE_OUT_CONNECTION, TimeUnit.SECONDS);
			if ("https".equals(baseUrl.getProtocol()) && serverCertificate != null) {
				httpBuilder.sslSocketFactory(sslSocketFactory, trustManager);
			}
			httpClient = httpBuilder.build();
			return new CloudformsClient(this);
		}

		private X509TrustManager trustManagerForCertificates() throws GeneralSecurityException {
			// Put the certificates a key store.
			char[] passwordKeyStore = "password".toCharArray();
			KeyStore keyStore = null;
			try {
				keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				InputStream in = null;
				if (keyStore != null) {
					keyStore.load(in, passwordKeyStore);
					keyStore.setCertificateEntry(serverCertificate.toString(), serverCertificate);
				}
			} catch (IOException e) {
				throw new AssertionError(e);
			} catch (NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
				LOGGER.error(ex.getMessage(), ex);
			}

			// Use it to build an X509 trust manager.
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, passwordKeyStore);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
			if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
				throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
			}
			return (X509TrustManager) trustManagers[0];
		}

	}
}
