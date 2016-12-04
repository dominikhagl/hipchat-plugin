package jenkins.plugins.hipchat;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.User;

public final class HipChatUtils {

	public static String getCurrentUser(Run<?, ?> run) {
		String currentUserName = null;
		
		if(run != null) {
			Cause.UserIdCause cause = run.getCause(Cause.UserIdCause.class);
			if(cause != null) {
	    		currentUserName = cause.getUserName();
	    	}
		}
		
    	if(currentUserName == null) {
			currentUserName = User.current().getFullName();
		}
		
		if(currentUserName != null) {
			return "@" + currentUserName.replace(" ", "");
    	}
		
		return null;
	}
	
	public static String appendCurrentUser(String users, Run<?, ?> run) {
		if(users == null || users.trim().isEmpty()) {
			return getCurrentUser(run);
		}
		
		return users + "," + getCurrentUser(run);
	}
	
}
