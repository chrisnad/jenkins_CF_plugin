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
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsBean;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsClient;
import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.OrderResponse;
import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CloudformsException;

public class CloudformsClientTest {

    private static final String SERVICE_NAME = "serviceName";
    private static final String BASE_URL = "http://localhost:8090";
    //private static final String REPONSE_CLOUDFORMS_REQUEST_OK = "{\"results\":[{\"id\":93000000004069,\"description\":\"Provisioning Service [serviceName] from [serviceName]\",\"approval_state\":\"pending_approval\",\"type\":\"ServiceTemplateProvisionRequest\",\"created_on\":\"2017-03-14T14:56:17Z\",\"updated_on\":\"2017-03-14T14:56:18Z\",\"requester_id\":93000000000012,\"requester_name\":\"UserSelfservice\",\"request_type\":\"clone_to_service\",\"request_state\":\"pending\",\"message\":\"Service_Template_Provisioning - Request Created\",\"status\":\"Ok\",\"options\":{\"dialog\":{\"tag_environment\":\"environment\"}},\"userid\":\"user\",\"source_id\":93000000000008,\"source_type\":\"ServiceTemplate\"}]}";
    private static final boolean DESTROY = false;
    
    private final Certificate cert = null;
    private final CloudformsBean bean;
    private CloudformsClient client;
    private final List<Map<String, Object>> result = new ArrayList<>();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);

    public CloudformsClientTest() throws MalformedURLException {
        String parameters = "{'environment':'environment', 'timeout':'1'}";
        this.bean = new CloudformsBean(SERVICE_NAME, parameters, cert, "credentialsId", new URL(BASE_URL), DESTROY, "", "uniqueid");

    }

//    // Test de la méthode order dans le cas nominal: c'est à dire avec tout les paramètres bien renseignés pour le provisionnement d'un serviceTemplate 
//    @Test
//    public void testOrderNominalCase() throws IOException, CloudformsException {
//
//        client = new CloudformsClient.CloudformsClientBuilder("userName", "password", new URL(BASE_URL)).build();
//        RequestOrder reponseOrder = client.order(SERVICE_NAME, bean.getParameters());
//        Gson gson = new Gson();
//        RequestOrder order = gson.fromJson(REPONSE_CLOUDFORMS_REQUEST_OK, RequestOrder.class);
//        assertThat(reponseOrder.getResults(), equalTo(order.getResults()));
//    }

      //Méthode qui permet de simuler la réponse du serveur avec différentes réponses.
    @Ignore
    public void simulateMonitoring() throws IOException, MalformedURLException, InterruptedException, InterruptedException, InterruptedException, InterruptedException, InterruptedException {
        Map<String, Object> mapTest = new HashMap<>();
        mapTest.put("status", "Ok");
        mapTest.put("id", "9.3000000004069E13");
        mapTest.put("request_state", "pending");
        Long timeout = (long) 10000;
        result.add(mapTest);
        OrderResponse order = new OrderResponse("9.3000000004069E13", "Ok");
        client = new CloudformsClient.CloudformsClientBuilder("userName", "password", new URL(BASE_URL)).build();
        boolean b = client.monitorServiceProvisioning(order, timeout);
        Assert.assertTrue(b);
    }
    
    
    @Ignore
    public void testBigInt() {
    	String json = "{\"results\":[{\"id\":93000000004060,\"description\":\"Provisioning Service [serviceName] from [serviceName]\",\"approval_state\":\"pending_approval\",\"type\":\"ServiceTemplateProvisionRequest\",\"created_on\":\"2017-03-14T14:56:17Z\",\"updated_on\":\"2017-03-14T14:56:18Z\",\"requester_id\":93000000000012,\"requester_name\":\"UserSelfservice\",\"request_type\":\"clone_to_service\",\"request_state\":\"pending\",\"message\":\"Service_Template_Provisioning - Request Created\",\"status\":\"Ok\",\"options\":{\"dialog\":{\"tag_environment\":\"environment\"}},\"userid\":\"user\",\"source_id\":93000000000008,\"source_type\":\"ServiceTemplate\"}]}";
    	GsonBuilder gsonBuilder = new GsonBuilder();
    	gsonBuilder.setLongSerializationPolicy(LongSerializationPolicy.STRING);
    	Gson gson = gsonBuilder.create();
    	OrderResponse reqOrder = gson.fromJson(json, OrderResponse.class);
    	
    	String id = reqOrder.getServiceId();
    	BigDecimal bigid = new BigDecimal(id);
    	System.out.println("bigid ---> " + bigid);
    	String newid = bigid.toPlainString();
    	System.out.println("newid ==> " + newid);
    	System.out.println(reqOrder.getServiceStatus());
    }

    @Test
    public void retireService() throws IOException, CloudformsException {
    	client = new CloudformsClient.CloudformsClientBuilder("userName", "password", new URL(BASE_URL)).build();
    	boolean b = client.retire(SERVICE_NAME);
    	Assert.assertTrue(b);
    }
    
    @Test
    public void testName() {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
    	Timestamp date = new Timestamp(System.currentTimeMillis() + 20000);
		String serviceName = "RHEL_62-"+ sdf.format(date);
		Date dateTime = new Date();
		String nameDate = "RHEL_62-" + sdf.format(dateTime);
		System.out.println("date pile : " + nameDate);
		System.out.println("date modif : " + serviceName);
		Assert.assertFalse(nameDate.equals(serviceName));
    }
}
