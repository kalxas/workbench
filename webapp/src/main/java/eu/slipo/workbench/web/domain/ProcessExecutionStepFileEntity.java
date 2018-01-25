package eu.slipo.workbench.web.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import eu.slipo.workbench.web.model.process.EnumStepFile;
import eu.slipo.workbench.web.model.process.ProcessExecutionStepFileRecord;

@Entity(name = "ProcessExecutionStepFile")
@Table(schema = "public", name = "process_execution_step_file")
public class ProcessExecutionStepFileEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(sequenceName = "process_execution_step_file_id_seq", name = "process_execution_step_file_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "process_execution_step_file_id_seq", strategy = GenerationType.SEQUENCE)
    long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "process_execution_step_id", nullable = false)
    ProcessExecutionStepEntity step;

    @ManyToOne()
    @JoinColumn(name = "resource_id", nullable = true)
    ResourceRevisionEntity resource;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "`type`")
    EnumStepFile type;

    @Column(name = "file_name")
    String fileName;

    @Column(name = "file_size")
    int fileSize;

    @Column(name = "table_name")
    UUID tableName;

    public ProcessExecutionStepEntity getStep() {
        return step;
    }

    public void setStep(ProcessExecutionStepEntity step) {
        this.step = step;
    }

    public ResourceRevisionEntity getResource() {
        return resource;
    }

    public void setResource(ResourceRevisionEntity resource) {
        this.resource = resource;
    }

    public EnumStepFile getType() {
        return type;
    }

    public void setType(EnumStepFile type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public UUID getTableName() {
        return tableName;
    }

    public void setTableName(UUID tableName) {
        this.tableName = tableName;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ProcessExecutionStepFileRecord toProcessExecutionStepFileRecord() {
        ProcessExecutionStepFileRecord f = new ProcessExecutionStepFileRecord();

        f.setId(this.id);
        f.setType(this.type);
        f.setFileName(this.fileName);
        f.setFileSize(this.fileSize);
        if (this.resource != null) {
            f.setResource(this.resource.parent.id, this.resource.version);
        }
        f.setTableName(this.tableName);

        return f;
    }

}
