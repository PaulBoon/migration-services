package nl.knaw.dans.dataverse.migration.rulebasedrole;

/**
 * Created by akmi on 23/11/16.
 */
public class ShibAttributesHandlerException extends Exception {
    public ShibAttributesHandlerException(String message) {
        super("Error " + message);
    }
}
