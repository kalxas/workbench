package eu.slipo.workbench.web.model.process;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.slipo.workbench.web.model.EnumOperation;
import eu.slipo.workbench.web.model.EnumTool;

public class ProcessExecutionStepRecord {

    private long id;

    private int key;

    private String name;

    private EnumProcessExecutionStatus status;

    private EnumTool component;

    private EnumOperation operation;

    private ZonedDateTime startedOn;

    private ZonedDateTime completedOn;

    private String errorMessage;

    private List<ProcessExecutionStepFileRecord> files = new ArrayList<ProcessExecutionStepFileRecord>();

    public ProcessExecutionStepRecord(long id, int key, String name) {
        this.id = id;
        this.key = key;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public int getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public EnumProcessExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(EnumProcessExecutionStatus status) {
        this.status = status;
    }

    public EnumTool getComponent() {
        return component;
    }

    public void setComponent(EnumTool component) {
        this.component = component;
    }

    public EnumOperation getOperation() {
        return operation;
    }

    public void setOperation(EnumOperation operation) {
        this.operation = operation;
    }

    public ZonedDateTime getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(ZonedDateTime startedOn) {
        this.startedOn = startedOn;
    }

    public ZonedDateTime getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(ZonedDateTime completedOn) {
        this.completedOn = completedOn;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ProcessExecutionStepFileRecord> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public void addFile(ProcessExecutionStepFileRecord f) {
        this.files.add(f);
    }

}
