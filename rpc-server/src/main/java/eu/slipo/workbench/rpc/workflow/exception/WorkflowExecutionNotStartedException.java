package eu.slipo.workbench.rpc.workflow.exception;

public class WorkflowExecutionNotStartedException extends WorkflowExecutionStopException
{
    private static final long serialVersionUID = 1L;
    
    public WorkflowExecutionNotStartedException()
    {
        super("The execution is not started");
    }
}