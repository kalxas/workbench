package eu.slipo.workbench.web.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.slipo.workbench.common.model.ApplicationException;
import eu.slipo.workbench.common.model.BasicErrorCode;
import eu.slipo.workbench.common.model.EnumRole;
import eu.slipo.workbench.common.model.ErrorCode;
import eu.slipo.workbench.common.model.FileSystemErrorCode;
import eu.slipo.workbench.common.model.QueryResultPage;
import eu.slipo.workbench.common.model.process.CatalogResource;
import eu.slipo.workbench.common.model.process.EnumInputType;
import eu.slipo.workbench.common.model.process.EnumProcessExecutionStatus;
import eu.slipo.workbench.common.model.process.EnumProcessTaskType;
import eu.slipo.workbench.common.model.process.EnumStepFile;
import eu.slipo.workbench.common.model.process.InvalidProcessDefinitionException;
import eu.slipo.workbench.common.model.process.ProcessDefinition;
import eu.slipo.workbench.common.model.process.ProcessErrorCode;
import eu.slipo.workbench.common.model.process.ProcessExecutionNotFoundException;
import eu.slipo.workbench.common.model.process.ProcessExecutionQuery;
import eu.slipo.workbench.common.model.process.ProcessExecutionRecord;
import eu.slipo.workbench.common.model.process.ProcessExecutionStartException;
import eu.slipo.workbench.common.model.process.ProcessExecutionStepFileRecord;
import eu.slipo.workbench.common.model.process.ProcessExecutionStepRecord;
import eu.slipo.workbench.common.model.process.ProcessExecutionStopException;
import eu.slipo.workbench.common.model.process.ProcessIdentifier;
import eu.slipo.workbench.common.model.process.ProcessNotFoundException;
import eu.slipo.workbench.common.model.process.ProcessQuery;
import eu.slipo.workbench.common.model.process.ProcessRecord;
import eu.slipo.workbench.common.model.resource.ResourceRecord;
import eu.slipo.workbench.common.repository.ProcessRepository;
import eu.slipo.workbench.common.repository.ResourceRepository;
import eu.slipo.workbench.common.service.ProcessOperator;
import eu.slipo.workbench.web.model.process.ProcessExecutionRecordView;

@Service
public class DefaultProcessService implements ProcessService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProcessService.class);

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IAuthenticationFacade authenticationFacade;

    @Autowired
    private IProcessValidationService processValidationService;

    @Autowired
    @Qualifier("defaultWebFileNamingStrategry")
    protected DefaultWebFileNamingStrategry fileNamingStrategy;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProcessOperator processOperator;

    private Integer currentUserId() {
        return authenticationFacade.getCurrentUserId();
    }

    private Locale currentUserLocale() {
        return authenticationFacade.getCurrentUserLocale();
    }

    private boolean isAdmin() {
        return this.authenticationFacade.isAdmin();
    }

    private ApplicationException wrapAndFormatException(ErrorCode errorCode) {
        return ApplicationException.fromPattern(errorCode).withFormattedMessage(messageSource, currentUserLocale());
    }

    private ApplicationException wrapAndFormatException(Exception ex, ErrorCode errorCode, String message) {
        return ApplicationException.fromMessage(ex, errorCode, message).withFormattedMessage(messageSource, currentUserLocale());
    }

    private ApplicationException accessDenied() {
        return ApplicationException.fromPattern(BasicErrorCode.AUTHORIZATION_FAILED).withFormattedMessage(messageSource, currentUserLocale());
    }

    @Override
    public QueryResultPage<ProcessRecord> find(ProcessQuery query, PageRequest pageRequest) {
        query.setTemplate(false);
        query.setCreatedBy(isAdmin() ? null : currentUserId());
        if (!this.authenticationFacade.isAdmin()) {
            query.setTaskType(EnumProcessTaskType.DATA_INTEGRATION);
        }

        final QueryResultPage<ProcessRecord> result = processRepository.query(query, pageRequest);
        updateProcessRecords(result.getItems());

        return result;
    }

    @Override
    public QueryResultPage<ProcessRecord> findTemplates(ProcessQuery query, PageRequest pageRequest) {
        query.setTaskType(EnumProcessTaskType.DATA_INTEGRATION);
        query.setTemplate(true);
        query.setCreatedBy(isAdmin() ? null : currentUserId());

        return processRepository.query(query, pageRequest);
    }

    @Override
    public QueryResultPage<ProcessExecutionRecord> find(ProcessExecutionQuery query, PageRequest pageRequest) {
        query.setCreatedBy(isAdmin() ? null : currentUserId());

        final QueryResultPage<ProcessExecutionRecord> result = processRepository.queryExecutions(query, pageRequest);
        updateProcessExecutionRecords(result.getItems());

        return result;
    }

    @Override
    public ProcessExecutionRecordView getProcessExecution(long id, long version, long executionId)
        throws ProcessExecutionNotFoundException{

        ProcessRecord processRecord = processRepository.findOne(id, version, false);
        ProcessExecutionRecord executionRecord = processRepository.getExecutionCompactView(id, version);
        if (processRecord == null ||
            executionRecord == null ||
            executionRecord.getProcess().getId() != id ||
            executionRecord.getProcess().getVersion() != version) {
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        }
        checkProcessExecutionAccess(processRecord);

        // For catalog resources update bounding box and table name values
        processRecord
            .getDefinition()
            .resources()
            .stream()
            .filter(r->r.getInputType() == EnumInputType.CATALOG)
            .map(r-> (CatalogResource) r)
            .forEach(r1-> {
                ResourceRecord r2 = resourceRepository.findOne(r1.getId(), r1.getVersion());
                if (r2 != null) {
                    r1.setBoundingBox(r2.getBoundingBox());
                    r1.setTableName(r2.getTableName());
                }
            });

        return new ProcessExecutionRecordView(processRecord, executionRecord);
    }

    @Override
    public ProcessRecord create(ProcessDefinition definition, EnumProcessTaskType taskType) throws InvalidProcessDefinitionException {
        return create(definition, taskType, false);
    }

    @Override
    public ProcessRecord create(ProcessDefinition definition, boolean isTemplate) throws InvalidProcessDefinitionException {
        return create(definition, EnumProcessTaskType.DATA_INTEGRATION, isTemplate);
    }

    private ProcessRecord create(ProcessDefinition definition, EnumProcessTaskType taskType, boolean isTemplate) throws InvalidProcessDefinitionException {
        if (!this.authenticationFacade.hasAnyRole(EnumRole.ADMIN, EnumRole.AUTHOR)) {
            throw this.accessDenied();
        }
        try {
            processValidationService.validate(null, definition, isTemplate);

            return  processRepository.create(definition, currentUserId(), taskType, isTemplate);
        } catch(InvalidProcessDefinitionException ex) {
            throw ex;
        } catch (ApplicationException ex) {
            throw ex.withFormattedMessage(messageSource, currentUserLocale());
        } catch (Exception ex) {
            throw wrapAndFormatException(ex, ProcessErrorCode.UNKNOWN, "Failed to create process");
        }
    }

    @Override
    public ProcessRecord update(long id, ProcessDefinition definition, boolean isTemplate) throws InvalidProcessDefinitionException {
        if (!this.authenticationFacade.hasAnyRole(EnumRole.ADMIN, EnumRole.AUTHOR)) {
            throw this.accessDenied();
        }
        try {
            processValidationService.validate(id, definition, isTemplate);

            ProcessRecord record = processRepository.findOne(id);
            checkProcessAccess(record);

            return processRepository.update(id, definition, currentUserId());
        } catch (InvalidProcessDefinitionException ex) {
            throw ex;
        } catch (ApplicationException ex) {
            throw ex.withFormattedMessage(messageSource, currentUserLocale());
        } catch (Exception ex) {
            throw wrapAndFormatException(ex, ProcessErrorCode.UNKNOWN, "Failed to update process");
        }
    }

    @Override
    public ProcessRecord findOne(long id) {
        ProcessRecord record = processRepository.findOne(id);
        checkProcessAccess(record);
        return record;
    }

    @Override
    public ProcessRecord findOne(long id, long version) {
        ProcessRecord record = processRepository.findOne(id, version);
        checkProcessAccess(record);
        return record;
    }

    @Override
    public List<ProcessExecutionRecord> findExecutions(long id, long version) {
        ProcessRecord record = processRepository.findOne(id, version, true);
        checkProcessAccess(record);
        return record == null ? Collections.emptyList() : record.getExecutions();
    }

    @Override
    public ProcessExecutionRecord start(
        long id, long version, EnumProcessTaskType task
    ) throws ProcessNotFoundException, ProcessExecutionStartException, IOException {

        final ProcessRecord processRecord = this.processRepository.findOne(id, version);

        if (processRecord == null) {
            throw new ProcessNotFoundException(id, version);
        }

        // Resolve authorization based on requested task type
        if (!this.authenticationFacade.isAdmin()) {
            // Check record owner
            if (!this.currentUserId().equals(processRecord.getCreatedBy().getId())) {
                throw this.accessDenied();
            }
            switch (task) {
                case REGISTRATION:
                case EXPORT:
                    // Registration/Export tasks can only be initiated by authors
                    if (!this.authenticationFacade.hasRole(EnumRole.AUTHOR)) {
                        throw this.accessDenied();
                    }
                    break;
                case DATA_INTEGRATION:
                    // Data integration tasks can be initiated by any user
                    break;
                default:
                    // When no specific task type is given, allow only data integration
                    // tasks
                    if (processRecord.getTaskType() != EnumProcessTaskType.DATA_INTEGRATION) {
                        throw this.accessDenied();
                    }
                    break;
            }
        }

        final ProcessExecutionRecord record = this.processOperator.poll(id, version);
        if ((record == null) ||
            (record.getStatus() == EnumProcessExecutionStatus.FAILED) ||
            (record.getStatus() == EnumProcessExecutionStatus.STOPPED)) {
            return this.processOperator.start(id, version, this.authenticationFacade.getCurrentUserId());
        }
        if ((record == null) || (record.getStatus() == EnumProcessExecutionStatus.COMPLETED)) {
            throw ApplicationException.fromMessage("Process has already been executed");
        }
        throw ApplicationException.fromMessage("Process failed to start");
    }

    @Override
    public void stop(long id, long version) throws ProcessNotFoundException, ProcessExecutionStopException {
        final ProcessRecord processRecord = this.processRepository.findOne(id, version);

        if (processRecord == null) {
            throw new ProcessNotFoundException(id, version);
        }

        checkProcessAccess(processRecord);

        final ProcessExecutionRecord record = this.processOperator.poll(id, version);
        if ((record != null) && (record.getStatus() == EnumProcessExecutionStatus.RUNNING)) {
            this.processOperator.stop(id, version);
        } else {
            throw ApplicationException.fromMessage("Process is not running");
        }
    }

    @Override
    public void exportMap(
        long id, long version, long executionId
    ) throws ProcessNotFoundException, ProcessExecutionNotFoundException {
        final ProcessRecord processRecord = this.processRepository.findOne(id, version);

        if (processRecord == null) {
            throw new ProcessNotFoundException(id, version);
        }

        checkProcessAccess(processRecord);

        final ProcessExecutionRecord executionRecord = this.processRepository.findExecution(executionId);
        if ((executionRecord == null) ||
            (executionRecord.getProcess().getId() != id) ||
            (executionRecord.getProcess().getVersion() != version)) {
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        } else {
            throw wrapAndFormatException(BasicErrorCode.NOT_IMPLEMENTED);
        }
    }

    @Override
    public File getProcessExecutionFile(long id, long version, long executionId, long fileId)
        throws ProcessNotFoundException, ProcessExecutionNotFoundException {

        ProcessRecord processRecord = processRepository.findOne(id, version, false);
        ProcessExecutionRecord executionRecord = processRepository.findExecution(executionId);
        if (processRecord == null ||
            executionRecord == null ||
            executionRecord.getProcess().getId() != id ||
            executionRecord.getProcess().getVersion() != version) {

            throw new ProcessNotFoundException(id, version);
        }

        checkProcessExecutionAccess(processRecord);

        final Optional<ProcessExecutionStepFileRecord> result = executionRecord
            .getSteps()
            .stream()
            .flatMap(s -> s.getFiles().stream())
            .filter(f -> f.getId() == fileId)
            .findFirst();

        if (!result.isPresent()) {
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        }

        final String filename = result.get().getFilePath();
        Path path;
        try {
            path = fileNamingStrategy.resolveExecutionPath(filename);
        } catch (URISyntaxException e) {
            return null;
        }

        return path.toFile();
    }

    @Override
    public Object getProcessExecutionKpiData(long id, long version, long executionId, long fileId)
        throws ApplicationException, ProcessExecutionNotFoundException {

        ProcessRecord processRecord = processRepository.findOne(id, version, false);
        ProcessExecutionRecord executionRecord = processRepository.findExecution(executionId);
        if (processRecord == null ||
            executionRecord == null ||
            executionRecord.getProcess().getId() != id ||
            executionRecord.getProcess().getVersion() != version) {
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        }

        checkProcessExecutionAccess(processRecord);

        final Optional<Pair<ProcessExecutionStepRecord, ProcessExecutionStepFileRecord>> result = executionRecord
            .getSteps()
            .stream()
            .flatMap(s -> {
                return s.getFiles()
                    .stream()
                    .map(f-> Pair.<ProcessExecutionStepRecord, ProcessExecutionStepFileRecord>of(s, f));
            })
            .filter(f -> f.getRight().getId() == fileId)
            .findFirst();

        if (!result.isPresent()) {
            throw ApplicationException.fromMessage(BasicErrorCode.NO_RESULT, "File was not found");
        }
        final ProcessExecutionStepRecord stepRecord = result.get().getLeft();
        final ProcessExecutionStepFileRecord fileRecord = result.get().getRight();
        if (fileRecord.getType() != EnumStepFile.KPI) {
            throw ApplicationException.fromMessage(BasicErrorCode.NOT_SUPPORTED, "File type is not supported");
        }

        final String filename = fileRecord.getFilePath();
        Path path;
        try {
            path = fileNamingStrategy.resolveExecutionPath(filename);
        } catch (URISyntaxException e) {
            throw ApplicationException.fromMessage(BasicErrorCode.NO_RESULT, "File was not found");
        }
        final File file = path.toFile();

        if (!file.exists()) {
            throw ApplicationException.fromMessage(FileSystemErrorCode.PATH_NOT_FOUND, "File was not found");
        }

        try {
            switch (stepRecord.getTool()) {
                case TRIPLEGEO:
                case FAGI:
                    JsonNode node = objectMapper.readTree(file);
                    return node;
                default:
                    Resource resource = new FileSystemResource(file);
                    Properties props = PropertiesLoaderUtils.loadProperties(resource);
                    return props;
            }
        } catch (JsonParseException ex) {
            String message = "Failed to parse JSON file";
            logger.error(message,ex);
            return ApplicationException.fromMessage(BasicErrorCode.UNKNOWN, message);
        } catch (IOException ex) {
            String message = "Failed to access file";
            logger.error(message,ex);
            return ApplicationException.fromMessage(BasicErrorCode.IO_ERROR, message);
        }
    }

    // TODO: Store process status in database redundant field to avoid querying RPC-server
    // on every request

    private void updateProcessRecords(List<ProcessRecord> records) {
        try {
            final List<ProcessIdentifier> running = this.processOperator.list();

            // Update most recent versions
            records.stream()
            .forEach(p -> {
                final Optional<ProcessIdentifier> identifier = running.stream()
                    .filter(e -> e.getId() == p.getId() && e.getVersion() == p.getVersion())
                    .findFirst();

                p.setRunning(identifier.isPresent());
            });

            // Update all versions for every process
            records.stream()
                .flatMap(p -> p.getRevisions().stream())
                .forEach(p -> {
                    final Optional<ProcessIdentifier> identifier = running.stream()
                        .filter(e -> e.getId() == p.getId() && e.getVersion() == p.getVersion())
                        .findFirst();

                    p.setRunning(identifier.isPresent());
                });
        } catch(Exception ex) {
            // Ignore
        }
    }

    private void updateProcessExecutionRecords(List<ProcessExecutionRecord> records) {
        try {
            final List<ProcessIdentifier> running = this.processOperator.list();

            records.stream()
            .forEach(e -> {
                final Optional<ProcessIdentifier> identifier = running.stream()
                    .filter(r -> r.getId() == e.getProcess().getId() && r.getVersion() == e.getProcess().getVersion())
                    .findFirst();

                e.setRunning(identifier.isPresent());
            });
        } catch(Exception ex) {
            // Ignore
        }
    }

    private void checkProcessAccess(ProcessRecord record) {
        if ((!this.authenticationFacade.isAdmin()) && (!this.currentUserId().equals(record.getCreatedBy().getId()))) {
            throw this.accessDenied();
        }
        if ((!this.authenticationFacade.isAdmin()) && (record.getTaskType() != EnumProcessTaskType.DATA_INTEGRATION)) {
            throw this.accessDenied();
        }
    }

    private void checkProcessExecutionAccess(ProcessRecord record) {
        if ((!this.authenticationFacade.isAdmin()) && (!this.currentUserId().equals(record.getCreatedBy().getId()))) {
            throw this.accessDenied();
        }
    }

}
