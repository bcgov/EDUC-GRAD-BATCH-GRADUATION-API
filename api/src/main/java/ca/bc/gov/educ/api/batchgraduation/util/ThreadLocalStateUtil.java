package ca.bc.gov.educ.api.batchgraduation.util;

public class ThreadLocalStateUtil {
    private static ThreadLocal<String> transaction = new ThreadLocal<>();
    private static ThreadLocal<String> user = new ThreadLocal<>();
    private static ThreadLocal<String> properName = new ThreadLocal<>();

    private ThreadLocalStateUtil() {}

    /**
     * Set the current correlationID for this thread
     */
    public static void setCorrelationID(String correlationID){
        transaction.set(correlationID);
    }

    /**
     * Get the current correlationID for this thread
     *
     * @return the correlationID, or null if it is unknown.
     */
    public static String getCorrelationID() {
        return transaction.get();
    }

    /**
     * Set the current user for this thread
     */
    public static void setCurrentUser(String currentUser){
        user.set(currentUser);
    }

    /**
     * Get the current user for this thread
     *
     * @return the username of the current user, or null if it is unknown.
     */
    public static String getCurrentUser() {
        return user.get();
    }

    /**
     * Set the current user for this thread
     */
    public static void setProperName(String name){
        properName.set(name);
    }

    /**
     * Get the current user for this thread
     *
     * @return the username of the current user, or null if it is unknown.
     */
    public static String getProperName() {
        return properName.get();
    }



    public static void clear() {
        transaction.remove();
        user.remove();
        properName.remove();
    }
}
