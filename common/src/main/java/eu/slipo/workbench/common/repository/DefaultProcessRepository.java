package eu.slipo.workbench.common.repository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.slipo.workbench.common.domain.AccountEntity;
import eu.slipo.workbench.common.domain.ProcessEntity;
import eu.slipo.workbench.common.domain.ProcessExecutionEntity;
import eu.slipo.workbench.common.domain.ProcessExecutionMonitorEntity;
import eu.slipo.workbench.common.domain.ProcessExecutionStepEntity;
import eu.slipo.workbench.common.domain.ProcessExecutionStepFileEntity;
import eu.slipo.workbench.common.domain.ProcessRevisionEntity;
import eu.slipo.workbench.common.domain.ResourceRevisionEntity;
import eu.slipo.workbench.common.model.QueryResultPage;
import eu.slipo.workbench.common.model.process.EnumProcessExecutionStatus;
import eu.slipo.workbench.common.model.process.EnumProcessTaskType;
import eu.slipo.workbench.common.model.process.ProcessDefinition;
import eu.slipo.workbench.common.model.process.ProcessExecutionNotFoundException;
import eu.slipo.workbench.common.model.process.ProcessExecutionQuery;
import eu.slipo.workbench.common.model.process.ProcessExecutionRecord;
import eu.slipo.workbench.common.model.process.ProcessExecutionStepFileRecord;
import eu.slipo.workbench.common.model.process.ProcessExecutionStepRecord;
import eu.slipo.workbench.common.model.process.ProcessIdentifier;
import eu.slipo.workbench.common.model.process.ProcessNotFoundException;
import eu.slipo.workbench.common.model.process.ProcessQuery;
import eu.slipo.workbench.common.model.process.ProcessRecord;
import eu.slipo.workbench.common.model.resource.ResourceIdentifier;
import eu.slipo.workbench.common.model.resource.ResourceRecord;

@Repository
@Transactional
public class DefaultProcessRepository implements ProcessRepository 
{
    private static Logger logger = LoggerFactory.getLogger(DefaultProcessRepository.class);
    
    @PersistenceContext(unitName = "default")
    private EntityManager entityManager;

    @Autowired
    private ResourceRepository resourceRepository;
    
    @Override
    public void clearRunningExecutions()
    {
        // Update execution steps
        
        Query q1 = entityManager.createQuery(
                "UPDATE ProcessExecutionStep e SET e.status = :nextStatus WHERE e.status = :status")
            .setParameter("status", EnumProcessExecutionStatus.RUNNING)
            .setParameter("nextStatus", EnumProcessExecutionStatus.STOPPED);
        int n1 = q1.executeUpdate();
        if (n1 > 0)
            logger.info("Cleared {} execution step(s) from RUNNING to STOPPED", n1);
        
        // Update executions
        
        Query q2 = entityManager.createQuery(
                "UPDATE ProcessExecution e SET e.status = :nextStatus " +
                "WHERE e.status = :status1 OR e.status = :status2")
            .setParameter("status1", EnumProcessExecutionStatus.UNKNOWN)
            .setParameter("status2", EnumProcessExecutionStatus.RUNNING)
            .setParameter("nextStatus", EnumProcessExecutionStatus.STOPPED);
        int n2 = q2.executeUpdate();  
        if (n2 > 0)
            logger.info("Cleared {} execution(s) from UNKNOWN/RUNNING to STOPPED", n2);
    }
    
    @Transactional(readOnly = true)
    @Override
    public QueryResultPage<ProcessRecord> query(ProcessQuery query, PageRequest pageReq)
    {
        return ProcessRepository.super.query(query, pageReq);
    }
    
    @Transactional(readOnly = true)
    @Override
    public QueryResultPage<ProcessRecord> query(
        ProcessQuery query, PageRequest pageReq, final boolean includeExecutions)
    {
        // Check query parameters
        if (pageReq == null) {
            pageReq = new PageRequest(0, 10);
        }

        String qlString = "";

        // Resolve filters
        
        List<String> filters = new ArrayList<>();
        if (query != null) {
            if (query.getCreatedBy() != null) 
                filters.add("(p.createdBy.id = :ownerId)");
            if (!StringUtils.isBlank(query.getName()))
                filters.add("(p.name like :name)");
            if (query.getTaskType() != null)
                filters.add("(p.taskType = :taskType)");
            if (query.getTemplate() != null)
                filters.add("(p.isTemplate = :template)");
        }

        // Count records
        qlString = "select count(p.id) from Process p ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }

        Integer count;
        TypedQuery<Number> countQuery = entityManager.createQuery(qlString, Number.class);
        if (query != null) 
            setFindParameters(query, countQuery);
        count = countQuery.getSingleResult().intValue();

        // Load records
        qlString = "select p from Process p ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }
        qlString += " order by p.name, p.updatedOn ";

        TypedQuery<ProcessEntity> selectQuery = entityManager.createQuery(qlString, ProcessEntity.class);
        if (query != null) 
            setFindParameters(query, selectQuery);

        selectQuery.setFirstResult(pageReq.getOffset());
        selectQuery.setMaxResults(pageReq.getPageSize());
        
        final boolean includeRevisions = true;
        
        List<ProcessRecord> records = selectQuery.getResultList().stream()
            .map(p -> p.toProcessRecord(includeRevisions, includeExecutions, false))
            .collect(Collectors.toList());
        return new QueryResultPage<ProcessRecord>(records, pageReq, count);
    }
    
    @Transactional(readOnly = true)
    @Override
    public QueryResultPage<ProcessExecutionRecord> queryExecutions(ProcessExecutionQuery query, PageRequest pageReq)
    {
        // Check query parameters
        if (pageReq == null) {
            pageReq = new PageRequest(0, 10);
        }

        String qlString = "";

        // Resolve filters
        
        List<String> filters = new ArrayList<>();
        if (query != null) {
            if (query.getId() != null)
                filters.add("(e.process.parent.id = :id)");
            if (query.getVersion() != null)
                filters.add("(e.process.version = :version)");
            if (query.getCreatedBy() != null)
                filters.add("(e.process.parent.createdBy.id = :ownerId)");
            if (!StringUtils.isBlank(query.getName()))
                filters.add("(e.process.name like :name)");
            if (query.getTaskType() != null)
                filters.add("(e.process.parent.taskType = :taskType)");
            if (query.getStatus() != null)
                filters.add("(e.status = :status)");
        }

        // Count records
        qlString = "select count(e.id) from ProcessExecution e ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }

        Integer count;
        TypedQuery<Number> countQuery = entityManager.createQuery(qlString, Number.class);
        if (query != null) 
            setFindParameters(query, countQuery);
        count = countQuery.getSingleResult().intValue();

        // Load records
        qlString = "select e from ProcessExecution e ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }
        qlString += " order by e.startedOn desc, e.process.parent.name ";

        TypedQuery<ProcessExecutionEntity> selectQuery =
            entityManager.createQuery(qlString, ProcessExecutionEntity.class);
        if (query != null)
            setFindParameters(query, selectQuery);

        selectQuery.setFirstResult(pageReq.getOffset());
        selectQuery.setMaxResults(pageReq.getPageSize());

        List<ProcessExecutionRecord> records = selectQuery.getResultList().stream()
            .map(ProcessExecutionEntity::toProcessExecutionRecord)
            .collect(Collectors.toList());
        return new QueryResultPage<ProcessExecutionRecord>(records, pageReq, count);
    }

    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(long id)
    {
        return ProcessRepository.super.findOne(id);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(long id, final boolean includeExecutions)
    {
        ProcessRevisionEntity entity = findLatestRevision(id);
        return entity == null? null : entity.toProcessRecord(includeExecutions, false);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(long id, long version, final boolean includeExecutions)
    {
        ProcessRevisionEntity r = findRevision(id, version);
        return r == null? null : r.toProcessRecord(includeExecutions, false);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(long id, long version)
    {
        return ProcessRepository.super.findOne(id, version);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(ProcessIdentifier processIdentifier, boolean includeExecutions)
    {
        return ProcessRepository.super.findOne(processIdentifier, includeExecutions);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(ProcessIdentifier processIdentifier)
    {
        return ProcessRepository.super.findOne(processIdentifier);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessRecord findOne(String name)
    {
        String queryString =
            "select p from ProcessRevision p where p.parent.name = :name " +
            "order by p.version desc";

        List<ProcessRevisionEntity> result = entityManager
            .createQuery(queryString, ProcessRevisionEntity.class)
            .setParameter("name", name)
            .setMaxResults(1)
            .getResultList();

        return (result.isEmpty() ? null : result.get(0).toProcessRecord(false, false));
    }

    @Transactional(readOnly = true)
    @Override
    public ProcessExecutionRecord findExecution(long executionId)
    {
        ProcessExecutionEntity e = entityManager.find(ProcessExecutionEntity.class, executionId);
        return e == null? null : e.toProcessExecutionRecord(true);
    }
    
    @Transactional(readOnly = true)
    @Override
    public ProcessExecutionRecord findLatestExecution(long id, long version)
    {
        final ProcessRevisionEntity revisionEntity = findRevision(id, version);
        Assert.notNull(revisionEntity, 
            "The pair of (id, version) does not correspond to a process revision entity");
        
        final List<ProcessExecutionEntity> executionEntities = revisionEntity.getExecutions(); 
        if (executionEntities.isEmpty())
            return null;
        if (!executionEntities.stream().anyMatch(e -> e.getStartedOn() != null))
            return null;
        
        Comparator<ProcessExecutionEntity> comparatorByStarted = Comparator.comparing(e -> e.getStartedOn());
        ProcessExecutionEntity executionEntity = executionEntities.stream()
            .filter(e -> e.getStartedOn() != null)
            .sorted(comparatorByStarted.reversed())
            .findFirst().get();
        long executionId = executionEntity.getId();
        
        List<Long> runningExecutionIds = executionEntities.stream()
            .filter(e -> e.isRunning())
            .collect(Collectors.mapping(e -> e.getId(), Collectors.toList()));
        Assert.state(runningExecutionIds.size() < 2, 
            "Expected at most 1 running execution for a given process revision");
        Assert.state(runningExecutionIds.isEmpty() || runningExecutionIds.get(0).equals(executionId),
            "Expected a running execution to be the one started most recently!");
        
        return executionEntity.toProcessExecutionRecord();
    }
    
    @Transactional(readOnly = true)
    @Override
    public List<ProcessExecutionRecord> findExecutions(long id, long version)
    {
        ProcessRecord r = findOne(id, version, true);
        return r == null? Collections.emptyList() : r.getExecutions();
    }

    @Override
    public ProcessRecord create(ProcessDefinition definition, int userId)
    {
        AccountEntity createdBy = entityManager.find(AccountEntity.class, userId);
        Assert.notNull(createdBy, "The userId does not correspond to a user entity");
        
        ZonedDateTime now = ZonedDateTime.now();

        // Create new process entity

        ProcessEntity entity = new ProcessEntity(definition);
        entity.setTaskType(EnumProcessTaskType.DATA_INTEGRATION);
        entity.setTemplate(false);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        entity.setCreatedOn(now);
        entity.setUpdatedOn(now);

        // Create an associated process revision, update reference

        ProcessRevisionEntity revisionEntity = new ProcessRevisionEntity(entity);
        entity.addRevision(revisionEntity);

        // Create an execution monitor for this revision
        
        ProcessExecutionMonitorEntity monitorEntity = new ProcessExecutionMonitorEntity(revisionEntity, now);
        revisionEntity.setMonitor(monitorEntity);
        
        // Save

        entityManager.persist(entity);
        entityManager.flush();
        return entity.toProcessRecord();
    }

    @Override
    public ProcessRecord update(long id, ProcessDefinition definition, int userId)
        throws ProcessNotFoundException
    {
        AccountEntity updatedBy = entityManager.find(AccountEntity.class, userId);
        Assert.notNull(updatedBy, "The userId does not correspond to a user entity");
        
        ProcessEntity entity = entityManager.find(ProcessEntity.class, id);
        if (entity == null)
            throw new ProcessNotFoundException(id);
        
        final ZonedDateTime now = ZonedDateTime.now();
        
        // Update process entity
        entity.setVersion(entity.getVersion() + 1);
        entity.setDescription(definition.description());
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedOn(now);
        entity.setDefinition(definition);

        // Create new process revision, update references
        ProcessRevisionEntity revisionEntity = new ProcessRevisionEntity(entity);
        entity.addRevision(revisionEntity);

        // Create an execution monitor for this revision
        ProcessExecutionMonitorEntity monitorEntity = new ProcessExecutionMonitorEntity(revisionEntity, now);
        revisionEntity.setMonitor(monitorEntity);
        
        // Save
        entityManager.flush();
        return entity.toProcessRecord();
    }

    @Override
    public ProcessExecutionRecord createExecution(long id, long version, int userId)
        throws ProcessNotFoundException, ProcessHasActiveExecutionException
    {
        final AccountEntity submittedBy = userId < 0? null : entityManager.find(AccountEntity.class, userId);
        Assert.notNull(submittedBy, "The user id does not correspond to a user entity");
        
        final ProcessRevisionEntity revisionEntity = findRevision(id, version);
        if (revisionEntity == null)
            throw new ProcessNotFoundException(id, version);
        
        final List<ProcessExecutionEntity> executionEntities = revisionEntity.getExecutions();
        
        // Check if a running execution already exists on this process
        
        if (executionEntities.stream().anyMatch(e -> !e.isTerminated()))
            throw new ProcessHasActiveExecutionException(id, version);
       
        // Create an execution entity
        
        final ZonedDateTime now = ZonedDateTime.now();
        
        ProcessExecutionEntity executionEntity = new ProcessExecutionEntity(revisionEntity);
        executionEntity.setSubmittedBy(submittedBy);
        executionEntity.setSubmittedOn(now);
        
        revisionEntity.getMonitor().setModifiedOn(now);
        
        entityManager.persist(executionEntity);
        entityManager.flush();
        return executionEntity.toProcessExecutionRecord();
    }
    
    @Override
    public ProcessExecutionRecord updateExecution(long executionId, ProcessExecutionRecord record)
        throws ProcessExecutionNotFoundException
    {
        Assert.notNull(record, "A process-execution record is required");
        return updateExecution(
            executionId, 
            record.getStatus(), 
            record.getStartedOn(), 
            record.getCompletedOn(),
            record.getErrorMessage());
    }
    
    @Override
    public ProcessExecutionRecord updateExecution(
        long executionId,
        EnumProcessExecutionStatus status, 
        ZonedDateTime started, 
        ZonedDateTime completed,
        String errorMessage) 
        throws ProcessExecutionNotFoundException
    {
        final ProcessExecutionEntity executionEntity = 
            entityManager.find(ProcessExecutionEntity.class, executionId);
        if (executionEntity == null)
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        
        final ProcessRevisionEntity revisionEntity = executionEntity.getProcess();
        final EnumProcessExecutionStatus previousStatus = executionEntity.getStatus();
        
        if (status != null && status != previousStatus) {
            Assert.isTrue(status != EnumProcessExecutionStatus.UNKNOWN, 
                "The status cannot be updated to UNKNOWN");
            switch (previousStatus) {
            case UNKNOWN:
                Assert.isTrue(status != EnumProcessExecutionStatus.COMPLETED, 
                    "A transition from a status of UNKNOWN to COMPLETED is impossible");
                break;
            case COMPLETED:
            case FAILED:
            case STOPPED:
                Assert.isTrue(false, 
                    "The execution status of [" + previousStatus + "] is a terminal status");
                break;
            default:
                break;
            }
            executionEntity.setStatus(status);
        }
        
        if (errorMessage != null) {
            Assert.isTrue(executionEntity.getStatus() == EnumProcessExecutionStatus.FAILED,
                "An error message is only expected for FAILED executions");
            executionEntity.setErrorMessage(errorMessage);
        }
        
        if (started != null && !started.equals(executionEntity.getStartedOn())) {
            Assert.isTrue(previousStatus == EnumProcessExecutionStatus.UNKNOWN &&
                    status == EnumProcessExecutionStatus.RUNNING, 
                "The `started` timestamp can only be updated for an execution moving from " +
                    "UNKNOWN to RUNNING status");
            executionEntity.setStartedOn(started);
        }
        
        if (completed != null) {
            Assert.isTrue(previousStatus == EnumProcessExecutionStatus.RUNNING && (
                    status == EnumProcessExecutionStatus.COMPLETED || 
                    status == EnumProcessExecutionStatus.FAILED), 
                "The `completed` timestamp is only expected for an execution moving from " +
                    "RUNNING to a COMPLETED/FAILED status");
            executionEntity.setCompletedOn(completed);
        }
        
        revisionEntity.getMonitor().setModifiedOn(ZonedDateTime.now());
        
        // Save
        
        entityManager.flush();
        return executionEntity.toProcessExecutionRecord(true);
    }

    @Override
    public ProcessExecutionRecord createExecutionStep(long executionId, ProcessExecutionStepRecord record)
        throws ProcessExecutionNotFoundException, ProcessExecutionNotActiveException
    {
        Assert.notNull(record, "A non-empty record is required");
        
        final ProcessExecutionEntity executionEntity = 
            entityManager.find(ProcessExecutionEntity.class, executionId);
        if (executionEntity == null)
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        if (executionEntity.isTerminated())
            throw new ProcessExecutionNotActiveException(executionId);
        
        // Set step metadata
        
        ProcessExecutionStepEntity executionStepEntity = new ProcessExecutionStepEntity(
            executionEntity, record.getKey(), record.getName(), record.getJobExecutionId());
        executionStepEntity.setStatus(record.getStatus());
        executionStepEntity.setOperation(record.getOperation());
        executionStepEntity.setTool(record.getTool());
        executionStepEntity.setStartedOn(record.getStartedOn());
        
        // Add file entities associated with this step
        
        for (ProcessExecutionStepFileRecord fileRecord: record.getFiles()) {
            Assert.state(fileRecord.getId() < 0, 
                "Did not expect an id for a record of a new file entity");
            ProcessExecutionStepFileEntity fileEntity = 
                new ProcessExecutionStepFileEntity(executionStepEntity, fileRecord);
            ResourceIdentifier resourceIdentifier = fileRecord.getResource();
            if (resourceIdentifier != null)
                fileEntity.setResource(findResourceEntity(resourceIdentifier, true));
            executionStepEntity.addFile(fileEntity);
        }
        
        executionEntity.addStep(executionStepEntity);
        
        // Save    
        entityManager.flush();
        return executionEntity.toProcessExecutionRecord(true);
    }

    @Override
    public ProcessExecutionRecord updateExecutionStep(long executionId, int stepKey, ProcessExecutionStepRecord record)
        throws ProcessExecutionNotFoundException, ProcessExecutionNotActiveException
    {
        Assert.notNull(record, "A non-empty record is required");
        
        final ProcessExecutionEntity executionEntity = 
            entityManager.find(ProcessExecutionEntity.class, executionId);
        if (executionEntity == null)
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        if (executionEntity.isTerminated())
            throw new ProcessExecutionNotActiveException(executionId);
        
        final ProcessExecutionStepEntity executionStepEntity = executionEntity.getStepByKey(stepKey);
        if (executionStepEntity == null)
            throw ProcessExecutionNotFoundException.forExecutionStep(executionId, stepKey);
        
        final List<ProcessExecutionStepFileRecord> fileRecords = record.getFiles();
        final List<Long> fids = fileRecords.stream()
            .collect(Collectors.mapping(r -> r.getId(), Collectors.toList()));
        
        // Update top-level step metadata
        
        executionStepEntity.setStatus(record.getStatus());
        executionStepEntity.setCompletedOn(record.getCompletedOn());
        executionStepEntity.setErrorMessage(record.getErrorMessage());
        
        // Examine and add/update contained file records
        // Due to the nature of a processing step, a file record can never be removed; it can
        // only be added or updated (on specific updatable fields).
        
        // Update existing file records
        
        for (ProcessExecutionStepFileEntity fileEntity: executionStepEntity.getFiles()) {
            // Every existing file entity must correspond to a given record
            final long fid = fileEntity.getId();
            final ProcessExecutionStepFileRecord fileRecord = 
                IterableUtils.find(fileRecords, r -> r.getId() == fid);
            Assert.state(fileRecord != null && IterableUtils.frequency(fids, fid) == 1, 
                "Expected a single file record to match to a given id!");
            // Set updatable metadata from current file record
            fileEntity.setSize(fileRecord.getFileSize());
            fileEntity.setBoundingBox(fileRecord.getBoundingBox());
            fileEntity.setTableName(fileRecord.getTableName());
            ResourceIdentifier resourceIdentifier = fileRecord.getResource();
            fileEntity.setResource(
                resourceIdentifier == null? null : findResourceEntity(resourceIdentifier, true));
        }
        
        // Add file records (if any)
        // A file record is considered as a new one if carrying an invalid (negative) id
        
        for (ProcessExecutionStepFileRecord fileRecord: 
                IterableUtils.filteredIterable(fileRecords, f -> f.getId() < 0)) 
        {
            ProcessExecutionStepFileEntity fileEntity = 
                new ProcessExecutionStepFileEntity(executionStepEntity, fileRecord);
            ResourceIdentifier resourceIdentifier = fileRecord.getResource();
            if (resourceIdentifier != null)
                fileEntity.setResource(findResourceEntity(resourceIdentifier, true));
            executionStepEntity.addFile(fileEntity);
        }
        
        // Save
        entityManager.flush();
        return executionEntity.toProcessExecutionRecord(true);
    }
  
    @Override
    public ProcessExecutionRecord updateExecutionStepAddingFile(long executionId, int stepKey, ProcessExecutionStepFileRecord fileRecord)
        throws ProcessExecutionNotFoundException, ProcessExecutionNotActiveException
    {
        Assert.notNull(fileRecord, "A non-empty record is required");
        Assert.state(fileRecord.getId() < 0, 
            "Did not expect an id for a record of a new file entity");
        
        final ProcessExecutionEntity executionEntity = 
            entityManager.find(ProcessExecutionEntity.class, executionId);
        if (executionEntity == null)
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        if (executionEntity.isTerminated())
            throw new ProcessExecutionNotActiveException(executionId);
        
        final ProcessExecutionStepEntity executionStepEntity = executionEntity.getStepByKey(stepKey);
        if (executionStepEntity == null)
            throw ProcessExecutionNotFoundException.forExecutionStep(executionId, stepKey);
        
        // Add file record
        
        ProcessExecutionStepFileEntity fileEntity = new ProcessExecutionStepFileEntity(
            executionStepEntity, 
            fileRecord.getType(), fileRecord.getFilePath(), fileRecord.getFileSize());
        ResourceIdentifier resourceIdentifier = fileRecord.getResource();
        if (resourceIdentifier != null)
            fileEntity.setResource(findResourceEntity(resourceIdentifier, true));
        executionStepEntity.addFile(fileEntity);
        
        // Save
        entityManager.flush();
        return executionEntity.toProcessExecutionRecord(true);
    }
    
    @Override
    public boolean discardExecution(long executionId, boolean forceIfNotEmpty)
        throws ProcessExecutionNotFoundException
    {
        final ProcessExecutionEntity executionEntity = 
            entityManager.find(ProcessExecutionEntity.class, executionId);
        if (executionEntity == null)
            throw ProcessExecutionNotFoundException.forExecution(executionId);
        
        if (executionEntity.getSteps().isEmpty()) {
            entityManager.remove(executionEntity);
            return true;
        } else if (forceIfNotEmpty) {
            // First remove each one of children steps (will cascade to children files)
            logger.info("Removing execution entity #{} along with {} processing step(s)",
                executionId, executionEntity.getSteps().size());
            for (ProcessExecutionStepEntity stepEntity: executionEntity.getSteps()) {
                entityManager.remove(stepEntity);
            }
            entityManager.remove(executionEntity);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean discardExecution(long executionId) throws ProcessExecutionNotFoundException
    {
        return ProcessRepository.super.discardExecution(executionId);
    }
        
    private void setFindParameters(ProcessQuery processQuery, Query query)
    {
        if (processQuery.getCreatedBy() != null)
            query.setParameter("ownerId", processQuery.getCreatedBy());

        if (!StringUtils.isBlank(processQuery.getName()))
            query.setParameter("name", "%" + processQuery.getName() + "%");
        
        if (processQuery.getTaskType() != null)
            query.setParameter("taskType", processQuery.getTaskType());
        
        if (processQuery.getTemplate() != null)
            query.setParameter("template", processQuery.getTemplate());
    }

    private void setFindParameters(ProcessExecutionQuery executionQuery, Query query)
    {
        if (executionQuery.getCreatedBy() != null)
            query.setParameter("ownerId", executionQuery.getCreatedBy());

        if (executionQuery.getId() != null)
            query.setParameter("id", executionQuery.getId());
        
        if (executionQuery.getVersion() != null)
            query.setParameter("version", executionQuery.getVersion());
        
        if (!StringUtils.isBlank(executionQuery.getName()))
            query.setParameter("name", "%" + executionQuery.getName() + "%");
       
        if (executionQuery.getTaskType() != null)
            query.setParameter("taskType", executionQuery.getTaskType());
        
        if (executionQuery.getStatus() != null)
            query.setParameter("status", executionQuery.getStatus());
    }
    
    /**
     * Find the latest {@link ProcessRevisionEntity} associated with a given process id.
     *
     * @param id The process id
     * @return
     */
    private ProcessRevisionEntity findLatestRevision(long id)
    {
        String queryString =
            "from ProcessRevision p where p.parent.id = :id order by p.version desc";

        List<ProcessRevisionEntity> result = entityManager
            .createQuery(queryString, ProcessRevisionEntity.class)
            .setParameter("id", id)
            .setMaxResults(1)
            .getResultList();

        return result.isEmpty()? null : result.get(0);
    }

    /**
     * Find the single {@link ProcessRevisionEntity} associated with a given pair of 
     * id and version.
     * 
     * @param id The process id
     * @param version The version
     * @return a revision entity or <tt>null</tt> if no matching entity exists
     */
    private ProcessRevisionEntity findRevision(long id, long version)
    {
        String queryString =
            "FROM ProcessRevision p WHERE p.parent.id = :id AND p.version = :version";

        TypedQuery<ProcessRevisionEntity> query = entityManager
            .createQuery(queryString, ProcessRevisionEntity.class)
            .setParameter("id", id)
            .setParameter("version", version);

        ProcessRevisionEntity r = null;
        try {
            r = query.getSingleResult();
        } catch (NoResultException ex) {
            r = null;
        }   
        return r;
    }
    
    private ResourceRevisionEntity findResourceEntity(long resourceId, long resourceVersion)
    {
        ResourceRecord resourceRecord = resourceRepository.findOne(resourceId, resourceVersion);
        return resourceRecord == null?
            null : entityManager.find(ResourceRevisionEntity.class, resourceRecord.getId());
    }
    
    private ResourceRevisionEntity findResourceEntity(
        ResourceIdentifier resourceIdentifier, boolean failIfNotExists)
    {
        ResourceRevisionEntity e = 
            findResourceEntity(resourceIdentifier.getId(), resourceIdentifier.getVersion());
        if (e == null && failIfNotExists) {
            throw new NoSuchElementException(
                "The resource identifier does not correspond to a resource entity");
        }
        return e;
    }
}
