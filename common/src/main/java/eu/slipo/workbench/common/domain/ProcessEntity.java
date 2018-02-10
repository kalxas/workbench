package eu.slipo.workbench.common.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import eu.slipo.workbench.common.domain.attributeconverter.ProcessConfigurationConverter;
import eu.slipo.workbench.common.model.process.EnumProcessTaskType;
import eu.slipo.workbench.common.model.process.ProcessDefinition;
import eu.slipo.workbench.common.model.process.ProcessRecord;

@Entity(name = "Process")
@Table(schema = "public", name = "process")
public class ProcessEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
        sequenceName = "process_id_seq", name = "process_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(
        generator = "process_id_seq", strategy = GenerationType.SEQUENCE)
    long id;

    @Column(name = "`version`")
    long version;

    @Version()
    @Column(name = "row_version")
    long rowVersion;

    @NotNull
    @Column(name = "`name`")
    String name;

    @NotNull
    @Column(name = "description")
    String description;

    @NotNull
    @Column(name = "created_on")
    ZonedDateTime createdOn = ZonedDateTime.now();

    @NotNull
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    AccountEntity createdBy;

    @NotNull
    @Column(name = "updated_on")
    ZonedDateTime updatedOn;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = false)
    AccountEntity updatedBy;

    @Column(name = "executed_on")
    ZonedDateTime executedOn;

    @NotNull
    @Column(name = "definition")
    @Convert(converter = ProcessConfigurationConverter.class)
    ProcessDefinition definition;

    @NotNull
    @Column(name = "is_template")
    boolean isTemplate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type")
    private EnumProcessTaskType taskType;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProcessRevisionEntity> versions = new ArrayList<>();

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public ZonedDateTime getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(ZonedDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }

    public AccountEntity getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(AccountEntity updatedBy) {
        this.updatedBy = updatedBy;
    }

    public ZonedDateTime getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(ZonedDateTime executedOn) {
        this.executedOn = executedOn;
    }

    public ProcessDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ProcessDefinition definition) {
        this.definition = definition;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        this.isTemplate = template;
    }

    public EnumProcessTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(EnumProcessTaskType t) {
        this.taskType = t;
    }

    public long getId() {
        return id;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public void setCreatedBy(AccountEntity createdBy) {
        this.createdBy = createdBy;
    }

    public AccountEntity getCreatedBy() {
        return createdBy;
    }

    public List<ProcessRevisionEntity> getVersions() {
        return versions;
    }

    public ProcessRecord toProcessRecord()
    {
        return toProcessRecord(false, false);
    }
    
    public ProcessRecord toProcessRecord(boolean includeExecutions, boolean includeSteps) {
        ProcessRecord p = new ProcessRecord(this.id, this.version);

        p.setCreatedOn(this.createdOn);
        p.setCreatedBy(this.createdBy.getId(), this.createdBy.getFullName());
        p.setUpdatedOn(this.updatedOn);
        p.setUpdatedBy(this.updatedBy.getId(), this.updatedBy.getFullName());
        p.setDescription(this.description);
        p.setName(this.name);
        p.setExecutedOn(this.executedOn);
        p.setTaskType(this.taskType);
        p.setConfiguration(this.definition);
        p.setTemplate(this.isTemplate);

        for (ProcessRevisionEntity h : this.getVersions()) {
            p.addVersion(h.toProcessRecord(includeExecutions, includeSteps));
        }

        return p;
    }

}