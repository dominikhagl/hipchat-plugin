package jenkins.plugins.hipchat.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import hudson.Util;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.InvalidResponseCodeException;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import net.sf.json.JSONObject;

public class HipChatV2Service extends HipChatService {

    private static final Logger logger = Logger.getLogger(HipChatV2Service.class.getName());
    private static final String[] DEFAULT_ROOMS = new String[0];
    private static final String[] DEFAULT_USERS = new String[0];

    private final String server;
    private final String token;
    private final String[] roomIds;
    private final String[] users;

    public HipChatV2Service(String server, String token, String roomIds, String users) {
        this.server = server;
        this.token = token;
        this.roomIds = roomIds == null || roomIds.trim().isEmpty() ? DEFAULT_ROOMS : roomIds.split("\\s*,\\s*");
        this.users = users == null || users.trim().isEmpty() ? DEFAULT_USERS : users.split("\\s*,\\s*");
    }

    @Override
    public void publish(String message, String color, boolean notify, boolean textFormat) throws NotificationException {
    	roomNotification(message, color, notify, textFormat);
    	userMessage(message, notify, textFormat);
    }
    
    protected void roomNotification(String message, String color, boolean notify, boolean textFormat) throws NotificationException {
        for (String roomId : roomIds) {
            logger.log(Level.FINE, "Posting to {0} room: {1} {2}", new Object[]{roomId, message, color});
            CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse httpResponse = null;

            try {
                HttpPost post = new HttpPost("https://" + server + "/v2/room/" + Util.rawEncode(roomId)
                        + "/notification");
                post.addHeader("Authorization", "Bearer " + token);

                JSONObject notification = new JSONObject();
                notification.put("message", message);
                notification.put("message_format", textFormat ? "text" : "html");
                notification.put("color", color);
                notification.put("notify", notify);
                post.setEntity(new StringEntity(notification.toString(), ContentType.APPLICATION_JSON));

                httpResponse = httpClient.execute(post);
                handleResponse(httpResponse);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
                throw new NotificationException(Messages.IOException(ioe.toString()));
            } finally {
                closeQuietly(httpResponse, httpClient);
            }
        }
    }
    
    protected void userMessage(String message, boolean notify, boolean textFormat) throws NotificationException {
    	for(String user : users) {
        	logger.log(Level.FINE, "Sending to user {0}: {1}", new Object[]{user, message});
            CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse httpResponse = null;
            
            try {
    	    	HttpPost post = new HttpPost("https://" + server + "/v2/user/" + Util.rawEncode(user)
    	                + "/message");
    	        post.addHeader("Authorization", "Bearer " + token);
    	        
    	        JSONObject notification = new JSONObject();
    	        notification.put("message", message);
    	        notification.put("message_format", textFormat ? "text" : "html");
    	        notification.put("notify", notify);
    	        post.setEntity(new StringEntity(notification.toString(), ContentType.APPLICATION_JSON));
    	        
    	        httpResponse = httpClient.execute(post);
                handleResponse(httpResponse);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
                throw new NotificationException(Messages.IOException(ioe.toString()));
            } finally {
                closeQuietly(httpResponse, httpClient);
            }
    	}
    }
    
    private void handleResponse(CloseableHttpResponse httpResponse) throws IOException, InvalidResponseCodeException {
    	int responseCode = httpResponse.getStatusLine().getStatusCode();
        // Always read response to ensure the inputstream is closed
        String response = readResponse(httpResponse.getEntity());

        if (responseCode != 204) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "HipChat post may have failed. ResponseCode: {0}, Response: {1}",
                        new Object[]{responseCode, response});
                throw new InvalidResponseCodeException(responseCode);
            }
        }
    }
}
