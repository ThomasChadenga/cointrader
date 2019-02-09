import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.*;

public class CoinTrader {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            if(jsonText.charAt(0)=='['){
                jsonText = "{data:" + jsonText + "}";
            }
            // System.out.println(jsonText);
            JSONObject json = new JSONObject(jsonText);
            return json;
        }
        finally {
            is.close();
        }
    }


    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, IOException {

        //Insert your secret key:
    	Scanner enterSecret = new Scanner(System.in); 
    	
    	System.out.println("Paste your secret key: ");
    	
    	String secretKey = enterSecret.next(); 
    	
    	enterSecret.close();
    	
    	//apiKey
        String apiKey = "iGZVL5AN2GzLcLcNmtvvLDQiiyV74eXlcnOAh4yWjbaHI0giMYfnEl2BxzZdlYjS";
        
        int BTCWalletID = 0;

        //my wallets list
        System.out.println("WALLETS:");
        
        final JSONArray wallets = new JSONArray(getEndpoint(apiKey, secretKey,"/api/wallet"));
        
        for(int i=0;i<wallets.length();i++) {
        	
        	final JSONObject wallet = wallets.getJSONObject(i);
        	
        	System.out.println(wallet.get("description") + "	" + wallet.getDouble("balance"));
        	
        	//get btc id
        	if(wallet.getJSONObject("currency").get("code") == "BTC") {
        		
        		BTCWalletID = wallet.getJSONObject("currency").getInt("id");
        		
        	}
        	
        }
        
        System.out.println();
      
        //transactions on BTC wallet
        System.out.println("BTC TRANSACTIONS:");
        
        if(BTCWalletID > 0) {
        	
	        final JSONArray walletTransactions = new JSONArray(getEndpoint(apiKey, secretKey, "/api/transaction/" + BTCWalletID));
	        
	        for(int i=0;i<walletTransactions.length();i++) {
	        	
	        	final JSONObject walletTransaction = walletTransactions.getJSONObject(i);
	        	
	        	System.out.println(walletTransaction.getDouble("amount") + "	" +walletTransaction.get("description") + "	" + walletTransaction.get("date"));
	        
	        }
	        
        }
        
        System.out.println();
        
        //filtered list of wallets by BTC
        System.out.println("BTC WALLETS:");
        
        String[] codes = {"BTC"};
        
        getFilteredWallets(apiKey, secretKey, codes);
        
        System.out.println();
        
    }

    public static String hmacDigest(String data, String key) {
    	
    	Mac sha256_HMAC = null;

        try {
          byte[] byteKey = key.getBytes("UTF-8");
          final String HMAC_SHA256 = "HmacSHA256";
          sha256_HMAC = Mac.getInstance(HMAC_SHA256);
          SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA256);
          sha256_HMAC.init(keySpec);
          byte[] mac_data = sha256_HMAC.doFinal(data.getBytes());
          return Base64.getEncoder().encodeToString(mac_data);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
          e.printStackTrace();
        } catch (InvalidKeyException e) {
          e.printStackTrace();
        } finally {
        }
        return "";

    }
    
    public static String getEndpoint(String apiKey, String secretKey, String endPoint){

        StringBuilder result = new StringBuilder();

        HttpClient client = HttpClientBuilder.create().build();
        try {

            HttpGet request = new HttpGet("https://api.coindirect.com" + endPoint);
            
            request.addHeader("authorization", generateAuthorisationString(apiKey,secretKey,"https://api.coindirect.com" + endPoint));

            HttpResponse response = client.execute(request);

            //System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line = "";

            while ((line = rd.readLine()) != null) {

                result.append(line);

            }

            //System.out.println(result);

        } catch (IOException e) {

            e.printStackTrace();

        }

        return result.toString();
    }
    
    public static void getFilteredWallets(String apiKey, String secretKey, String[] codes) {
    	
    	final JSONArray wallets = new JSONArray(getEndpoint(apiKey, secretKey,"/api/wallet"));
    	
    	//loop thru only printing those in array
    	for(int i=0;i<wallets.length();i++) {
    		
        	final JSONObject wallet = wallets.getJSONObject(i);
        	
        	if(Arrays.asList(codes).contains(wallet.getJSONObject("currency").get("code"))) {
        		
        		System.out.println(wallet.get("description") + "	" + wallet.getDouble("balance"));
        		
        	}
        }
    }
    
    public static String generateAuthorisationString(String apiKey, String secretKey,String requestURL) {
    	
    	long timestamp = Math.round(System.currentTimeMillis() / 1000);
    	
    	try {
	    	URL url = new URL(requestURL);
	
	        String nonce = UUID.randomUUID().toString().substring(0, 8);
	        String method = "GET";
	        String path = url.getPath();
	        String query = url.getQuery();
	        String host = url.getHost();
	        int port = url.getPort();
	
	        if (port == -1) {
	          port = url.getDefaultPort();
	        }
	
	        StringBuilder hawkHeader = new StringBuilder();
	        hawkHeader.append("hawk.1.header\n");
	        hawkHeader.append(timestamp);
	        hawkHeader.append("\n");
	        hawkHeader.append(nonce);
	        hawkHeader.append("\n");
	        hawkHeader.append(method);
	        hawkHeader.append("\n");
	        hawkHeader.append(path);
	        if (query != null) {
	          hawkHeader.append("?");
	          hawkHeader.append(query);
	        }
	        hawkHeader.append("\n");
	        hawkHeader.append(host);
	        hawkHeader.append("\n");
	        hawkHeader.append(port);
	        hawkHeader.append("\n");
	        // body
	        hawkHeader.append("\n");
	        // app data
	        hawkHeader.append("\n");
	        
	        String mac = hmacDigest(hawkHeader.toString(), secretKey);

	        String authorization =
	            "Hawk id=\""
	                + apiKey
	                + "\",ts=\""
	                + timestamp
	                + "\",nonce=\""
	                + nonce
	                + "\",mac=\""
	                + mac + "\"";
	        
	        return authorization;
    	}
    	catch(MalformedURLException ignored) {
    		
    	}
    	return null;
    }

}

