package eu.slipo.workbench.common.domain;

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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.NaturalId;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Geometry;

import eu.slipo.workbench.common.model.poi.EnumDataFormat;
import eu.slipo.workbench.common.model.poi.EnumTool;
import eu.slipo.workbench.common.model.process.EnumStepFile;
import eu.slipo.workbench.common.model.process.ProcessExecutionStepFileRecord;
import eu.slipo.workbench.common.model.tool.output.OutputPart;


@Entity(name = "ProcessExecutionStepFile")
@Table(
    schema = "public", name = "process_execution_step_file",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_process_execution_step_file_step_and_path",
            columnNames = { "process_execution_step", "file_path" }),
    })
public class ProcessExecutionStepFileEntity {

    @Id
    @Column(name = "id", updatable = false)
    @SequenceGenerator(
        sequenceName = "process_execution_step_file_id_seq", name = "process_execution_step_file_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "process_execution_step_file_id_seq", strategy = GenerationType.SEQUENCE)
    long id = -1L;

    @NotNull
    @NaturalId
    @ManyToOne
    @JoinColumn(name = "process_execution_step", nullable = false, updatable = false)
    ProcessExecutionStepEntity step;

    /**
     * A link to a registered resource (if any)
     */
    @ManyToOne
    @JoinColumn(name = "resource", nullable = true)
    ResourceRevisionEntity resource;

    /**
     * The role of this file (input, output, configuration, KPIs etc.) inside the
     * processing step.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "`type`", nullable = false, updatable = false)
    EnumStepFile type;

    /**
     * The file path relative to root directory for workflow data.
     */
    @NotNull
    @NaturalId
    @Column(name = "file_path", nullable = false, updatable = false)
    String path;

    /**
     * The file size measured in bytes.
     */
    @Min(0)
    @Column(name = "file_size")
    Long size;

    /**
     * The data format of a input/output file (irrelevant for configuration, QA data ...).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_format", updatable = false)
    EnumDataFormat dataFormat;

    /**
     * A bounding-box that contains geometries of this file resource (relevant only to
     * output file resources).
     */
    @Column(name = "bbox")
    Geometry boundingBox;

    /**
     * The name of the PostGis table that contains vector data of this file resource (relevant
     * only to output file resources.
     */
    @Column(name = "table_name", columnDefinition = "uuid")
    UUID tableName;

    /**
     * A flag that indicates that a step file is verified to exist.
     *
     * This flag is only relevant to output files (as input files must always exist for a step
     * to be created).
     */
    @Column(name = "verified")
    boolean verified;

    /**
     * A key identifying an output part. This is, of course, only relevant to output files.
     *
     * <p>A part key is expected to match an {@link OutputPart} object from the enumeration of
     * available parts provided by each tool (so it is tool-specific key).
     *
     * @see EnumTool#getOutputPartEnumeration()
     */
    @Column(name = "output_part", updatable = false)
    String outputPartKey;

    @Column(name = "layer_style", updatable = true, nullable = true)
    JsonNode style;

    protected ProcessExecutionStepFileEntity() {}

    public ProcessExecutionStepFileEntity(
        ProcessExecutionStepEntity stepExecutionEntity, ProcessExecutionStepFileRecord record)
    {
        this.step = stepExecutionEntity;
        this.type = record.getType();
        this.path = record.getFilePath();
        this.size = record.getFileSize();
        this.dataFormat = record.getDataFormat();
        this.boundingBox = record.getBoundingBox();
        this.tableName = record.getTableName();
        this.outputPartKey = record.getOutputPartKey();
        this.verified = false;
    }

    public ProcessExecutionStepFileEntity(
        ProcessExecutionStepEntity stepExecutionEntity,
        EnumStepFile type, String filePath, Long fileSize, EnumDataFormat dataFormat)
    {
        this.step = stepExecutionEntity;
        this.type = type;
        this.path = filePath;
        this.size = fileSize;
        this.dataFormat = dataFormat;
        this.verified = false;
    }

    public ProcessExecutionStepFileEntity(
        ProcessExecutionStepEntity stepExecutionEntity, EnumStepFile type, String filePath)
    {
        this(stepExecutionEntity, type, filePath, null, null);
    }

    public ProcessExecutionStepFileEntity(
        ProcessExecutionStepEntity stepExecutionEntity, EnumStepFile type, String filePath, Long fileSize)
    {
        this(stepExecutionEntity, type, filePath, fileSize, null);
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    public ProcessExecutionStepEntity getStep()
    {
        return step;
    }

    public void setStep(ProcessExecutionStepEntity step)
    {
        this.step = step;
    }

    public ResourceRevisionEntity getResource()
    {
        return resource;
    }

    public void setResource(ResourceRevisionEntity resource)
    {
        this.resource = resource;
    }

    public EnumStepFile getType()
    {
        return type;
    }

    public void setType(EnumStepFile type)
    {
        this.type = type;
    }

    public EnumDataFormat getDataFormat()
    {
        return dataFormat;
    }

    public void setDataFormat(EnumDataFormat dataFormat)
    {
        this.dataFormat = dataFormat;
    }

    public void setBoundingBox(Geometry boundingBox)
    {
        this.boundingBox = boundingBox;
    }

    public Geometry getBoundingBox()
    {
        return boundingBox;
    }

    public void setTableName(UUID tableName)
    {
        this.tableName = tableName;
    }

    public UUID getTableName()
    {
        return tableName;
    }

    public void setVerified(boolean verified)
    {
        this.verified = verified;
    }

    public boolean isVerified()
    {
        return verified;
    }

    public void setOutputPartKey(String outputPartKey)
    {
        this.outputPartKey = outputPartKey;
    }

    public String getOutputPartKey()
    {
        return outputPartKey;
    }

    public JsonNode getStyle()
    {
        return style;
    }

    public void setStyle(JsonNode style)
    {
        this.style = style;
    }

    public ProcessExecutionStepFileRecord toProcessExecutionStepFileRecord()
    {
        ProcessExecutionStepFileRecord fileRecord =
            new ProcessExecutionStepFileRecord(type, path, size, dataFormat);
        fileRecord.setId(id);

        fileRecord.setOutputPartKey(outputPartKey);

        if (resource != null) {
            fileRecord.setResource(resource.parent.id, resource.version);
        }

        fileRecord.setBoundingBox(boundingBox);
        fileRecord.setTableName(tableName);
        fileRecord.setStyle(style);

        return fileRecord;
    }
}
