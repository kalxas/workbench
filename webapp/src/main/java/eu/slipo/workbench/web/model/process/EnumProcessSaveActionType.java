package eu.slipo.workbench.web.model.process;

/**
 * Process save action enumeration
 */
public enum EnumProcessSaveActionType {
    
    /**
     * Save/Update new/existing process instance
     */
    SAVE,
    
    /**
     * Save and schedule the execution of a new/existing instance
     */
    SAVE_AND_EXECUTE,
    
    /**
     * Save a process instance as a template
     */
    SAVE_TEMPLATE,;

    public static EnumProcessSaveActionType fromString(String value) {
        for (EnumProcessSaveActionType item : EnumProcessSaveActionType.values()) {
            if (item.name().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

}
