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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import fr.asipsante.jenkins.cloudforms.plugin.cloudforms.CloudformsBean;

/**
 *
 * @author aboittiaux
 */
public class CloudformsBeanTest {
    
    private CloudformsBean bean;
    private Certificate cert;
    private Properties prop = new Properties();
    private Gson gson = new Gson();
    
    @Test
    public void testOneparameter() throws CertificateException, MalformedURLException, IOException {
        String parameters = "\"valeur1\"";
        bean = new CloudformsBean("serviceName", parameters, cert, "credentialsId", new URL("http://baseUrl"), false, "echo hi mom!", "uniqueid");
        JsonObject jsonParam = new JsonObject();
        jsonParam.add("test", gson.toJsonTree("valeur1"));
        assertTrue(parameters.equals(jsonParam.get("test").toString()));
    }
    
    @Test
    public void testMultipleParam() throws MalformedURLException, IOException {
        String parameters = "test=valeur1\n" +
                            "test2   =    valeur2\n" +
                            "test3 =   valeur3";
        bean = new CloudformsBean("serviceName", parameters, cert, "credentialsId", new URL("http://baseUrl"), false, "echo hi mom!", "uniqueid");
        prop.load(new StringReader(bean.getParameters()));
        assertTrue(prop.getProperty("test").equals("valeur1"));
        assertTrue(prop.getProperty("test2").equals("valeur2"));
        assertTrue(prop.getProperty("test3").equals("valeur3"));
        
    }
    
    @Test
    public void addParametersToProperties() throws IOException {
        String parameters = "test=valeur1\n" +
                            "test2   =    valeur2\n" +
                            "test3 =   valeur3";
        
        Properties prop = new Properties();
        prop.load(new StringReader(parameters));
        
        prop.setProperty("href", "href");
        
        assertTrue(prop.getProperty("href").equals("href"));
        
    }
    
    
    
}
