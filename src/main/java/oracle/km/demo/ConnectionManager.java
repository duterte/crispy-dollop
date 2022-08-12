/**
* Copyright (c) 2016, Oracle Corporation and/or its affiliates. All rights reserved.
*
* The sample code in this document or accessed through this document is not certified or
* supported by Oracle. It is intended for educational or testing purposes only. Use of this
* sample code implies acceptance of the License Agreement
* (http://www.oracle.com/technetwork/licenses/standard-license-152015.html).package oracle.km.demo;
* 
*/

package oracle.km.demo;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class ConnectionManager {
	public static CloseableHttpClient getHttpConnection( boolean ignoreSSL) {
		CloseableHttpClient client = null;
		try {
			if (ignoreSSL) {
			    HttpClientBuilder b = HttpClientBuilder.create();
			 
			    // setup a Trust Strategy that allows all certificates.
			    //
			    SSLContext sslContext = new SSLContextBuilder().useProtocol("TLSv1.2").loadTrustMaterial(null, new TrustStrategy() {
			        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			            return true;
			        }
			    }).build();
			    b.setSSLContext( sslContext);
			 		 
			    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
			    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
			            .register("http", PlainConnectionSocketFactory.getSocketFactory())
			            .register("https", sslSocketFactory)
			            .build();
			 
			    // now, we create connection-manager using our Registry.
			    //      -- allows multi-threaded use
			    PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
			    b.setConnectionManager( connMgr);
			 
			    // finally, build the HttpClient;
			    //      -- done!
			    client = b.build();
			}
			else {
				client = HttpClients.createDefault();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	    return client;
	}

}
