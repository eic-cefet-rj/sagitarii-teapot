package cmabreu.sagitarii.teapot.comm;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import cmabreu.sagitarii.teapot.Configurator;

public class WebClient {
	private Configurator gf;

	public WebClient( Configurator gf) {
		this.gf = gf;
	}
	
	public Configurator getConfig() {
		return gf;
	}

		
	public String doGet(String action, String parameter) throws Exception {
		String resposta = "SEM_RESPOSTA";
		String mhpHost = gf.getHostURL();
		String url = mhpHost + "/" + action + "?" + parameter;
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		if ( gf.useProxy() ) {
			if ( gf.getProxyInfo() != null ) {
				HttpHost httpproxy = new HttpHost( gf.getProxyInfo().getHost(), gf.getProxyInfo().getPort() ); 
				httpClient.getCredentialsProvider().setCredentials(
						  new AuthScope( gf.getProxyInfo().getHost(), gf.getProxyInfo().getPort() ),
						  new UsernamePasswordCredentials( gf.getProxyInfo().getUser(), gf.getProxyInfo().getPassword() ));
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, httpproxy);		
			}
		}
		
		httpClient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET,"UTF-8");
		
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Content-Type", "plain/text; charset=utf-8");
		
		HttpResponse response = httpClient.execute(getRequest);
		response.setHeader("Content-Type", "plain/text; charset=UTF-8");

		int stCode = response.getStatusLine().getStatusCode();
		if ( stCode != 200) {
			System.out.println("Sagitarii nao recebeu meu anuncio. Codigo de erro " + stCode );
			System.out.println(url);
		} else {
			HttpEntity entity = response.getEntity();
			InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8");
			resposta = convertStreamToString(isr);
			Charset.forName("UTF-8").encode(resposta);
			httpClient.getConnectionManager().shutdown();
			isr.close();
		}
		
		return resposta;
	}
	
	private String convertStreamToString(java.io.InputStreamReader is) {
	    java.util.Scanner s = new java.util.Scanner(is);
		s.useDelimiter("\\A");
	    String retorno = s.hasNext() ? s.next() : "";
	    s.close();
	    return retorno;
	}	
}
