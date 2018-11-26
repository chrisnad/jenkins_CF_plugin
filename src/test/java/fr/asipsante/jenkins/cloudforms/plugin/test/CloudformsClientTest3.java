/*
 * The MIT License
 *
 * Copyright 2017 aboittiaux.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.asipsante.jenkins.cloudforms.plugin.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsClient;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.OrderResponse;
import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CloudformsException;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okio.Buffer;

public class CloudformsClientTest3 {

    public static final int TIME_OUT_CONNECTION = 60;
    private static final String BASE_URL = "https://server/";
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudformsClient.class);
    private OkHttpClient httpClient = new OkHttpClient();
    private Certificate cert;
    private X509TrustManager trustManager;
    private String parameters;
    private OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
    private SSLSocketFactory sslSocketFactory;

    public CloudformsClientTest3() throws MalformedURLException, CertificateException {
    	
    	parameters = "{\"action\":\"order\",\"name\":\"f2ed9f88-bb10-11e8-96f8-529269fb1463\",\"resource\":{\"option_0_flavour\":\"small\",\"option_0_cname\":\"tomtnr1\",\"option_0_asip_vlan\":\"QUAL_HENIX_FRONT\",\"tag_0_asip_application\":\"93000000000283\",\"tag_0_asip_environment\":\"henix_tnr1\",\"tag_0_asip_role\":\"93000000000591\",\"some_var_key\":\"whatevermadude\",\"option_0_service_name\":\"OkHttp3-test\",\"href\":\"https://server/api/service_templates/93000000000102\"}}";
    	//parameters = "{\"action\":\"order\",\"name\":\"server_test\",\"resource\":{\"option_0_flavour\":\"small\",\"option_0_cname\":\"tomtnr1\",\"option_0_asip_vlan\":\"QUAL_HENIX_FRONT\",\"tag_0_asip_application\":\"93000000000283\",\"tag_0_asip_environment\":\"henix_tnr1\",\"tag_0_asip_role\":\"93000000000591\",\"some_var_key\":\"whatevermadude\",\"option_0_service_name\":\"server_test\",\"href\":\"https://server/api/service_templates/93000000000023\"}}";
    	
    	String cert116 = "-----BEGIN CERTIFICATE-----\r\n" + 
        		"MIICLzCCAZgCCQDJadopVxF+KzANBgkqhkiG9w0BAQUFADBcMQswCQYDVQQGEwJV\r\n" + 
        		"UzEVMBMGA1UEBwwMRGVmYXVsdCBDaXR5MREwDwYDVQQKDAhNYW5hZ2VJUTESMBAG\r\n" + 
        		"A1UECwwJTGljZW5zaW5nMQ8wDQYDVQQDDAZzZXJ2ZXIwHhcNMTgwNzA0MTEwMDI5\r\n" + 
        		"WhcNMjgwNzAxMTEwMDI5WjBcMQswCQYDVQQGEwJVUzEVMBMGA1UEBwwMRGVmYXVs\r\n" + 
        		"dCBDaXR5MREwDwYDVQQKDAhNYW5hZ2VJUTESMBAGA1UECwwJTGljZW5zaW5nMQ8w\r\n" + 
        		"DQYDVQQDDAZzZXJ2ZXIwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOhE5c8h\r\n" + 
        		"P+IAzvAe96Yd0NCcB3wtZ3Nd8nvykizjp7mJcRSR1+4EfTbzkgipnxJkdffyPLZv\r\n" + 
        		"qt2wGQQ1ysHkQOJkAY9DagdgNnhSFn34rrqtQdrdBptuQEtHgZBNu1x5497KTsFL\r\n" + 
        		"TocDlFYXsTobqlwaSHN5BqUb9ItZ+Lq69g1BAgMBAAEwDQYJKoZIhvcNAQEFBQAD\r\n" + 
        		"gYEAr4uiJ1idOEPRgZf21X4WDEWC169sNTzOp6VWeuYY59qu4rjjAYMwBeZ7EmYp\r\n" + 
        		"yxNRuNzHJe337T7/QaMChA0+HHLo1DBT1aXLzovBU0xtSgXaRJwSP/KoBuh6FrS1\r\n" + 
        		"nHlfPFfsG5P+xKjQSycg33wZqXhDW7k3qAMjj6J1YofPRP0=\r\n" + 
        		"-----END CERTIFICATE-----";
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        this.cert = factory.generateCertificate(new Buffer().writeUtf8(cert116).inputStream());
    }
    
    
    public OkHttpClient buildClient() throws MalformedURLException {

		try {
			trustManager = trustManagerForCertificates();
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { trustManager }, null);
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
    	
    	Authenticator auth = new Authenticator() {
			@Override
			public Request authenticate(Route route, Response response) throws IOException {
				String credentials = Credentials.basic("tnrjenkins", "Zpxn@v49la");
				return response.request().newBuilder().header("Authorization", credentials).build();
			}
		};
		
		URL baseUrl = new URL(BASE_URL);
		
    	httpBuilder.authenticator(auth).connectTimeout(TIME_OUT_CONNECTION, TimeUnit.SECONDS).readTimeout(60000, TimeUnit.MILLISECONDS);
		if ("https".equals(baseUrl.getProtocol()) && cert != null) {
			httpBuilder.sslSocketFactory(sslSocketFactory, trustManager);
		}
		httpClient = httpBuilder.build();
		
		return httpClient;
	}
    

    // Test de la méthode order lorsque le serviceTemplateName n'existe pas sur l'api Cloudforms
    @Ignore
    public void postRequestToCF() throws MalformedURLException, IOException, CloudformsException, InterruptedException{
    	
		URL url = new URL(BASE_URL + "/api/services?filter[]=name=firstoneletsgo&expand=resources&attributes=id");
		
		LOGGER.info("URL => {} ", url);
		
		Request request = new Request.Builder().url(url).build();
		
		LOGGER.info("test");
		
		httpClient = buildClient();
		
		Response response = httpClient.newCall(request).execute();
		
		LOGGER.info(response.toString());
		
		URL urlOrder = new URL("https://server//api/service_catalogs/93000000000001/service_templates/93000000000023");
		
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), parameters);
		Request postRequest = new Request.Builder().post(body).url(urlOrder).build();
		LOGGER.info("dis url : {}", postRequest.url());
		Response postResponse = httpClient.newCall(postRequest).execute();
		
		String responseBody = postResponse.body().string();
		LOGGER.info(postResponse.toString());
		LOGGER.info(responseBody);
		
		OrderResponse requestOrder = new Gson().fromJson(responseBody, OrderResponse.class);
		LOGGER.info(requestOrder.getServiceId());
		LOGGER.info(requestOrder.getServiceStatus());
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
				keyStore.setCertificateEntry(cert.toString(), cert);
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